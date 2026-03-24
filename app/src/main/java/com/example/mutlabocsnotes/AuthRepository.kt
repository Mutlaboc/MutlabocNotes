package com.example.mutlabocsnotes

import com.example.mutlabocsnotes.network.AuthApi
import com.example.mutlabocsnotes.network.AuthCredentialsDto
import com.example.mutlabocsnotes.network.AuthResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

data class AuthorizedSession(
    val email: String
)

class AuthRepository(
    context: android.content.Context,
    baseUrl: String = ApiConfig.BASE_URL,
    private val sessionManager: SessionManager = SessionManager(context),
    private val api: AuthApi = createAuthApi(baseUrl),
) {

    suspend fun login(email: String, password: String): Result<AuthorizedSession> {
        return authenticate {
            api.login(AuthCredentialsDto(email.trim(), password))
        }
    }

    suspend fun register(email: String, password: String): Result<AuthorizedSession> {
        return authenticate {
            api.register(AuthCredentialsDto(email.trim(), password))
        }
    }

    suspend fun restoreSession(): Result<AuthorizedSession> = withContext(Dispatchers.IO) {
        val token = sessionManager.getAccessToken()
            ?: return@withContext Result.failure(IllegalStateException("No saved access token"))

        return@withContext runCatching {
            val me = api.me("Bearer $token")
            val email = me.email

            sessionManager.saveSession(
                accessToken = token,
                email = email
            )

            AuthorizedSession(email = email)
        }.onFailure {
            sessionManager.clear()
        }
    }

    fun logout() {
        sessionManager.clear()
    }

    private suspend fun authenticate(
        block: suspend () -> AuthResponseDto
    ): Result<AuthorizedSession> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            val response = block()
            val token = response.accessToken
            val me = api.me("Bearer $token")
            val email = me.email

            sessionManager.saveSession(
                accessToken = token,
                email = email
            )

            AuthorizedSession(email = email)
        }
    }

    private companion object {
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