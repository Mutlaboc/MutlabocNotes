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
            val loadedCards = repository.getAllCards()
            launch(Dispatchers.Main) {
                cards.clear()
                cards.addAll(loadedCards.sortedByDescending { it.updatedAt })
                isLoading = false
            }
        }
    }

    fun addCard(card: HomeInfoCard) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insert(card)
            if (id != null) {
                val cardWithId = card.copy(id = id)
                launch(Dispatchers.Main) {
                    cards.add(cardWithId)
                    cards.sortByDescending { it.updatedAt }
                }
            }
        }
    }

    fun updateCard(card: HomeInfoCard) {
        if (card.id.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.update(card)
            if (success) {
                val index = cards.indexOfFirst { it.id == card.id }
                if (index != -1) {
                    launch(Dispatchers.Main) {
                        cards[index] = card
                        cards.sortByDescending { it.updatedAt }
                    }
                }
            }
        }
    }

    fun deleteCard(cardId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.delete(cardId)
            if (success) {
                val card = cards.find { it.id == cardId }
                if (card != null) {
                    launch(Dispatchers.Main) {
                        cards.remove(card)
                    }
                }
            }
        }
    }
}