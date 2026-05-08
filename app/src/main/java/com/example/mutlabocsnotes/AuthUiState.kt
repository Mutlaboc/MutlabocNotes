package com.example.mutlabocsnotes

sealed interface AuthState {
    data object Checking : AuthState
    data class Authenticated(val email: String) : AuthState
    data class Unauthenticated(val errorMessage: UiText? = null) : AuthState
}

data class AuthUiState(
    val authState: AuthState = AuthState.Checking,
    val isLoading: Boolean = false
) {
    val isCheckingSession: Boolean
        get() = authState is AuthState.Checking

    val isAuthenticated: Boolean
        get() = authState is AuthState.Authenticated

    val currentEmail: String
        get() = (authState as? AuthState.Authenticated)?.email.orEmpty()

    val errorMessage: UiText?
        get() = (authState as? AuthState.Unauthenticated)?.errorMessage
}
