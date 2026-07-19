package app.homenotes.android.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

/**
 * Retrofit-контракт достижений. Бекенд может ещё не поддерживать эти эндпоинты —
 * синк относится к ним толерантно (см. OfflineSyncEngine): 404/405/HTML-заглушка
 * не роняют синхронизацию, приложение полностью работает на локальных данных.
 */
interface AchievementsApi {
    // Снапшот метрик и анлоков текущего пользователя.
    @GET("achievements")
    suspend fun getAchievements(): AchievementsSnapshotDto

    // Полная замена метрик пользователя последним локальным снапшотом (last-write-wins).
    @PUT("achievements/metrics")
    suspend fun putMetrics(@Body request: AchievementMetricsRequestDto): AchievementsAckDto

    // Регистрация взятой ступени; идемпотентно по operationId.
    @POST("achievements/unlocks")
    suspend fun postUnlock(@Body request: AchievementUnlockRequestDto): AchievementsAckDto
}
