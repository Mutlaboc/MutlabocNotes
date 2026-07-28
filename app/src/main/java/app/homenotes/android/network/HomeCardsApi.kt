package app.homenotes.android.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// Retrofit-контракт с backend-эндпоинтами.
interface HomeCardsApi {
    // Получение карточки дома.
    @GET("home-cards")
    suspend fun getHomeCards(): List<HomeCardDto>

    // Получение карточки дома.
    @GET("home-cards/{id}")
    suspend fun getHomeCardById(
        @Path("id") cardId: String,
    ): HomeCardDto

    // Создание карточки дома.
    @POST("home-cards")
    suspend fun createHomeCard(
        @Body request: HomeCardUpsertRequestDto,
    ): HomeCardDto

    // Обновление карточки дома.
    @PUT("home-cards/{id}")
    suspend fun updateHomeCard(
        @Path("id") cardId: String,
        @Body request: HomeCardUpsertRequestDto,
    ): HomeCardDto

    // Удаление карточки дома.
    @DELETE("home-cards/{id}")
    suspend fun deleteHomeCard(
        @Path("id") cardId: String,
    )
}
