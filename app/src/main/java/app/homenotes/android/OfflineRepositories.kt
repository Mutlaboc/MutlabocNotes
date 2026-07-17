package app.homenotes.android

import app.homenotes.android.local.LocalChecklistItemEntity
import app.homenotes.android.local.LocalCharacterEntity
import app.homenotes.android.local.LocalCharacterSkillEntity
import app.homenotes.android.local.LocalCharacterStatEntity
import app.homenotes.android.local.LocalHomeCardEntity
import app.homenotes.android.local.LocalHomeCardWithChildren
import app.homenotes.android.local.LocalHomeFieldEntity
import app.homenotes.android.local.LocalHomeLinkEntity
import app.homenotes.android.local.LocalNoteEntity
import app.homenotes.android.local.LocalNoteWithChecklist
import app.homenotes.android.local.OfflineDao
import app.homenotes.android.local.OutboxEntity
import com.google.gson.Gson
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal fun normalizeAccountKey(email: String?): String? = email
    ?.trim()
    ?.lowercase(Locale.ROOT)
    ?.takeIf { it.isNotEmpty() }

interface SyncScheduler {
    fun request(accountKey: String)
}

internal object OutboxKind {
    const val NOTE_CREATE = "NOTE_CREATE"
    const val NOTE_UPDATE = "NOTE_UPDATE"
    const val NOTE_COMPLETION = "NOTE_COMPLETION"
    const val NOTE_DELETE = "NOTE_DELETE"
    const val CARD_CREATE = "CARD_CREATE"
    const val CARD_UPDATE = "CARD_UPDATE"
    const val CARD_DELETE = "CARD_DELETE"
    const val CHARACTER_UPDATE = "CHARACTER_UPDATE"
    const val CHARACTER_XP = "CHARACTER_XP"
    const val CHARACTER_STAT_UPGRADE = "CHARACTER_STAT_UPGRADE"
    const val CHARACTER_RENAME = "CHARACTER_RENAME"
}

private data class CompletionPayload(val isCompleted: Boolean)
internal data class XpPayload(val characterXp: Int, val skillKey: String?, val skillXp: Int)
internal data class StatPayload(val statKey: String)
internal data class RenamePayload(val name: String)

class OfflineNotesRepository(
    private val dao: OfflineDao,
    private val session: AuthSessionStore,
    private val scheduler: SyncScheduler,
    private val gson: Gson = Gson(),
) : NotesDataSource {

    override fun observeNotes(): Flow<List<Note>> {
        val account = requireAccount()
        return dao.observeNotes(account).map { rows -> rows.map(::toDomain) }
    }

    override suspend fun getAllNotes(): Result<List<Note>> = runCatching {
        dao.notes(requireAccount()).map(::toDomain)
    }

    override suspend fun insert(note: Note): Result<String> = runCatching {
        val account = requireAccount()
        val localId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        dao.replaceNote(
            note.toEntity(localId, account, null, now, now),
            note.checklist.mapIndexed { index, item -> item.toEntity(localId, index) },
        )
        enqueue(account, OutboxKind.NOTE_CREATE, localId, null, localId)
        scheduler.request(account)
        localId
    }

    override suspend fun update(note: Note): Result<Unit> = runCatching {
        val account = requireAccount()
        require(note.id.isNotBlank()) { "Blank note id" }
        val old = requireNotNull(dao.note(account, note.id)) { "Unknown local note" }
        dao.replaceNote(
            note.toEntity(
                localId = old.note.localId,
                accountKey = account,
                remoteId = old.note.remoteId,
                createdAt = old.note.createdAt,
                updatedAt = System.currentTimeMillis(),
                recurrenceParentLocalId = old.note.recurrenceParentLocalId,
                isProvisional = old.note.isProvisional,
            ),
            note.checklist.mapIndexed { index, item -> item.toEntity(note.id, index) },
        )
        enqueue(account, OutboxKind.NOTE_UPDATE, note.id, old.note.remoteId)
        scheduler.request(account)
    }

    override suspend fun updateCompletion(noteId: String, isCompleted: Boolean): Result<CompletionUpdate> = runCatching {
        val account = requireAccount()
        val current = requireNotNull(dao.note(account, noteId)) { "Unknown local note" }
        val completed = current.note.copy(isCompleted = isCompleted, updatedAt = System.currentTimeMillis())
        dao.putNote(completed)
        var next: Note? = null
        if (isCompleted && completed.category == NoteCategory.RECURRING_TASKS.name) {
            val existing = dao.notes(account).firstOrNull { it.note.recurrenceParentLocalId == noteId }
            next = if (existing != null) {
                toDomain(existing)
            } else {
                val nextId = UUID.randomUUID().toString()
                val nextStart = nextOccurrence(
                    completed.startAtMillis ?: completed.updatedAt,
                    System.currentTimeMillis(),
                    RepeatRule.valueOf(completed.repeatRule),
                )
                val nextEntity = completed.copy(
                    localId = nextId,
                    remoteId = null,
                    startAtMillis = nextStart,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    recurrenceParentLocalId = noteId,
                    isProvisional = true,
                )
                dao.putNote(nextEntity)
                toDomain(LocalNoteWithChecklist(nextEntity, emptyList()))
            }
        }
        enqueue(
            account, OutboxKind.NOTE_COMPLETION, noteId, current.note.remoteId,
            payload = gson.toJson(CompletionPayload(isCompleted)),
        )
        scheduler.request(account)
        CompletionUpdate(toDomain(LocalNoteWithChecklist(completed, current.checklist)), next)
    }

    override suspend fun delete(noteId: String): Result<Unit> = runCatching {
        val account = requireAccount()
        val current = requireNotNull(dao.note(account, noteId)) { "Unknown local note" }
        if (current.note.remoteId == null) {
            dao.removeOutboxForLocal(account, noteId)
        } else {
            enqueue(account, OutboxKind.NOTE_DELETE, noteId, current.note.remoteId)
        }
        dao.deleteNote(account, noteId)
        scheduler.request(account)
    }

    private suspend fun enqueue(
        account: String,
        kind: String,
        localId: String,
        remoteId: String?,
        operationId: String = UUID.randomUUID().toString(),
        payload: String? = null,
    ) {
        dao.enqueue(OutboxEntity(
            accountKey = account,
            kind = kind,
            localId = localId,
            remoteId = remoteId,
            operationId = operationId,
            payload = payload,
            createdAt = System.currentTimeMillis(),
        ))
    }

    private fun requireAccount(): String = normalizeAccountKey(session.getEmail())
        ?: throw IllegalStateException("No active account")
}

