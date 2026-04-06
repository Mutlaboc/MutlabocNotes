package com.example.mutlabocsnotes

import android.app.Application
import com.example.mutlabocsnotes.network.NotesApi
import com.example.mutlabocsnotes.network.toDomain
import com.example.mutlabocsnotes.network.toUpsertRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Notes API repository that uses JWT Bearer authorization from SessionManager.
 */
class NotesRepository(
    application: Application,
    baseUrl: String = ApiConfig.BASE_URL,
    private val api: NotesApi = createNotesApi(application, baseUrl),
) {

    // Returns data from the current source.
    suspend fun getAllNotes(): List<Note> = withContext(Dispatchers.IO) {
        return@withContext try {
            api.getNotes().map { it.toDomain() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Inserts a new entity into storage or backend.
    suspend fun insert(note: Note): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val created = api.createNote(note.toUpsertRequestDto())
            created.id
        } catch (_: Exception) {
            null
        }
    }

    // Updates existing data with new values.
    suspend fun update(note: Note): Boolean = withContext(Dispatchers.IO) {
        if (note.id.isBlank()) return@withContext false

        return@withContext try {
            api.updateNote(note.id, note.toUpsertRequestDto())
            true
        } catch (_: Exception) {
            false
        }
    }

    // Deletes the target entity from storage or backend.
    suspend fun delete(noteId: String): Boolean = withContext(Dispatchers.IO) {
        if (noteId.isBlank()) return@withContext false

        return@withContext try {
            api.deleteNote(noteId)
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        // Creates and returns a configured instance.
        private fun createNotesApi(
            application: Application,
            baseUrl: String
        ): NotesApi {
            return AuthenticatedApiFactory
                .createRetrofit(application, baseUrl)
                .create(NotesApi::class.java)
        }
    }
}
