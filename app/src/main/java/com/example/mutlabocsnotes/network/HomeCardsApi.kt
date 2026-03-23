package com.example.mutlabocsnotes.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface HomeCardsApi {
    @GET("home-cards")
    suspend fun getHomeCards(
        @Header("X-Firebase-Uid") firebaseUid: String,
    ): List<HomeCardDto>

    @GET("home-cards/{id}")
    suspend fun getHomeCardById(
        @Header("X-Firebase-Uid") firebaseUid: String,
        @Path("id") cardId: String,
    ): HomeCardDto

    @POST("home-cards")
    suspend fun createHomeCard(
        @Header("X-Firebase-Uid") firebaseUid: String,
        @Body request: HomeCardUpsertRequestDto,
    ): HomeCardDto

    @PUT("home-cards/{id}")
    suspend fun updateHomeCard(
        @Header("X-Firebase-Uid") firebaseUid: String,
        @Path("id") cardId: String,
        @Body request: HomeCardUpsertRequestDto,
    ): HomeCardDto

    @DELETE("home-cards/{id}")
    suspend fun deleteHomeCard(
        @Header("X-Firebase-Uid") firebaseUid: String,
        @Path("id") cardId: String,
    )
}
