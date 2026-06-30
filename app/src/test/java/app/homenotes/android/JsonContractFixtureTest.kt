package app.homenotes.android

import app.homenotes.android.network.AuthResponseDto
import app.homenotes.android.network.HomeCardDto
import app.homenotes.android.network.HomeCardUpsertRequestDto
import app.homenotes.android.network.NoteDto
import app.homenotes.android.network.NoteUpsertRequestDto
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class JsonContractFixtureTest {

    private val gson = Gson()

    @Test
    fun authResponseFixtureMatchesAndroidDto() {
        val dto = gson.fromJson(fixture("auth-response.json"), AuthResponseDto::class.java)

        assertEquals("access-token", dto.accessToken)
        assertEquals("refresh-token", dto.refreshToken)
        assertEquals(1800L, dto.expiresInSeconds)
        assertEquals(1209600L, dto.refreshExpiresInSeconds)
        assertEquals("fixture-user@example.com", dto.resolvedEmail())
        assertEquals("local:fixture-user@example.com", dto.user?.bridgeUserKey)
    }

    @Test
    fun noteFixturesMatchAndroidDtos() {
        val response = gson.fromJson(fixture("note-response.json"), NoteDto::class.java)
        val request = gson.fromJson(fixture("note-upsert-request.json"), NoteUpsertRequestDto::class.java)

        assertEquals("Groceries", response.title)
        assertEquals("SHOPPING", response.category)
        assertEquals("Milk", response.checklist.single().text)
        assertEquals(RepeatRule.NONE.name, response.repeatRule)

        assertEquals("Updated task", request.title)
        assertEquals("TASKS", request.category)
        assertEquals(3, request.coinCount)
        assertEquals(RepeatRule.WEEKLY.name, request.repeatRule)
    }

    @Test
    fun homeCardFixturesMatchAndroidDtos() {
        val response = gson.fromJson(fixture("home-card-response.json"), HomeCardDto::class.java)
        val request = gson.fromJson(fixture("home-card-upsert-request.json"), HomeCardUpsertRequestDto::class.java)

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
        val response = gson.fromJson(fixture("error-response.json"), ApiErrorDto::class.java)

        assertEquals("invalid_request", response.code)
        assertEquals("Email is required", response.message)
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource("contracts/$name")) {
            "Missing fixture contracts/$name"
        }.readText()

    private data class ApiErrorDto(
        val code: String? = null,
        val message: String? = null
    )
}
