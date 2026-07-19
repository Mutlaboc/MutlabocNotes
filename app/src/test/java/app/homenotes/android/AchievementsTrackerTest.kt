package app.homenotes.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Чистая логика метрик и порогов достижений (без Room — см. AchievementsTracker). */
class AchievementsTrackerTest {

    private val account = "user@example.com"

    // --- Каталог ---

    @Test
    fun catalog_hasThirtyAchievements_withUniqueIdsAndStrings() {
        assertEquals(30, AchievementsCatalog.all.size)
        assertEquals(30, AchievementsCatalog.all.map { it.id }.distinct().size)
        assertEquals(30, AchievementsCatalog.all.map { it.titleRes }.distinct().size)
        assertEquals(30, AchievementsCatalog.all.map { it.descriptionRes }.distinct().size)
    }

    @Test
    fun catalog_thresholdsAreStrictlyAscending() {
        AchievementsCatalog.all.forEach { def ->
            assertEquals(def.id, def.thresholds, def.thresholds.sorted())
            assertEquals(def.id, 4, def.thresholds.distinct().size)
        }
    }

    @Test
    fun tiersReached_returnsAllCoveredTiers() {
        val def = AchievementsCatalog.byId("notes_created")!!
        assertTrue(def.tiersReached(0).isEmpty())
        assertEquals(listOf(AchievementTier.BRONZE), def.tiersReached(5))
        assertEquals(
            listOf(AchievementTier.BRONZE, AchievementTier.SILVER),
            def.tiersReached(120),
        )
        assertEquals(AchievementTier.entries.toList(), def.tiersReached(5000))
    }

    // --- Счётчики и максимумы ---

    @Test
    fun counter_accumulates() {
        val metrics = mutableMapOf<String, Long>()
        applyAchievementCounter(metrics, AchievementMetrics.NOTES_CREATED, 1, today = 10)
        applyAchievementCounter(metrics, AchievementMetrics.NOTES_CREATED, 4, today = 10)
        assertEquals(5L, metrics[AchievementMetrics.NOTES_CREATED])
    }

    @Test
    fun max_neverDecreases() {
        val metrics = mutableMapOf(AchievementMetrics.CHARACTER_LEVEL to 7L)
        applyAchievementMax(metrics, AchievementMetrics.CHARACTER_LEVEL, 5)
        assertEquals(7L, metrics[AchievementMetrics.CHARACTER_LEVEL])
        applyAchievementMax(metrics, AchievementMetrics.CHARACTER_LEVEL, 9)
        assertEquals(9L, metrics[AchievementMetrics.CHARACTER_LEVEL])
    }

    @Test
    fun dailyTrackedCounter_maintainsBestDay_acrossDayRollover() {
        val metrics = mutableMapOf<String, Long>()
        repeat(4) { applyAchievementCounter(metrics, AchievementMetrics.NOTES_COMPLETED, 1, today = 10) }
        assertEquals(4L, metrics[AchievementMetrics.NOTES_COMPLETED_BEST_DAY])

        // Новый день: дневной счётчик обнуляется, best_day остаётся.
        applyAchievementCounter(metrics, AchievementMetrics.NOTES_COMPLETED, 1, today = 11)
        assertEquals(5L, metrics[AchievementMetrics.NOTES_COMPLETED])
        assertEquals(1L, metrics["${AchievementMetrics.NOTES_COMPLETED}.today"])
        assertEquals(4L, metrics[AchievementMetrics.NOTES_COMPLETED_BEST_DAY])

        // День продуктивнее прежнего рекорда — best_day растёт.
        repeat(6) { applyAchievementCounter(metrics, AchievementMetrics.NOTES_COMPLETED, 1, today = 11) }
        assertEquals(7L, metrics[AchievementMetrics.NOTES_COMPLETED_BEST_DAY])
    }

    // --- Дни активности и серия ---

