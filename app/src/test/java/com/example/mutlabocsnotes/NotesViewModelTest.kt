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
            ioDispatcher = mainDispatcherRule.dispatcher
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

        val expected = created.copy(id = "created-id")
        val state = viewModel.uiState as NotesUiState.Content
        assertEquals(listOf(expected), state.notes)
        assertEquals(listOf(expected), scheduler.scheduleCalls)
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
        assertEquals(listOf(optimistic), repository.updateCalls)
        assertEquals(listOf(optimistic), scheduler.scheduleCalls.drop(1))
        assertEquals(null, viewModel.uiMessage)
    }

    @Test
    fun setNoteCompletion_failureRollsBackStateAndEmitsSnackbarMessage() = runTest(mainDispatcherRule.dispatcher) {
        val existing = note(id = "task", isCompleted = false, coinCount = 3)
        repository.updateResult = Result.failure(IOException("offline"))
        loadContent(existing)

        viewModel.setNoteCompletion("task", true)
        advanceUntilIdle()

        val optimistic = existing.copy(isCompleted = true)
        val state = viewModel.uiState as NotesUiState.Content
        assertEquals(listOf(existing), state.notes)
        assertEquals(0, state.totalCoins)
        assertEquals(listOf(optimistic), repository.updateCalls)
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
    var deleteResult: Result<Unit> = Result.failure(IllegalStateException("not set"))
    val insertCalls = mutableListOf<Note>()
    val updateCalls = mutableListOf<Note>()
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

    override suspend fun delete(noteId: String): Result<Unit> {
        deleteCalls.add(noteId)
        return deleteResult
    }
}

private class FakeDeadlineScheduler : DeadlineScheduler {
    val scheduleCalls = mutableListOf<Note>()
    val scheduleAllCalls = mutableListOf<List<Note>>()
    val cancelCalls = mutableListOf<String>()

    override fun schedule(note: Note) {
        scheduleCalls.add(note)
    }

    override fun cancel(noteId: String) {
        cancelCalls.add(noteId)
    }

    override fun scheduleAll(notes: List<Note>) {
        scheduleAllCalls.add(notes.toList())
        scheduleCalls.addAll(notes)
    }
}
