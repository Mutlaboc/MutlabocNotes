package com.example.homenotes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class AuthViewModel(
    application: Application,
    private val repository: AuthSessionRepository,
    autoRestore: Boolean
) : AndroidViewModel(application) {

    var uiState by mutableStateOf(AuthUiState())
        private set

    init {
        if (autoRestore) {
            restoreSession()
        }
    }

    fun restoreSession() {
        viewModelScope.launch {
            uiState = AuthUiState(authState = AuthState.Checking)

            repository.restoreSession()
                .onSuccess { session ->
                    uiState = AuthUiState(
                        authState = AuthState.Authenticated(session.email)
                    )
                }
                .onFailure {
                    uiState = AuthUiState(
                        authState = AuthState.Unauthenticated
                    )
                }
        }
    }

    fun signIn(email: String, password: String) {
        clearInlineError()
        if (!validateCredentials(email, password)) return

        submitAuth {
            repository.login(email, password)
        }
    }

    fun signUp(email: String, password: String) {
        clearInlineError()
        if (!validateCredentials(email, password)) return

        submitAuth {
            repository.register(email, password)
        }
    }

    fun signInWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            showSnackbarMessage(R.string.auth_error_google_token_empty)
            return
        }

        submitAuth {
            repository.loginWithGoogle(idToken)
        }
    }

    fun signInWithYandex(accessToken: String) {
        if (accessToken.isBlank()) {
            showSnackbarMessage(R.string.auth_error_yandex_token_empty)
            return
        }

        submitAuth {
            repository.loginWithYandex(accessToken)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            uiState = AuthUiState(
                authState = AuthState.Unauthenticated
            )
        }
    }

    fun handleSessionExpired() {
        repository.clearLocalSession()
        uiState = AuthUiState(
            authState = AuthState.Unauthenticated,
            uiMessage = UiMessage(
                id = UiMessageId.next(),
                text = UiText.StringResource(R.string.api_error_unauthorized)
            )
        )
    }

    fun onGoogleTokenEmpty() {
        showSnackbarMessage(R.string.auth_error_google_token_empty)
    }

    fun onGoogleSignInFailed() {
        showSnackbarMessage(R.string.auth_error_google_sign_in_failed)
    }

    fun onYandexTokenEmpty() {
        showSnackbarMessage(R.string.auth_error_yandex_token_empty)
    }

    fun onYandexSignInFailed() {
        showSnackbarMessage(R.string.auth_error_yandex_sign_in_failed)
    }

    fun onYandexSignInCancelled() {
        showSnackbarMessage(R.string.auth_error_yandex_sign_in_cancelled)
    }

    fun onMessageShown(messageId: Long) {
        if (uiState.uiMessage?.id == messageId) {
            uiState = uiState.copy(uiMessage = null)
        }
    }

    fun clearInlineError() {
        if (uiState.inlineErrorMessage != null) {
            uiState = uiState.copy(inlineErrorMessage = null)
        }
    }

    private fun validateCredentials(email: String, password: String): Boolean {
        if (email.isBlank() || password.length < 8) {
            showInlineError(R.string.auth_error_invalid_credentials)
            return false
        }

        return true
    }

    private fun submitAuth(block: suspend () -> Result<AuthorizedSession>) {
        viewModelScope.launch {
            uiState = AuthUiState(
                authState = AuthState.Unauthenticated,
                isLoading = true
            )

            block()
                .onSuccess { session ->
                    uiState = AuthUiState(
                        authState = AuthState.Authenticated(session.email)
                    )
                }
                .onFailure { error ->
                    showAuthFailure(error)
                }
        }
    }

    private fun showAuthFailure(error: Throwable) {
        val message = ApiErrorMapper.map(error)
        if (message.isInvalidCredentialsError()) {
            uiState = AuthUiState(
                authState = AuthState.Unauthenticated,
                inlineErrorMessage = message
            )
        } else {
            uiState = AuthUiState(
                authState = AuthState.Unauthenticated,
                uiMessage = UiMessage(
                    id = UiMessageId.next(),
                    text = message
                )
            )
        }
    }

    private fun showInlineError(messageResId: Int) {
        uiState = AuthUiState(
            authState = AuthState.Unauthenticated,
            inlineErrorMessage = UiText.StringResource(messageResId)
        )
    }

    private fun showSnackbarMessage(messageResId: Int) {
        uiState = uiState.copy(
            authState = AuthState.Unauthenticated,
            isLoading = false,
            uiMessage = UiMessage(
                id = UiMessageId.next(),
                text = UiText.StringResource(messageResId)
            )
        )
    }

    private fun UiText.isInvalidCredentialsError(): Boolean {
        return this is UiText.StringResource && resId == R.string.auth_error_invalid_credentials
    }
}
