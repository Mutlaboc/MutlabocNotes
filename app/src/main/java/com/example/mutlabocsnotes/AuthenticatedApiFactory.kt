package com.example.mutlabocsnotes

import android.content.Context
import com.example.mutlabocsnotes.network.AuthResponseDto
import com.example.mutlabocsnotes.network.RefreshTokenRequestDto
import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// HTTP-интерсептор, дополняющий исходящие запросы.
class AuthorizationInterceptor(
    context: Context
) : Interceptor {

    private val sessionManager = SessionManager(context.applicationContext)

    // Добавляет данные авторизации перед отправкой запроса.
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionManager.getAccessToken()

        val request = chain.request()
            .newBuilder()
            .apply {
                if (!token.isNullOrBlank()) {
                    header("Authorization", "Bearer $token")
                }
            }
            .build()

        return chain.proceed(request)
    }
}

// HTTP-аутентификатор, обновляющий истекшие учётные данные.
class RefreshTokenAuthenticator(
    context: Context
) : Authenticator {

    private val appContext = context.applicationContext
    private val sessionManager = SessionManager(appContext)
    private val gson = Gson()

    // Пытается обновить токен, когда backend возвращает unauthorized.
    override fun authenticate(route: Route?, response: Response): Request? {
        // Останавливается после ограниченного числа попыток, чтобы избежать циклов повторов.
        if (responseCount(response) >= 2) {
            clearSessionAndNotify()
            return null
        }

        val storedRefreshToken = sessionManager.getRefreshToken()
            ?: run {
                clearSessionAndNotify()
                return null
            }

        synchronized(this) {
            val currentAccessToken = sessionManager.getAccessToken()
            val requestAccessToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
                ?.trim()

            // Если другой запрос уже обновил токен, сразу используем его.
            if (!currentAccessToken.isNullOrBlank() && currentAccessToken != requestAccessToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            // Иначе обновляем токены, сохраняем их и повторяем исходный запрос.
            val refreshResponse = refreshTokens(storedRefreshToken)
                ?: run {
                    clearSessionAndNotify()
                    return null
                }

            val currentEmail = sessionManager.getEmail()
            sessionManager.saveSession(
                accessToken = refreshResponse.accessToken,
                refreshToken = refreshResponse.refreshToken,
                email = currentEmail
            )

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${refreshResponse.accessToken}")
                .build()
        }
    }

    // Вызывает endpoint обновления и разбирает новую пару токенов.
    private fun refreshTokens(refreshToken: String): AuthResponseDto? {
        val requestBody = gson.toJson(
            RefreshTokenRequestDto(refreshToken = refreshToken)
        ).toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${ApiConfig.BASE_URL}auth/refresh")
            .post(requestBody)
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return null
                }

                val body = response.body?.string() ?: return null
                gson.fromJson(body, AuthResponseDto::class.java)
            }
        }.getOrNull()
    }

    // Очищает временные и сохранённые данные состояния.
    private fun clearSessionAndNotify() {
        sessionManager.clear()
        SessionEventBus.emit(SessionEvent.SessionExpired)
    }

    // Подсчитывает предыдущие ответы, связанные через OkHttp, чтобы контролировать глубину повторов.
    private fun responseCount(response: Response): Int {
        var currentResponse: Response? = response
        var count = 1

        while (currentResponse?.priorResponse != null) {
            count++
            currentResponse = currentResponse.priorResponse
        }

        return count
    }
}

// Создаёт настроенные клиенты и зависимости для сетевого слоя.
object AuthenticatedApiFactory {

    // Создаёт и возвращает настроенный экземпляр.
    fun createOkHttpClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(context.applicationContext))
            .authenticator(RefreshTokenAuthenticator(context.applicationContext))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // Создаёт и возвращает настроенный экземпляр.
    fun createRetrofit(
        context: Context,
        baseUrl: String = ApiConfig.BASE_URL
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
