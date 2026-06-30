package app.homenotes.android.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

// Retrofit-контракт эндпоинтов листа персонажа.
interface CharacterApi {
    // Получение листа персонажа (бекенд создаёт дефолтный при первом обращении).
    @GET("character")
    suspend fun getCharacter(): CharacterSheetDto

    // Полное обновление листа персонажа.
    @PUT("character")
    suspend fun updateCharacter(
        @Body request: CharacterSheetDto,
    ): CharacterSheetDto

    // Начисление опыта персонажу и (опционально) одному навыку.
    @POST("character/xp")
    suspend fun addExperience(
        @Body request: CharacterXpRequestDto,
    ): CharacterSheetDto
}
