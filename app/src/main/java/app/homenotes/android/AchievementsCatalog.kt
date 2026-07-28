package app.homenotes.android

/**
 * Ключи метрик достижений. Базовые ключи инкрементируются хуками в репозиториях;
 * ключи с точечными суффиксами — производные, их ведёт AchievementsTracker
 * (`.best_day` — максимум за день, `activity.*` — серия дней, `focus_session.*` —
 * лучшая сессия). Достижения ссылаются только на стабильные проверяемые ключи.
 */
object AchievementMetrics {
    // Заметки
    const val NOTES_CREATED = "notes_created"
    const val NOTES_CREATED_SHOPPING = "notes_created_shopping"
    const val NOTES_CREATED_TASKS = "notes_created_tasks"
    const val NOTES_CREATED_RECURRING = "notes_created_recurring"
    const val NOTES_COMPLETED = "notes_completed"
    const val NOTES_COMPLETED_BEST_DAY = "notes_completed.best_day"
    const val NOTES_EDITED = "notes_edited"
    const val NOTES_DELETED = "notes_deleted"
    const val DEADLINES_SET = "deadlines_set"
    const val COMPLETED_BEFORE_DEADLINE = "completed_before_deadline"
    const val COMPLETED_EARLY_MORNING = "completed_early_morning"
    const val CHECKLIST_ITEMS_CHECKED = "checklist_items_checked"
    const val VOICE_INPUTS = "voice_inputs"

    // Фокус
    const val FOCUS_MINUTES = "focus_minutes"
    const val FOCUS_SESSIONS = "focus_sessions"
    const val FOCUS_SESSION_BEST = "focus_session.best"
    const val FOCUS_EVENTS_RECEIVED = "focus_events_received"

    // Персонаж
    const val CHARACTER_LEVEL = "character_level"
    const val CHARACTER_XP_EARNED = "character_xp_earned"
    const val SKILLS_LEARNED = "skills_learned"
    const val STAT_UPGRADES = "stat_upgrades"
    const val CHARACTER_RENAMES = "character_renames"

    // Инвентарь и монеты
    const val ITEMS_RECEIVED = "items_received"
    const val ITEMS_EQUIPPED = "items_equipped"
    const val COINS_EARNED = "coins_earned"
    const val COINS_SPENT = "coins_spent"

    // Дом
    const val HOME_CARDS_CREATED = "home_cards_created"
    const val HOME_CARDS_EDITED = "home_cards_edited"

    // Регулярность
    const val ACTIVE_DAYS = "active_days"
    const val ACTIVITY_STREAK_BEST = "activity.streak_best"

    // Служебные производные ключи (достижения на них не ссылаются)
    const val FOCUS_SESSION_CURRENT = "focus_session.current"
    const val ACTIVITY_LAST_DAY = "activity.last_day"
    const val ACTIVITY_STREAK = "activity.streak"

    /** Базовые метрики, для которых ведётся дневной максимум `X.best_day`. */
    val DAILY_TRACKED: Set<String> = setOf(NOTES_COMPLETED)

    /**
     * Участвует ли ключ в синхронизации с бекендом. Летучие ключи (дневные счётчики,
     * текущая сессия, пара last_day/streak) живут только на устройстве: merge по max
     * между устройствами исказил бы их — например, вчерашний `X.today` с сервера
     * завысил бы сегодняшний дневной рекорд.
     */
    fun isSyncable(key: String): Boolean =
        !key.endsWith(".today") && !key.endsWith(".today_day") &&
            key != FOCUS_SESSION_CURRENT && key != ACTIVITY_LAST_DAY && key != ACTIVITY_STREAK
}

