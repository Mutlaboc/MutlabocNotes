package app.homenotes.android

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.homenotes.android.local.LocalChecklistItemEntity
import app.homenotes.android.local.LocalCharacterEntity
import app.homenotes.android.local.LocalCharacterSkillEntity
import app.homenotes.android.local.LocalCharacterStatEntity
import app.homenotes.android.local.LocalHomeCardEntity
import app.homenotes.android.local.LocalHomeFieldEntity
import app.homenotes.android.local.LocalHomeLinkEntity
import app.homenotes.android.local.LocalNoteEntity
import app.homenotes.android.local.LocalSyncStateEntity
import app.homenotes.android.local.LocalWalletEntity
import app.homenotes.android.local.LocalAchievementMetricEntity
import app.homenotes.android.local.LocalAchievementUnlockEntity
import app.homenotes.android.local.OfflineDao
import app.homenotes.android.local.OutboxEntity
import app.homenotes.android.network.AchievementMetricDto
import app.homenotes.android.network.AchievementMetricsRequestDto
import app.homenotes.android.network.AchievementUnlockRequestDto
import app.homenotes.android.network.AchievementsApi
import app.homenotes.android.network.CharacterApi
import app.homenotes.android.network.CharacterRenameRequestDto
import app.homenotes.android.network.CharacterSheetDto
import app.homenotes.android.network.CharacterStatUpgradeRequestDto
import app.homenotes.android.network.CharacterXpRequestDto
import app.homenotes.android.network.EventsApi
import app.homenotes.android.network.FocusEventClaimRequestDto
import app.homenotes.android.network.HomeCardDto
import app.homenotes.android.network.HomeCardsApi
import app.homenotes.android.network.InventoryApi
import app.homenotes.android.network.InventoryDto
import app.homenotes.android.network.InventoryEquipRequestDto
import app.homenotes.android.network.InventoryUnequipRequestDto
import app.homenotes.android.network.NoteCompletionRequestDto
import app.homenotes.android.network.NoteDto
import app.homenotes.android.network.NotesApi
import app.homenotes.android.network.ApiJson
import app.homenotes.android.network.toDomain
import app.homenotes.android.network.toEntity
import app.homenotes.android.network.toUpdateRequest
import app.homenotes.android.network.toUpsertRequestDto
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import retrofit2.HttpException

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Pending(val count: Int) : SyncStatus
    data class Offline(val count: Int) : SyncStatus
    data class Blocked(val message: String?) : SyncStatus
}

class WorkManagerSyncCoordinator(
    context: Context,
    private val dao: OfflineDao,
) : SyncScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    override fun request(accountKey: String) {
        val normalized = normalizeAccountKey(accountKey) ?: return
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
            .setConstraints(constraints)
            .setInputData(Data.Builder().putString(OfflineSyncWorker.ACCOUNT_KEY, normalized).build())
            .build()
        workManager.enqueueUniqueWork(oneTimeName(normalized), ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        schedulePeriodic(normalized)
        // Плашку не показываем сразу: синк обычно проходит за секунды,
        // баннер появится только если операции залежатся (см. refreshStatus).
    }

    fun activateAccount(accountKey: String) {
        request(accountKey)
        schedulePeriodic(accountKey)
    }

    fun cancel(accountKey: String) {
        workManager.cancelUniqueWork(oneTimeName(accountKey))
        workManager.cancelUniqueWork(periodicName(accountKey))
        _status.value = SyncStatus.Idle
    }

    internal suspend fun refreshStatus(accountKey: String) {
        _status.value = staleAwareStatus(accountKey) { count -> SyncStatus.Pending(count) }
    }

    /** Синк упал по сети: показываем «нет сети», но тоже только для залежавшихся операций. */
    internal suspend fun reportOffline(accountKey: String) {
        _status.value = staleAwareStatus(accountKey) { count -> SyncStatus.Offline(count) }
    }

    /**
     * Статус с учётом «возраста» очереди: пока самые старые операции моложе
     * [PENDING_BANNER_AGE_MS], плашка не показывается — фоновый синк успеет сам.
     */
    private suspend fun staleAwareStatus(accountKey: String, visible: (Int) -> SyncStatus): SyncStatus {
        val count = dao.pendingCount(accountKey)
        if (count == 0) return SyncStatus.Idle
        val oldest = dao.oldestPendingCreatedAt(accountKey) ?: return SyncStatus.Idle
        val age = System.currentTimeMillis() - oldest
        return if (age >= PENDING_BANNER_AGE_MS) visible(count) else SyncStatus.Idle
    }

    /** Показывает «Синхронизация…» только если плашка уже была видна (не мигаем на каждый фоновый синк). */
    internal fun showSyncing() {
        if (_status.value != SyncStatus.Idle) _status.value = SyncStatus.Syncing
    }

    internal fun setRunning(status: SyncStatus) {
        _status.value = status
    }

    private fun schedulePeriodic(accountKey: String) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = PeriodicWorkRequestBuilder<OfflineSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(Data.Builder().putString(OfflineSyncWorker.ACCOUNT_KEY, accountKey).build())
            .build()
        workManager.enqueueUniquePeriodicWork(
            periodicName(accountKey), ExistingPeriodicWorkPolicy.UPDATE, request,
        )
    }

    private fun oneTimeName(account: String) = "offline-sync-once::$account"
    private fun periodicName(account: String) = "offline-sync-periodic::$account"

    companion object {
        /** Сколько операции должны «висеть» в очереди, чтобы показать плашку синхронизации. */
        const val PENDING_BANNER_AGE_MS: Long = 15L * 60L * 1000L
    }
}

