package app.homenotes.android

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.homenotes.android.local.HomeNotesDatabase
import app.homenotes.android.local.LocalChecklistItemEntity
import app.homenotes.android.local.LocalNoteEntity
import app.homenotes.android.local.OutboxEntity
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineDatabaseTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: HomeNotesDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(context, HomeNotesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun notesAndOutboxAreIsolatedByNormalizedAccount() = runTest {
        val dao = database.offlineDao()
        dao.replaceNote(note("local-a", "a@example.com"), listOf(LocalChecklistItemEntity("local-a", 0, "A", false)))
        dao.replaceNote(note("local-b", "b@example.com"), emptyList())
        dao.enqueue(outbox("a@example.com", "local-a"))
        dao.enqueue(outbox("b@example.com", "local-b"))

        assertEquals(listOf("local-a"), dao.notes("a@example.com").map { it.note.localId })
        assertEquals(listOf("local-b"), dao.notes("b@example.com").map { it.note.localId })
        assertEquals(1, dao.outbox("a@example.com").size)
        assertEquals(1, dao.outbox("b@example.com").size)
    }

    @Test
    fun deletingNoteCascadesChecklistWithoutTouchingAnotherAccount() = runTest {
        val dao = database.offlineDao()
        dao.replaceNote(note("local-a", "a@example.com"), listOf(LocalChecklistItemEntity("local-a", 0, "A", false)))
        dao.replaceNote(note("local-b", "b@example.com"), listOf(LocalChecklistItemEntity("local-b", 0, "B", false)))

        dao.deleteNote("a@example.com", "local-a")

        val cursor = database.query("SELECT note_local_id FROM note_checklist_items", emptyArray())
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("local-b", it.getString(0))
            assertTrue(!it.moveToNext())
        }
    }

    @Test
    fun outboxSurvivesDatabaseReopen() = runTest {
        database.close()
        val name = "offline-reopen-${UUID.randomUUID()}.db"
        context.deleteDatabase(name)
        var fileDatabase = Room.databaseBuilder(context, HomeNotesDatabase::class.java, name).build()
        fileDatabase.offlineDao().enqueue(outbox("a@example.com", "stable-local-id"))
        fileDatabase.close()

        fileDatabase = Room.databaseBuilder(context, HomeNotesDatabase::class.java, name).build()
        assertEquals("stable-local-id", fileDatabase.offlineDao().outbox("a@example.com").single().localId)
        fileDatabase.close()
        context.deleteDatabase(name)

        database = Room.inMemoryDatabaseBuilder(context, HomeNotesDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    private fun note(id: String, account: String) = LocalNoteEntity(
        localId = id,
        accountKey = account,
        title = id,
        content = "",
        category = NoteCategory.SHOPPING.name,
        deadlineMillis = null,
        startAtMillis = null,
        durationMinutes = null,
        repeatRule = RepeatRule.NONE.name,
        coinCount = 1,
        isCompleted = false,
        createdAt = 1,
        updatedAt = 1,
    )

    private fun outbox(account: String, localId: String) = OutboxEntity(
        accountKey = account,
        kind = OutboxKind.NOTE_CREATE,
        localId = localId,
        operationId = UUID.randomUUID().toString(),
        createdAt = System.currentTimeMillis(),
    )
}
