package com.example.mutlabocsnotes

import com.example.mutlabocsnotes.network.NotesApi
import com.example.mutlabocsnotes.network.toDomain
import com.example.mutlabocsnotes.network.toUpsertRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Переходная реализация для этапа 5.
 *
 * Имя класса оставлено прежним, чтобы не ломать NotesViewModel и экранный слой.
 * Внутри вместо Firestore теперь используется backend Notes API.
 */
class FirestoreRepository(
    baseUrl: String = ApiConfig.BASE_URL,
    private val firebaseUidProvider: FirebaseUidProvider = FirebaseUidProvider(),
    private val api: NotesApi = createNotesApi(baseUrl),
) {

    suspend fun getAllNotes(): List<Note> = withContext(Dispatchers.IO) {
        val uid = firebaseUidProvider.getUidOrNull() ?: return@withContext emptyList()
        return@withContext try {
            api.getNotes(uid).map { it.toDomain() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun insert(note: Note): String? = withContext(Dispatchers.IO) {
        val uid = firebaseUidProvider.getUidOrNull() ?: return@withContext null
        return@withContext try {
            val created = api.createNote(uid, note.toUpsertRequestDto())
            created.id
        } catch (_: Exception) {
            null
        }
    }

    suspend fun update(note: Note): Boolean = withContext(Dispatchers.IO) {
        if (note.id.isBlank()) return@withContext false
        val uid = firebaseUidProvider.getUidOrNull() ?: return@withContext false
        return@withContext try {
            api.updateNote(uid, note.id, note.toUpsertRequestDto())
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun delete(noteId: String): Boolean = withContext(Dispatchers.IO) {
        if (noteId.isBlank()) return@withContext false
        val uid = firebaseUidProvider.getUidOrNull() ?: return@withContext false
        return@withContext try {
            api.deleteNote(uid, noteId)
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private fun createNotesApi(baseUrl: String): NotesApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NotesApi::class.java)
        }
    }
}
