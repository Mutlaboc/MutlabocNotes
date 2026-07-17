package app.homenotes.android

import app.homenotes.android.network.AuthApi
import app.homenotes.android.network.AuthCredentialsDto
import app.homenotes.android.network.AuthResponseDto
import app.homenotes.android.network.GoogleSocialLoginRequestDto
import app.homenotes.android.network.LogoutRequestDto
import app.homenotes.android.network.RefreshTokenRequestDto
import app.homenotes.android.network.YandexSocialLoginRequestDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.io.IOException

data class AuthorizedSession(
    val email: String
)

private class SessionChangedException : IllegalStateException("Saved session changed")

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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val onSessionAvailable: (String) -> Unit = {},
    private val onSessionCleared: (String?) -> Unit = {},
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
        val savedSession = sessionManager.getSessionSnapshot()
            ?: return@withContext Result.failure(IllegalStateException("No saved session"))

        val restored = runCatching {
            try {
                val me = api.me("Bearer ${savedSession.accessToken}")
                saveRestoredSessionOrThrow(
                    expectedRefreshToken = savedSession.refreshToken,
                    accessToken = savedSession.accessToken,
                    refreshToken = savedSession.refreshToken,
                    email = me.email
                )
                AuthorizedSession(email = me.email)
            } catch (e: HttpException) {
                if (e.code() != 401) throw e

                val refreshed = api.refresh(RefreshTokenRequestDto(savedSession.refreshToken))
                val newAccessToken = refreshed.accessToken
                val newRefreshToken = refreshed.refreshToken
                val me = api.me("Bearer $newAccessToken")

                saveRestoredSessionOrThrow(
                    expectedRefreshToken = savedSession.refreshToken,
                    accessToken = newAccessToken,
                    refreshToken = newRefreshToken,
                    email = me.email
                )

                AuthorizedSession(email = me.email)
            }
        }
        val error = restored.exceptionOrNull()
        if (error != null && error.isTransientRestoreFailure()) {
            val cachedEmail = savedSession.email?.trim().orEmpty()
            if (cachedEmail.isNotEmpty()) {
                onSessionAvailable(cachedEmail)
                return@withContext Result.success(AuthorizedSession(cachedEmail))
            }
        }
        restored.onSuccess { onSessionAvailable(it.email) }.onFailure {
            if (it !is SessionChangedException) {
                sessionManager.clearSessionIfRefreshTokenMatches(savedSession.refreshToken)
            }
        }
        return@withContext restored
    }

    override suspend fun logout() = withContext(ioDispatcher) {
        val email = sessionManager.getEmail()
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
            onSessionCleared(email)
        }
    }

    override fun clearLocalSession() {
        val email = sessionManager.getEmail()
        sessionManager.clear()
        onSessionCleared(email)
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
            AuthorizedSession(email = email).also { onSessionAvailable(email) }
        }
    }

    private fun saveRestoredSessionOrThrow(
        expectedRefreshToken: String,
        accessToken: String,
        refreshToken: String,
        email: String
    ) {
        val saved = sessionManager.saveSessionIfRefreshTokenMatches(
            expectedRefreshToken = expectedRefreshToken,
            accessToken = accessToken,
            refreshToken = refreshToken,
            email = email
        )
        if (!saved) {
            throw SessionChangedException()
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

private fun Throwable.isTransientRestoreFailure(): Boolean =
    this is IOException || (this as? HttpException)?.code()?.let { it >= 500 } == true