enum class SyncRunResult { SUCCESS, RETRY, BLOCKED, SESSION_CHANGED }

class OfflineSyncEngine(
    private val dao: OfflineDao,
    private val notesApi: NotesApi,
    private val cardsApi: HomeCardsApi,
    private val characterApi: CharacterApi,
    private val inventoryApi: InventoryApi,
    private val eventsApi: EventsApi,
    private val session: AuthSessionStore,
    private val achievementsApi: AchievementsApi? = null,
) {
    suspend fun sync(accountKey: String): SyncRunResult {
        if (normalizeAccountKey(session.getEmail()) != accountKey) return SyncRunResult.SESSION_CHANGED
        updateState(accountKey, "SYNCING")
        for (operation in dao.outbox(accountKey)) {
            if (normalizeAccountKey(session.getEmail()) != accountKey) return SyncRunResult.SESSION_CHANGED
            try {
                push(operation)
                dao.removeOutbox(operation.id)
            } catch (error: Throwable) {
                val statusCode = (error as? HttpException)?.code()
                if (operation.kind == OutboxKind.CHARACTER_STAT_UPGRADE && statusCode in setOf(400, 409)) {
                    dao.removeOutbox(operation.id)
                    replaceServerCharacter(accountKey, characterApi.getCharacter())
                    continue
                }
                // Конфликт экипировки (предмет удалён/уже надет) или бекенд ещё без
                // эндпоинтов инвентаря — принимаем серверное состояние и не роняем синк.
                if (operation.kind in setOf(OutboxKind.INVENTORY_EQUIP, OutboxKind.INVENTORY_UNEQUIP) &&
                    (statusCode in setOf(400, 404, 409) || isEndpointUnsupported(error))
                ) {
                    dao.removeOutbox(operation.id)
                    pullInventoryTolerant(accountKey)
                    continue
                }
                // Неизвестное событие или бекенд ещё без /events — снимаем клейм и
                // принимаем серверное состояние персонажа, не роняя синк.
                if (operation.kind == OutboxKind.EVENT_CLAIM &&
                    (statusCode in setOf(400, 404, 409) || isEndpointUnsupported(error))
                ) {
                    dao.removeOutbox(operation.id)
                    continue
                }
                // Достижения не критичны и бекенд может быть ещё без /achievements —
                // конфликт или отсутствие эндпоинта не должны ронять синк.
                if (operation.kind in setOf(OutboxKind.ACHIEVEMENT_UNLOCK, OutboxKind.ACHIEVEMENT_METRICS) &&
                    (statusCode in setOf(400, 404, 409) || isEndpointUnsupported(error))
                ) {
                    dao.removeOutbox(operation.id)
                    continue
                }
                val retryable = error is IOException || (error as? HttpException)?.code()?.let { it >= 500 } == true
                val unauthorized = (error as? HttpException)?.code() == 401
                if (unauthorized) return SyncRunResult.SESSION_CHANGED
                dao.failOutbox(operation.id, error.message, blocked = !retryable)
                updateState(accountKey, if (retryable) "OFFLINE" else "BLOCKED", error.message)
                return if (retryable) SyncRunResult.RETRY else SyncRunResult.BLOCKED
            }
        }

        return try {
            pull(accountKey)
            updateState(accountKey, "IDLE", lastSyncedAt = System.currentTimeMillis())
            SyncRunResult.SUCCESS
        } catch (error: Throwable) {
            val retryable = error is IOException || (error as? HttpException)?.code()?.let { it >= 500 } == true
            updateState(accountKey, if (retryable) "OFFLINE" else "BLOCKED", error.message)
            if (retryable) SyncRunResult.RETRY else SyncRunResult.BLOCKED
        }
    }

    private suspend fun push(operation: OutboxEntity) {
        when (operation.kind) {
            OutboxKind.NOTE_CREATE -> pushNoteCreate(operation)
            OutboxKind.NOTE_UPDATE -> pushNoteUpdate(operation)
            OutboxKind.NOTE_COMPLETION -> pushNoteCompletion(operation)
            OutboxKind.NOTE_DELETE -> deleteIgnoringMissing { notesApi.deleteNote(requireNotNull(operation.remoteId)) }
            OutboxKind.CARD_CREATE -> pushCardCreate(operation)
            OutboxKind.CARD_UPDATE -> pushCardUpdate(operation)
            OutboxKind.CARD_DELETE -> deleteIgnoringMissing { cardsApi.deleteHomeCard(requireNotNull(operation.remoteId)) }
            OutboxKind.CHARACTER_UPDATE -> {
                val sheet = localCharacter(operation.accountKey)
                characterApi.updateCharacter(sheet.toUpdateRequest())
            }
            OutboxKind.CHARACTER_XP -> {
                val payload = ApiJson.decodeFromString<XpPayload>(requireNotNull(operation.payload))
                characterApi.addExperience(CharacterXpRequestDto(
                    payload.characterXp, payload.skillKey, payload.skillXp, operation.operationId,
                ))
            }
            OutboxKind.CHARACTER_STAT_UPGRADE -> {
                val payload = ApiJson.decodeFromString<StatPayload>(requireNotNull(operation.payload))
                characterApi.upgradeStat(CharacterStatUpgradeRequestDto(operation.operationId, payload.statKey))
            }
            OutboxKind.CHARACTER_RENAME -> {
                val payload = ApiJson.decodeFromString<RenamePayload>(requireNotNull(operation.payload))
                characterApi.rename(CharacterRenameRequestDto(operation.operationId, payload.name))
            }
            OutboxKind.INVENTORY_EQUIP -> {
                val payload = ApiJson.decodeFromString<EquipPayload>(requireNotNull(operation.payload))
                inventoryApi.equip(InventoryEquipRequestDto(operation.operationId, payload.itemId))
            }
            OutboxKind.INVENTORY_UNEQUIP -> {
                val payload = ApiJson.decodeFromString<UnequipPayload>(requireNotNull(operation.payload))
                inventoryApi.unequip(InventoryUnequipRequestDto(operation.operationId, payload.slot))
            }
            OutboxKind.EVENT_CLAIM -> {
                val payload = ApiJson.decodeFromString<EventClaimPayload>(requireNotNull(operation.payload))
                eventsApi.claim(FocusEventClaimRequestDto(
                    operationId = operation.operationId,
                    eventKey = payload.eventKey,
                    skillKey = payload.skillKey,
                    locale = payload.locale,
                ))
            }
            OutboxKind.ACHIEVEMENT_UNLOCK -> {
                val api = achievementsApi ?: return
                val payload = ApiJson.decodeFromString<AchievementUnlockPayload>(requireNotNull(operation.payload))
                api.postUnlock(AchievementUnlockRequestDto(
                    operationId = operation.operationId,
                    achievementId = payload.achievementId,
                    tier = payload.tier,
                    points = payload.points,
                    unlockedAt = payload.unlockedAt,
                ))
            }
            OutboxKind.ACHIEVEMENT_METRICS -> {
                val api = achievementsApi ?: return
                // Снапшот берётся в момент push, а не enqueue: одна операция уносит
                // всё накопленное, поэтому частые инкременты не плодят очередь.
                // Летучие ключи (дневные/сессионные) на сервер не уходят.
                val metrics = dao.achievementMetrics(operation.accountKey)
                    .filter { AchievementMetrics.isSyncable(it.metricKey) }
                if (metrics.isNotEmpty()) {
                    api.putMetrics(AchievementMetricsRequestDto(
                        operationId = operation.operationId,
                        metrics = metrics.map { AchievementMetricDto(it.metricKey, it.value) },
                    ))
                }
            }
        }
    }

    private suspend fun pushNoteCreate(operation: OutboxEntity) {
        val row = dao.note(operation.accountKey, requireNotNull(operation.localId)) ?: return
        val created = notesApi.createNote(toDomain(row).toUpsertRequestDto(operation.operationId))
        dao.bindNoteRemoteId(row.note.localId, created.id)
    }

    private suspend fun pushNoteUpdate(operation: OutboxEntity) {
        val row = dao.note(operation.accountKey, requireNotNull(operation.localId)) ?: return
        val remoteId = row.note.remoteId ?: run {
            pushNoteCreate(operation.copy(operationId = operation.operationId))
            return
        }
        try {
            notesApi.updateNote(remoteId, toDomain(row).toUpsertRequestDto())
        } catch (error: HttpException) {
            if (error.code() != 404) throw error
            val recreated = notesApi.createNote(toDomain(row).toUpsertRequestDto(operation.operationId))
            dao.bindNoteRemoteId(row.note.localId, recreated.id)
        }
    }

    private suspend fun pushNoteCompletion(operation: OutboxEntity) {
        val localId = requireNotNull(operation.localId)
        val row = dao.note(operation.accountKey, localId) ?: return
        val remoteId = row.note.remoteId ?: run {
            pushNoteCreate(operation.copy(kind = OutboxKind.NOTE_CREATE))
            dao.note(operation.accountKey, localId)?.note?.remoteId
        } ?: return
        val payload = ApiJson.decodeFromString<CompletionPayloadWire>(requireNotNull(operation.payload))
        val response = notesApi.updateNoteCompletion(remoteId, NoteCompletionRequestDto(payload.isCompleted))
        replaceServerNote(operation.accountKey, response.completedNote, localId)
        response.nextNote?.let { next ->
            val provisional = dao.notes(operation.accountKey).firstOrNull {
                it.note.recurrenceParentLocalId == localId && it.note.isProvisional
            }
            replaceServerNote(operation.accountKey, next, provisional?.note?.localId ?: UUID.randomUUID().toString())
        }
    }

    private suspend fun pushCardCreate(operation: OutboxEntity) {
        val row = dao.card(operation.accountKey, requireNotNull(operation.localId)) ?: return
        val created = cardsApi.createHomeCard(toDomain(row).toUpsertRequestDto(operation.operationId))
        dao.bindCardRemoteId(row.card.localId, created.id)
    }

    private suspend fun pushCardUpdate(operation: OutboxEntity) {
        val row = dao.card(operation.accountKey, requireNotNull(operation.localId)) ?: return
        val remoteId = row.card.remoteId ?: run {
            pushCardCreate(operation)
            return
        }
        try {
            cardsApi.updateHomeCard(remoteId, toDomain(row).toUpsertRequestDto())
        } catch (error: HttpException) {
            if (error.code() != 404) throw error
            val recreated = cardsApi.createHomeCard(toDomain(row).toUpsertRequestDto(operation.operationId))
            dao.bindCardRemoteId(row.card.localId, recreated.id)
        }
    }

    private suspend fun pull(accountKey: String) {
        val serverNotes = notesApi.getNotes()
        val remoteNoteIds = serverNotes.mapTo(hashSetOf()) { it.id }
        serverNotes.forEach { dto ->
            val existing = dao.noteByRemoteId(accountKey, dto.id)
            if (existing == null || !dao.hasPending(accountKey, existing.note.localId)) {
                replaceServerNote(accountKey, dto, existing?.note?.localId ?: UUID.randomUUID().toString())
            }
        }
        dao.notes(accountKey).filter {
            it.note.remoteId != null && it.note.remoteId !in remoteNoteIds && !dao.hasPending(accountKey, it.note.localId)
        }.forEach { dao.deleteNote(accountKey, it.note.localId) }

        val serverCards = cardsApi.getHomeCards()
        val remoteCardIds = serverCards.mapTo(hashSetOf()) { it.id }
        serverCards.forEach { dto ->
            val existing = dao.cardByRemoteId(accountKey, dto.id)
            if (existing == null || !dao.hasPending(accountKey, existing.card.localId)) {
                replaceServerCard(accountKey, dto, existing?.card?.localId ?: UUID.randomUUID().toString())
            }
        }
        dao.cards(accountKey).filter {
            it.card.remoteId != null && it.card.remoteId !in remoteCardIds && !dao.hasPending(accountKey, it.card.localId)
        }.forEach { dao.deleteCard(accountKey, it.card.localId) }

        if (!dao.hasPending(accountKey, accountKey)) replaceServerCharacter(accountKey, characterApi.getCharacter())

        if (!dao.hasPending(accountKey, inventoryLocalId(accountKey))) {
            pullInventoryTolerant(accountKey)
        }

        pullFocusEventsTolerant()

        pullAchievementsTolerant(accountKey)
    }

    /**
     * Pull достижений, не роняющий синк, пока бекенд без /achievements. Метрики
     * сливаются по max(local, server) — счётчики монотонные; серверные анлоки
     * вставляются с notified=true, чтобы не тостить взятое на другом устройстве.
     * Пока в очереди висит локальный снапшот метрик, merge пропускается.
     */
    private suspend fun pullAchievementsTolerant(accountKey: String) {
        val api = achievementsApi ?: return
        if (dao.hasPending(accountKey, achievementsLocalId(accountKey))) return
        try {
            val snapshot = api.getAchievements()
            val local = dao.achievementMetrics(accountKey).associate { it.metricKey to it.value }
            val mergedChanges = snapshot.metrics
                .filter { AchievementMetrics.isSyncable(it.key) && it.value > (local[it.key] ?: 0) }
                .map { LocalAchievementMetricEntity(accountKey, it.key, it.value) }
            val serverUnlocks = snapshot.unlocks.map { dto ->
                LocalAchievementUnlockEntity(
                    accountKey = accountKey,
                    achievementId = dto.achievementId,
                    tier = dto.tier,
                    points = dto.points,
                    unlockedAt = dto.unlockedAt,
                    notified = true,
                )
            }
            if (mergedChanges.isNotEmpty() || serverUnlocks.isNotEmpty()) {
                dao.applyAchievementProgress(mergedChanges, serverUnlocks)
            }
        } catch (error: Throwable) {
            if (!isEndpointUnsupported(error)) throw error
        }
    }

    /**
     * Pull глобального каталога событий фокус-таймера в Room-кэш. Пока бекенд без
     * /events (404/405/HTML-заглушка) — молча пропускаем, клиент живёт на фолбэке.
     */
    private suspend fun pullFocusEventsTolerant() {
        try {
            val catalog = eventsApi.getCatalog().events.map { it.toDomain().toEntity() }
            if (catalog.isNotEmpty()) dao.replaceFocusEvents(catalog)
        } catch (error: Throwable) {
            if (!isEndpointUnsupported(error)) throw error
        }
    }

    /**
     * Pull инвентаря, не роняющий общий синк, пока бекенд не поддерживает /inventory:
     * 404/405 или HTML-заглушка вместо JSON (reverse-proxy) пропускаются молча.
     * Настоящие сетевые ошибки пробрасываются дальше как обычно.
     */
    private suspend fun pullInventoryTolerant(accountKey: String) {
        try {
            replaceServerInventory(accountKey, inventoryApi.getInventory())
        } catch (error: Throwable) {
            if (!isEndpointUnsupported(error)) throw error
        }
    }

    /** Ответ выглядит как «эндпоинта на бекенде ещё нет», а не как сетевая ошибка. */
    private fun isEndpointUnsupported(error: Throwable): Boolean {
        val code = (error as? HttpException)?.code()
        if (code == 404 || code == 405) return true
        // Вместо JSON пришёл HTML (заглушка nginx/лендинг) — ошибка парсинга kotlinx.serialization.
        var cause: Throwable? = error
        while (cause != null) {
            if (cause is SerializationException) return true
            cause = cause.cause
        }
        return false
    }

    private suspend fun replaceServerNote(account: String, dto: NoteDto, localId: String) {
        val domain = dto.toDomain()
        val now = System.currentTimeMillis()
        val entity = LocalNoteEntity(
            localId = localId, accountKey = account, remoteId = dto.id,
            title = domain.title, content = domain.content, category = domain.category.name,
            deadlineMillis = domain.deadlineMillis, startAtMillis = domain.startAtMillis,
            durationMinutes = domain.durationMinutes, repeatRule = domain.repeatRule.name,
            coinCount = domain.coinCount, isCompleted = domain.isCompleted,
            createdAt = dto.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = dto.updatedAt.takeIf { it > 0 } ?: now,
        )
        dao.replaceNote(entity, domain.checklist.mapIndexed { i, item ->
            LocalChecklistItemEntity(localId, i, item.text, item.isChecked)
        })
    }

    private suspend fun replaceServerCard(account: String, dto: HomeCardDto, localId: String) {
        dao.replaceCard(
            LocalHomeCardEntity(localId, account, dto.id, dto.title, dto.section, dto.note, dto.createdAt, dto.updatedAt),
            dto.fields.mapIndexed { i, field -> LocalHomeFieldEntity(localId, i, field.key, field.value) },
            dto.links.mapIndexed { i, link -> LocalHomeLinkEntity(localId, i, link) },
        )
    }

    private suspend fun replaceServerCharacter(account: String, dto: CharacterSheetDto) {
        dao.replaceCharacter(
            LocalCharacterEntity(account, dto.name, dto.level, dto.xp, dto.xpToNext, true, ApiJson.encodeToString(dto)),
            dto.stats.mapIndexed { i, stat -> LocalCharacterStatEntity(account, stat.key, i, stat.name, stat.description, stat.value) },
            dto.skills.mapIndexed { i, skill -> LocalCharacterSkillEntity(account, skill.key, i, skill.name, skill.level, skill.progress) },
            LocalWalletEntity(account, dto.wallet.earnedCoins, dto.wallet.spentCoins, dto.wallet.availableCoins, true),
        )
    }

    private suspend fun replaceServerInventory(account: String, dto: InventoryDto) {
        dao.replaceInventory(account, dto.toDomain().items.mapIndexed { i, item ->
            item.toEntity(account, i)
        })
    }

    private suspend fun localCharacter(account: String): CharacterSheet {
        val character = requireNotNull(dao.character(account))
        return CharacterSheet(
            name = character.name, level = character.level, xp = character.xp, xpToNext = character.xpToNext,
            portraitRes = R.drawable.mascot_stand_01,
            stats = dao.characterStats(account).map { CharacterStat(it.name, it.description, it.value, it.statKey) },
            skills = dao.characterSkills(account).map { CharacterSkill(it.name, it.level, it.progress.toFloat(), it.skillKey) },
        )
    }

    private suspend fun deleteIgnoringMissing(block: suspend () -> Unit) {
        try { block() } catch (error: HttpException) { if (error.code() != 404) throw error }
    }

    private suspend fun updateState(account: String, status: String, error: String? = null, lastSyncedAt: Long? = null) {
        dao.putSyncState(LocalSyncStateEntity(account, status, dao.pendingCount(account), error, lastSyncedAt))
    }

    @Serializable
    private data class CompletionPayloadWire(val isCompleted: Boolean)
}

class OfflineSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val account = inputData.getString(ACCOUNT_KEY) ?: return Result.failure()
        val app = applicationContext as? HomeNotesApplication ?: return Result.failure()
        val coordinator = app.appContainer.syncCoordinator
        coordinator.showSyncing()
        return when (app.appContainer.syncEngine.sync(account)) {
            SyncRunResult.SUCCESS, SyncRunResult.SESSION_CHANGED -> {
                coordinator.refreshStatus(account)
                Result.success()
            }
            SyncRunResult.RETRY -> {
                coordinator.reportOffline(account)
                Result.retry()
            }
            SyncRunResult.BLOCKED -> {
                coordinator.setRunning(SyncStatus.Blocked(null))
                Result.failure()
            }
        }
    }

    companion object { const val ACCOUNT_KEY = "account_key" }
}
