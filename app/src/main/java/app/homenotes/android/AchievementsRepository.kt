package app.homenotes.android

import app.homenotes.android.local.LocalAchievementUnlockEntity
import app.homenotes.android.local.OfflineDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Read-side достижений: наблюдаемые метрики, анлоки и суммарные очки текущего
 * аккаунта. Запись идёт только через [AchievementsTracker] и pull синка.
 */
class AchievementsRepository(
    private val dao: OfflineDao,
    private val session: AuthSessionStore,
) {
    fun observeMetrics(): Flow<Map<String, Long>> =
        dao.observeAchievementMetrics(requireAccount())
            .map { rows -> rows.associate { it.metricKey to it.value } }

    fun observeUnlocks(): Flow<List<LocalAchievementUnlockEntity>> =
        dao.observeAchievementUnlocks(requireAccount())

    /** Очередь тостов: взятые, но ещё не показанные ступени (старые первыми). */
    fun observeUnnotified(): Flow<List<LocalAchievementUnlockEntity>> =
        dao.observeUnnotifiedUnlocks(requireAccount())

    fun observeTotalPoints(): Flow<Int> =
        dao.observeAchievementPoints(requireAccount())

    suspend fun markNotified(achievementId: String, tier: String) {
        dao.markUnlockNotified(requireAccount(), achievementId, tier)
    }

    private fun requireAccount(): String = normalizeAccountKey(session.getEmail())
        ?: throw IllegalStateException("No active account")
}
