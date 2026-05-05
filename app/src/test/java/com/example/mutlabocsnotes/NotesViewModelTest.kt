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
    fun loadNotes_successUpdatesStateRecalculatesCoinsAndSchedulesAll() = runTest(mainDispatcherRule.dispatcher) {
        val completed = note(id = "1", isCompleted = true, coinCount = 5)
        val open = note(id = "2", isCompleted = false, coinCount = 7)
        repository.notesResult = Result.success(listOf(completed, open))

        viewModel.loadNotes()
        advanceUntilIdle()

        assertEquals(listOf(completed, open), viewModel.notes.toList())
        assertEquals(5, viewModel.totalCoins)
        assertEquals(listOf(listOf(completed, open)), scheduler.scheduleAllCalls)
        assertEquals(null, viewModel.errorMessage)
        assertFalse(viewModel.isLoading)
    }

    @Test
    fun loadNotes_emptySuccessStaysEmptyWithoutError() = runTest(mainDispatcherRule.dispatcher) {
        repository.notesResult = Result.success(emptyList())

        viewModel.loadNotes()
        advanceUntilIdle()

        assertTrue(viewModel.notes.isEmpty())
        assertEquals(null, viewModel.errorMessage)
        assertFalse(viewModel.isLoading)
    }

    @Test
    fun loadNotes_failureKeepsExistingNotesAndSetsError() = runTest(mainDispatcherRule.dispatcher) {
        val existing = note(id = "cached")
        viewModel.notes.add(existing)
        repository.notesResult = Result.failure(IllegalStateException("Network unavailable"))

        viewModel.loadNotes()
        advanceUntilIdle()

        assertEquals(listOf(existing), viewModel.notes.toList())
        assertEquals("Network unavailable", viewModel.errorMessage)
        assertFalse(viewModel.isLoading)
        assertTrue(scheduler.scheduleAllCalls.isEmpty())
    }

    @Test
    fun addNote_successAddsCreatedNoteAndSchedulesIt() = runTest(mainDispatcherRule.dispatcher) {
        val created = note(title = "Created")
        repository.insertResult = Result.success("created-id")

        viewModel.addNote(created)
        advanceUntilIdle()

        val expected = created.copy(id = "created-id")
        assertEquals(listOf(expected), viewModel.notes.toList())
        assertEquals(listOf(expected), scheduler.scheduleCalls)
        assertEquals(null, viewModel.errorMessage)
    }

    @Test
    fun addNote_failureDoesNotChangeStateAndSetsError() = runTest(mainDispatcherRule.dispatcher) {
        repository.insertResult = Result.failure(IllegalStateException("Cannot save"))

        viewModel.addNote(note(title = "Created"))
        advanceUntilIdle()

        assertTrue(viewModel.notes.isEmpty())
        assertEquals("Cannot save", viewModel.errorMessage)
        assertTrue(scheduler.scheduleCalls.isEmpty())
    }

    @Test
    fun setNoteCompletion_successOptimisticallyUpdatesAndPersists() = runTest(mainDispatcherRule.dispatcher) {
        val existing = note(id = "task", isCompleted = false, coinCount = 3)
        viewModel.notes.add(existing)

        viewModel.setNoteCompletion("task", true)

        val optimistic = existing.copy(isCompleted = true)
        assertEquals(listOf(optimistic), viewModel.notes.toList())
        assertEquals(3, viewModel.totalCoins)

        advanceUntilIdle()

        assertEquals(listOf(optimistic), viewModel.notes.toList())
        assertEquals(listOf(optimistic), repository.updateCalls)
        assertEquals(listOf(optimistic), scheduler.scheduleCalls)
        assertEquals(null, viewModel.errorMessage)
    }

    @Test
    fun setNoteCompletion_failureRollsBackStateAndSetsError() = runTest(mainDispatcherRule.dispatcher) {
        val existing = note(id = "task", isCompleted = false, coinCount = 3)
        repository.updateResult = Result.failure(IllegalStateException("Cannot update"))
        viewModel.notes.add(existing)

        viewModel.setNoteCompletion("task", true)
        advanceUntilIdle()

        val optimistic = existing.copy(isCompleted = true)
        assertEquals(listOf(existing), viewModel.notes.toList())
        assertEquals(0, viewModel.totalCoins)
        assertEquals(listOf(optimistic), repository.updateCalls)
        assertEquals(listOf(optimistic, existing), scheduler.scheduleCalls)
        assertEquals("Cannot update", viewModel.errorMessage)
    }

    @Test
    fun deleteNote_successRemovesNoteAndCancelsSchedule() = runTest(mainDispatcherRule.dispatcher) {
        val existing = note(id = "delete-me")
        repository.deleteResult = Result.success(Unit)
        viewModel.notes.add(existing)

        viewModel.deleteNote("delete-me")
        advanceUntilIdle()

        assertTrue(viewModel.notes.isEmpty())
        assertEquals(listOf("delete-me"), repository.deleteCalls)
        assertEquals(listOf("delete-me"), scheduler.cancelCalls)
        assertEquals(null, viewModel.errorMessage)
    }

    @Test
    fun deleteNote_failureKeepsNoteAndSetsError() = runTest(mainDispatcherRule.dispatcher) {
        val existing = note(id = "delete-me")
        repository.deleteResult = Result.failure(IllegalStateException("Cannot delete"))
        viewModel.notes.add(existing)

        viewModel.deleteNote("delete-me")
        advanceUntilIdle()

        assertEquals(listOf(existing), viewModel.notes.toList())
        assertEquals(listOf("delete-me"), repository.deleteCalls)
        assertTrue(scheduler.cancelCalls.isEmpty())
        assertEquals("Cannot delete", viewModel.errorMessage)
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
    }
}
