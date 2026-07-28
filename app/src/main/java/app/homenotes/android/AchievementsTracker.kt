package app.homenotes.android

import app.homenotes.android.local.LocalAchievementMetricEntity
import app.homenotes.android.local.LocalAchievementUnlockEntity
import app.homenotes.android.local.OfflineDao
import app.homenotes.android.local.OutboxEntity
import app.homenotes.android.network.ApiJson
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/** Ключ outbox/hasPending для снапшота метрик достижений. */
internal fun achievementsLocalId(account: String): String = "achievements::$account"

@Serializable
internal data class AchievementUnlockPayload(
    val achievementId: String,
    val tier: String,
    val points: Int,
    val unlockedAt: Long,
)

/* ------------------------------------------------------------------ *
 * Чистая логика метрик — отдельными функциями, чтобы тестироваться
 * без Room. Все операции меняют переданную карту метрик на месте.
 * ------------------------------------------------------------------ */

/** Инкремент накопительной метрики; для DAILY_TRACKED ведёт дневной максимум `key.best_day`. */
internal fun applyAchievementCounter(
    metrics: MutableMap<String, Long>,
    key: String,
    delta: Long,
    today: Long,
) {
    metrics[key] = (metrics[key] ?: 0) + delta
    if (key in AchievementMetrics.DAILY_TRACKED) {
        val sameDay = metrics["$key.today_day"] == today
        val todayValue = (if (sameDay) metrics["$key.today"] ?: 0 else 0) + delta
        metrics["$key.today_day"] = today
        metrics["$key.today"] = todayValue
        applyAchievementMax(metrics, "$key.best_day", todayValue)
    }
}

/** Метрика-максимум: записывается, только если значение выросло. */
internal fun applyAchievementMax(metrics: MutableMap<String, Long>, key: String, value: Long) {
    if (value > (metrics[key] ?: 0)) metrics[key] = value
}

/** Дни активности и серия подряд: обновляются при любом зарегистрированном действии. */
internal fun touchAchievementActivity(metrics: MutableMap<String, Long>, today: Long) {
    val lastDay = metrics[AchievementMetrics.ACTIVITY_LAST_DAY]
    if (lastDay == today) return
    val streak = if (lastDay == today - 1) (metrics[AchievementMetrics.ACTIVITY_STREAK] ?: 0) + 1 else 1
    metrics[AchievementMetrics.ACTIVITY_LAST_DAY] = today
    metrics[AchievementMetrics.ACTIVITY_STREAK] = streak
    applyAchievementMax(metrics, AchievementMetrics.ACTIVITY_STREAK_BEST, streak)
    applyAchievementCounter(metrics, AchievementMetrics.ACTIVE_DAYS, 1, today)
}

/** Все ступени, покрытые текущими значениями изменившихся метрик (дедуп — на вставке IGNORE). */
internal fun reachedAchievementUnlocks(
    account: String,
    changedKeys: Collection<String>,
    metrics: Map<String, Long>,
    now: Long,
): List<LocalAchievementUnlockEntity> = changedKeys
    .flatMap { key -> AchievementsCatalog.byMetric[key].orEmpty() }
    .distinct()
    .flatMap { def ->
        def.tiersReached(metrics[def.metricKey] ?: 0).map { tier ->
            LocalAchievementUnlockEntity(
                accountKey = account,
                achievementId = def.id,
                tier = tier.name,
                points = tier.points,
                unlockedAt = now,
                notified = false,
            )
        }
    }

/**
 * Центральная точка учёта достижений. Хуки в репозиториях зовут [report]/[reportMax]
 * fire-and-forget: запись метрик и анлоков идёт на [scope] и никогда не ломает
 * основное действие. Помимо базовых счётчиков ведёт производные метрики:
 * дневные максимумы (`X.best_day`), серию дней активности (`activity.*`) и
 * лучшую фокус-сессию (`focus_session.*`). Новые анлоки уходят на бекенд через
 * outbox (ACHIEVEMENT_UNLOCK), метрики — троттлённым снапшотом (ACHIEVEMENT_METRICS).
 */