/** Статический каталог достижений: 30 штук, каждое с 4 ступенями. */
object AchievementsCatalog {
    val all: List<AchievementDefinition> = listOf(
        // --- Заметки ---
        AchievementDefinition(
            id = "notes_created",
            titleRes = R.string.achievement_notes_created_title,
            descriptionRes = R.string.achievement_notes_created_desc,
            metricKey = AchievementMetrics.NOTES_CREATED,
            thresholds = listOf(5, 50, 500, 5000),
            icon = "📝",
        ),
        AchievementDefinition(
            id = "notes_completed",
            titleRes = R.string.achievement_notes_completed_title,
            descriptionRes = R.string.achievement_notes_completed_desc,
            metricKey = AchievementMetrics.NOTES_COMPLETED,
            thresholds = listOf(5, 50, 500, 5000),
            icon = "✅",
        ),
        AchievementDefinition(
            id = "notes_shopping",
            titleRes = R.string.achievement_notes_shopping_title,
            descriptionRes = R.string.achievement_notes_shopping_desc,
            metricKey = AchievementMetrics.NOTES_CREATED_SHOPPING,
            thresholds = listOf(3, 25, 150, 1000),
            icon = "🛒",
        ),
        AchievementDefinition(
            id = "notes_tasks",
            titleRes = R.string.achievement_notes_tasks_title,
            descriptionRes = R.string.achievement_notes_tasks_desc,
            metricKey = AchievementMetrics.NOTES_CREATED_TASKS,
            thresholds = listOf(3, 25, 150, 1000),
            icon = "📋",
        ),
        AchievementDefinition(
            id = "notes_recurring",
            titleRes = R.string.achievement_notes_recurring_title,
            descriptionRes = R.string.achievement_notes_recurring_desc,
            metricKey = AchievementMetrics.NOTES_CREATED_RECURRING,
            thresholds = listOf(1, 10, 50, 250),
            icon = "🔁",
        ),
        AchievementDefinition(
            id = "notes_edited",
            titleRes = R.string.achievement_notes_edited_title,
            descriptionRes = R.string.achievement_notes_edited_desc,
            metricKey = AchievementMetrics.NOTES_EDITED,
            thresholds = listOf(10, 100, 500, 2500),
            icon = "✏️",
        ),
        AchievementDefinition(
            id = "notes_deleted",
            titleRes = R.string.achievement_notes_deleted_title,
            descriptionRes = R.string.achievement_notes_deleted_desc,
            metricKey = AchievementMetrics.NOTES_DELETED,
            thresholds = listOf(5, 25, 150, 1000),
            icon = "🧹",
        ),
        AchievementDefinition(
            id = "deadlines_set",
            titleRes = R.string.achievement_deadlines_set_title,
            descriptionRes = R.string.achievement_deadlines_set_desc,
            metricKey = AchievementMetrics.DEADLINES_SET,
            thresholds = listOf(5, 25, 150, 1000),
            icon = "📅",
        ),
        AchievementDefinition(
            id = "before_deadline",
            titleRes = R.string.achievement_before_deadline_title,
            descriptionRes = R.string.achievement_before_deadline_desc,
            metricKey = AchievementMetrics.COMPLETED_BEFORE_DEADLINE,
            thresholds = listOf(3, 25, 100, 500),
            icon = "⏰",
        ),
        AchievementDefinition(
            id = "checklist_checked",
            titleRes = R.string.achievement_checklist_checked_title,
            descriptionRes = R.string.achievement_checklist_checked_desc,
            metricKey = AchievementMetrics.CHECKLIST_ITEMS_CHECKED,
            thresholds = listOf(10, 100, 1000, 5000),
            icon = "☑️",
        ),
        AchievementDefinition(
            id = "productive_day",
            titleRes = R.string.achievement_productive_day_title,
            descriptionRes = R.string.achievement_productive_day_desc,
            metricKey = AchievementMetrics.NOTES_COMPLETED_BEST_DAY,
            thresholds = listOf(3, 5, 10, 20),
            icon = "🚀",
        ),
        AchievementDefinition(
            id = "voice_inputs",
            titleRes = R.string.achievement_voice_inputs_title,
            descriptionRes = R.string.achievement_voice_inputs_desc,
            metricKey = AchievementMetrics.VOICE_INPUTS,
            thresholds = listOf(1, 10, 50, 250),
            icon = "🎙️",
        ),
        // --- Фокус ---
        AchievementDefinition(
            id = "focus_minutes",
            titleRes = R.string.achievement_focus_minutes_title,
            descriptionRes = R.string.achievement_focus_minutes_desc,
            metricKey = AchievementMetrics.FOCUS_MINUTES,
            thresholds = listOf(5, 60, 600, 6000),
            icon = "🧘",
        ),
        AchievementDefinition(
            id = "focus_marathon",
            titleRes = R.string.achievement_focus_marathon_title,
            descriptionRes = R.string.achievement_focus_marathon_desc,
            metricKey = AchievementMetrics.FOCUS_SESSION_BEST,
            thresholds = listOf(5, 15, 30, 60),
            icon = "🏃",
        ),
        AchievementDefinition(
            id = "focus_sessions",
            titleRes = R.string.achievement_focus_sessions_title,
            descriptionRes = R.string.achievement_focus_sessions_desc,
            metricKey = AchievementMetrics.FOCUS_SESSIONS,
            thresholds = listOf(1, 10, 100, 1000),
            icon = "🎯",
        ),
        AchievementDefinition(
            id = "focus_events",
            titleRes = R.string.achievement_focus_events_title,
            descriptionRes = R.string.achievement_focus_events_desc,
            metricKey = AchievementMetrics.FOCUS_EVENTS_RECEIVED,
            thresholds = listOf(5, 50, 500, 2500),
            icon = "✨",
        ),
        // --- Персонаж ---
        AchievementDefinition(
            id = "character_level",
            titleRes = R.string.achievement_character_level_title,
            descriptionRes = R.string.achievement_character_level_desc,
            metricKey = AchievementMetrics.CHARACTER_LEVEL,
            thresholds = listOf(2, 5, 10, 25),
            icon = "🌟",
        ),
        AchievementDefinition(
            id = "character_xp",
            titleRes = R.string.achievement_character_xp_title,
            descriptionRes = R.string.achievement_character_xp_desc,
            metricKey = AchievementMetrics.CHARACTER_XP_EARNED,
            thresholds = listOf(100, 1000, 10000, 100000),
            icon = "📖",
        ),
        AchievementDefinition(
            id = "skills_learned",
            titleRes = R.string.achievement_skills_learned_title,
            descriptionRes = R.string.achievement_skills_learned_desc,
            metricKey = AchievementMetrics.SKILLS_LEARNED,
            thresholds = listOf(1, 3, 6, 12),
            icon = "🎓",
        ),
        AchievementDefinition(
            id = "stat_upgrades",
            titleRes = R.string.achievement_stat_upgrades_title,
            descriptionRes = R.string.achievement_stat_upgrades_desc,
            metricKey = AchievementMetrics.STAT_UPGRADES,
            thresholds = listOf(1, 5, 25, 100),
            icon = "💪",
        ),
        AchievementDefinition(
            id = "character_renames",
            titleRes = R.string.achievement_character_renames_title,
            descriptionRes = R.string.achievement_character_renames_desc,
            metricKey = AchievementMetrics.CHARACTER_RENAMES,
            thresholds = listOf(1, 3, 10, 25),
            icon = "🎭",
        ),
        // --- Инвентарь и монеты ---
        AchievementDefinition(
            id = "items_received",
            titleRes = R.string.achievement_items_received_title,
            descriptionRes = R.string.achievement_items_received_desc,
            metricKey = AchievementMetrics.ITEMS_RECEIVED,
            thresholds = listOf(1, 10, 50, 200),
            icon = "🎁",
        ),
        AchievementDefinition(
            id = "items_equipped",
            titleRes = R.string.achievement_items_equipped_title,
            descriptionRes = R.string.achievement_items_equipped_desc,
            metricKey = AchievementMetrics.ITEMS_EQUIPPED,
            thresholds = listOf(1, 10, 50, 200),
            icon = "🛡️",
        ),
        AchievementDefinition(
            id = "coins_earned",
            titleRes = R.string.achievement_coins_earned_title,
            descriptionRes = R.string.achievement_coins_earned_desc,
            metricKey = AchievementMetrics.COINS_EARNED,
            thresholds = listOf(10, 100, 1000, 10000),
            icon = "💰",
        ),
        AchievementDefinition(
            id = "coins_spent",
            titleRes = R.string.achievement_coins_spent_title,
            descriptionRes = R.string.achievement_coins_spent_desc,
            metricKey = AchievementMetrics.COINS_SPENT,
            thresholds = listOf(5, 50, 500, 5000),
            icon = "🪙",
        ),
        // --- Дом ---
        AchievementDefinition(
            id = "home_cards_created",
            titleRes = R.string.achievement_home_cards_created_title,
            descriptionRes = R.string.achievement_home_cards_created_desc,
            metricKey = AchievementMetrics.HOME_CARDS_CREATED,
            thresholds = listOf(1, 5, 15, 50),
            icon = "🏠",
        ),
        AchievementDefinition(
            id = "home_cards_edited",
            titleRes = R.string.achievement_home_cards_edited_title,
            descriptionRes = R.string.achievement_home_cards_edited_desc,
            metricKey = AchievementMetrics.HOME_CARDS_EDITED,
            thresholds = listOf(3, 15, 75, 300),
            icon = "🔧",
        ),
        // --- Регулярность ---
        AchievementDefinition(
            id = "active_days",
            titleRes = R.string.achievement_active_days_title,
            descriptionRes = R.string.achievement_active_days_desc,
            metricKey = AchievementMetrics.ACTIVE_DAYS,
            thresholds = listOf(3, 14, 60, 365),
            icon = "🗓️",
        ),
        AchievementDefinition(
            id = "streak",
            titleRes = R.string.achievement_streak_title,
            descriptionRes = R.string.achievement_streak_desc,
            metricKey = AchievementMetrics.ACTIVITY_STREAK_BEST,
            thresholds = listOf(3, 7, 30, 100),
            icon = "🔥",
        ),
        AchievementDefinition(
            id = "early_bird",
            titleRes = R.string.achievement_early_bird_title,
            descriptionRes = R.string.achievement_early_bird_desc,
            metricKey = AchievementMetrics.COMPLETED_EARLY_MORNING,
            thresholds = listOf(1, 10, 50, 200),
            icon = "🐤",
        ),
    )

    /** Достижения по метрике — для быстрой проверки порогов в трекере. */
    val byMetric: Map<String, List<AchievementDefinition>> = all.groupBy { it.metricKey }

    private val byId: Map<String, AchievementDefinition> = all.associateBy { it.id }

    fun byId(id: String): AchievementDefinition? = byId[id]
}
