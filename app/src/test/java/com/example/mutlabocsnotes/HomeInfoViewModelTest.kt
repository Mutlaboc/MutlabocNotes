package com.example.mutlabocsnotes

import android.app.Application
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeInfoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeHomeInfoDataSource
    private lateinit var viewModel: HomeInfoViewModel

    @Before
    fun setUp() {
        repository = FakeHomeInfoDataSource()
        viewModel = HomeInfoViewModel(
            application = Application(),
            repository = repository,
            ioDispatcher = mainDispatcherRule.dispatcher
        )
    }

    @Test
    fun loadCards_successSortsByUpdatedAtDescending() = runTest(mainDispatcherRule.dispatcher) {
        val older = card(id = "older", updatedAt = 1)
        val newer = card(id = "newer", updatedAt = 3)
        val middle = card(id = "middle", updatedAt = 2)
        repository.cardsResult = Result.success(listOf(older, newer, middle))

        viewModel.loadCards()
        advanceUntilIdle()

        assertEquals(listOf(newer, middle, older), viewModel.cards.toList())
        assertFalse(viewModel.isLoading)
        assertEquals(null, viewModel.errorMessage)
    }

    @Test
    fun loadCards_failureSetsErrorAndStopsLoading() = runTest(mainDispatcherRule.dispatcher) {
        repository.cardsResult = Result.failure(IllegalStateException("Network unavailable"))

        viewModel.loadCards()
        advanceUntilIdle()

        assertTrue(viewModel.cards.isEmpty())
        assertFalse(viewModel.isLoading)
        assertEquals("Network unavailable", viewModel.errorMessage)
    }

    @Test
    fun addCard_successAddsCreatedCardAndKeepsSortOrder() = runTest(mainDispatcherRule.dispatcher) {
        val existing = card(id = "existing", updatedAt = 10)
        val created = card(title = "Created", updatedAt = 20)
        viewModel.cards.add(existing)
        repository.insertResult = Result.success("created-id")

        viewModel.addCard(created)
        advanceUntilIdle()

        assertEquals(listOf(created.copy(id = "created-id"), existing), viewModel.cards.toList())
        assertEquals(listOf(created), repository.insertCalls)
    }

    @Test
    fun addCard_failureDoesNotChangeCardsAndSetsError() = runTest(mainDispatcherRule.dispatcher) {
        val existing = card(id = "existing")
        val created = card(title = "Created")
        viewModel.cards.add(existing)
        repository.insertResult = Result.failure(IllegalStateException("Cannot save"))

        viewModel.addCard(created)
        advanceUntilIdle()

        assertEquals(listOf(existing), viewModel.cards.toList())
        assertEquals("Cannot save", viewModel.errorMessage)
    }

    @Test
    fun updateCard_successReplacesCardAndKeepsSortOrder() = runTest(mainDispatcherRule.dispatcher) {
        val first = card(id = "first", updatedAt = 10)
        val second = card(id = "second", updatedAt = 20)
        val updatedFirst = first.copy(title = "Updated", updatedAt = 30)
        viewModel.cards.addAll(listOf(second, first))
        repository.updateResult = Result.success(Unit)

        viewModel.updateCard(updatedFirst)
        advanceUntilIdle()

        assertEquals(listOf(updatedFirst, second), viewModel.cards.toList())
        assertEquals(listOf(updatedFirst), repository.updateCalls)
    }

    @Test
    fun deleteCard_successRemovesCard() = runTest(mainDispatcherRule.dispatcher) {
        val first = card(id = "first")
        val second = card(id = "second")
        viewModel.cards.addAll(listOf(first, second))
        repository.deleteResult = Result.success(Unit)

        viewModel.deleteCard("first")
        advanceUntilIdle()

        assertEquals(listOf(second), viewModel.cards.toList())
        assertEquals(listOf("first"), repository.deleteCalls)
    }

    private fun card(
        id: String = "",
        title: String = "Card",
        updatedAt: Long = 0L
    ): HomeInfoCard = HomeInfoCard(
        id = id,
        title = title,
        section = HomeSection.OTHER,
        updatedAt = updatedAt
    )
}

private class FakeHomeInfoDataSource : HomeInfoDataSource {
    var cardsResult: Result<List<HomeInfoCard>> = Result.success(emptyList())
    var insertResult: Result<String> = Result.failure(IllegalStateException("not set"))
    var updateResult: Result<Unit> = Result.failure(IllegalStateException("not set"))
    var deleteResult: Result<Unit> = Result.failure(IllegalStateException("not set"))
    val insertCalls = mutableListOf<HomeInfoCard>()
    val updateCalls = mutableListOf<HomeInfoCard>()
    val deleteCalls = mutableListOf<String>()

    override suspend fun getAllCards(): Result<List<HomeInfoCard>> = cardsResult

    override suspend fun insert(card: HomeInfoCard): Result<String> {
        insertCalls.add(card)
        return insertResult
    }

    override suspend fun update(card: HomeInfoCard): Result<Unit> {
        updateCalls.add(card)
        return updateResult
    }

    override suspend fun delete(cardId: String): Result<Unit> {
        deleteCalls.add(cardId)
        return deleteResult
    }
}
