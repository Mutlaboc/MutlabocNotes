package app.homenotes.android.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Retrofit-контракт эндпоинтов инвентаря (см. docs/INVENTORY_API_CONTRACT.md).
interface InventoryApi {
    // Получение инвентаря (бекенд создаёт стартовый набор при первом обращении).
    @GET("inventory")
    suspend fun getInventory(): InventoryDto

    // Надеть предмет в его слот; предмет, занимавший слот, снимается автоматически.
    @POST("inventory/equip")
    suspend fun equip(@Body request: InventoryEquipRequestDto): InventoryDto

    // Снять предмет из слота.
    @POST("inventory/unequip")
    suspend fun unequip(@Body request: InventoryUnequipRequestDto): InventoryDto
}
