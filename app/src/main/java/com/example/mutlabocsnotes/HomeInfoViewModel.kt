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

class HomeInfoViewModel(
    application: Application,
    private val repository: HomeInfoDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    var uiState by mutableStateOf<HomeInfoUiState>(HomeInfoUiState.Loading)
        private set

    var uiMessage by mutableStateOf<UiMessage?>(null)
        private set

    fun loadCards() {
        viewModelScope.launch(ioDispatcher) {
            launch(Dispatchers.Main) {
                uiState = HomeInfoUiState.Loading
            }

            val result = repository.getAllCards()

            launch(Dispatchers.Main) {
                result.onSuccess { loadedCards ->
                    applyCards(loadedCards.sortedByDescending { it.updatedAt })
                }.onFailure { error ->
                    uiState = HomeInfoUiState.Error(ApiErrorMapper.map(error))
                }
            }
        }
    }

    fun addCard(card: HomeInfoCard) {
        viewModelScope.launch(ioDispatcher) {
            val result = repository.insert(card)
            launch(Dispatchers.Main) {
                result.onSuccess { created ->
                    val cards = currentCards() + created
                    applyCards(cards.sortedByDescending { it.updatedAt })
                }.onFailure { error ->
                    showMessage(error)
                }
            }
        }
    }

    fun updateCard(card: HomeInfoCard) {
        if (card.id.isEmpty()) {
            showMessage(IllegalArgumentException("Blank card id"))
            return
        }

        viewModelScope.launch(ioDispatcher) {
            val result = repository.update(card)
            launch(Dispatchers.Main) {
                result.onSuccess { updated ->
                    val cards = currentCards().map { existing ->
                        if (existing.id == updated.id) updated else existing
                    }
                    applyCards(cards.sortedByDescending { it.updatedAt })
                }.onFailure { error ->
                    showMessage(error)
                }
            }
        }
    }

    fun deleteCard(cardId: String) {
        viewModelScope.launch(ioDispatcher) {
            val result = repository.delete(cardId)
            launch(Dispatchers.Main) {
                result.onSuccess {
                    applyCards(currentCards().filterNot { it.id == cardId })
                }.onFailure { error ->
                    showMessage(error)
                }
            }
        }
    }

    fun clearAll() {
        uiState = HomeInfoUiState.Empty
        uiMessage = null
    }

    fun onMessageShown(messageId: Long) {
        if (uiMessage?.id == messageId) {
            uiMessage = null
        }
    }

    private fun currentCards(): List<HomeInfoCard> {
        return (uiState as? HomeInfoUiState.Content)?.cards.orEmpty()
    }

    private fun applyCards(cards: List<HomeInfoCard>) {
        uiState = if (cards.isEmpty()) {
            HomeInfoUiState.Empty
        } else {
            HomeInfoUiState.Content(cards)
        }
    }

    private fun showMessage(error: Throwable) {
        uiMessage = UiMessage(
            id = UiMessageId.next(),
            text = ApiErrorMapper.map(error)
        )
    }
}
