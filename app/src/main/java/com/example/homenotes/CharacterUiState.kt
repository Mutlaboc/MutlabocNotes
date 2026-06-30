package com.example.homenotes

sealed interface CharacterUiState {
    data object Loading : CharacterUiState
    data class Content(val sheet: CharacterSheet) : CharacterUiState
    data class Error(val message: UiText) : CharacterUiState
}
