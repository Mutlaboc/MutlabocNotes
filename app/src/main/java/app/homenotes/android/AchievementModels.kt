package app.homenotes.android

import androidx.annotation.StringRes

/* ------------------------------------------------------------------ *
 * Доменная модель достижений. Каждое достижение многоступенчатое:
 * бронза/серебро/золото/платина, по фиксированным очкам за ступень.
 * Прогресс считается по метрикам-счётчикам (см. AchievementsTracker).
 * ------------------------------------------------------------------ */

/** Ступень достижения. Порядок объявления — от бронзы к платине. */
enum class AchievementTier(val points: Int) {
    BRONZE(10), SILVER(10), GOLD(10), PLATINUM(10);

    companion object {
        fun fromKey(key: String?): AchievementTier? =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
    }
}

/**
 * Определение достижения из статического каталога. [thresholds] — ровно 4 порога
 * по возрастанию (бронза..платина) для метрики [metricKey]. Описание содержит
 * placeholder %1$d для порога, одна строка обслуживает все ступени.
 */
data class AchievementDefinition(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val metricKey: String,
    val thresholds: List<Long>,
    val icon: String,
) {
    init {
        require(thresholds.size == AchievementTier.entries.size) {
            "Achievement $id must have exactly ${AchievementTier.entries.size} thresholds"
        }
    }

    /** Порог для ступени. */
    fun thresholdFor(tier: AchievementTier): Long = thresholds[tier.ordinal]

    /** Все ступени, чей порог покрыт значением метрики [value]. */
    fun tiersReached(value: Long): List<AchievementTier> =
        AchievementTier.entries.filter { value >= thresholdFor(it) }
}
