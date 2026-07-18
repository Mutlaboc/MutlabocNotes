package app.homenotes.android

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InventoryViewModel(
    application: Application,
    private val repository: InventoryDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    var uiState by mutableStateOf<InventoryUiState>(InventoryUiState.Loading)
        private set

    fun loadInventory() {
        viewModelScope.launch(ioDispatcher) {
            launch(Dispatchers.Main) {
                uiState = InventoryUiState.Loading
            }

            val result = repository.getInventory()

            launch(Dispatchers.Main) {
                result.onSuccess { inventory ->
                    uiState = InventoryUiState.Content(inventory)
                }.onFailure { error ->
                    uiState = InventoryUiState.Error(ApiErrorMapper.map(error))
                }
            }
        }
    }

    // Надевает предмет (optimistic: слот обновляется сразу, при ошибке — откат).
    fun equip(itemId: String) {
        val current = (uiState as? InventoryUiState.Content)?.inventory ?: return
        val optimistic = current.applyEquip(itemId)
        if (optimistic == current) return
        uiState = InventoryUiState.Content(optimistic)

        viewModelScope.launch(ioDispatcher) {
            val result = repository.equip(itemId)
            launch(Dispatchers.Main) {
                result.onSuccess { inventory ->
                    uiState = InventoryUiState.Content(inventory)
                }.onFailure {
                    uiState = InventoryUiState.Content(current)
                }
            }
        }
    }

    // Снимает предмет из слота (optimistic, с откатом при ошибке).
    fun unequip(slot: EquipSlot) {
        val current = (uiState as? InventoryUiState.Content)?.inventory ?: return
        val optimistic = current.applyUnequip(slot)
        if (optimistic == current) return
        uiState = InventoryUiState.Content(optimistic)

        viewModelScope.launch(ioDispatcher) {
            val result = repository.unequip(slot)
            launch(Dispatchers.Main) {
                result.onSuccess { inventory ->
                    uiState = InventoryUiState.Content(inventory)
                }.onFailure {
                    uiState = InventoryUiState.Content(current)
                }
            }
        }
    }
}
