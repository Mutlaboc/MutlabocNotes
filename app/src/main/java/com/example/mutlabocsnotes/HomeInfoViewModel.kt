package com.example.mutlabocsnotes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Хранит UI-состояние карточек домашнего экрана и обрабатывает действия пользователя.
// Репозиторий передаётся из AppContainer, поэтому экран не занимается созданием зависимостей.
class HomeInfoViewModel(
    application: Application,
    private val repository: HomeInfoDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    val cards = mutableStateListOf<HomeInfoCard>()
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    // Загружает карточки и сортирует их по времени обновления.
    fun loadCards() {
        viewModelScope.launch(ioDispatcher) {
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

    // Создаёт карточку через репозиторий и добавляет её в локальный список.
    fun addCard(card: HomeInfoCard) {
        viewModelScope.launch(ioDispatcher) {
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

    // Обновляет карточку и сохраняет сортировку после успешного ответа backend.
    fun updateCard(card: HomeInfoCard) {
        if (card.id.isEmpty()) return
        viewModelScope.launch(ioDispatcher) {
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

    // Удаляет карточку на backend и затем из локального списка.
    fun deleteCard(cardId: String) {
        viewModelScope.launch(ioDispatcher) {
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

    // Сбрасывает локальное состояние карточек, например при смене пользователя.
    fun clearAll() {
        cards.clear()
        errorMessage = null
        isLoading = false
    }
}
