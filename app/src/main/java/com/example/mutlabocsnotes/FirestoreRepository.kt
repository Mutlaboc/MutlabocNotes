package com.example.mutlabocsnotes

import android.app.Application
import com.example.mutlabocsnotes.network.NotesApi
import com.example.mutlabocsnotes.network.toDomain
import com.example.mutlabocsnotes.network.toUpsertRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Реализация этапа 9:
 * Notes API теперь авторизуется через JWT Bearer token,
 * который OkHttp interceptor берёт из SessionManager.
 */
class FirestoreRepository(
    application: Application,
    baseUrl: String = ApiConfig.BASE_URL,
    private val api: NotesApi = createNotesApi(application, baseUrl),
) {

    suspend fun getAllNotes(): List<Note> = withContext(Dispatchers.IO) {
        return@withContext try {
            api.getNotes().map { it.toDomain() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun insert(note: Note): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val created = api.createNote(note.toUpsertRequestDto())
            created.id
        } catch (_: Exception) {
            null
        }
    }

    suspend fun update(note: Note): Boolean = withContext(Dispatchers.IO) {
        if (note.id.isBlank()) return@withContext false

        return@withContext try {
            api.updateNote(note.id, note.toUpsertRequestDto())
            true
        } catch (_: Exception) {
            false
        }
    }

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