class OfflineHomeInfoRepository(
    private val dao: OfflineDao,
    private val session: AuthSessionStore,
    private val scheduler: SyncScheduler,
) : HomeInfoDataSource {
    override fun observeCards(): Flow<List<HomeInfoCard>> {
        val account = requireAccount()
        return dao.observeCards(account).map { it.map(::toDomain) }
    }

    override suspend fun getAllCards(): Result<List<HomeInfoCard>> = runCatching {
        dao.cards(requireAccount()).map(::toDomain)
    }

    override suspend fun insert(card: HomeInfoCard): Result<HomeInfoCard> = runCatching {
        val account = requireAccount()
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val canonical = card.copy(id = id, createdAt = now, updatedAt = now)
        dao.replaceCard(
            canonical.toEntity(account, null),
            canonical.fields.mapIndexed { i, field -> LocalHomeFieldEntity(id, i, field.key, field.value) },
            canonical.links.mapIndexed { i, link -> LocalHomeLinkEntity(id, i, link) },
        )
        enqueue(account, OutboxKind.CARD_CREATE, id, null, id)
        scheduler.request(account)
        canonical
    }

    override suspend fun update(card: HomeInfoCard): Result<HomeInfoCard> = runCatching {
        val account = requireAccount()
        val old = requireNotNull(dao.card(account, card.id)) { "Unknown local card" }
        val updated = card.copy(createdAt = old.card.createdAt, updatedAt = System.currentTimeMillis())
        dao.replaceCard(
            updated.toEntity(account, old.card.remoteId),
            updated.fields.mapIndexed { i, field -> LocalHomeFieldEntity(card.id, i, field.key, field.value) },
            updated.links.mapIndexed { i, link -> LocalHomeLinkEntity(card.id, i, link) },
        )
        enqueue(account, OutboxKind.CARD_UPDATE, card.id, old.card.remoteId)
        scheduler.request(account)
        updated
    }

    override suspend fun delete(cardId: String): Result<Unit> = runCatching {
        val account = requireAccount()
        val old = requireNotNull(dao.card(account, cardId)) { "Unknown local card" }
        if (old.card.remoteId == null) dao.removeOutboxForLocal(account, cardId)
        else enqueue(account, OutboxKind.CARD_DELETE, cardId, old.card.remoteId)
        dao.deleteCard(account, cardId)
        scheduler.request(account)
    }

    private suspend fun enqueue(account: String, kind: String, localId: String, remoteId: String?, operationId: String = UUID.randomUUID().toString()) {
        dao.enqueue(OutboxEntity(
            accountKey = account, kind = kind, localId = localId, remoteId = remoteId,
            operationId = operationId, createdAt = System.currentTimeMillis(),
        ))
    }

    private fun requireAccount(): String = normalizeAccountKey(session.getEmail())
        ?: throw IllegalStateException("No active account")
}

