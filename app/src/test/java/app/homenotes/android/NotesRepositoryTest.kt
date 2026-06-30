package app.homenotes.android

import app.homenotes.android.network.ChecklistItemDto
import app.homenotes.android.network.NoteCompletionRequestDto
import app.homenotes.android.network.NoteDto
import app.homenotes.android.network.NoteUpsertRequestDto
import app.homenotes.android.network.NotesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotesRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var api: FakeNotesApi
    private lateinit var repository: NotesRepository

    @Before
    fun setUp() {
        api = FakeNotesApi()
        repository = NotesRepository(api)
    }

    @Test
    fun getAllNotes_successReturnsMappedNotes() = runTest(mainDispatcherRule.dispatcher) {
        api.notesResponse = listOf(noteDto(id = "1", title = "Server note"))

        val result = repository.getAllNotes()

        assertTrue(result.isSuccess)
        assertEquals(listOf(note(id = "1", title = "Server note")), result.getOrThrow())
    }

    @Test
    fun getAllNotes_failurePropagatesError() = runTest(mainDispatcherRule.dispatcher) {
        val error = IllegalStateException("backend down")
        api.getNotesError = error

        val result = repository.getAllNotes()

        assertTrue(result.isFailure)
        assertSame(error, result.exceptionOrNull())
    }

    @Test
    fun insert_successReturnsCreatedIdAndSendsPayload() = runTest(mainDispatcherRule.dispatcher) {
        val created = note(id = "", title = "Created", coinCount = 4)
        api.createResponse = noteDto(id = "created-id", title = "Created", coinCount = 4)

        val result = repository.insert(created)

        assertEquals(Result.success("created-id"), result)
        assertEquals(
            NoteUpsertRequestDto(
                title = "Created",
                content = "",
                category = NoteCategory.TASKS.name,
                checklist = emptyList(),
                deadlineMillis = null,
                repeatRule = RepeatRule.NONE.name,
                coinCount = 4,
                isCompleted = false
            ),
            api.lastCreateRequest
        )
    }

    @Test
    fun insert_failurePropagatesError() = runTest(mainDispatcherRule.dispatcher) {
        val error = IllegalStateException("cannot create")
        api.createError = error

        val result = repository.insert(note())

        assertTrue(result.isFailure)
        assertSame(error, result.exceptionOrNull())
    }

    @Test
    fun update_blankIdReturnsFailureWithoutCallingApi() = runTest(mainDispatcherRule.dispatcher) {
        val result = repository.update(note(id = ""))

        assertTrue(result.isFailure)
        assertEquals("Blank note id", result.exceptionOrNull()?.message)
        assertEquals(null, api.lastUpdateRequest)
    }

    @Test
    fun update_failurePropagatesError() = runTest(mainDispatcherRule.dispatcher) {
        val error = IllegalStateException("cannot update")
        api.updateError = error

        val result = repository.update(note(id = "note-id"))

        assertTrue(result.isFailure)
        assertSame(error, result.exceptionOrNull())
    }

    @Test
    fun updateCompletion_successCallsPatchEndpointWithCompletionPayload() = runTest(mainDispatcherRule.dispatcher) {
        val result = repository.updateCompletion("note-id", true)

        assertEquals(Result.success(Unit), result)
        assertEquals("note-id", api.lastCompletionId)
        assertEquals(NoteCompletionRequestDto(isCompleted = true), api.lastCompletionRequest)
        assertEquals(null, api.lastUpdateRequest)
    }

    @Test
    fun updateCompletion_blankIdReturnsFailureWithoutCallingApi() = runTest(mainDispatcherRule.dispatcher) {
        val result = repository.updateCompletion("", true)

        assertTrue(result.isFailure)
        assertEquals("Blank note id", result.exceptionOrNull()?.message)
        assertEquals(null, api.lastCompletionRequest)
    }

    @Test
    fun updateCompletion_failurePropagatesError() = runTest(mainDispatcherRule.dispatcher) {
        val error = IllegalStateException("cannot patch completion")
        api.completionError = error

        val result = repository.updateCompletion("note-id", false)

        assertTrue(result.isFailure)
        assertSame(error, result.exceptionOrNull())
    }

    @Test
    fun delete_successReturnsUnit() = runTest(mainDispatcherRule.dispatcher) {
        val result = repository.delete("delete-me")

        assertEquals(Result.success(Unit), result)
        assertEquals("delete-me", api.lastDeleteId)
    }

    @Test
    fun delete_failurePropagatesError() = runTest(mainDispatcherRule.dispatcher) {
        val error = IllegalStateException("cannot delete")
        api.deleteError = error

        val result = repository.delete("delete-me")

        assertTrue(result.isFailure)
        assertSame(error, result.exceptionOrNull())
    }

    private fun note(
        id: String = "note-id",
        title: String = "Note",
        coinCount: Int = 0
    ): Note = Note(
        id = id,
        title = title,
        category = NoteCategory.TASKS,
        coinCount = coinCount
    )

    private fun noteDto(
        id: String,
        title: String,
        coinCount: Int = 0
    ): NoteDto = NoteDto(
        id = id,
        title = title,
        content = "",
        category = NoteCategory.TASKS.name,
        checklist = emptyList(),
        deadlineMillis = null,
        repeatRule = RepeatRule.NONE.name,
        coinCount = coinCount,
        isCompleted = false
    )
}

private class FakeNotesApi : NotesApi {
    var notesResponse: List<NoteDto> = emptyList()
    var createResponse: NoteDto = noteDto("created-id")
    var getNotesError: Exception? = null
    var createError: Exception? = null
    var updateError: Exception? = null
    var completionError: Exception? = null
    var deleteError: Exception? = null
    var lastCreateRequest: NoteUpsertRequestDto? = null
    var lastUpdateId: String? = null
    var lastUpdateRequest: NoteUpsertRequestDto? = null
    var lastCompletionId: String? = null
    var lastCompletionRequest: NoteCompletionRequestDto? = null
    var lastDeleteId: String? = null

    override suspend fun getNotes(): List<NoteDto> {
        getNotesError?.let { throw it }
        return notesResponse
    }

    override suspend fun getNoteById(noteId: String): NoteDto {
        return notesResponse.first { it.id == noteId }
    }

    override suspend fun createNote(request: NoteUpsertRequestDto): NoteDto {
        createError?.let { throw it }
        lastCreateRequest = request
        return createResponse
    }

    override suspend fun updateNote(noteId: String, request: NoteUpsertRequestDto): NoteDto {
        updateError?.let { throw it }
        lastUpdateId = noteId
        lastUpdateRequest = request
        return noteDto(noteId)
    }

    override suspend fun updateNoteCompletion(noteId: String, request: NoteCompletionRequestDto) {
        completionError?.let { throw it }
        lastCompletionId = noteId
        lastCompletionRequest = request
    }

    override suspend fun deleteNote(noteId: String) {
        deleteError?.let { throw it }
        lastDeleteId = noteId
    }

    private fun noteDto(id: String): NoteDto = NoteDto(
        id = id,
        title = "Note",
        content = "",
        category = NoteCategory.TASKS.name,
        checklist = emptyList<ChecklistItemDto>(),
        deadlineMillis = null,
        repeatRule = RepeatRule.NONE.name,
        coinCount = 0,
        isCompleted = false
    )
}
