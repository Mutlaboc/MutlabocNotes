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

class NotesViewModel(
    application: Application,
    private val repository: NotesDataSource,
    private val notificationScheduler: DeadlineScheduler,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    var uiState by mutableStateOf<NotesUiState>(NotesUiState.Loading)
        private set

    var uiMessage by mutableStateOf<UiMessage?>(null)
        private set

    fun loadNotes() {
        viewModelScope.launch(ioDispatcher) {
            launch(Dispatchers.Main) {
                uiState = NotesUiState.Loading
            }

            val result = repository.getAllNotes()

            launch(Dispatchers.Main) {
                result.onSuccess { loadedNotes ->
                    applyNotes(loadedNotes)
                    notificationScheduler.scheduleAll(loadedNotes)
                }.onFailure { error ->
                    uiState = NotesUiState.Error(ApiErrorMapper.map(error))
                }
            }
        }
    }

    fun addNote(note: Note) {
        viewModelScope.launch(ioDispatcher) {
            val result = repository.insert(note)
            launch(Dispatchers.Main) {
                result.onSuccess { id ->
                    val noteWithId = note.copy(id = id)
                    val notes = currentNotes() + noteWithId
                    applyNotes(notes)
                    notificationScheduler.schedule(noteWithId)
                }.onFailure { error ->
                    showMessage(error)
                }
            }
        }
    }

    fun clearAll() {
        uiState = NotesUiState.Empty
        uiMessage = null
    }

    fun onMessageShown(messageId: Long) {
        if (uiMessage?.id == messageId) {
            uiMessage = null
        }
    }

    fun updateNote(note: Note) {
        if (note.id.isEmpty()) {
            showMessage(IllegalArgumentException("Blank note id"))
            return
        }

        viewModelScope.launch(ioDispatcher) {
            val result = repository.update(note)
            launch(Dispatchers.Main) {
                result.onSuccess {
                    val notes = currentNotes().map { existing ->
                        if (existing.id == note.id) note else existing
                    }
                    applyNotes(notes)
                    notificationScheduler.schedule(note)
                }.onFailure { error ->
                    showMessage(error)
                }
            }
        }
    }

    fun setNoteCompletion(noteId: String, isCompleted: Boolean) {
        val existingNotes = currentNotes()
        val index = existingNotes.indexOfFirst { it.id == noteId }
        if (index == -1) return

        val existing = existingNotes[index]
        val updatedNote = existing.copy(isCompleted = isCompleted)
        val optimisticNotes = existingNotes.toMutableList().apply {
            this[index] = updatedNote
        }
        applyNotes(optimisticNotes)
        notificationScheduler.schedule(updatedNote)

        viewModelScope.launch(ioDispatcher) {
            val result = repository.update(updatedNote)
            launch(Dispatchers.Main) {
                result.onFailure { error ->
                    applyNotes(existingNotes)
                    notificationScheduler.schedule(existing)
                    showMessage(error)
                }
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch(ioDispatcher) {
            val result = repository.delete(noteId)
            launch(Dispatchers.Main) {
                result.onSuccess {
                    val notes = currentNotes()
                    val deletedNote = notes.find { it.id == noteId }
                    applyNotes(notes.filterNot { it.id == noteId })
                    if (deletedNote != null) {
                        notificationScheduler.cancel(noteId)
                    }
                }.onFailure { error ->
                    showMessage(error)
                }
            }
        }
    }

    private fun currentNotes(): List<Note> {
        return (uiState as? NotesUiState.Content)?.notes.orEmpty()
    }

    private fun applyNotes(notes: List<Note>) {
        uiState = if (notes.isEmpty()) {
            NotesUiState.Empty
        } else {
            NotesUiState.Content(
                notes = notes,
                totalCoins = notes.sumOf { if (it.isCompleted) it.coinCount else 0 }
            )
        }
    }

    private fun showMessage(error: Throwable) {
        uiMessage = UiMessage(
            id = UiMessageId.next(),
            text = ApiErrorMapper.map(error)
        )
    }
}
