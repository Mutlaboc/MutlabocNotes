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

// HTTP interceptor that enriches outgoing requests.
class AuthorizationInterceptor(
    context: Context
) : Interceptor {

    private val sessionManager = SessionManager(context.applicationContext)

    // Adds authorization data before sending the request.
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

// HTTP authenticator that refreshes expired credentials.
class RefreshTokenAuthenticator(
    context: Context
) : Authenticator {

    private val appContext = context.applicationContext
    private val sessionManager = SessionManager(appContext)
    private val gson = Gson()

    // Attempts token refresh when the backend returns unauthorized.
    override fun authenticate(route: Route?, response: Response): Request? {
        // Stop after a limited number of attempts to avoid retry loops.
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

            // If another request already refreshed the token, reuse it immediately.
            if (!currentAccessToken.isNullOrBlank() && currentAccessToken != requestAccessToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            // Otherwise refresh tokens and persist them before retrying the original request.
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

    // Calls refresh endpoint and parses a fresh token pair.
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

    // Clears temporary or persisted state values.
    private fun clearSessionAndNotify() {
        sessionManager.clear()
        SessionEventBus.emit(SessionEvent.SessionExpired)
    }

    // Counts prior responses chained by OkHttp to control retry depth.
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

// Creates configured clients and dependencies for networking.
object AuthenticatedApiFactory {

    // Creates and returns a configured instance.
    fun createOkHttpClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(AuthorizationInterceptor(context.applicationContext))
            .authenticator(RefreshTokenAuthenticator(context.applicationContext))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // Creates and returns a configured instance.
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
