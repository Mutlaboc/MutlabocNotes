package com.example.homenotes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CharacterViewModel(
    application: Application,
    private val repository: CharacterDataSource,
    private val coinWallet: CoinWalletRepository = InMemoryCoinWalletRepository(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    var uiState by mutableStateOf<CharacterUiState>(CharacterUiState.Loading)
        private set

    // Сколько монет уже потрачено на прокачку текущим аккаунтом (из кошелька).
    var spentCoins by mutableStateOf(0)
        private set

    // Аккаунт, к которому привязаны списания монет. null — баланс прокачки скрыт.
    private var userKey: String? = null

    fun loadCharacter() {
        viewModelScope.launch(ioDispatcher) {
            launch(Dispatchers.Main) {
                uiState = CharacterUiState.Loading
            }

            val result = repository.getCharacter()

            launch(Dispatchers.Main) {
                result.onSuccess { sheet ->
                    uiState = CharacterUiState.Content(sheet)
                }.onFailure { error ->
                    uiState = CharacterUiState.Error(ApiErrorMapper.map(error))
                }
            }
        }
    }

    /** Переключает кошелёк прокачки на указанный аккаунт и подгружает сумму списаний. */
    fun setUser(key: String?) {
        val normalized = key?.trim()?.takeIf { it.isNotEmpty() }
        userKey = normalized
        if (normalized == null) {
            spentCoins = 0
            return
        }
        viewModelScope.launch(ioDispatcher) {
            val loaded = coinWallet.spentCoins(normalized)
            launch(Dispatchers.Main) { spentCoins = loaded }
        }
    }

    /**
     * Тратит монеты на прокачку характеристики [statKey] (+1 к значению).
     * [earnedCoins] — заработано монет (сумма из выполненных заметок); доступный
     * баланс = earnedCoins − spentCoins. Списание подтверждается только после успешного
     * сохранения листа на бекенде; при ошибке прокачка откатывается.
     */
    fun upgradeStat(statKey: String, earnedCoins: Int) {
        val current = (uiState as? CharacterUiState.Content)?.sheet ?: return
        val key = userKey ?: return
        val stat = current.stats.firstOrNull { it.key == statKey } ?: return
        if (stat.value >= CHARACTER_STAT_MAX) return

        val cost = statUpgradeCost(stat.value)
        val available = (earnedCoins - spentCoins).coerceAtLeast(0)
        if (cost > available) return

        // Оптимистичное обновление: значение характеристики и баланс меняются сразу.
        val upgraded = current.copy(
            stats = current.stats.map { existing ->
                if (existing.key == statKey) existing.copy(value = existing.value + 1) else existing
            }
        )
        uiState = CharacterUiState.Content(upgraded)
        spentCoins += cost

        viewModelScope.launch(ioDispatcher) {
            val result = repository.updateCharacter(upgraded)
            val persistedSpent = result.getOrNull()?.let { coinWallet.recordSpend(key, cost) }
            launch(Dispatchers.Main) {
                result.onSuccess { sheet ->
                    uiState = CharacterUiState.Content(sheet)
                    if (persistedSpent != null) spentCoins = persistedSpent
                }.onFailure {
                    // Откат: возвращаем прежний лист и не списываем монеты.
                    uiState = CharacterUiState.Content(current)
                    spentCoins = (spentCoins - cost).coerceAtLeast(0)
                }
            }
        }
    }

    // Credits earned experience to the character / a skill, then caches the fresh sheet.
    // При повышении уровня дополнительно случайно прокачивается одна характеристика или навык.
    fun grantXp(characterXp: Int, skillKey: String?, skillXp: Int) {
        if (characterXp <= 0 && skillXp <= 0) return
        val previousLevel = (uiState as? CharacterUiState.Content)?.sheet?.level
        viewModelScope.launch(ioDispatcher) {
            val result = repository.addExperience(characterXp, skillKey, skillXp)
            val resolved = result.getOrNull()?.let { sheet ->
                val gained = sheet.level - (previousLevel ?: sheet.level)
                if (gained > 0) {
                    val rewarded = applyLevelUpRewards(sheet, gained)
                    // Случайная прокачка не известна бекенду — сохраняем лист целиком.
                    repository.updateCharacter(rewarded).getOrDefault(rewarded)
                } else {
                    sheet
                }
            }
            launch(Dispatchers.Main) {
                if (resolved != null) {
                    uiState = CharacterUiState.Content(resolved)
                }
            }
        }
    }

    // Renames the character (optimistic update, reverts on failure).
    fun updateName(newName: String) {
        val current = (uiState as? CharacterUiState.Content)?.sheet ?: return
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == current.name) return

        val optimistic = current.copy(name = trimmed)
        uiState = CharacterUiState.Content(optimistic)

        viewModelScope.launch(ioDispatcher) {
            val result = repository.updateCharacter(optimistic)
            launch(Dispatchers.Main) {
                result.onSuccess { sheet ->
                    uiState = CharacterUiState.Content(sheet)
                }.onFailure {
                    uiState = CharacterUiState.Content(current)
                }
            }
        }
    }
}
