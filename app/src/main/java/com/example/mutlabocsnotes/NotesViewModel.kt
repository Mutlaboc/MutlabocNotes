package com.example.mutlabocsnotes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotesViewModel(
    application: Application,
    private val repository: NotesDataSource,
    private val notificationScheduler: DeadlineScheduler,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    val notes = mutableStateListOf<Note>()

    var totalCoins by mutableIntStateOf(0)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun loadNotes() {
        viewModelScope.launch(ioDispatcher) {
            launch(Dispatchers.Main) {
                isLoading = true
                errorMessage = null
            }

            val result = repository.getAllNotes()

            launch(Dispatchers.Main) {
                result.onSuccess { loadedNotes ->
                    notes.clear()
                    notes.addAll(loadedNotes)
                    notificationScheduler.scheduleAll(notes)
                    recalculateTotalCoins()
                    errorMessage = null
                }.onFailure { error ->
                    errorMessage = noteErrorMessage(error, "Failed to load notes")
                }
                isLoading = false
            }
        }
    }

    fun addNote(note: Note) {
        viewModelScope.launch(ioDispatcher) {
            val result = repository.insert(note)
            launch(Dispatchers.Main) {
                result.onSuccess { id ->
                    val noteWithId = note.copy(id = id)
                    notes.add(noteWithId)
                    notificationScheduler.schedule(noteWithId)
                    recalculateTotalCoins()
                    errorMessage = null
                }.onFailure { error ->
                    errorMessage = noteErrorMessage(error, "Failed to save note")
                }
            }
        }
    }

    fun clearAll() {
        notes.clear()
        totalCoins = 0
        errorMessage = null
        isLoading = false
    }

    fun clearError() {
        errorMessage = null
    }

    fun updateNote(note: Note) {
        if (note.id.isEmpty()) {
            errorMessage = "Blank note id"
            return
        }

        viewModelScope.launch(ioDispatcher) {
            val result = repository.update(note)
            launch(Dispatchers.Main) {
                result.onSuccess {
                    val index = notes.indexOfFirst { it.id == note.id }
                    if (index != -1) {
                        notes[index] = note
                        notificationScheduler.schedule(note)
                        recalculateTotalCoins()
                    }
                    errorMessage = null
                }.onFailure { error ->
                    errorMessage = noteErrorMessage(error, "Failed to update note")
                }
            }
        }
    }

    fun setNoteCompletion(noteId: String, isCompleted: Boolean) {
        val index = notes.indexOfFirst { it.id == noteId }
        if (index == -1) return

        val existing = notes[index]
        val updatedNote = existing.copy(isCompleted = isCompleted)
        notes[index] = updatedNote
        notificationScheduler.schedule(updatedNote)
        recalculateTotalCoins()

        viewModelScope.launch(ioDispatcher) {
            val result = repository.update(updatedNote)
            launch(Dispatchers.Main) {
                result.onSuccess {
                    errorMessage = null
                }.onFailure { error ->
                    val currentIndex = notes.indexOfFirst { it.id == noteId }
                    if (currentIndex != -1) {
                        notes[currentIndex] = existing
                        notificationScheduler.schedule(existing)
                        recalculateTotalCoins()
                    }
                    errorMessage = noteErrorMessage(error, "Failed to update note")
                }
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch(ioDispatcher) {
            val result = repository.delete(noteId)
            launch(Dispatchers.Main) {
                result.onSuccess {
                    val note = notes.find { it.id == noteId }
                    if (note != null) {
                        notes.remove(note)
                        notificationScheduler.cancel(noteId)
                        recalculateTotalCoins()
                    }
                    errorMessage = null
                }.onFailure { error ->
                    errorMessage = noteErrorMessage(error, "Failed to delete note")
                }
            }
        }
    }

    private fun recalculateTotalCoins() {
        totalCoins = notes.sumOf { if (it.isCompleted) it.coinCount else 0 }
    }

    private fun noteErrorMessage(error: Throwable, fallback: String): String {
        return error.message?.takeIf { it.isNotBlank() } ?: fallback
    }
}
