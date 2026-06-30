package com.example.homenotes

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
class NotesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeNotesDataSource
    private lateinit var scheduler: FakeDeadlineScheduler
    private lateinit var viewModel: NotesViewModel

    @Before
    fun setUp() {
        repository = FakeNotesDataSource()
        scheduler = FakeDeadlineScheduler()
        viewModel = NotesViewModel(
            application = Application(),
            repository = repository,
            notificationScheduler = scheduler,
            ioDispatcher = mainDispatcherRule.dispatcher,
            coinRewardProvider = { 2 }
        )
    }

    @Test
    fun loadNotes_successSetsContentStateAndSchedulesAll() = runTest(mainDispatcherRule.dispatcher) {
        val completed = note(id = "1", isCompleted = true, coinCount = 5)
        val open = note(id = "2", isCompleted = false, coinCount = 7)
        repository.notesResult = Result.success(listOf(completed, open))

        viewModel.loadNotes()
        advanceUntilIdle()

        val state = viewModel.uiState as NotesUiState.Content
        assertEquals(listOf(completed, open), state.notes)
        assertEquals(5, state.totalCoins)
        assertEquals(listOf(listOf(completed, open)), scheduler.scheduleAllCalls)
        assertEquals(null, viewModel.uiMessage)
    }

    @Test
    fun loadNotes_emptySuccessSetsEmptyState() = runTest(mainDispatcherRule.dispatcher) {
        repository.notesResult = Result.success(emptyList())

        viewModel.loadNotes()
        advanceUntilIdle()

        assertEquals(NotesUiState.Empty, viewModel.uiState)
        assertEquals(null, viewModel.uiMessage)
    }

    @Test
    fun loadNotes_setsLoadingStateBeforeRepositoryReturns() = runTest(mainDispatcherRule.dispatcher) {
        loadContent(note(id = "existing"))
        repository.notesResult = Result.success(emptyList())

        viewModel.loadNotes()

        assertEquals(NotesUiState.Loading, viewModel.uiState)

        advanceUntilIdle()
        assertEquals(NotesUiState.Empty, viewModel.uiState)
    }

    @Test
    fun loadNotes_failureSetsInlineErrorState() = runTest(mainDispatcherRule.dispatcher) {
        repository.notesResult = Result.failure(IOException("offline"))

        viewModel.loadNotes()
        advanceUntilIdle()

        val state = viewModel.uiState as NotesUiState.Error
        assertStringResource(R.string.api_error_network, state.message)
        assertTrue(scheduler.scheduleAllCalls.isEmpty())
    }

    @Test
    fun loadNotes_retryCanRecoverFromErrorToContent() = runTest(mainDispatcherRule.dispatcher) {
        val loaded = note(id = "recovered")
        repository.notesResult = Result.failure(IOException("offline"))

        viewModel.loadNotes()
        advanceUntilIdle()
        assertTrue(viewModel.uiState is NotesUiState.Error)

        repository.notesResult = Result.success(listOf(loaded))
        viewModel.loadNotes()
        advanceUntilIdle()

        val state = viewModel.uiState as NotesUiState.Content
        assertEquals(listOf(loaded), state.notes)
    }

    @Test
    fun addNote_successAddsCreatedNoteAndSchedulesIt() = runTest(mainDispatcherRule.dispatcher) {
        val created = note(title = "Created")
        repository.insertResult = Result.success("created-id")

        viewModel.addNote(created)
        advanceUntilIdle()

        val expected = created.copy(id = "created-id", coinCount = 2)
        val state = viewModel.uiState as NotesUiState.Content
        assertEquals(listOf(expected), state.notes)
        assertEquals(listOf(created.copy(coinCount = 2)), repository.insertCalls)
        assertEquals(listOf(expected), scheduler.scheduleCalls)
        assertEquals(null, viewModel.uiMessage)
    }

    @Test
    fun addNote_inexactFallbackShowsExactAlarmPermissionMessage() = runTest(mainDispatcherRule.dispatcher) {
        val created = note(title = "Created")
        repository.insertResult = Result.success("created-id")
        scheduler.scheduleResult = DeadlineScheduleResult.ScheduledInexactPermissionRequired

        viewModel.addNote(created)
        advanceUntilIdle()

        val message = checkNotNull(viewModel.uiMessage)
        assertStringResource(R.string.exact_alarm_permission_message, message.text)
        assertEquals(UiMessageAction.OPEN_EXACT_ALARM_SETTINGS, message.action)
        assertStringResource(R.string.action_allow, checkNotNull(message.actionText))
    }

    @Test
    fun addNote_exactScheduleDoesNotShowPermissionMessage() = runTest(mainDispatcherRule.dispatcher) {
        repository.insertResult = Result.success("created-id")
        scheduler.scheduleResult = DeadlineScheduleResult.ScheduledExact

        viewModel.addNote(note(title = "Created"))
        advanceUntilIdle()

        assertEquals(null, viewModel.uiMessage)
    }

    @Test
    fun addNote_failureKeepsCurrentStateAndEmitsSnackbarMessage() = runTest(mainDispatcherRule.dispatcher) {
        val existing = note(id = "existing")
        loadContent(existing)
        repository.insertResult = Result.failure(IOException("offline"))

        viewModel.addNote(note(title = "Created"))
        advanceUntilIdle()

        val state = viewModel.uiState as NotesUiState.Content
        assertEquals(listOf(existing), state.notes)
        assertStringResource(R.string.api_error_network, checkNotNull(viewModel.uiMessage).text)
        assertEquals(listOf(existing), scheduler.scheduleCalls)
    }

    @Test
    fun updateNote_successReplacesNoteAndSchedulesIt() = runTest(mainDispatcherRule.dispatcher) {
        val existing = note(id = "update-me", title = "Original", isCompleted = false, coinCount = 3)
        val updated = existing.copy(title = "Updated", isCompleted = true, coinCount = 7)
        repository.updateResult = Result.success(Unit)
        loadContent(existing)

        viewModel.updateNote(updated)
        advanceUntilIdle()

        val state = viewModel.uiState as NotesUiState.Content
        assertEquals(listOf(updated), state.notes)
        assertEquals(7, state.totalCoins)
        assertEquals(listOf(updated), repository.updateCalls)
        assertEquals(listOf(updated), scheduler.scheduleCalls.drop(1))
        assertEquals(null, viewModel.uiMessage)
    }

    @Test
    fun updateNote_failureKeepsCurrentStateAndEmitsSnackbarMessage() = runTest(mainDispatcherRule.dispatcher) {
        val existing = note(id = "update-me", title = "Original")
        val updated = existing.copy(title = "Updated")
        repository.updateResult = Result.failure(IOException("offline"))
        loadContent(existing)

        viewModel.updateNote(updated)
        advanceUntilIdle()

        val state = viewModel.uiState as NotesUiState.Content
        assertEquals(listOf(existing), state.notes)
        assertEquals(listOf(updated), repository.updateCalls)
        assertEquals(listOf(existing), scheduler.scheduleCalls)
        assertStringResource(R.string.api_error_network, checkNotNull(viewModel.uiMessage).text)
    }

    @Test
    fun messageShownClearsMatchingSnackbarMessage() = runTest(mainDispatcherRule.dispatcher) {
        repository.insertResult = Result.failure(IOException("offline"))

        viewModel.addNote(note(title = "Created"))
        advanceUntilIdle()

        val messageId = checkNotNull(viewModel.uiMessage).id
        viewModel.onMessageShown(messageId)

        assertEquals(null, viewModel.uiMessage)
    }

    @Test
    fun setNoteCompletion_successOptimisticallyUpdatesAndPersists() = runTest(mainDispatcherRule.dispatcher) {
        val existing = note(id = "task", isCompleted = false, coinCount = 3)
        loadContent(existing)

        viewModel.setNoteCompletion("task", true)

        val optimistic = existing.copy(isCompleted = true)
        var state = viewModel.uiState as NotesUiState.Content
        assertEquals(listOf(optimistic), state.notes)
        assertEquals(3, state.totalCoins)

        advanceUntilIdle()

        state = viewModel.uiState as NotesUiState.Content
        assertEquals(listOf(optimistic), state.notes)
        assertEquals(listOf("task" to true), repository.updateCompletionCalls)
        assertTrue(repository.updateCalls.isEmpty())
        assertEquals(listOf(optimistic), scheduler.scheduleCalls.drop(1))
        assertEquals(null, viewModel.uiMessage)
    }

    @Test
    fun setNoteCompletion_failureRollsBackStateAndEmitsSnackbarMessage() = runTest(mainDispatcherRule.dispatcher) {
        val existing = note(id = "task", isCompleted = false, coinCount = 3)
        repository.updateCompletionResult = Result.failure(IOException("offline"))
        loadContent(existing)

        viewModel.setNoteCompletion("task", true)
        advanceUntilIdle()

        val optimistic = existing.copy(isCompleted = true)
        val state = viewModel.uiState as NotesUiState.Content
        assertEquals(listOf(existing), state.notes)
        assertEquals(0, state.totalCoins)
        assertEquals(listOf("task" to true), repository.updateCompletionCalls)
        assertTrue(repository.updateCalls.isEmpty())
        assertEquals(listOf(optimistic, existing), scheduler.scheduleCalls.drop(1))
        assertStringResource(R.string.api_error_network, checkNotNull(viewModel.uiMessage).text)
    }

    @Test
    fun deleteNote_successRemovesNoteAndCancelsSchedule() = runTest(mainDispatcherRule.dispatcher) {
        val existing = note(id = "delete-me")
        repository.deleteResult = Result.success(Unit)
        loadContent(existing)

        viewModel.deleteNote("delete-me")
        advanceUntilIdle()

        assertEquals(NotesUiState.Empty, viewModel.uiState)
        assertEquals(listOf("delete-me"), repository.deleteCalls)
        assertEquals(listOf("delete-me"), scheduler.cancelCalls)
        assertEquals(null, viewModel.uiMessage)
    }

    @Test
    fun deleteNote_failureKeepsCurrentStateAndEmitsSnackbarMessage() = runTest(mainDispatcherRule.dispatcher) {
        val existing = note(id = "delete-me")
        repository.deleteResult = Result.failure(IOException("offline"))
        loadContent(existing)

        viewModel.deleteNote("delete-me")
        advanceUntilIdle()

        val state = viewModel.uiState as NotesUiState.Content
        assertEquals(listOf(existing), state.notes)
        assertEquals(listOf("delete-me"), repository.deleteCalls)
        assertTrue(scheduler.cancelCalls.isEmpty())
        assertStringResource(R.string.api_error_network, checkNotNull(viewModel.uiMessage).text)
    }

    @Test
    fun clearAllClearsStateAndCancelsAllSchedules() = runTest(mainDispatcherRule.dispatcher) {
        val existing = note(id = "scheduled")
        scheduler.scheduleAllResult = DeadlineScheduleResult.ScheduledInexactPermissionRequired
        loadContent(existing)
        checkNotNull(viewModel.uiMessage)

        viewModel.clearAll()
        advanceUntilIdle()

        assertEquals(NotesUiState.Empty, viewModel.uiState)
        assertEquals(null, viewModel.uiMessage)
        assertEquals(1, scheduler.cancelAllCalls)
    }

    private fun loadContent(vararg notes: Note) {
        repository.notesResult = Result.success(notes.toList())
        viewModel.loadNotes()
        mainDispatcherRule.dispatcher.scheduler.advanceUntilIdle()
    }

    private fun assertStringResource(expectedResId: Int, text: UiText) {
        val resource = text as UiText.StringResource
        assertEquals(expectedResId, resource.resId)
    }

    private fun note(
        id: String = "",
        title: String = "Note",
        isCompleted: Boolean = false,
        coinCount: Int = 0
    ): Note = Note(
        id = id,
        title = title,
        category = NoteCategory.TASKS,
        isCompleted = isCompleted,
        coinCount = coinCount
    )
}

