package com.example.mutlabocsnotes

import com.example.mutlabocsnotes.network.AuthApi
import com.example.mutlabocsnotes.network.AuthCredentialsDto
import com.example.mutlabocsnotes.network.AuthResponseDto
import com.example.mutlabocsnotes.network.AuthUserDto
import com.example.mutlabocsnotes.network.GoogleSocialLoginRequestDto
import com.example.mutlabocsnotes.network.MeResponseDto
import com.example.mutlabocsnotes.network.RefreshTokenRequestDto
import com.example.mutlabocsnotes.network.YandexSocialLoginRequestDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var api: FakeAuthApi
    private lateinit var sessionStore: FakeAuthSessionStore
    private lateinit var repository: AuthRepository

    @Before
    fun setUp() {
        api = FakeAuthApi()
        sessionStore = FakeAuthSessionStore()
        repository = AuthRepository(
            sessionManager = sessionStore,
            api = api,
            ioDispatcher = mainDispatcherRule.dispatcher
        )
    }

    @Test
    fun login_savesSessionAndReturnsAuthorizedSession() = runTest(mainDispatcherRule.dispatcher) {
        // Email login должен идти через единый facade и сохранять ту же сессию, что использует protected API.
        api.loginResponse = authResponse(
            accessToken = "access",
            refreshToken = "refresh",
            email = "user@example.com"
        )
        api.meResponses.add(meResponse("user@example.com"))

        val result = repository.login(" user@example.com ", "12345678")

        assertEquals(Result.success(AuthorizedSession("user@example.com")), result)
        assertEquals(AuthCredentialsDto("user@example.com", "12345678"), api.lastLoginRequest)
        assertEquals("access", sessionStore.storedAccessToken)
        assertEquals("refresh", sessionStore.storedRefreshToken)
        assertEquals("user@example.com", sessionStore.storedEmail)
    }

    @Test
    fun register_usesSameFacadeAndSavesSession() = runTest(mainDispatcherRule.dispatcher) {
        // Register проверяется отдельно, чтобы не вернуть старый email-only repository path.
        api.registerResponse = authResponse(
            accessToken = "register-access",
            refreshToken = "register-refresh",
            email = "new@example.com"
        )

        val result = repository.register("new@example.com", "12345678")

        assertEquals(Result.success(AuthorizedSession("new@example.com")), result)
        assertEquals(AuthCredentialsDto("new@example.com", "12345678"), api.lastRegisterRequest)
        assertEquals("register-access", sessionStore.storedAccessToken)
        assertEquals("register-refresh", sessionStore.storedRefreshToken)
        assertEquals("new@example.com", sessionStore.storedEmail)
    }

    @Test
    fun loginWithGoogle_sendsTokenPayloadAndSavesSession() = runTest(mainDispatcherRule.dispatcher) {
        // Social auth edge-case: проверяем, что Google token не преобразуется в email/password DTO.
        api.googleResponse = authResponse(
            accessToken = "google-access",
            refreshToken = "google-refresh",
            email = "google@example.com"
        )

        val result = repository.loginWithGoogle("google-token")

        assertEquals(Result.success(AuthorizedSession("google@example.com")), result)
        assertEquals(GoogleSocialLoginRequestDto("google-token"), api.lastGoogleRequest)
        assertEquals("google-access", sessionStore.storedAccessToken)
        assertEquals("google-refresh", sessionStore.storedRefreshToken)
        assertEquals("google@example.com", sessionStore.storedEmail)
    }

    @Test
    fun loginWithYandex_sendsTokenPayloadAndSavesSession() = runTest(mainDispatcherRule.dispatcher) {
        // Yandex auth использует отдельное поле accessToken и тот же session store.
        api.yandexResponse = authResponse(
            accessToken = "yandex-access",
            refreshToken = "yandex-refresh",
            email = "yandex@example.com"
        )

        val result = repository.loginWithYandex("yandex-token")

        assertEquals(Result.success(AuthorizedSession("yandex@example.com")), result)
        assertEquals(YandexSocialLoginRequestDto("yandex-token"), api.lastYandexRequest)
        assertEquals("yandex-access", sessionStore.storedAccessToken)
        assertEquals("yandex-refresh", sessionStore.storedRefreshToken)
        assertEquals("yandex@example.com", sessionStore.storedEmail)
    }

    @Test
    fun restoreSession_successUsesSavedAccessTokenAndMeEndpoint() = runTest(mainDispatcherRule.dispatcher) {
        // Restore без refresh должен подтвердить сохранённый access token через /auth/me.
        sessionStore.saveSession("saved-access", "saved-refresh", "old@example.com")
        api.meResponses.add(meResponse("restored@example.com"))

        val result = repository.restoreSession()

        assertEquals(Result.success(AuthorizedSession("restored@example.com")), result)
        assertEquals(listOf("Bearer saved-access"), api.meAuthorizationCalls)
        assertEquals("saved-access", sessionStore.storedAccessToken)
        assertEquals("saved-refresh", sessionStore.storedRefreshToken)
        assertEquals("restored@example.com", sessionStore.storedEmail)
    }

    @Test
    fun restoreSession_unauthorizedRefreshesTokensAndRetriesMe() = runTest(mainDispatcherRule.dispatcher) {
        // При 401 единый facade обновляет токены и повторяет /auth/me уже с новым access token.
        sessionStore.saveSession("expired-access", "saved-refresh", "old@example.com")
        api.meThrowables.add(unauthorized())
        api.meResponses.add(meResponse("fresh@example.com"))
        api.refreshResponse = authResponse(
            accessToken = "fresh-access",
            refreshToken = "fresh-refresh",
            email = "fresh@example.com"
        )

        val result = repository.restoreSession()

        assertEquals(Result.success(AuthorizedSession("fresh@example.com")), result)
        assertEquals(listOf("Bearer expired-access", "Bearer fresh-access"), api.meAuthorizationCalls)
        assertEquals(RefreshTokenRequestDto("saved-refresh"), api.lastRefreshRequest)
        assertEquals("fresh-access", sessionStore.storedAccessToken)
        assertEquals("fresh-refresh", sessionStore.storedRefreshToken)
        assertEquals("fresh@example.com", sessionStore.storedEmail)
    }

    @Test
    fun restoreSession_failureClearsSession() = runTest(mainDispatcherRule.dispatcher) {
        // Любой не-refreshable сбой restore очищает локальную сессию.
        sessionStore.saveSession("access", "refresh", "user@example.com")
        api.meThrowables.add(IllegalStateException("backend down"))

        val result = repository.restoreSession()

        assertTrue(result.isFailure)
        assertNull(sessionStore.storedAccessToken)
        assertNull(sessionStore.storedRefreshToken)
        assertNull(sessionStore.storedEmail)
        assertEquals(1, sessionStore.clearCalls)
    }

    @Test
    fun logoutClearsSession() {
        // Logout остаётся простой операцией над общим AuthSessionStore.
        sessionStore.saveSession("access", "refresh", "user@example.com")

        repository.logout()

        assertNull(sessionStore.storedAccessToken)
        assertNull(sessionStore.storedRefreshToken)
        assertNull(sessionStore.storedEmail)
        assertEquals(1, sessionStore.clearCalls)
    }

    private fun authResponse(
        accessToken: String = "access",
        refreshToken: String = "refresh",
        email: String? = null
    ): AuthResponseDto = AuthResponseDto(
        // Response может не содержать top-level email, поэтому тесты задают user.email тем же helper-ом.
        accessToken = accessToken,
        refreshToken = refreshToken,
        email = email,
        user = email?.let { AuthUserDto(email = it) }
    )

    private fun meResponse(email: String): MeResponseDto = MeResponseDto(
        id = "user-id",
        email = email
    )

    private fun unauthorized(): HttpException {
        val response = Response.error<Unit>(401, "Unauthorized".toResponseBody())
        return HttpException(response)
    }
}

