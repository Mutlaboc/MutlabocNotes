package app.homenotes.android

import app.homenotes.android.network.ApiJson
import app.homenotes.android.network.AuthResponseDto
import app.homenotes.android.network.RefreshTokenRequestDto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class AuthorizationInterceptor(
    private val sessionManager: AuthSessionStore
) : Interceptor {

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

class RefreshTokenAuthenticator(
    private val sessionManager: AuthSessionStore,
    private val baseUrl: String = ApiConfig.BASE_URL
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val requestAccessToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        if (responseCount(response) >= 2) {
            clearSessionAndNotify(sessionManager.getSessionSnapshot()?.refreshToken)
            return null
        }

        synchronized(this) {
            val currentSession = sessionManager.getSessionSnapshot()
                ?: run {
                    clearSessionAndNotify(expectedRefreshToken = null)
                    return null
                }

            if (currentSession.accessToken != requestAccessToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${currentSession.accessToken}")
                    .build()
            }

            val refreshResponse = when (val refresh = refreshTokens(currentSession.refreshToken)) {
                is RefreshResult.Success -> refresh.response
                RefreshResult.TemporaryFailure -> return null
                RefreshResult.Rejected -> {
                    clearSessionAndNotify(currentSession.refreshToken)
                    return null
                }
            }

            val saved = sessionManager.saveSessionIfRefreshTokenMatches(
                expectedRefreshToken = currentSession.refreshToken,
                accessToken = refreshResponse.accessToken,
                refreshToken = refreshResponse.refreshToken,
                email = currentSession.email
            )
            if (!saved) {
                return null
            }

            return response.request.newBuilder()
                .header("Authorization", "Bearer ${refreshResponse.accessToken}")
                .build()
        }
    }

    private fun refreshTokens(refreshToken: String): RefreshResult {
        val requestBody = ApiJson.encodeToString(
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

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return if (response.code >= 500) RefreshResult.TemporaryFailure else RefreshResult.Rejected
                }

                val body = response.body?.string() ?: return RefreshResult.TemporaryFailure
                RefreshResult.Success(ApiJson.decodeFromString<AuthResponseDto>(body))
            }
        } catch (_: java.io.IOException) {
            RefreshResult.TemporaryFailure
        }
    }

    private sealed interface RefreshResult {
        data class Success(val response: AuthResponseDto) : RefreshResult
        data object Rejected : RefreshResult
        data object TemporaryFailure : RefreshResult
    }

    private fun clearSessionAndNotify(expectedRefreshToken: String?) {
        val shouldNotify = expectedRefreshToken == null ||
            sessionManager.clearSessionIfRefreshTokenMatches(expectedRefreshToken)
        if (shouldNotify) {
            SessionEventBus.emit(SessionEvent.SessionExpired)
        }
    }

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

object AuthenticatedApiFactory {

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

    fun createRetrofit(
        sessionManager: AuthSessionStore,
        baseUrl: String = ApiConfig.BASE_URL
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient(sessionManager, baseUrl))
            .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}
