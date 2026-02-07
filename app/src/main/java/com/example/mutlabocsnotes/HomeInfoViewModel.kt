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

class HomeInfoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HomeInfoRepository()

    val cards = mutableStateListOf<HomeInfoCard>()
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

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

        fun deleteCard(cardId: String) {
            viewModelScope.launch(Dispatchers.IO) {
                val success = repository.delete(cardId)
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
    }
