package app.homenotes.android.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// Retrofit-контракт с backend-эндпоинтами.
interface NotesApi {
    // Получение списка заметок.
    @GET("notes")
    suspend fun getNotes(): List<NoteDto>

    // Получение заметки по ID.
    @GET("notes/{id}")
    suspend fun getNoteById(
        @Path("id") noteId: String,
    ): NoteDto

    // Создание заметки.
    @POST("notes")
    suspend fun createNote(
        @Body request: NoteUpsertRequestDto,
    ): NoteDto

    // Обновление заметки.
    @PUT("notes/{id}")
    suspend fun updateNote(
        @Path("id") noteId: String,
        @Body request: NoteUpsertRequestDto,
    ): NoteDto

    @PATCH("notes/{id}/completion")
    suspend fun updateNoteCompletion(
        @Path("id") noteId: String,
        @Body request: NoteCompletionRequestDto,
    )

    // Удаление заметки.
    @DELETE("notes/{id}")
    suspend fun deleteNote(
        @Path("id") noteId: String,
    )
}
