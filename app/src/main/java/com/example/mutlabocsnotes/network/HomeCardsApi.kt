package com.example.mutlabocsnotes.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// Retrofit-контракт с backend-эндпоинтами этой функциональности.
interface HomeCardsApi {
    // Возвращает данные из текущего источника.
    @GET("home-cards")
    suspend fun getHomeCards(): List<HomeCardDto>

    // Возвращает данные из текущего источника.
    @GET("home-cards/{id}")
    suspend fun getHomeCardById(
        @Path("id") cardId: String,
    ): HomeCardDto

    // Создаёт и возвращает настроенный экземпляр.
    @POST("home-cards")
    suspend fun createHomeCard(
        @Body request: HomeCardUpsertRequestDto,
    ): HomeCardDto

    // Обновляет существующие данные новыми значениями.
    @PUT("home-cards/{id}")
    suspend fun updateHomeCard(
        @Path("id") cardId: String,
        @Body request: HomeCardUpsertRequestDto,
    ): HomeCardDto

    // Удаляет целевую сущность из хранилища или backend.
    @DELETE("home-cards/{id}")
    suspend fun deleteHomeCard(
        @Path("id") cardId: String,
    )
}
