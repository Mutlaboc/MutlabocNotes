package com.example.homenotes

import com.example.homenotes.network.NoteCompletionRequestDto
import com.example.homenotes.network.NotesApi
import com.example.homenotes.network.toDomain
import com.example.homenotes.network.toUpsertRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface NotesDataSource {
    suspend fun getAllNotes(): Result<List<Note>>
    suspend fun insert(note: Note): Result<String>
    suspend fun update(note: Note): Result<Unit>
    suspend fun updateCompletion(noteId: String, isCompleted: Boolean): Result<Unit>
    suspend fun delete(noteId: String): Result<Unit>
}

class NotesRepository(
    private val api: NotesApi,
) : NotesDataSource {

    override suspend fun getAllNotes(): Result<List<Note>> = withContext(Dispatchers.IO) {
        return@withContext try {
            Result.success(api.getNotes().map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun insert(note: Note): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val created = api.createNote(note.toUpsertRequestDto())
            Result.success(created.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun update(note: Note): Result<Unit> = withContext(Dispatchers.IO) {
        if (note.id.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Blank note id"))
        }

        return@withContext try {
            api.updateNote(note.id, note.toUpsertRequestDto())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCompletion(noteId: String, isCompleted: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (noteId.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Blank note id"))
            }

            return@withContext try {
                api.updateNoteCompletion(noteId, NoteCompletionRequestDto(isCompleted))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun delete(noteId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (noteId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Blank note id"))
        }

        return@withContext try {
            api.deleteNote(noteId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
