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
    fun loadNotes_updatesStateRecalculatesCoinsAndSchedulesAll() = runTest(mainDispatcherRule.dispatcher) {
        val completed = note(id = "1", isCompleted = true, coinCount = 5)
        val open = note(id = "2", isCompleted = false, coinCount = 7)
        repository.notesToReturn = listOf(completed, open)

        viewModel.loadNotes()
        advanceUntilIdle()

        assertEquals(listOf(completed, open), viewModel.notes.toList())
        assertEquals(5, viewModel.totalCoins)
        assertEquals(listOf(listOf(completed, open)), scheduler.scheduleAllCalls)
    }

    @Test
    fun addNote_addsCreatedNoteAndSchedulesIt() = runTest(mainDispatcherRule.dispatcher) {
        val created = note(title = "Created")
        repository.insertId = "created-id"

        viewModel.addNote(created)
        advanceUntilIdle()

        val expected = created.copy(id = "created-id")
        assertEquals(listOf(expected), viewModel.notes.toList())
        assertEquals(listOf(expected), scheduler.scheduleCalls)
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
    }

    @Test
    fun setNoteCompletion_failureRollsBackStateAndSchedule() = runTest(mainDispatcherRule.dispatcher) {
        val existing = note(id = "task", isCompleted = false, coinCount = 3)
        repository.updateResult = false
        viewModel.notes.add(existing)

        viewModel.setNoteCompletion("task", true)
        advanceUntilIdle()

        val optimistic = existing.copy(isCompleted = true)
        assertEquals(listOf(existing), viewModel.notes.toList())
        assertEquals(0, viewModel.totalCoins)
        assertEquals(listOf(optimistic), repository.updateCalls)
        assertEquals(listOf(optimistic, existing), scheduler.scheduleCalls)
    }

    @Test
    fun deleteNote_successRemovesNoteAndCancelsSchedule() = runTest(mainDispatcherRule.dispatcher) {
        val existing = note(id = "delete-me")
        repository.deleteResult = true
        viewModel.notes.add(existing)

        viewModel.deleteNote("delete-me")
        advanceUntilIdle()

        assertTrue(viewModel.notes.isEmpty())
        assertEquals(listOf("delete-me"), repository.deleteCalls)
        assertEquals(listOf("delete-me"), scheduler.cancelCalls)
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
    var notesToReturn: List<Note> = emptyList()
    var insertId: String? = null
    var updateResult: Boolean = true
    var deleteResult: Boolean = false
    val insertCalls = mutableListOf<Note>()
    val updateCalls = mutableListOf<Note>()
    val deleteCalls = mutableListOf<String>()

    override suspend fun getAllNotes(): List<Note> = notesToReturn

    override suspend fun insert(note: Note): String? {
        insertCalls.add(note)
        return insertId
    }

    override suspend fun update(note: Note): Boolean {
        updateCalls.add(note)
        return updateResult
    }

    override suspend fun delete(noteId: String): Boolean {
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
