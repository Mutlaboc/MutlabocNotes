package com.example.mutlabocsnotes

import com.example.mutlabocsnotes.network.AuthApi
import com.example.mutlabocsnotes.network.AuthCredentialsDto
import com.example.mutlabocsnotes.network.AuthResponseDto
import com.example.mutlabocsnotes.network.GoogleSocialLoginRequestDto
import com.example.mutlabocsnotes.network.LogoutRequestDto
import com.example.mutlabocsnotes.network.RefreshTokenRequestDto
import com.example.mutlabocsnotes.network.YandexSocialLoginRequestDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

data class AuthorizedSession(
    val email: String
)

interface AuthSessionRepository {
    suspend fun login(email: String, password: String): Result<AuthorizedSession>
    suspend fun register(email: String, password: String): Result<AuthorizedSession>
    suspend fun loginWithGoogle(idToken: String): Result<AuthorizedSession>
    suspend fun loginWithYandex(accessToken: String): Result<AuthorizedSession>
    suspend fun restoreSession(): Result<AuthorizedSession>
    suspend fun logout()
    fun clearLocalSession()
}

// Репозиторий авторизации работает с готовыми AuthSessionStore и AuthApi из AppContainer.
// Так root ViewModel не создаёт общие зависимости самостоятельно.
class AuthRepository(
    private val sessionManager: AuthSessionStore,
    private val api: AuthApi,
    // В production используется IO, а тесты подставляют управляемый dispatcher.
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AuthSessionRepository {

    override suspend fun login(email: String, password: String): Result<AuthorizedSession> {
        return authenticate {
            api.login(AuthCredentialsDto(email.trim(), password))
        }
    }

    override suspend fun register(email: String, password: String): Result<AuthorizedSession> {
        return authenticate {
            api.register(AuthCredentialsDto(email.trim(), password))
        }
    }

    override suspend fun loginWithGoogle(idToken: String): Result<AuthorizedSession> {
        return authenticate {
            api.loginWithGoogle(GoogleSocialLoginRequestDto(idToken))
        }
    }

    override suspend fun loginWithYandex(accessToken: String): Result<AuthorizedSession> {
        return authenticate {
            api.loginWithYandex(YandexSocialLoginRequestDto(accessToken))
        }
    }

    override suspend fun restoreSession(): Result<AuthorizedSession> = withContext(ioDispatcher) {
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

    override suspend fun logout() = withContext(ioDispatcher) {
        val refreshToken = sessionManager.getRefreshToken()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        try {
            if (refreshToken != null) {
                runCatching {
                    api.logout(LogoutRequestDto(refreshToken))
                }
            }
        } finally {
            sessionManager.clear()
        }
    }

    override fun clearLocalSession() {
        sessionManager.clear()
    }

    private suspend fun authenticate(
        block: suspend () -> AuthResponseDto
    ): Result<AuthorizedSession> = withContext(ioDispatcher) {
        return@withContext runCatching {
            val response = block()
            val token = response.accessToken
            val refreshToken = response.refreshToken
            val email = runCatching { api.me("Bearer $token").email }
                .getOrNull()
                ?.ifBlank { null }
                ?: response.resolvedEmail().orEmpty()

            sessionManager.saveSession(
                accessToken = token,
                refreshToken = refreshToken,
                email = email
            )

            AuthorizedSession(email = email)
        }
    }

    companion object {
        // Отдельный AuthApi нужен без авторизационного interceptor, чтобы логин и refresh не зависели от Bearer-токена.
        fun createAuthApi(baseUrl: String): AuthApi {
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
