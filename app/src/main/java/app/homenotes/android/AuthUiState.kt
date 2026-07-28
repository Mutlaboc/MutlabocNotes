package app.homenotes.android

sealed interface AuthState {
    data object Checking : AuthState
    data class Authenticated(val email: String) : AuthState
    data object Unauthenticated : AuthState
}

data class AuthUiState(
    val authState: AuthState = AuthState.Checking,
    val isLoading: Boolean = false,
    val inlineErrorMessage: UiText? = null,
    val uiMessage: UiMessage? = null
) {
    val isCheckingSession: Boolean
        get() = authState is AuthState.Checking

    val isAuthenticated: Boolean
        get() = authState is AuthState.Authenticated

    val currentEmail: String
        get() = (authState as? AuthState.Authenticated)?.email.orEmpty()
}
