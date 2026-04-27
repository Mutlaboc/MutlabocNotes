package com.example.mutlabocsnotes

import androidx.compose.runtime.mutableStateListOf
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

// Хранит UI-состояние и обрабатывает действия пользователя.
class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NotesRepository(application)
    private val notificationScheduler = DeadlineNotificationScheduler(application)

    // Локальный кэш заметок, можно сделать LiveData или StateFlow для наблюдения за изменениями
    val notes = mutableStateListOf<Note>()
    var totalCoins by mutableIntStateOf(0)
    private set


    // Загружаем заметки
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

// Добавляем заметку
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

    // Очищает временные и сохранённые данные состояния.
    fun clearAll() {
        notes.clear()
        totalCoins = 0
    }

    // Обновляем заметку
    // TODO Надо бы сделать защиту от сбоя одновления
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
    // Помечаем заметку как выполненную (с защитой от сбоя)
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

    // Удаление заметки
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
    // Пересчитывает заработанные монеты, учитывая только помеченные как выполненные заметки.
    private fun recalculateTotalCoins() {
        totalCoins = notes.sumOf { if (it.isCompleted) it.coinCount else 0 }
    }
}

