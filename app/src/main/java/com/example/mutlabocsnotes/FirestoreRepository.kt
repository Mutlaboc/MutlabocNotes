package com.example.mutlabocsnotes

import android.app.Application
import com.example.mutlabocsnotes.network.NotesApi
import com.example.mutlabocsnotes.network.toDomain
import com.example.mutlabocsnotes.network.toUpsertRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class FirestoreRepository(
    application: Application,
    baseUrl: String = ApiConfig.BASE_URL,
    private val bridgeUserKeyProvider: BridgeUserKeyProvider =
        BridgeUserKeyProvider(SessionManager(application)),
    private val api: NotesApi = createNotesApi(baseUrl),
) {

    suspend fun getAllNotes(): List<Note> = withContext(Dispatchers.IO) {
        val bridgeUserKey = bridgeUserKeyProvider.getBridgeUserKeyOrNull()
            ?: return@withContext emptyList()

        return@withContext try {
            api.getNotes(bridgeUserKey).map { it.toDomain() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun insert(note: Note): String? = withContext(Dispatchers.IO) {
        val bridgeUserKey = bridgeUserKeyProvider.getBridgeUserKeyOrNull()
            ?: return@withContext null

        return@withContext try {
            val created = api.createNote(bridgeUserKey, note.toUpsertRequestDto())
            created.id
        } catch (_: Exception) {
            null
        }
    }

    suspend fun update(note: Note): Boolean = withContext(Dispatchers.IO) {
        if (note.id.isBlank()) return@withContext false

        val bridgeUserKey = bridgeUserKeyProvider.getBridgeUserKeyOrNull()
            ?: return@withContext false

        return@withContext try {
            api.updateNote(bridgeUserKey, note.id, note.toUpsertRequestDto())
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun delete(noteId: String): Boolean = withContext(Dispatchers.IO) {
        if (noteId.isBlank()) return@withContext false

        val bridgeUserKey = bridgeUserKeyProvider.getBridgeUserKeyOrNull()
            ?: return@withContext false

        return@withContext try {
            api.deleteNote(bridgeUserKey, noteId)
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