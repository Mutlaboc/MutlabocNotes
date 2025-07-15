package com.example.mutlabocsnotes

import androidx.compose.runtime.mutableStateListOf
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FirestoreRepository()

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
            val id = repository.insert(title, content)
            val note = Note(id = id, title = title, content = content)
            launch(Dispatchers.Main) {
                notes.add(note)
            }
        }

    }

    fun updateNote(noteId: String, title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(noteId, title, content)

        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(noteId)
            val note = notes.find { it.id == noteId }
            if (note != null) {
                launch(Dispatchers.Main) {
                    notes.remove(note)
                }
            }
        }
    }
}