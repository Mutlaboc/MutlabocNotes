package app.homenotes.android

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.homenotes.android.local.LocalAchievementUnlockEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Достижение для списка на экране. */
data class AchievementUi(
    val definition: AchievementDefinition,
    val value: Long,
    val unlockedTiers: Set<AchievementTier>,
    val earnedPoints: Int,
) {
    /** Следующая невзятая ступень или null, если взята платина. */
    val nextTier: AchievementTier? =
        AchievementTier.entries.firstOrNull { it !in unlockedTiers }

    /** Порог, к которому идёт прогресс (для платины — её же порог). */
    val targetThreshold: Long =
        definition.thresholdFor(nextTier ?: AchievementTier.PLATINUM)
}

/** Плашка-тост о взятой ступени. */
data class AchievementToastUi(
    val definition: AchievementDefinition,
    val tier: AchievementTier,
    val points: Int,
)

class AchievementsViewModel(
    application: Application,
    private val repository: AchievementsRepository,
    private val tracker: AchievementsTracker? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AndroidViewModel(application) {

    var screenItems by mutableStateOf<List<AchievementUi>>(emptyList())
        private set

    var totalPoints by mutableStateOf(0)
        private set

    /** Голова очереди непоказанных анлоков; после markNotified поток подставит следующий. */
    var pendingToast by mutableStateOf<AchievementToastUi?>(null)
        private set

    private var collectJob: Job? = null

    /**
     * Переключение аккаунта: коллекторы пересоздаются, чтобы очередь тостов и очки
     * не протекали между пользователями; null (выход) очищает состояние.
     */
    fun setUser(email: String?) {
        collectJob?.cancel()
        collectJob = null
        screenItems = emptyList()
        totalPoints = 0
        pendingToast = null
        if (normalizeAccountKey(email) == null) return

        collectJob = viewModelScope.launch(ioDispatcher) {
            runCatching {
                combine(
                    repository.observeMetrics(),
                    repository.observeUnlocks(),
                    repository.observeTotalPoints(),
                    repository.observeUnnotified(),
                ) { metrics, unlocks, points, unnotified ->
                    // Голова очереди — первый анлок, известный каталогу (неизвестные
                    // серверные записи пропускаются, не блокируя остальные).
                    val toast = unnotified.asSequence().mapNotNull { it.toToast() }.firstOrNull()
                    Snapshot(buildItems(metrics, unlocks), points, toast)
                }.collect { snapshot ->
                    withContext(Dispatchers.Main) {
                        screenItems = snapshot.items
                        totalPoints = snapshot.points
                        pendingToast = snapshot.toast
                    }
                }
            }
        }
    }

    fun onToastDismissed(toast: AchievementToastUi) = markNotified(toast)

    fun onToastTapped(toast: AchievementToastUi) = markNotified(toast)

    /** Голосовой ввод — единственная метрика, репортящаяся из UI-слоя (нет репозитория). */
    fun reportVoiceInputUsed() {
        tracker?.report(AchievementMetrics.VOICE_INPUTS)
    }

    private fun markNotified(toast: AchievementToastUi) {
        viewModelScope.launch(ioDispatcher) {
            runCatching { repository.markNotified(toast.definition.id, toast.tier.name) }
        }
    }

    private data class Snapshot(
        val items: List<AchievementUi>,
        val points: Int,
        val toast: AchievementToastUi?,
    )

    private fun buildItems(
        metrics: Map<String, Long>,
        unlocks: List<LocalAchievementUnlockEntity>,
    ): List<AchievementUi> {
        val unlocksById = unlocks.groupBy { it.achievementId }
        return AchievementsCatalog.all.map { def ->
            val rows = unlocksById[def.id].orEmpty()
            AchievementUi(
                definition = def,
                value = metrics[def.metricKey] ?: 0,
                unlockedTiers = rows.mapNotNull { AchievementTier.fromKey(it.tier) }.toSet(),
                earnedPoints = rows.sumOf { it.points },
            )
        }
    }

    private fun LocalAchievementUnlockEntity.toToast(): AchievementToastUi? {
        val definition = AchievementsCatalog.byId(achievementId) ?: return null
        val tier = AchievementTier.fromKey(tier) ?: return null
        return AchievementToastUi(definition, tier, points)
    }
}