private class FakeNotesDataSource : NotesDataSource {
    var notesResult: Result<List<Note>> = Result.success(emptyList())
    var insertResult: Result<String> = Result.failure(IllegalStateException("not set"))
    var updateResult: Result<Unit> = Result.success(Unit)
    var updateCompletionResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.failure(IllegalStateException("not set"))
    val insertCalls = mutableListOf<Note>()
    val updateCalls = mutableListOf<Note>()
    val updateCompletionCalls = mutableListOf<Pair<String, Boolean>>()
    val deleteCalls = mutableListOf<String>()

    override suspend fun getAllNotes(): Result<List<Note>> = notesResult

    override suspend fun insert(note: Note): Result<String> {
        insertCalls.add(note)
        return insertResult
    }

    override suspend fun update(note: Note): Result<Unit> {
        updateCalls.add(note)
        return updateResult
    }

    override suspend fun updateCompletion(noteId: String, isCompleted: Boolean): Result<Unit> {
        updateCompletionCalls.add(noteId to isCompleted)
        return updateCompletionResult
    }

    override suspend fun delete(noteId: String): Result<Unit> {
        deleteCalls.add(noteId)
        return deleteResult
    }
}

private class FakeDeadlineScheduler : DeadlineScheduler {
    var scheduleResult: DeadlineScheduleResult = DeadlineScheduleResult.ScheduledExact
    var scheduleAllResult: DeadlineScheduleResult = DeadlineScheduleResult.ScheduledExact
    val scheduleCalls = mutableListOf<Note>()
    val scheduleAllCalls = mutableListOf<List<Note>>()
    val cancelCalls = mutableListOf<String>()
    var cancelAllCalls = 0

    override fun schedule(note: Note): DeadlineScheduleResult {
        scheduleCalls.add(note)
        return scheduleResult
    }

    override fun cancel(noteId: String) {
        cancelCalls.add(noteId)
    }

    override fun scheduleAll(notes: List<Note>): DeadlineScheduleResult {
        scheduleAllCalls.add(notes.toList())
        scheduleCalls.addAll(notes)
        return scheduleAllResult
    }

    override fun cancelAll() {
        cancelAllCalls += 1
    }
}
