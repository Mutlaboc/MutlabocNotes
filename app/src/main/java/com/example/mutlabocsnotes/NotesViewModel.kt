package com.example.mutlabocsnotes

import androidx.compose.runtime.mutableStateListOf
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val noteDao = NoteDatabase.getDatabase(application).noteDao()
    private val repository = NoteRepository(noteDao)

    // Локальный кэш заметок, можно сделать LiveData или StateFlow для наблюдения за изменениями
    val notes = mutableStateListOf<Note>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            notes.clear()
            notes.addAll(repository.getAllNotes())

        }
    }


    fun addNote(title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = Note(title = title, content = content)
            val newId = repository.insert(note).toInt()
            val savedNote = note.copy(id = newId)
            launch(Dispatchers.Main) {
                notes.add(savedNote)
            }
        }

    }

    fun updateNote(noteId: Int, title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = Note(id = noteId, title = title, content = content)
            repository.update(note)

        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = notes.find { it.id == noteId }
            if (note != null) {
                repository.delete(note)
                launch(Dispatchers.Main) {
                    notes.remove(note)
                }
            }
        }
    }
}