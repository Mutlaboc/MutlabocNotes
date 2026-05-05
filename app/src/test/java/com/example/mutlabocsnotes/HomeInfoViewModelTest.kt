package com.example.mutlabocsnotes

import android.app.Application
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

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
    fun loadCards_successSetsContentStateSortedByUpdatedAtDescending() = runTest(mainDispatcherRule.dispatcher) {
        val older = card(id = "older", updatedAt = 1)
        val newer = card(id = "newer", updatedAt = 3)
        val middle = card(id = "middle", updatedAt = 2)
        repository.cardsResult = Result.success(listOf(older, newer, middle))

        viewModel.loadCards()
        advanceUntilIdle()

        val state = viewModel.uiState as HomeInfoUiState.Content
        assertEquals(listOf(newer, middle, older), state.cards)
        assertEquals(null, viewModel.uiMessage)
    }

    @Test
    fun loadCards_emptySuccessSetsEmptyState() = runTest(mainDispatcherRule.dispatcher) {
        repository.cardsResult = Result.success(emptyList())

        viewModel.loadCards()
        advanceUntilIdle()

        assertEquals(HomeInfoUiState.Empty, viewModel.uiState)
    }

    @Test
    fun loadCards_failureSetsInlineErrorState() = runTest(mainDispatcherRule.dispatcher) {
        repository.cardsResult = Result.failure(IOException("offline"))

        viewModel.loadCards()
        advanceUntilIdle()

        val state = viewModel.uiState as HomeInfoUiState.Error
        assertStringResource(R.string.api_error_network, state.message)
    }

    @Test
    fun loadCards_retryCanRecoverFromErrorToContent() = runTest(mainDispatcherRule.dispatcher) {
        val loaded = card(id = "recovered")
        repository.cardsResult = Result.failure(IOException("offline"))

        viewModel.loadCards()
        advanceUntilIdle()
        assertTrue(viewModel.uiState is HomeInfoUiState.Error)

        repository.cardsResult = Result.success(listOf(loaded))
        viewModel.loadCards()
        advanceUntilIdle()

        val state = viewModel.uiState as HomeInfoUiState.Content
        assertEquals(listOf(loaded), state.cards)
    }

    @Test
    fun addCard_successAddsCreatedCardAndKeepsSortOrder() = runTest(mainDispatcherRule.dispatcher) {
        val existing = card(id = "existing", updatedAt = 10)
        val created = card(title = "Created", updatedAt = 20)
        loadContent(existing)
        repository.insertResult = Result.success("created-id")

        viewModel.addCard(created)
        advanceUntilIdle()

        val state = viewModel.uiState as HomeInfoUiState.Content
        assertEquals(listOf(created.copy(id = "created-id"), existing), state.cards)
        assertEquals(listOf(created), repository.insertCalls)
        assertEquals(null, viewModel.uiMessage)
    }

    @Test
    fun addCard_failureKeepsCurrentStateAndEmitsSnackbarMessage() = runTest(mainDispatcherRule.dispatcher) {
        val existing = card(id = "existing")
        loadContent(existing)
        repository.insertResult = Result.failure(IOException("offline"))

        viewModel.addCard(card(title = "Created"))
        advanceUntilIdle()

        val state = viewModel.uiState as HomeInfoUiState.Content
        assertEquals(listOf(existing), state.cards)
        assertStringResource(R.string.api_error_network, checkNotNull(viewModel.uiMessage).text)
    }

    @Test
    fun updateCard_successReplacesCardAndKeepsSortOrder() = runTest(mainDispatcherRule.dispatcher) {
        val first = card(id = "first", updatedAt = 10)
        val second = card(id = "second", updatedAt = 20)
        val updatedFirst = first.copy(title = "Updated", updatedAt = 30)
        loadContent(second, first)
        repository.updateResult = Result.success(Unit)

        viewModel.updateCard(updatedFirst)
        advanceUntilIdle()

        val state = viewModel.uiState as HomeInfoUiState.Content
        assertEquals(listOf(updatedFirst, second), state.cards)
        assertEquals(listOf(updatedFirst), repository.updateCalls)
    }

    @Test
    fun updateCard_failureKeepsCurrentStateAndEmitsSnackbarMessage() = runTest(mainDispatcherRule.dispatcher) {
        val existing = card(id = "card")
        loadContent(existing)
        repository.updateResult = Result.failure(IOException("offline"))

        viewModel.updateCard(existing.copy(title = "Updated"))
        advanceUntilIdle()

        val state = viewModel.uiState as HomeInfoUiState.Content
        assertEquals(listOf(existing), state.cards)
        assertStringResource(R.string.api_error_network, checkNotNull(viewModel.uiMessage).text)
    }

    @Test
    fun deleteCard_successRemovesCard() = runTest(mainDispatcherRule.dispatcher) {
        val first = card(id = "first")
        val second = card(id = "second")
        loadContent(first, second)
        repository.deleteResult = Result.success(Unit)

        viewModel.deleteCard("first")
        advanceUntilIdle()

        val state = viewModel.uiState as HomeInfoUiState.Content
        assertEquals(listOf(second), state.cards)
        assertEquals(listOf("first"), repository.deleteCalls)
    }

    @Test
    fun deleteCard_failureKeepsCurrentStateAndEmitsSnackbarMessage() = runTest(mainDispatcherRule.dispatcher) {
        val existing = card(id = "first")
        loadContent(existing)
        repository.deleteResult = Result.failure(IOException("offline"))

        viewModel.deleteCard("first")
        advanceUntilIdle()

        val state = viewModel.uiState as HomeInfoUiState.Content
        assertEquals(listOf(existing), state.cards)
        assertEquals(listOf("first"), repository.deleteCalls)
        assertStringResource(R.string.api_error_network, checkNotNull(viewModel.uiMessage).text)
    }

    @Test
    fun messageShownClearsMatchingSnackbarMessage() = runTest(mainDispatcherRule.dispatcher) {
        repository.insertResult = Result.failure(IOException("offline"))

        viewModel.addCard(card(title = "Created"))
        advanceUntilIdle()

        val messageId = checkNotNull(viewModel.uiMessage).id
        viewModel.onMessageShown(messageId)

        assertEquals(null, viewModel.uiMessage)
    }

    private fun loadContent(vararg cards: HomeInfoCard) {
        repository.cardsResult = Result.success(cards.toList())
        viewModel.loadCards()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
    }

    private fun assertStringResource(expectedResId: Int, text: UiText) {
        val resource = text as UiText.StringResource
        assertEquals(expectedResId, resource.resId)
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
