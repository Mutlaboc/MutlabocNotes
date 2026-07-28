package app.homenotes.android

sealed interface InventoryUiState {
    data object Loading : InventoryUiState
    data class Content(val inventory: Inventory) : InventoryUiState
    data class Error(val message: UiText) : InventoryUiState
}
