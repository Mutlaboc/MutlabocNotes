package com.example.mutlabocsnotes.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// Retrofit-контракт с backend-эндпоинтами этой функциональности.
interface NotesApi {
    // Возвращает данные из текущего источника.
    @GET("notes")
    suspend fun getNotes(): List<NoteDto>

    // Возвращает данные из текущего источника.
    @GET("notes/{id}")
    suspend fun getNoteById(
        @Path("id") noteId: String,
    ): NoteDto

    // Создаёт и возвращает настроенный экземпляр.
    @POST("notes")
    suspend fun createNote(
        @Body request: NoteUpsertRequestDto,
    ): NoteDto

    // Обновляет существующие данные новыми значениями.
    @PUT("notes/{id}")
    suspend fun updateNote(
        @Path("id") noteId: String,
        @Body request: NoteUpsertRequestDto,
    ): NoteDto

    // Удаляет целевую сущность из хранилища или backend.
    @DELETE("notes/{id}")
    suspend fun deleteNote(
        @Path("id") noteId: String,
    )
}
