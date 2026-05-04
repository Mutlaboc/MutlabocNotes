package com.example.mutlabocsnotes

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

// HTTP-интерсептор добавляет access token из общего AuthSessionStore в защищённые запросы.
class AuthorizationInterceptor(
    private val sessionManager: AuthSessionStore
) : Interceptor {

    // Если токена нет, запрос уходит без заголовка Authorization.
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

// HTTP-аутентификатор обновляет истёкший access token через то же хранилище, что и AuthRepository.
class RefreshTokenAuthenticator(
    private val sessionManager: AuthSessionStore,
    private val baseUrl: String = ApiConfig.BASE_URL
) : Authenticator {

    private val gson = Gson()

    // Вызывается OkHttp, когда backend возвращает unauthorized.
    override fun authenticate(route: Route?, response: Response): Request? {
        // Останавливаемся после ограниченного числа попыток, чтобы избежать циклов повтора.
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

            // Если другой запрос уже обновил токен, используем свежий токен без повторного refresh.
            if (!currentAccessToken.isNullOrBlank() && currentAccessToken != requestAccessToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            // Иначе обновляем пару токенов, сохраняем её и повторяем исходный запрос.
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
            .url("${baseUrl}auth/refresh")
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

    // Очищает сохранённую сессию и сообщает UI, что нужно вернуться к авторизации.
    private fun clearSessionAndNotify() {
        sessionManager.clear()
        SessionEventBus.emit(SessionEvent.SessionExpired)
    }

    // Считает цепочку повторов OkHttp, чтобы контролировать глубину retry.
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

// Создаёт настроенные клиенты и зависимости для защищённого сетевого слоя.
object AuthenticatedApiFactory {

    // OkHttpClient получает общий AuthSessionStore, чтобы не создавать новое хранилище токенов.
    fun createOkHttpClient(
        sessionManager: AuthSessionStore,
        baseUrl: String = ApiConfig.BASE_URL
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(sessionManager))
            .authenticator(RefreshTokenAuthenticator(sessionManager, baseUrl))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // Retrofit для API, которым нужна авторизация через текущую сессию.
    fun createRetrofit(
        sessionManager: AuthSessionStore,
        baseUrl: String = ApiConfig.BASE_URL
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient(sessionManager, baseUrl))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
