package com.example.homenotes

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeAuthSessionRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        repository = FakeAuthSessionRepository()
        viewModel = AuthViewModel(
            application = Application(),
            repository = repository,
            autoRestore = false
        )
    }

    @Test
    fun restoreSession_success_setsAuthenticatedState() = runTest(mainDispatcherRule.dispatcher) {
        repository.restoreResult = Result.success(AuthorizedSession("user@example.com"))

        viewModel.restoreSession()
        assertEquals(AuthState.Checking, viewModel.uiState.authState)

        advanceUntilIdle()

        assertEquals(AuthState.Authenticated("user@example.com"), viewModel.uiState.authState)
        assertFalse(viewModel.uiState.isLoading)
    }

    @Test
    fun restoreSession_failure_setsUnauthenticatedState() = runTest(mainDispatcherRule.dispatcher) {
        repository.restoreResult = Result.failure(IllegalStateException("No saved access token"))

        viewModel.restoreSession()
        advanceUntilIdle()

        assertEquals(AuthState.Unauthenticated, viewModel.uiState.authState)
        assertFalse(viewModel.uiState.isLoading)
    }

    @Test
    fun handleSessionExpired_logsOutSetsUnauthenticatedStateAndEmitsSnackbar() {
        viewModel.handleSessionExpired()

        assertTrue(repository.clearLocalSessionCalled)
        assertEquals(AuthState.Unauthenticated, viewModel.uiState.authState)
        assertSnackbarMessage(R.string.api_error_unauthorized)
    }

    @Test
    fun signIn_shortPassword_doesNotCallRepositoryAndShowsError() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.signIn("user@example.com", "1234567")
        advanceUntilIdle()

        assertEquals(0, repository.loginCalls)
        assertInlineError(R.string.auth_error_invalid_credentials)
    }

    @Test
    fun signUp_shortPassword_doesNotCallRepositoryAndShowsError() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.signUp("user@example.com", "1234567")
        advanceUntilIdle()

        assertEquals(0, repository.registerCalls)
        assertInlineError(R.string.auth_error_invalid_credentials)
    }

    @Test
    fun signIn_validCredentials_callsRepositoryAndSetsAuthenticatedState() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.signIn("user@example.com", "12345678")
        advanceUntilIdle()

        assertEquals(1, repository.loginCalls)
        assertEquals("user@example.com", repository.lastLoginEmail)
        assertEquals("12345678", repository.lastLoginPassword)
        assertEquals(AuthState.Authenticated("user@example.com"), viewModel.uiState.authState)
        assertFalse(viewModel.uiState.isLoading)
    }

    @Test
    fun signIn_repositoryFailure_setsUnauthenticatedErrorAndStopsLoading() = runTest(mainDispatcherRule.dispatcher) {
        repository.loginResult = Result.failure(IOException("offline"))

        viewModel.signIn("user@example.com", "12345678")
        advanceUntilIdle()

        assertEquals(1, repository.loginCalls)
        assertSnackbarMessage(R.string.api_error_network)
        assertFalse(viewModel.uiState.isLoading)
    }

    @Test
    fun signIn_invalidCredentialsFromBackend_setsInlineErrorAndStopsLoading() = runTest(mainDispatcherRule.dispatcher) {
        repository.loginResult = Result.failure(
            httpException(401, """{"code":"invalid_email_or_password"}""")
        )

        viewModel.signIn("user@example.com", "12345678")
        advanceUntilIdle()

        assertEquals(1, repository.loginCalls)
        assertInlineError(R.string.auth_error_invalid_credentials)
        assertNull(viewModel.uiState.uiMessage)
        assertFalse(viewModel.uiState.isLoading)
    }

    @Test
    fun signUp_validCredentials_callsRepositoryAndSetsAuthenticatedState() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.signUp("new@example.com", "12345678")
        advanceUntilIdle()

        assertEquals(1, repository.registerCalls)
        assertEquals("new@example.com", repository.lastRegisterEmail)
        assertEquals("12345678", repository.lastRegisterPassword)
        assertEquals(AuthState.Authenticated("new@example.com"), viewModel.uiState.authState)
        assertFalse(viewModel.uiState.isLoading)
    }

    @Test
    fun signInWithGoogle_validToken_callsRepositoryAndSetsAuthenticatedState() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.signInWithGoogle("google-token")
        advanceUntilIdle()

        assertEquals(1, repository.googleCalls)
        assertEquals("google-token", repository.lastGoogleToken)
        assertEquals(AuthState.Authenticated("google@example.com"), viewModel.uiState.authState)
        assertFalse(viewModel.uiState.isLoading)
    }

    @Test
    fun signInWithGoogle_repositoryFailure_setsUnauthenticatedErrorAndStopsLoading() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.googleResult = Result.failure(IOException("offline"))

            viewModel.signInWithGoogle("google-token")
            advanceUntilIdle()

            assertEquals(1, repository.googleCalls)
            assertEquals("google-token", repository.lastGoogleToken)
            assertSnackbarMessage(R.string.api_error_network)
            assertFalse(viewModel.uiState.isLoading)
        }

    @Test
    fun signInWithYandex_validToken_callsRepositoryAndSetsAuthenticatedState() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.signInWithYandex("yandex-token")
        advanceUntilIdle()

        assertEquals(1, repository.yandexCalls)
        assertEquals("yandex-token", repository.lastYandexToken)
        assertEquals(AuthState.Authenticated("yandex@example.com"), viewModel.uiState.authState)
        assertFalse(viewModel.uiState.isLoading)
    }

    @Test
    fun signInWithYandex_repositoryFailure_setsUnauthenticatedErrorAndStopsLoading() =
        runTest(mainDispatcherRule.dispatcher) {
            repository.yandexResult = Result.failure(IOException("offline"))

            viewModel.signInWithYandex("yandex-token")
            advanceUntilIdle()

            assertEquals(1, repository.yandexCalls)
            assertEquals("yandex-token", repository.lastYandexToken)
            assertSnackbarMessage(R.string.api_error_network)
            assertFalse(viewModel.uiState.isLoading)
        }

    @Test
    fun signInWithGoogle_blankToken_doesNotCallRepositoryAndShowsError() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.signInWithGoogle(" ")
        advanceUntilIdle()

        assertEquals(0, repository.googleCalls)
        assertSnackbarMessage(R.string.auth_error_google_token_empty)
    }

    @Test
    fun signInWithYandex_blankToken_doesNotCallRepositoryAndShowsError() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.signInWithYandex(" ")
        advanceUntilIdle()

        assertEquals(0, repository.yandexCalls)
        assertSnackbarMessage(R.string.auth_error_yandex_token_empty)
    }

    @Test
    fun socialUiFailures_emitSnackbarMessages() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.onGoogleTokenEmpty()
        assertSnackbarMessage(R.string.auth_error_google_token_empty)

        viewModel.onGoogleSignInFailed()
        assertSnackbarMessage(R.string.auth_error_google_sign_in_failed)

        viewModel.onYandexTokenEmpty()
        assertSnackbarMessage(R.string.auth_error_yandex_token_empty)

        viewModel.onYandexSignInFailed()
        assertSnackbarMessage(R.string.auth_error_yandex_sign_in_failed)

        viewModel.onYandexSignInCancelled()
        assertSnackbarMessage(R.string.auth_error_yandex_sign_in_cancelled)
    }

    @Test
    fun messageShown_clearsMatchingSnackbarMessageOnly() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.onGoogleSignInFailed()
        val firstMessageId = checkNotNull(viewModel.uiState.uiMessage).id

        viewModel.onMessageShown(firstMessageId + 1)
        assertSnackbarMessage(R.string.auth_error_google_sign_in_failed)

        viewModel.onMessageShown(firstMessageId)

        assertNull(viewModel.uiState.uiMessage)
    }

    @Test
    fun clearInlineError_removesInlineErrorWithoutClearingSnackbar() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.signIn("user@example.com", "short")
        viewModel.onGoogleSignInFailed()
        assertInlineError(R.string.auth_error_invalid_credentials)
        assertSnackbarMessage(R.string.auth_error_google_sign_in_failed)

        viewModel.clearInlineError()

        assertNull(viewModel.uiState.inlineErrorMessage)
        assertSnackbarMessage(R.string.auth_error_google_sign_in_failed)
    }

    @Test
    fun logout_callsRepositoryAndSetsUnauthenticatedState() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.logout()
        advanceUntilIdle()

        assertTrue(repository.logoutCalled)
        assertEquals(AuthState.Unauthenticated, viewModel.uiState.authState)
        assertFalse(viewModel.uiState.isLoading)
    }

    private fun assertInlineError(expectedResId: Int) {
        assertEquals(AuthState.Unauthenticated, viewModel.uiState.authState)
        val message = checkNotNull(viewModel.uiState.inlineErrorMessage) as UiText.StringResource
        assertEquals(expectedResId, message.resId)
    }

    private fun assertSnackbarMessage(expectedResId: Int) {
        assertEquals(AuthState.Unauthenticated, viewModel.uiState.authState)
        val message = checkNotNull(viewModel.uiState.uiMessage).text as UiText.StringResource
        assertEquals(expectedResId, message.resId)
    }

    private fun httpException(code: Int, body: String = "error"): HttpException {
        return HttpException(
            Response.error<Unit>(
                code,
                body.toResponseBody("application/json".toMediaType())
            )
        )
    }
}

