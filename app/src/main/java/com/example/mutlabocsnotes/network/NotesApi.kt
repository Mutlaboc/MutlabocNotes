package com.example.mutlabocsnotes.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// Retrofit contract with backend endpoints for this feature.
interface NotesApi {
    // Returns data from the current source.
    @GET("notes")
    suspend fun getNotes(): List<NoteDto>

    // Returns data from the current source.
    @GET("notes/{id}")
    suspend fun getNoteById(
        @Path("id") noteId: String,
    ): NoteDto

    // Creates and returns a configured instance.
    @POST("notes")
    suspend fun createNote(
        @Body request: NoteUpsertRequestDto,
    ): NoteDto

    // Updates existing data with new values.
    @PUT("notes/{id}")
    suspend fun updateNote(
        @Path("id") noteId: String,
        @Body request: NoteUpsertRequestDto,
    ): NoteDto

    // Deletes the target entity from storage or backend.
    @DELETE("notes/{id}")
    suspend fun deleteNote(
        @Path("id") noteId: String,
    )
}
