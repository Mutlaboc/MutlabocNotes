package com.example.mutlabocsnotes

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
                        authState = AuthState.Unauthenticated()
                    )
                }
        }
    }

    fun signIn(email: String, password: String) {
        if (!validateCredentials(email, password)) return

        submitAuth {
            repository.login(email, password)
        }
    }

    fun signUp(email: String, password: String) {
        if (!validateCredentials(email, password)) return

        submitAuth {
            repository.register(email, password)
        }
    }

    fun signInWithGoogle(idToken: String) {
        if (idToken.isBlank()) {
            showAuthError(R.string.auth_error_google_token_empty)
            return
        }

        submitAuth {
            repository.loginWithGoogle(idToken)
        }
    }

    fun signInWithYandex(accessToken: String) {
        if (accessToken.isBlank()) {
            showAuthError(R.string.auth_error_yandex_token_empty)
            return
        }

        submitAuth {
            repository.loginWithYandex(accessToken)
        }
    }

    fun logout() {
        repository.logout()
        uiState = AuthUiState(
            authState = AuthState.Unauthenticated()
        )
    }

    fun handleSessionExpired() {
        repository.logout()
        uiState = AuthUiState(
            authState = AuthState.Unauthenticated(
                errorMessage = UiText.StringResource(R.string.api_error_unauthorized)
            )
        )
    }

    fun clearError() {
        if (uiState.authState is AuthState.Unauthenticated) {
            uiState = uiState.copy(
                authState = AuthState.Unauthenticated()
            )
        }
    }

    private fun validateCredentials(email: String, password: String): Boolean {
        if (email.isBlank() || password.length < 8) {
            showAuthError(R.string.auth_error_invalid_credentials)
            return false
        }

        return true
    }

    private fun submitAuth(block: suspend () -> Result<AuthorizedSession>) {
        viewModelScope.launch {
            uiState = AuthUiState(
                authState = AuthState.Unauthenticated(),
                isLoading = true
            )

            block()
                .onSuccess { session ->
                    uiState = AuthUiState(
                        authState = AuthState.Authenticated(session.email)
                    )
                }
                .onFailure { error ->
                    uiState = AuthUiState(
                        authState = AuthState.Unauthenticated(
                            errorMessage = ApiErrorMapper.map(error)
                        ),
                        isLoading = false
                    )
                }
        }
    }

    private fun showAuthError(messageResId: Int) {
        uiState = AuthUiState(
            authState = AuthState.Unauthenticated(UiText.StringResource(messageResId)),
            isLoading = false
        )
    }
}
