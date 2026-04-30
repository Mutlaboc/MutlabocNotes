package com.example.mutlabocsnotes

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

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

        assertEquals(AuthState.Unauthenticated(), viewModel.uiState.authState)
        assertFalse(viewModel.uiState.isLoading)
    }

    @Test
    fun handleSessionExpired_logsOutAndSetsUnauthenticatedState() {
        viewModel.handleSessionExpired()

        assertTrue(repository.logoutCalled)
        assertEquals(
            AuthState.Unauthenticated("Session expired. Please sign in again."),
            viewModel.uiState.authState
        )
    }
}

private class FakeAuthSessionRepository : AuthSessionRepository {
    var restoreResult: Result<AuthorizedSession> = Result.failure(IllegalStateException("not set"))
    var logoutCalled: Boolean = false

    override suspend fun login(email: String, password: String): Result<AuthorizedSession> {
        return Result.success(AuthorizedSession(email))
    }

    override suspend fun register(email: String, password: String): Result<AuthorizedSession> {
        return Result.success(AuthorizedSession(email))
    }

    override suspend fun loginWithGoogle(idToken: String): Result<AuthorizedSession> {
        return Result.success(AuthorizedSession("google@example.com"))
    }

    override suspend fun loginWithYandex(accessToken: String): Result<AuthorizedSession> {
        return Result.success(AuthorizedSession("yandex@example.com"))
    }

    override suspend fun restoreSession(): Result<AuthorizedSession> {
        return restoreResult
    }

    override fun logout() {
        logoutCalled = true
    }
}
