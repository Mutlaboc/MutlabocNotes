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
import kotlinx.coroutines.withContext

class InventoryViewModel(
    application: Application,
    private val repository: InventoryDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    var uiState by mutableStateOf<InventoryUiState>(InventoryUiState.Loading)
        private set

    // Экран уже офлайн-first (репозиторий читает Room-кэш), поэтому повторный вход
    // (навигация назад/вперёд) не должен гасить уже показанный инвентарь спиннером —
    // Loading показываем только пока данных ещё не было ни разу.
    fun loadInventory() {
        if (uiState !is InventoryUiState.Content) {
            uiState = InventoryUiState.Loading
        }
        viewModelScope.launch(ioDispatcher) {
            val result = repository.getInventory()
            withContext(Dispatchers.Main) {
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
