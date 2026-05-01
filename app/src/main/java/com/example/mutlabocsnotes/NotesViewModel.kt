package com.example.mutlabocsnotes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Хранит UI-состояние заметок и обрабатывает действия пользователя.
// Репозиторий и планировщик приходят из AppContainer, а не создаются внутри ViewModel.
class NotesViewModel(
    application: Application,
    private val repository: NotesRepository,
    private val notificationScheduler: DeadlineNotificationScheduler
) : AndroidViewModel(application) {

    // Локальный Compose-кэш заметок, который читают экраны.
    val notes = mutableStateListOf<Note>()
    var totalCoins by mutableIntStateOf(0)
        private set

    // Загружает заметки и пересоздаёт расписание напоминаний для актуального списка.
    fun loadNotes() {
        viewModelScope.launch(Dispatchers.IO) {
            val loadNotes = repository.getAllNotes()
            launch(Dispatchers.Main) {
                notes.clear()
                notes.addAll(loadNotes)
                notificationScheduler.scheduleAll(notes)
                recalculateTotalCoins()
            }
        }
    }

    // Добавляет заметку на backend, затем обновляет локальный список и уведомления.
    fun addNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insert(note)
            if (id != null) {
                val noteWithId = note.copy(id = id)
                launch(Dispatchers.Main) {
                    notes.add(noteWithId)
                    notificationScheduler.schedule(noteWithId)
                    recalculateTotalCoins()
                }
            }
        }
    }

    // Очищает локальное состояние, например после выхода пользователя.
    fun clearAll() {
        notes.clear()
        totalCoins = 0
    }

    // Обновляет заметку на backend и синхронизирует локальное состояние при успехе.
    fun updateNote(note: Note) {
        if (note.id.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.update(note)
            if (success) {
                val index = notes.indexOfFirst { it.id == note.id }
                if (index != -1) {
                    launch(Dispatchers.Main) {
                        notes[index] = note
                        notificationScheduler.schedule(note)
                        recalculateTotalCoins()
                    }
                }
            }

        }
    }

    // Оптимистично меняет статус выполнения и откатывает изменение, если backend не принял обновление.
    fun setNoteCompletion(noteId: String, isCompleted: Boolean) {
        val index = notes.indexOfFirst { it.id == noteId }
        if (index == -1) return
        val existing = notes[index]
        val updatedNote = existing.copy(isCompleted = isCompleted)
        notes[index] = updatedNote
        notificationScheduler.schedule(updatedNote)
        recalculateTotalCoins()
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.update(updatedNote)
            if (!success) {
                launch(Dispatchers.Main) {
                    val currentIndex = notes.indexOfFirst { it.id == noteId }
                    if (currentIndex != -1) {
                        notes[currentIndex] = existing
                        notificationScheduler.schedule(existing)
                        recalculateTotalCoins()
                    }
                }
            }
        }
    }

    // Удаляет заметку и отменяет связанное с ней напоминание.
    fun deleteNote(noteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.delete(noteId)
            if (success) {
                val note = notes.find { it.id == noteId }
                if (note != null) {
                    launch(Dispatchers.Main) {
                        notes.remove(note)
                        notificationScheduler.cancel(noteId)
                        recalculateTotalCoins()
                    }
                }
            }
        }
    }

    // Пересчитывает заработанные монеты только по выполненным заметкам.
    private fun recalculateTotalCoins() {
        totalCoins = notes.sumOf { if (it.isCompleted) it.coinCount else 0 }
    }
}
