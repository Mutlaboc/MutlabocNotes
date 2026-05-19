package com.example.mutlabocsnotes

import com.example.mutlabocsnotes.network.HomeCardDto
import com.example.mutlabocsnotes.network.HomeCardUpsertRequestDto
import com.example.mutlabocsnotes.network.HomeCardsApi
import com.example.mutlabocsnotes.network.HomeFieldDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeInfoRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var api: FakeHomeCardsApi
    private lateinit var repository: HomeInfoRepository

    @Before
    fun setUp() {
        api = FakeHomeCardsApi()
        repository = HomeInfoRepository(api)
    }

    @Test
    fun insert_returnsCanonicalServerCardAndSendsTimestampFreePayload() = runTest(mainDispatcherRule.dispatcher) {
        val clientCard = card(
            title = "Client title",
            createdAt = 123,
            updatedAt = 456
        )
        api.createResponse = cardDto(
            id = "server-id",
            title = "Server title",
            createdAt = 1710000000000,
            updatedAt = 1710000001000
        )

        val result = repository.insert(clientCard)

        assertEquals(
            Result.success(
                card(
                    id = "server-id",
                    title = "Server title",
                    createdAt = 1710000000000,
                    updatedAt = 1710000001000
                )
            ),
            result
        )
        assertEquals("Client title", api.lastCreateRequest?.title)
        assertEquals("METERS", api.lastCreateRequest?.section)
        assertEquals(listOf(HomeFieldDto("serial", "A-1")), api.lastCreateRequest?.fields)
        assertEquals("Keep visible", api.lastCreateRequest?.note)
        assertEquals(listOf("https://example.com/first"), api.lastCreateRequest?.links)
    }

    @Test
    fun update_returnsCanonicalServerCardAndSendsTimestampFreePayload() = runTest(mainDispatcherRule.dispatcher) {
        val clientCard = card(
            id = "server-id",
            title = "Client update",
            createdAt = 123,
            updatedAt = 456
        )
        api.updateResponse = cardDto(
            id = "server-id",
            title = "Server update",
            createdAt = 1710000000000,
            updatedAt = 1710000002000
        )

        val result = repository.update(clientCard)

        assertEquals(
            Result.success(
                card(
                    id = "server-id",
                    title = "Server update",
                    createdAt = 1710000000000,
                    updatedAt = 1710000002000
                )
            ),
            result
        )
        assertEquals("server-id", api.lastUpdateId)
        assertEquals("Client update", api.lastUpdateRequest?.title)
        assertEquals("METERS", api.lastUpdateRequest?.section)
        assertEquals(listOf(HomeFieldDto("serial", "A-1")), api.lastUpdateRequest?.fields)
        assertEquals("Keep visible", api.lastUpdateRequest?.note)
        assertEquals(listOf("https://example.com/first"), api.lastUpdateRequest?.links)
    }

    @Test
    fun update_blankIdReturnsFailureWithoutCallingApi() = runTest(mainDispatcherRule.dispatcher) {
        val result = repository.update(card(id = ""))

        assertTrue(result.isFailure)
        assertEquals(null, api.lastUpdateRequest)
    }

    @Test
    fun getAll_failurePropagatesError() = runTest(mainDispatcherRule.dispatcher) {
        val error = IllegalStateException("backend down")
        api.getAllError = error

        val result = repository.getAllCards()

        assertTrue(result.isFailure)
        assertSame(error, result.exceptionOrNull())
    }

    private fun card(
        id: String = "",
        title: String = "Meter",
        createdAt: Long = 0,
        updatedAt: Long = 0
    ): HomeInfoCard = HomeInfoCard(
        id = id,
        title = title,
        section = HomeSection.METERS,
        fields = listOf(HomeField("serial", "A-1")),
        note = "Keep visible",
        links = listOf("https://example.com/first"),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun cardDto(
        id: String = "server-id",
        title: String = "Meter",
        createdAt: Long = 0,
        updatedAt: Long = 0
    ): HomeCardDto = HomeCardDto(
        id = id,
        title = title,
        section = "METERS",
        fields = listOf(HomeFieldDto("serial", "A-1")),
        note = "Keep visible",
        links = listOf("https://example.com/first"),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

private class FakeHomeCardsApi : HomeCardsApi {
    var cardsResponse: List<HomeCardDto> = emptyList()
    var createResponse: HomeCardDto = cardDto()
    var updateResponse: HomeCardDto = cardDto()
    var getAllError: Exception? = null
    var createError: Exception? = null
    var updateError: Exception? = null
    var deleteError: Exception? = null
    var lastCreateRequest: HomeCardUpsertRequestDto? = null
    var lastUpdateId: String? = null
    var lastUpdateRequest: HomeCardUpsertRequestDto? = null
    var lastDeleteId: String? = null

    override suspend fun getHomeCards(): List<HomeCardDto> {
        getAllError?.let { throw it }
        return cardsResponse
    }

    override suspend fun getHomeCardById(cardId: String): HomeCardDto =
        cardsResponse.first { it.id == cardId }

    override suspend fun createHomeCard(request: HomeCardUpsertRequestDto): HomeCardDto {
        createError?.let { throw it }
        lastCreateRequest = request
        return createResponse
    }

    override suspend fun updateHomeCard(cardId: String, request: HomeCardUpsertRequestDto): HomeCardDto {
        updateError?.let { throw it }
        lastUpdateId = cardId
        lastUpdateRequest = request
        return updateResponse
    }

    override suspend fun deleteHomeCard(cardId: String) {
        deleteError?.let { throw it }
        lastDeleteId = cardId
    }

    private fun cardDto(): HomeCardDto = HomeCardDto(
        id = "server-id",
        title = "Meter",
        section = "METERS",
        fields = listOf(HomeFieldDto("serial", "A-1")),
        note = "Keep visible",
        links = listOf("https://example.com/first"),
        createdAt = 1710000000000,
        updatedAt = 1710000001000
    )
}
