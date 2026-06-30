package com.example.homenotes

sealed interface NotesUiState {
    data object Loading : NotesUiState
    data object Empty : NotesUiState
    data class Content(
        val notes: List<Note>,
        val totalCoins: Int
    ) : NotesUiState
    data class Error(
        val message: UiText
    ) : NotesUiState
}
