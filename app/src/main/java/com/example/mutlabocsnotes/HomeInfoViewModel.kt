package com.example.mutlabocsnotes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Holds UI state and handles user-driven actions.
class HomeInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HomeInfoRepository(application)

    val cards = mutableStateListOf<HomeInfoCard>()
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // Loads data required by the current screen or feature.
    fun loadCards() {
        viewModelScope.launch(Dispatchers.IO) {
            launch(Dispatchers.Main) {
                isLoading = true
                errorMessage = null
            }
            val result = repository.getAllCards()
            launch(Dispatchers.Main) {
                result.onSuccess { loadedCards ->
                    cards.clear()
                    cards.addAll(loadedCards.sortedByDescending { it.updatedAt })
                }.onFailure { error ->
                    errorMessage = error.message ?: "Не удалось загрузить карточки"
                }
                isLoading = false
            }
        }
    }

    // Adds a card through the repository and refreshes local state.
    fun addCard(card: HomeInfoCard) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.insert(card)
            launch(Dispatchers.Main) {
                result.onSuccess { id ->
                    val cardWithId = card.copy(id = id)
                    cards.add(cardWithId)
                    cards.sortByDescending { it.updatedAt }
                }.onFailure { error ->
                    errorMessage = error.message ?: "Не удалось сохранить карточку"
                }
            }
        }
    }

    // Updates existing data with new values.
    fun updateCard(card: HomeInfoCard) {
        if (card.id.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.update(card)
            launch(Dispatchers.Main) {
                result.onSuccess {
                    val index = cards.indexOfFirst { it.id == card.id }
                    if (index != -1) {
                        cards[index] = card
                        cards.sortByDescending { it.updatedAt }
                    }
                }.onFailure { error ->
                    errorMessage = error.message ?: "Не удалось обновить карточку"
                }
            }
        }
    }

        // Deletes the target entity from storage or backend.
        fun deleteCard(cardId: String) {
            viewModelScope.launch(Dispatchers.IO) {
                val result = repository.delete(cardId)
                launch(Dispatchers.Main) {
                    result.onSuccess {
                        val card = cards.find { it.id == cardId }
                        if (card != null) {
                            cards.remove(card)
                        }
                    }.onFailure { error ->
                        errorMessage = error.message ?: "Не удалось удалить карточку"
                    }
                }
            }
        }

    // Clears temporary or persisted state values.
    fun clearAll() {
        cards.clear()
        errorMessage = null
        isLoading = false
    }
    }