class AchievementsTracker(
    private val dao: OfflineDao,
    private val session: AuthSessionStore,
    private val scope: CoroutineScope,
    private val scheduler: SyncScheduler? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutex = Mutex()

    /** Инкремент накопительной метрики. */
    fun report(metricKey: String, delta: Long = 1) {
        if (delta <= 0) return
        submit { metrics, today -> applyAchievementCounter(metrics, metricKey, delta, today) }
    }

    /** Метрика-максимум (уровень персонажа и т.п.). */
    fun reportMax(metricKey: String, value: Long) {
        submit { metrics, _ -> applyAchievementMax(metrics, metricKey, value) }
    }

    /** Начало фокус-сессии: сбрасывает счётчик минут текущей сессии. */
    fun startFocusSession() {
        submit { metrics, _ -> metrics[AchievementMetrics.FOCUS_SESSION_CURRENT] = 0 }
    }

    /**
     * Минута фокуса: общий счётчик, минуты текущей сессии, лучшая сессия;
     * первая минута сессии засчитывает саму сессию.
     */
    fun reportFocusMinute() {
        submit { metrics, today ->
            applyAchievementCounter(metrics, AchievementMetrics.FOCUS_MINUTES, 1, today)
            val sessionMinutes = (metrics[AchievementMetrics.FOCUS_SESSION_CURRENT] ?: 0) + 1
            metrics[AchievementMetrics.FOCUS_SESSION_CURRENT] = sessionMinutes
            if (sessionMinutes == 1L) {
                applyAchievementCounter(metrics, AchievementMetrics.FOCUS_SESSIONS, 1, today)
            }
            applyAchievementMax(metrics, AchievementMetrics.FOCUS_SESSION_BEST, sessionMinutes)
        }
    }

    private fun submit(mutate: (MutableMap<String, Long>, today: Long) -> Unit) {
        val account = normalizeAccountKey(session.getEmail()) ?: return
        scope.launch {
            runCatching { apply(account, mutate) }
        }
    }

    private suspend fun apply(account: String, mutate: (MutableMap<String, Long>, Long) -> Unit) {
        mutex.withLock {
            val today = todayEpochDay()
            val stored = dao.achievementMetrics(account).associate { it.metricKey to it.value }
            val metrics = stored.toMutableMap()
            mutate(metrics, today)
            touchAchievementActivity(metrics, today)
            val changed = metrics.filter { (key, value) -> stored[key] != value }
            if (changed.isEmpty()) return

            val inserted = dao.applyAchievementProgress(
                changed.map { (key, value) -> LocalAchievementMetricEntity(account, key, value) },
                reachedAchievementUnlocks(account, changed.keys, metrics, clock()),
            )
            enqueueSync(account, inserted)
        }
    }

    /** Ставит новые анлоки и (без дублей) снапшот метрик в outbox на синхронизацию. */
    private suspend fun enqueueSync(account: String, inserted: List<LocalAchievementUnlockEntity>) {
        val now = clock()
        inserted.forEach { unlock ->
            dao.enqueue(OutboxEntity(
                accountKey = account,
                kind = OutboxKind.ACHIEVEMENT_UNLOCK,
                localId = null,
                operationId = UUID.randomUUID().toString(),
                payload = ApiJson.encodeToString(AchievementUnlockPayload(
                    achievementId = unlock.achievementId,
                    tier = unlock.tier,
                    points = unlock.points,
                    unlockedAt = unlock.unlockedAt,
                )),
                createdAt = now,
            ))
        }
        val metricsLocalId = achievementsLocalId(account)
        if (!dao.hasPending(account, metricsLocalId)) {
            dao.enqueue(OutboxEntity(
                accountKey = account,
                kind = OutboxKind.ACHIEVEMENT_METRICS,
                localId = metricsLocalId,
                operationId = UUID.randomUUID().toString(),
                createdAt = now,
            ))
        }
        scheduler?.request(account)
    }

    /** Номер дня в таймзоне устройства (без java.time — minSdk). */
    private fun todayEpochDay(): Long {
        val now = clock()
        return (now + TimeZone.getDefault().getOffset(now)) / DAY_MILLIS
    }

    private companion object {
        const val DAY_MILLIS = 86_400_000L
    }
}
