package app.homenotes.android.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Retrofit-контракт событий фокус-таймера: каталог для кэша и идемпотентный клейм наград.
interface EventsApi {
    // Полный каталог событий (глобальный, кэшируется в Room для офлайна).
    @GET("events")
    suspend fun getCatalog(): FocusEventsCatalogDto

    // Начисление наград сролленного клиентом события; идемпотентно по operationId.
    @POST("events/claims")
    suspend fun claim(@Body request: FocusEventClaimRequestDto): FocusEventClaimResponseDto
}