private class FakeAuthSessionRepository : AuthSessionRepository {
    var restoreResult: Result<AuthorizedSession> = Result.failure(IllegalStateException("not set"))
    var logoutCalled: Boolean = false
    var clearLocalSessionCalled: Boolean = false
    var loginCalls: Int = 0
    var registerCalls: Int = 0
    var googleCalls: Int = 0
    var yandexCalls: Int = 0
    var loginResult: Result<AuthorizedSession>? = null
    var googleResult: Result<AuthorizedSession>? = null
    var yandexResult: Result<AuthorizedSession>? = null
    var lastLoginEmail: String? = null
    var lastLoginPassword: String? = null
    var lastRegisterEmail: String? = null
    var lastRegisterPassword: String? = null
    var lastGoogleToken: String? = null
    var lastYandexToken: String? = null

    override suspend fun login(email: String, password: String): Result<AuthorizedSession> {
        loginCalls++
        lastLoginEmail = email
        lastLoginPassword = password
        return loginResult ?: Result.success(AuthorizedSession(email))
    }

    override suspend fun register(email: String, password: String): Result<AuthorizedSession> {
        registerCalls++
        lastRegisterEmail = email
        lastRegisterPassword = password
        return Result.success(AuthorizedSession(email))
    }

    override suspend fun loginWithGoogle(idToken: String): Result<AuthorizedSession> {
        googleCalls++
        lastGoogleToken = idToken
        return googleResult ?: Result.success(AuthorizedSession("google@example.com"))
    }

    override suspend fun loginWithYandex(accessToken: String): Result<AuthorizedSession> {
        yandexCalls++
        lastYandexToken = accessToken
        return yandexResult ?: Result.success(AuthorizedSession("yandex@example.com"))
    }

    override suspend fun restoreSession(): Result<AuthorizedSession> {
        return restoreResult
    }

    override suspend fun logout() {
        logoutCalled = true
    }

    override fun clearLocalSession() {
        clearLocalSessionCalled = true
    }
}
