package app.homenotes.android

import kotlin.random.Random
import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------ *
 * Доменная модель случайных событий фокус-таймера («полоса событий»).
 * Каталог живёт на бекенде (GET /events) и кэшируется в Room; пока кэш
 * пуст (первый запуск офлайн), используется встроенный фолбэк-набор.
 * ------------------------------------------------------------------ */

/** Тип события. Порядок — от частого к редкому. */
enum class FocusEventType {
    TEXT, CHARACTER_XP, SKILL_XP, ITEM, NEW_SKILL;

    companion object {
        fun fromKey(key: String?): FocusEventType =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: TEXT
    }
}

/** Предмет-награда события (обе локали, слоты/редкость — как в инвентаре). */
@Serializable
data class FocusEventItem(
    val key: String,
    val nameRu: String,
    val nameEn: String,
    val descriptionRu: String = "",
    val descriptionEn: String = "",
    val icon: String = "",
    val slot: String? = null,
    val rarity: String = "COMMON",
    val bonusStatKey: String? = null,
    val bonusStatNameRu: String? = null,
    val bonusStatNameEn: String? = null,
    val bonusValue: Int? = null,
) {
    fun name(english: Boolean): String = if (english) nameEn else nameRu
    fun description(english: Boolean): String = if (english) descriptionEn else descriptionRu
    fun bonusStatName(english: Boolean): String? = if (english) bonusStatNameEn else bonusStatNameRu
}

/** Новый навык-награда события. */
@Serializable
data class FocusEventNewSkill(
    val key: String,
    val nameRu: String,
    val nameEn: String,
) {
    fun name(english: Boolean): String = if (english) nameEn else nameRu
}

/** Одно событие каталога. [weight] — вес взвешенного ролла. */
data class FocusEvent(
    val key: String,
    val type: FocusEventType,
    val weight: Int,
    val textRu: String,
    val textEn: String,
    val characterXp: Int = 0,
    val skillXp: Int = 0,
    val item: FocusEventItem? = null,
    val newSkill: FocusEventNewSkill? = null,
) {
    fun text(english: Boolean): String = if (english) textEn else textRu
}

/** Запись полосы событий: уже отролленное событие с разрешёнными для UI полями. */
data class FocusFeedEntry(
    val id: Long,
    val type: FocusEventType,
    val text: String,
    val characterXp: Int = 0,
    val skillXp: Int = 0,
    val skillName: String? = null,
    val itemName: String? = null,
    val itemIcon: String? = null,
    val newSkillName: String? = null,
)

/** Взвешенно-случайный выбор события; null для пустого списка. */
fun pickWeightedFocusEvent(events: List<FocusEvent>, random: Random = Random.Default): FocusEvent? {
    val total = events.sumOf { it.weight.coerceAtLeast(0) }
    if (total <= 0) return null
    var roll = random.nextInt(total)
    for (event in events) {
        roll -= event.weight.coerceAtLeast(0)
        if (roll < 0) return event
    }
    return events.lastOrNull()
}

/**
 * Встроенный фолбэк-каталог на случай пустого Room-кэша (первый запуск без сети).
 * Только текст и опыт: предметы и новые навыки требуют серверного каталога.
 */
val builtInFocusEvents: List<FocusEvent> = listOf(
    FocusEvent("text-cozy-spot", FocusEventType.TEXT, 6,
        "Мутлабок нашёл уютное местечко под деревом и одобрительно кивает.",
        "Mutlaboc found a cozy spot under the tree and nods approvingly."),
    FocusEvent("text-butterfly", FocusEventType.TEXT, 6,
        "Мимо пролетела бабочка. Мутлабок проводил её взглядом.",
        "A butterfly fluttered by. Mutlaboc watched it go."),
    FocusEvent("text-grass-scent", FocusEventType.TEXT, 6,
        "Ветер принёс запах свежескошенной травы.",
        "The wind carried the scent of freshly cut grass."),
    FocusEvent("text-sunbeam", FocusEventType.TEXT, 6,
        "Солнечный луч пробился сквозь листву.",
        "A sunbeam broke through the leaves."),
    FocusEvent("text-humming", FocusEventType.TEXT, 6,
        "Мутлабок напевает песенку — работа спорится.",
        "Mutlaboc hums a tune — the work is going well."),
    FocusEvent("text-dew", FocusEventType.TEXT, 6,
        "В траве блеснула роса. Красота!",
        "Dew glistened in the grass. Lovely!"),
    FocusEvent("text-silence", FocusEventType.TEXT, 6,
        "Тишина. Только ветер в листве и ты за делом.",
        "Silence. Just the wind in the leaves and you at work."),
    FocusEvent("text-mint-tea", FocusEventType.TEXT, 6,
        "Мутлабок заварил чай с мятой. Пахнет чудесно.",
        "Mutlaboc brewed some mint tea. It smells wonderful."),
    FocusEvent("xp-focus", FocusEventType.CHARACTER_XP, 5,
        "Отличная концентрация!", "Excellent focus!", characterXp = 15),
    FocusEvent("xp-tidiness", FocusEventType.CHARACTER_XP, 5,
        "Порядок вокруг вдохновляет.", "The tidiness around is inspiring.", characterXp = 10),
    FocusEvent("xp-small-step", FocusEventType.CHARACTER_XP, 5,
        "Маленький шаг — тоже шаг.", "A small step is still a step.", characterXp = 6),
    FocusEvent("skill-practice", FocusEventType.SKILL_XP, 3,
        "Опыт приходит с практикой.", "Experience comes with practice.", skillXp = 35),
    FocusEvent("skill-muscle-memory", FocusEventType.SKILL_XP, 3,
        "Руки сами помнят, что делать.", "Your hands remember what to do.", skillXp = 20),
)
