package com.example.mutlabocsnotes.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface NotesApi {
    @GET("notes")
    suspend fun getNotes(): List<NoteDto>

    @GET("notes/{id}")
    suspend fun getNoteById(
        @Path("id") noteId: String,
    ): NoteDto

    @POST("notes")
    suspend fun createNote(
        @Body request: NoteUpsertRequestDto,
    ): NoteDto

    @PUT("notes/{id}")
    suspend fun updateNote(
        @Path("id") noteId: String,
        @Body request: NoteUpsertRequestDto,
    ): NoteDto

    @DELETE("notes/{id}")
    suspend fun deleteNote(
        @Path("id") noteId: String,
    )
}
