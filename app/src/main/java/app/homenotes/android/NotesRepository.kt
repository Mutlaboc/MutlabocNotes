package app.homenotes.android

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface NotesDataSource {
    fun observeNotes(): Flow<List<Note>> = flow { emit(getAllNotes().getOrThrow()) }
    suspend fun getAllNotes(): Result<List<Note>>
    suspend fun insert(note: Note): Result<String>
    suspend fun update(note: Note): Result<Unit>
    suspend fun updateCompletion(noteId: String, isCompleted: Boolean): Result<CompletionUpdate>
    suspend fun delete(noteId: String): Result<Unit>
}

data class CompletionUpdate(
    val completedNote: Note,
    val nextNote: Note?
)
