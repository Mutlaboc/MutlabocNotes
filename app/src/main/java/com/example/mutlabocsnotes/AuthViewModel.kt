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

    constructor(application: Application) : this(
        application = application,
        repository = AuthRepository(application),
        autoRestore = true
    )

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
            showAuthError("Google id token is empty")
            return
        }

        submitAuth {
            repository.loginWithGoogle(idToken)
        }
    }

    fun signInWithYandex(accessToken: String) {
        if (accessToken.isBlank()) {
            showAuthError("Yandex access token is empty")
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
                errorMessage = "Session expired. Please sign in again."
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
        if (email.isBlank() || password.length < 6) {
            showAuthError("Enter a valid email and a password of at least 6 characters")
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
                            errorMessage = error.message ?: "Authentication failed"
                        ),
                        isLoading = false
                    )
                }
        }
    }

    private fun showAuthError(message: String) {
        uiState = AuthUiState(
            authState = AuthState.Unauthenticated(message),
            isLoading = false
        )
    }
}