class OfflineCharacterRepository(
    private val dao: OfflineDao,
    private val session: AuthSessionStore,
    private val scheduler: SyncScheduler,
    private val gson: Gson = Gson(),
) : CharacterDataSource {
    override suspend fun getCharacter(): Result<CharacterSheet> = runCatching {
        val account = requireAccount()
        val character = requireNotNull(dao.character(account)) { "Character has not been loaded yet" }
        character.toDomain(dao.characterStats(account), dao.characterSkills(account))
    }

    override suspend fun updateCharacter(sheet: CharacterSheet): Result<CharacterSheet> = runCatching {
        val account = requireAccount()
        val existing = dao.character(account)
        putProjection(account, sheet, existing?.hasServerSnapshot == true, existing?.snapshotJson)
        val operationId = UUID.randomUUID().toString()
        dao.enqueue(OutboxEntity(
            accountKey = account, kind = OutboxKind.CHARACTER_UPDATE, localId = account,
            operationId = operationId, createdAt = System.currentTimeMillis(),
        ))
        scheduler.request(account)
        sheet
    }

    override suspend fun addExperience(characterXp: Int, skillKey: String?, skillXp: Int): Result<CharacterSheet> = runCatching {
        val account = requireAccount()
        val currentEntity = requireNotNull(dao.character(account)) { "Character has not been loaded yet" }
        val current = currentEntity.toDomain(dao.characterStats(account), dao.characterSkills(account))
        val updated = applyExperience(current, characterXp, skillKey, skillXp)
        putProjection(account, updated, currentEntity.hasServerSnapshot, currentEntity.snapshotJson)
        dao.enqueue(OutboxEntity(
            accountKey = account, kind = OutboxKind.CHARACTER_XP, localId = account,
            operationId = UUID.randomUUID().toString(),
            payload = gson.toJson(XpPayload(characterXp, skillKey, skillXp)),
            createdAt = System.currentTimeMillis(),
        ))
        scheduler.request(account)
        updated
    }

    override suspend fun upgradeStat(sheet: CharacterSheet, statKey: String, cost: Int): Result<CharacterSheet> = runCatching {
        val account = requireAccount()
        val existing = requireNotNull(dao.character(account)) { "Character has not been loaded yet" }
        putProjection(account, sheet, existing.hasServerSnapshot, existing.snapshotJson)
        dao.enqueue(OutboxEntity(
            accountKey = account, kind = OutboxKind.CHARACTER_STAT_UPGRADE, localId = account,
            operationId = UUID.randomUUID().toString(), payload = gson.toJson(StatPayload(statKey)),
            createdAt = System.currentTimeMillis(),
        ))
        scheduler.request(account)
        sheet
    }

    override suspend fun rename(sheet: CharacterSheet): Result<CharacterSheet> = runCatching {
        val account = requireAccount()
        val existing = requireNotNull(dao.character(account)) { "Character has not been loaded yet" }
        putProjection(account, sheet, existing.hasServerSnapshot, existing.snapshotJson)
        dao.enqueue(OutboxEntity(
            accountKey = account, kind = OutboxKind.CHARACTER_RENAME, localId = account,
            operationId = UUID.randomUUID().toString(), payload = gson.toJson(RenamePayload(sheet.name)),
            createdAt = System.currentTimeMillis(),
        ))
        scheduler.request(account)
        sheet
    }

    private suspend fun putProjection(account: String, sheet: CharacterSheet, hasSnapshot: Boolean, snapshotJson: String?) {
        dao.putCharacter(LocalCharacterEntity(account, sheet.name, sheet.level, sheet.xp, sheet.xpToNext, hasSnapshot, snapshotJson))
        dao.clearCharacterStats(account)
        dao.clearCharacterSkills(account)
        dao.putCharacterStats(sheet.stats.mapIndexed { i, stat ->
            LocalCharacterStatEntity(account, stat.key, i, stat.name, stat.description, stat.value)
        })
        dao.putCharacterSkills(sheet.skills.mapIndexed { i, skill ->
            LocalCharacterSkillEntity(account, skill.key, i, skill.name, skill.level, skill.progress.toDouble())
        })
    }

    private fun requireAccount(): String = normalizeAccountKey(session.getEmail())
        ?: throw IllegalStateException("No active account")
}

