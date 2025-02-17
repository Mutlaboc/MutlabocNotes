package com.example.mutlabocsnotes

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class NotesViewModel : ViewModel() {
    private val _notes = mutableStateListOf<Note>()
    val notes: List<Note> get() = _notes

    fun addNote(title:String, content: String) {
        val newId = if (_notes.isEmpty()) 1 else _notes.maxOf {it.id} + 1
        _notes.add(Note(newId, title, content))

    }
    fun updateNote(noteId: Int, title:String, content: String) {
        val index = _notes.indexOfFirst { it.id == noteId }
        if (index != -1) {
        _notes[index] = Note(noteId, title, content)
    }
    }
}