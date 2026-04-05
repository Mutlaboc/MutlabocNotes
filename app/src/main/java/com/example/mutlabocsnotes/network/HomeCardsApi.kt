package com.example.mutlabocsnotes.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// Retrofit contract with backend endpoints for this feature.
interface HomeCardsApi {
    // Returns data from the current source.
    @GET("home-cards")
    suspend fun getHomeCards(): List<HomeCardDto>

    // Returns data from the current source.
    @GET("home-cards/{id}")
    suspend fun getHomeCardById(
        @Path("id") cardId: String,
    ): HomeCardDto

    // Creates and returns a configured instance.
    @POST("home-cards")
    suspend fun createHomeCard(
        @Body request: HomeCardUpsertRequestDto,
    ): HomeCardDto

    // Updates existing data with new values.
    @PUT("home-cards/{id}")
    suspend fun updateHomeCard(
        @Path("id") cardId: String,
        @Body request: HomeCardUpsertRequestDto,
    ): HomeCardDto

    // Deletes the target entity from storage or backend.
    @DELETE("home-cards/{id}")
    suspend fun deleteHomeCard(
        @Path("id") cardId: String,
    )
}
