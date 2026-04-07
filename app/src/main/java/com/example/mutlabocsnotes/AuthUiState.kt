package com.example.mutlabocsnotes

// Неизменяемая модель UI-состояния для этого экрана.
data class AuthUiState(
    val isCheckingSession: Boolean = true,
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val currentEmail: String = "",
    val errorMessage: String? = null
)
