package com.example.homenotes

import com.example.homenotes.network.AuthResponseDto
import com.example.homenotes.network.RefreshTokenRequestDto
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

    private val gson = Gson()

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

            val refreshResponse = refreshTokens(currentSession.refreshToken)
                ?: run {
                    clearSessionAndNotify(currentSession.refreshToken)
                    return null
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
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
