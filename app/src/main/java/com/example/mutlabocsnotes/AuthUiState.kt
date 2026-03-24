package com.example.mutlabocsnotes

data class AuthUiState(
    val isCheckingSession: Boolean = true,
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val currentEmail: String = "",
    val errorMessage: String? = null
)