// Fake API фиксирует payloads и позволяет вручную выстроить последовательность /auth/me ответов.
private class FakeAuthApi : AuthApi {
    var loginResponse: AuthResponseDto = AuthResponseDto("access", "refresh")
    var registerResponse: AuthResponseDto = AuthResponseDto("access", "refresh")
    var googleResponse: AuthResponseDto = AuthResponseDto("access", "refresh")
    var yandexResponse: AuthResponseDto = AuthResponseDto("access", "refresh")
    var refreshResponse: AuthResponseDto = AuthResponseDto("fresh-access", "fresh-refresh")
    var lastLoginRequest: AuthCredentialsDto? = null
    var lastRegisterRequest: AuthCredentialsDto? = null
    var lastGoogleRequest: GoogleSocialLoginRequestDto? = null
    var lastYandexRequest: YandexSocialLoginRequestDto? = null
    var lastRefreshRequest: RefreshTokenRequestDto? = null
    val meAuthorizationCalls = mutableListOf<String>()
    val meResponses = mutableListOf<MeResponseDto>()
    val meThrowables = mutableListOf<Throwable>()

    override suspend fun register(request: AuthCredentialsDto): AuthResponseDto {
        lastRegisterRequest = request
        return registerResponse
    }

    override suspend fun login(request: AuthCredentialsDto): AuthResponseDto {
        lastLoginRequest = request
        return loginResponse
    }

    override suspend fun loginWithGoogle(request: GoogleSocialLoginRequestDto): AuthResponseDto {
        lastGoogleRequest = request
        return googleResponse
    }

    override suspend fun loginWithYandex(request: YandexSocialLoginRequestDto): AuthResponseDto {
        lastYandexRequest = request
        return yandexResponse
    }

    override suspend fun refresh(request: RefreshTokenRequestDto): AuthResponseDto {
        lastRefreshRequest = request
        return refreshResponse
    }

    override suspend fun me(authorization: String): MeResponseDto {
        // Очередь ошибок/ответов нужна для сценария: 401 -> refresh -> повторный /auth/me.
        meAuthorizationCalls.add(authorization)
        if (meThrowables.isNotEmpty()) {
            throw meThrowables.removeAt(0)
        }
        return meResponses.removeAt(0)
    }
}

// In-memory store повторяет контракт SessionManager без Android EncryptedSharedPreferences.
private class FakeAuthSessionStore : AuthSessionStore {
    var storedAccessToken: String? = null
    var storedRefreshToken: String? = null
    var storedEmail: String? = null
    var clearCalls: Int = 0

    override fun saveSession(accessToken: String, refreshToken: String, email: String?) {
        this.storedAccessToken = accessToken
        this.storedRefreshToken = refreshToken
        this.storedEmail = email
    }

    override fun getAccessToken(): String? = storedAccessToken

    override fun getRefreshToken(): String? = storedRefreshToken

    override fun getEmail(): String? = storedEmail

    override fun hasSession(): Boolean = !storedAccessToken.isNullOrBlank()

    override fun clear() {
        storedAccessToken = null
        storedRefreshToken = null
        storedEmail = null
        clearCalls++
    }
}