private fun Note.toEntity(
    localId: String,
    accountKey: String,
    remoteId: String?,
    createdAt: Long,
    updatedAt: Long,
    recurrenceParentLocalId: String? = null,
    isProvisional: Boolean = false,
) = LocalNoteEntity(
    localId, accountKey, remoteId, title, content, category.name, deadlineMillis,
    startAtMillis, durationMinutes, repeatRule.name, coinCount, isCompleted,
    createdAt, updatedAt, recurrenceParentLocalId, isProvisional,
)

private fun ChecklistItem.toEntity(localId: String, position: Int) =
    LocalChecklistItemEntity(localId, position, text, isChecked)

internal fun toDomain(row: LocalNoteWithChecklist) = Note(
    id = row.note.localId,
    title = row.note.title,
    content = row.note.content,
    category = runCatching { NoteCategory.valueOf(row.note.category) }.getOrDefault(NoteCategory.TASKS),
    checklist = row.checklist.sortedBy { it.position }.map { ChecklistItem(it.text, it.isChecked) },
    deadlineMillis = row.note.deadlineMillis,
    startAtMillis = row.note.startAtMillis,
    durationMinutes = row.note.durationMinutes,
    repeatRule = runCatching { RepeatRule.valueOf(row.note.repeatRule) }.getOrDefault(RepeatRule.NONE),
    coinCount = row.note.coinCount,
    isCompleted = row.note.isCompleted,
)

private fun HomeInfoCard.toEntity(account: String, remoteId: String?) = LocalHomeCardEntity(
    localId = id, accountKey = account, remoteId = remoteId, title = title,
    section = section.name, note = note, createdAt = createdAt, updatedAt = updatedAt,
)

internal fun toDomain(row: LocalHomeCardWithChildren) = HomeInfoCard(
    id = row.card.localId,
    title = row.card.title,
    section = runCatching { HomeSection.valueOf(row.card.section) }.getOrDefault(HomeSection.OTHER),
    fields = row.fields.sortedBy { it.position }.map { HomeField(it.key, it.value) },
    note = row.card.note,
    links = row.links.sortedBy { it.position }.map { it.url },
    createdAt = row.card.createdAt,
    updatedAt = row.card.updatedAt,
)

private fun LocalCharacterEntity.toDomain(
    stats: List<LocalCharacterStatEntity>,
    skills: List<LocalCharacterSkillEntity>,
) = CharacterSheet(
    name = name,
    level = level,
    xp = xp,
    xpToNext = xpToNext,
    portraitRes = R.drawable.mascot_stand_01,
    stats = stats.map { CharacterStat(it.name, it.description, it.value, it.statKey) },
    skills = skills.map { CharacterSkill(it.name, it.level, it.progress.toFloat(), it.skillKey) },
)

private fun applyExperience(sheet: CharacterSheet, characterXp: Int, skillKey: String?, skillXp: Int): CharacterSheet {
    var level = sheet.level
    var xp = sheet.xp.toLong() + characterXp.coerceAtLeast(0)
    var threshold = sheet.xpToNext.coerceAtLeast(1)
    while (level < CHARACTER_MAX_LEVEL && xp >= threshold) {
        xp -= threshold
        level++
        threshold = if (threshold > Int.MAX_VALUE / 2) Int.MAX_VALUE else threshold * 2
    }
    val skills = sheet.skills.map { skill ->
        if (skill.key != skillKey || skillXp <= 0) skill else {
            val total = skill.progress * 100f + skillXp
            val gained = (total / 100f).toInt()
            skill.copy(level = (skill.level + gained).coerceAtMost(CHARACTER_MAX_LEVEL), progress = (total % 100f) / 100f)
        }
    }
    return sheet.copy(level = level, xp = xp.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), xpToNext = threshold, skills = skills)
}

private fun nextOccurrence(startMillis: Long, nowMillis: Long, rule: RepeatRule): Long {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = startMillis }
    val anchorDay = calendar.get(Calendar.DAY_OF_MONTH)
    do {
        when (rule) {
            RepeatRule.DAILY -> calendar.add(Calendar.DAY_OF_MONTH, 1)
            RepeatRule.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RepeatRule.MONTHLY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.add(Calendar.MONTH, 1)
                calendar.set(Calendar.DAY_OF_MONTH, minOf(anchorDay, calendar.getActualMaximum(Calendar.DAY_OF_MONTH)))
            }
            RepeatRule.NONE -> return startMillis
        }
    } while (calendar.timeInMillis <= nowMillis)
    return calendar.timeInMillis
}