    @Test
    fun activity_countsEachDayOnce_andTracksStreak() {
        val metrics = mutableMapOf<String, Long>()
        touchAchievementActivity(metrics, today = 10)
        touchAchievementActivity(metrics, today = 10)
        assertEquals(1L, metrics[AchievementMetrics.ACTIVE_DAYS])
        assertEquals(1L, metrics[AchievementMetrics.ACTIVITY_STREAK_BEST])

        touchAchievementActivity(metrics, today = 11)
        touchAchievementActivity(metrics, today = 12)
        assertEquals(3L, metrics[AchievementMetrics.ACTIVE_DAYS])
        assertEquals(3L, metrics[AchievementMetrics.ACTIVITY_STREAK_BEST])
    }

    @Test
    fun activity_streakResetsAfterGap_bestStreakSurvives() {
        val metrics = mutableMapOf<String, Long>()
        touchAchievementActivity(metrics, today = 10)
        touchAchievementActivity(metrics, today = 11)
        touchAchievementActivity(metrics, today = 12)
        // Пропуск дня: серия начинается заново, рекорд серии сохраняется.
        touchAchievementActivity(metrics, today = 14)
        assertEquals(1L, metrics[AchievementMetrics.ACTIVITY_STREAK])
        assertEquals(3L, metrics[AchievementMetrics.ACTIVITY_STREAK_BEST])
        assertEquals(4L, metrics[AchievementMetrics.ACTIVE_DAYS])
    }

    // --- Анлоки ---

    @Test
    fun unlocks_singleTierCrossing() {
        val metrics = mapOf(AchievementMetrics.NOTES_CREATED to 5L)
        val unlocks = reachedAchievementUnlocks(
            account, listOf(AchievementMetrics.NOTES_CREATED), metrics, now = 123,
        )
        assertEquals(1, unlocks.size)
        with(unlocks.single()) {
            assertEquals("notes_created", achievementId)
            assertEquals(AchievementTier.BRONZE.name, tier)
            assertEquals(10, points)
            assertEquals(123L, unlockedAt)
        }
    }

    @Test
    fun unlocks_multiTierJump_inOneIncrement() {
        val metrics = mapOf(AchievementMetrics.NOTES_CREATED to 600L)
        val unlocks = reachedAchievementUnlocks(
            account, listOf(AchievementMetrics.NOTES_CREATED), metrics, now = 1,
        )
        assertEquals(
            listOf("BRONZE", "SILVER", "GOLD"),
            unlocks.map { it.tier },
        )
    }

    @Test
    fun unlocks_ignoreMetricsWithoutAchievements() {
        val metrics = mapOf(AchievementMetrics.FOCUS_SESSION_CURRENT to 3L)
        val unlocks = reachedAchievementUnlocks(
            account, listOf(AchievementMetrics.FOCUS_SESSION_CURRENT), metrics, now = 1,
        )
        assertTrue(unlocks.isEmpty())
    }

    @Test
    fun unlocks_belowThreshold_none() {
        val metrics = mapOf(AchievementMetrics.NOTES_CREATED to 4L)
        val unlocks = reachedAchievementUnlocks(
            account, listOf(AchievementMetrics.NOTES_CREATED), metrics, now = 1,
        )
        assertTrue(unlocks.isEmpty())
        assertNull(metrics["unexpected"])
    }

    @Test
    fun syncable_excludesVolatileKeys_keepsCheckableOnes() {
        // Летучие ключи не синхронизируются (max-merge между устройствами их исказил бы).
        assertTrue(!AchievementMetrics.isSyncable("notes_completed.today"))
        assertTrue(!AchievementMetrics.isSyncable("notes_completed.today_day"))
        assertTrue(!AchievementMetrics.isSyncable(AchievementMetrics.FOCUS_SESSION_CURRENT))
        assertTrue(!AchievementMetrics.isSyncable(AchievementMetrics.ACTIVITY_LAST_DAY))
        assertTrue(!AchievementMetrics.isSyncable(AchievementMetrics.ACTIVITY_STREAK))
        // Все проверяемые достижениями метрики — синхронизируемые.
        AchievementsCatalog.all.forEach { def ->
            assertTrue(def.metricKey, AchievementMetrics.isSyncable(def.metricKey))
        }
    }

    @Test
    fun points_fixedTenPerTier_totalTwelveHundred() {
        AchievementTier.entries.forEach { assertEquals(10, it.points) }
        val maxTotal = AchievementsCatalog.all.size * AchievementTier.entries.sumOf { it.points }
        assertEquals(1200, maxTotal)
    }
}
