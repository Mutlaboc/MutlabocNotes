package com.example.mutlabocsnotes

import android.app.Application
import com.example.mutlabocsnotes.network.NotesApi
import com.example.mutlabocsnotes.network.toDomain
import com.example.mutlabocsnotes.network.toUpsertRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Репозиторий Notes API, использующий JWT Bearer-авторизацию из SessionManager.
 */
class NotesRepository(
    application: Application,
    baseUrl: String = ApiConfig.BASE_URL,
    private val api: NotesApi = createNotesApi(application, baseUrl),
) {

    // Возвращает данные из текущего источника.
    suspend fun getAllNotes(): List<Note> = withContext(Dispatchers.IO) {
        return@withContext try {
            api.getNotes().map { it.toDomain() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Добавляет новую сущность в хранилище или backend.
    suspend fun insert(note: Note): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val created = api.createNote(note.toUpsertRequestDto())
            created.id
        } catch (_: Exception) {
            null
        }
    }

    // Обновляет существующие данные новыми значениями.
    suspend fun update(note: Note): Boolean = withContext(Dispatchers.IO) {
        if (note.id.isBlank()) return@withContext false

        return@withContext try {
            api.updateNote(note.id, note.toUpsertRequestDto())
            true
        } catch (_: Exception) {
            false
        }
    }

    // Удаляет целевую сущность из хранилища или backend.
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
        // Создаёт и возвращает настроенный экземпляр.
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
