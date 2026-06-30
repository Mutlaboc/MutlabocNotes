package com.example.homenotes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class NotesViewModel(
    application: Application,
    private val repository: NotesDataSource,
    private val notificationScheduler: DeadlineScheduler,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val coinRewardProvider: () -> Int = { (1..3).random() }
) : AndroidViewModel(application) {

    var uiState by mutableStateOf<NotesUiState>(NotesUiState.Loading)
        private set

    var uiMessage by mutableStateOf<UiMessage?>(null)
        private set

    fun loadNotes() {
        uiState = NotesUiState.Loading
        viewModelScope.launch(ioDispatcher) {
            val result = repository.getAllNotes()

            launch(Dispatchers.Main) {
                result.onSuccess { loadedNotes ->
                    applyNotes(loadedNotes)
                    reschedule { notificationScheduler.scheduleAll(loadedNotes) }
                }.onFailure { error ->
                    uiState = NotesUiState.Error(ApiErrorMapper.map(error))
                }
            }
        }
    }

    fun addNote(note: Note) {
        viewModelScope.launch(ioDispatcher) {
            val rewardedNote = note.copy(coinCount = coinRewardProvider())
            val result = repository.insert(rewardedNote)
            launch(Dispatchers.Main) {
                result.onSuccess { id ->
                    val noteWithId = rewardedNote.copy(id = id)
                    val notes = currentNotes() + noteWithId
                    applyNotes(notes)
                    reschedule { notificationScheduler.schedule(noteWithId) }
                }.onFailure { error ->
                    showMessage(error)
                }
            }
        }
    }

    fun clearAll() {
        runScheduler { notificationScheduler.cancelAll() }
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
                    reschedule { notificationScheduler.schedule(note) }
                }.onFailure { error ->
                    showMessage(error)
                }
            }
        }
    }

    /**
     * Toggles a single checklist item on a shopping note and persists the whole note.
     * Applied optimistically so the focus overlay reflects the tap immediately; on a
     * backend failure the previous state is restored and the error surfaced.
     */
    fun toggleChecklistItem(noteId: String, index: Int, isChecked: Boolean) {
        val existingNotes = currentNotes()
        val noteIndex = existingNotes.indexOfFirst { it.id == noteId }
        if (noteIndex == -1) return

        val existing = existingNotes[noteIndex]
        if (index !in existing.checklist.indices) return

        val updatedChecklist = existing.checklist.toMutableList().apply {
            this[index] = this[index].copy(isChecked = isChecked)
        }
        val updatedNote = existing.copy(checklist = updatedChecklist)
        val optimisticNotes = existingNotes.toMutableList().apply {
            this[noteIndex] = updatedNote
        }
        applyNotes(optimisticNotes)

        viewModelScope.launch(ioDispatcher) {
            val result = repository.update(updatedNote)
            launch(Dispatchers.Main) {
                result.onFailure { error ->
                    applyNotes(existingNotes)
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
        reschedule { notificationScheduler.schedule(updatedNote) }

        viewModelScope.launch(ioDispatcher) {
            val result = repository.updateCompletion(noteId, isCompleted)
            launch(Dispatchers.Main) {
                result.onFailure { error ->
                    applyNotes(existingNotes)
                    reschedule { notificationScheduler.schedule(existing) }
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
                        runScheduler { notificationScheduler.cancel(noteId) }
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

    // Alarm scheduling touches AlarmManager + SharedPreferences, so it must run off the
    // main thread; [scheduleMutex] serialises those writes (they share one prefs record).
    private val scheduleMutex = Mutex()

    private fun reschedule(block: () -> DeadlineScheduleResult) {
        viewModelScope.launch(ioDispatcher) {
            val result = scheduleMutex.withLock { block() }
            withContext(Dispatchers.Main) { handleScheduleResult(result) }
        }
    }

    private fun runScheduler(block: () -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            scheduleMutex.withLock { block() }
        }
    }

    private fun handleScheduleResult(result: DeadlineScheduleResult) {
        if (!result.exactAlarmPermissionRequired) return
        uiMessage = UiMessage(
            id = UiMessageId.next(),
            text = UiText.StringResource(R.string.exact_alarm_permission_message),
            action = UiMessageAction.OPEN_EXACT_ALARM_SETTINGS,
            actionText = UiText.StringResource(R.string.action_allow)
        )
    }
}
