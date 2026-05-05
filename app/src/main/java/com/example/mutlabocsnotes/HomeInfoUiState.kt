package com.example.mutlabocsnotes

sealed interface HomeInfoUiState {
    data object Loading : HomeInfoUiState
    data object Empty : HomeInfoUiState
    data class Content(
        val cards: List<HomeInfoCard>
    ) : HomeInfoUiState
    data class Error(
        val message: UiText
    ) : HomeInfoUiState
}
