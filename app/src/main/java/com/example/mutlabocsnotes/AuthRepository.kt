package com.example.mutlabocsnotes

import com.example.mutlabocsnotes.network.AuthApi
import com.example.mutlabocsnotes.network.AuthCredentialsDto
import com.example.mutlabocsnotes.network.AuthResponseDto
import com.example.mutlabocsnotes.network.RefreshTokenRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Data model shared between layers of this module.
data class AuthorizedSession(
    val email: String
)

// Encapsulates data access and business-oriented operations.
class AuthRepository(
    context: android.content.Context,
    baseUrl: String = ApiConfig.BASE_URL,
    private val sessionManager: SessionManager = SessionManager(context),
    private val api: AuthApi = createAuthApi(baseUrl),
) {

    // Authenticates the user and updates local auth state.
    suspend fun login(email: String, password: String): Result<AuthorizedSession> {
        return authenticate {
            api.login(AuthCredentialsDto(email.trim(), password))
        }
    }

    // Registers a user and updates local auth state.
    suspend fun register(email: String, password: String): Result<AuthorizedSession> {
        return authenticate {
            api.register(AuthCredentialsDto(email.trim(), password))
        }
    }

    // Restores persisted session and user information.
    suspend fun restoreSession(): Result<AuthorizedSession> = withContext(Dispatchers.IO) {
        val accessToken = sessionManager.getAccessToken()
            ?: return@withContext Result.failure(IllegalStateException("No saved access token"))
        val refreshToken = sessionManager.getRefreshToken()
            ?: return@withContext Result.failure(IllegalStateException("No saved refresh token"))

        return@withContext runCatching {
            try {
                val me = api.me("Bearer $accessToken")
                sessionManager.saveSession(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    email = me.email
                )
                AuthorizedSession(email = me.email)
            } catch (e: HttpException) {
                if (e.code() != 401) throw e

                val refreshed = api.refresh(RefreshTokenRequestDto(refreshToken))
                val newAccessToken = refreshed.accessToken
                val newRefreshToken = refreshed.refreshToken
                val me = api.me("Bearer $newAccessToken")

                sessionManager.saveSession(
                    accessToken = newAccessToken,
                    refreshToken = newRefreshToken,
                    email = me.email
                )

                AuthorizedSession(email = me.email)
            }
        }.onFailure {
            sessionManager.clear()
        }
    }

    // Ends the current session and clears auth data.
    fun logout() {
        sessionManager.clear()
    }

    // Attempts token refresh when the backend returns unauthorized.
    private suspend fun authenticate(
        block: suspend () -> AuthResponseDto
    ): Result<AuthorizedSession> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val response = block()
            val token = response.accessToken
            val refreshToken = response.refreshToken
            val me = api.me("Bearer $token")
            val email = me.email.ifBlank {
                response.resolvedEmail().orEmpty()
            }

            sessionManager.saveSession(
                accessToken = token,
                refreshToken = refreshToken,
                email = email
            )

            AuthorizedSession(email = email)
        }
    }

    private companion object {
        // Creates and returns a configured instance.
        private fun createAuthApi(baseUrl: String): AuthApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AuthApi::class.java)
        }
    }
}
