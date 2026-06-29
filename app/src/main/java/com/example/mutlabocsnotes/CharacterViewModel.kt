package com.example.mutlabocsnotes

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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    var uiState by mutableStateOf<CharacterUiState>(CharacterUiState.Loading)
        private set

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

    // Credits earned experience to the character / a skill, then caches the fresh sheet.
    fun grantXp(characterXp: Int, skillKey: String?, skillXp: Int) {
        if (characterXp <= 0 && skillXp <= 0) return
        viewModelScope.launch(ioDispatcher) {
            val result = repository.addExperience(characterXp, skillKey, skillXp)
            launch(Dispatchers.Main) {
                result.onSuccess { sheet ->
                    uiState = CharacterUiState.Content(sheet)
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
