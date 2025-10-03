package com.example.mutlabocsnotes

import androidx.compose.runtime.mutableStateListOf
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateListOf

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FirestoreRepository()

    // Локальный кэш заметок, можно сделать LiveData или StateFlow для наблюдения за изменениями
    val notes = mutableStateListOf<Note>()

    init {
        if (FirebaseAuth.getInstance().currentUser != null) {
            loadNotes()
        }
    }
    fun loadNotes() {
        viewModelScope.launch(Dispatchers.IO) {
            val loadNotes = repository.getAllNotes()
            launch(Dispatchers.Main) {
                notes.clear()
                notes.addAll(loadNotes)
            }
        }
    }


    fun addNote(note: Note) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insert(note)
            if (id != null) {
                val noteWithId = note.copy(id = id)
                launch(Dispatchers.Main) {
                    notes.add(noteWithId)
                }
            }
        }
    }

    fun updateNote(note: Note) {
        if (note.id.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.update(note)
            if (success) {
                val index = notes.indexOfFirst { it.id == note.id }
                if (index != -1) {
                    launch(Dispatchers.Main) {
                        notes[index] = note
                    }
                }
            }

        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = repository.delete(noteId)
            if (success) {
                val note = notes.find { it.id == noteId }
                if (note != null) {
                    launch(Dispatchers.Main) {
                        notes.remove(note)
                    }
                }
            }
        }
    }
}