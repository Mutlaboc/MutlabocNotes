package app.homenotes.android

import app.homenotes.android.network.ApiJson
import app.homenotes.android.network.AuthResponseDto
import app.homenotes.android.network.HomeCardDto
import app.homenotes.android.network.HomeCardUpsertRequestDto
import app.homenotes.android.network.InventoryDto
import app.homenotes.android.network.toDomain
import app.homenotes.android.network.NoteDto
import app.homenotes.android.network.NoteUpsertRequestDto
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Test

class JsonContractFixtureTest {

    @Test
    fun authResponseFixtureMatchesAndroidDto() {
        val dto = ApiJson.decodeFromString<AuthResponseDto>(fixture("auth-response.json"))

        assertEquals("access-token", dto.accessToken)
        assertEquals("refresh-token", dto.refreshToken)
        assertEquals(1800L, dto.expiresInSeconds)
        assertEquals(1209600L, dto.refreshExpiresInSeconds)
        assertEquals("fixture-user@example.com", dto.resolvedEmail())
        assertEquals("local:fixture-user@example.com", dto.user?.bridgeUserKey)
    }

    @Test
    fun noteFixturesMatchAndroidDtos() {
        val response = ApiJson.decodeFromString<NoteDto>(fixture("note-response.json"))
        val request = ApiJson.decodeFromString<NoteUpsertRequestDto>(fixture("note-upsert-request.json"))

        assertEquals("Groceries", response.title)
        assertEquals("SHOPPING", response.category)
        assertEquals("Milk", response.checklist.single().text)
        assertEquals(RepeatRule.NONE.name, response.repeatRule)

        assertEquals("Updated task", request.title)
        assertEquals("RECURRING_TASKS", request.category)
        assertEquals(60L, request.durationMinutes)
        assertEquals(3, request.coinCount)
        assertEquals(RepeatRule.WEEKLY.name, request.repeatRule)
    }

    @Test
    fun homeCardFixturesMatchAndroidDtos() {
        val response = ApiJson.decodeFromString<HomeCardDto>(fixture("home-card-response.json"))
        val request = ApiJson.decodeFromString<HomeCardUpsertRequestDto>(fixture("home-card-upsert-request.json"))

        assertEquals("Meter", response.title)
        assertEquals("METERS", response.section)
        assertEquals("serial", response.fields.single().key)
        assertEquals(1710000000000L, response.createdAt)
        assertEquals(1710000001000L, response.updatedAt)

        assertEquals("Manuals", request.title)
        assertEquals("DOCUMENTS", request.section)
        assertEquals("serial", request.fields.single().key)
        assertEquals("A-1", request.fields.single().value)
        assertEquals("Changed", request.note)
        assertEquals(listOf("https://example.com/first"), request.links)
    }

    @Test
    fun errorFixtureMatchesAndroidErrorShape() {
        val response = ApiJson.decodeFromString<ApiErrorDto>(fixture("error-response.json"))

        assertEquals("invalid_request", response.code)
        assertEquals("Email is required", response.message)
    }

    @Test
    fun inventoryFixtureMatchesAndroidDto() {
        val dto = ApiJson.decodeFromString<InventoryDto>(fixture("inventory-response.json"))
        val inventory = dto.toDomain()

        val helmet = inventory.items.first { it.id == "iron-helmet" }
        assertEquals(EquipSlot.HEAD, helmet.slot)
        assertEquals(EquipSlot.HEAD, helmet.equippedSlot)
        assertEquals(ItemRarity.COMMON, helmet.rarity)
        assertEquals(1, helmet.bonuses.single().value)
        assertEquals("con", helmet.bonuses.single().statKey)

        val acorn = inventory.items.first { it.id == "lucky-acorn" }
        assertEquals(null, acorn.slot)
        assertEquals(ItemRarity.RARE, acorn.rarity)
        assertEquals(false, acorn.isEquippable)
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("contracts/$name")) {
            "Missing fixture contracts/$name"
        }.readText()

    @Serializable
    private data class ApiErrorDto(
        val code: String? = null,
        val message: String? = null
    )
}
