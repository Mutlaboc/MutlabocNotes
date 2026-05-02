package com.example.mutlabocsnotes

import com.example.mutlabocsnotes.network.NotesApi
import com.example.mutlabocsnotes.network.toDomain
import com.example.mutlabocsnotes.network.toUpsertRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface NotesDataSource {
    suspend fun getAllNotes(): List<Note>
    suspend fun insert(note: Note): String?
    suspend fun update(note: Note): Boolean
    suspend fun delete(noteId: String): Boolean
}

// Репозиторий отвечает только за операции Notes API.
// Настройка Retrofit и авторизация приходят извне через AppContainer.
class NotesRepository(
    private val api: NotesApi,
) : NotesDataSource {

    // Загружает заметки текущего пользователя; при ошибке отдаёт пустой список, как и раньше.
    override suspend fun getAllNotes(): List<Note> = withContext(Dispatchers.IO) {
        return@withContext try {
            api.getNotes().map { it.toDomain() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // Создаёт заметку на backend и возвращает id, выданный сервером.
    override suspend fun insert(note: Note): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val created = api.createNote(note.toUpsertRequestDto())
            created.id
        } catch (_: Exception) {
            null
        }
    }

    // Обновляет существующую заметку; пустой id не отправляем в сеть.
    override suspend fun update(note: Note): Boolean = withContext(Dispatchers.IO) {
        if (note.id.isBlank()) return@withContext false

        return@withContext try {
            api.updateNote(note.id, note.toUpsertRequestDto())
            true
        } catch (_: Exception) {
            false
        }
    }

    // Удаляет заметку по id; результат сообщает ViewModel, можно ли менять локальное состояние.
    override suspend fun delete(noteId: String): Boolean = withContext(Dispatchers.IO) {
        if (noteId.isBlank()) return@withContext false

        return@withContext try {
            api.deleteNote(noteId)
            true
        } catch (_: Exception) {
            false
        }
    }
}
