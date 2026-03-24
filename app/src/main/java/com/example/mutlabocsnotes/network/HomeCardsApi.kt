package com.example.mutlabocsnotes.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface HomeCardsApi {
    @GET("home-cards")
    suspend fun getHomeCards(): List<HomeCardDto>

    @GET("home-cards/{id}")
    suspend fun getHomeCardById(
        @Path("id") cardId: String,
    ): HomeCardDto

    @POST("home-cards")
    suspend fun createHomeCard(
        @Body request: HomeCardUpsertRequestDto,
    ): HomeCardDto

    @PUT("home-cards/{id}")
    suspend fun updateHomeCard(
        @Path("id") cardId: String,
        @Body request: HomeCardUpsertRequestDto,
    ): HomeCardDto

    @DELETE("home-cards/{id}")
    suspend fun deleteHomeCard(
        @Path("id") cardId: String,
    )
}
