package app.homenotes.android

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ------------------------------------------------------------------ *
 * Hardcoded model. Swap [sampleCharacterSheet] for backend data later
 * (the screen reads only the [CharacterSheet] passed in).
 * ------------------------------------------------------------------ */

data class CharacterStat(
    val name: String,
    val description: String,
    val value: Int,
    val key: String = ""
) {
    /** D&D ability modifier: floor((value - 10) / 2). */
    val modifier: Int get() = Math.floorDiv(value - 10, 2)
}

data class CharacterSkill(
    val name: String,
    val level: Int,
    val progress: Float,
    val key: String = ""
)

data class CharacterSheet(
    val name: String,
    val level: Int,
    val xp: Int,
    val xpToNext: Int,
    val portraitRes: Int,
    val stats: List<CharacterStat>,
    val skills: List<CharacterSkill>
)

fun sampleCharacterSheet(): CharacterSheet = CharacterSheet(
    name = "Юра",
    level = 1,
    xp = 0,
    xpToNext = 100,
    portraitRes = R.drawable.mascot_stand_01,
    stats = listOf(
        CharacterStat("Сила", "Физическая мощь", 1),
        CharacterStat("Ловкость", "Проворство, рефлексы и равновесие", 1),
        CharacterStat("Телосложение", "Здоровье и выносливость", 1),
        CharacterStat("Интеллект", "Логика и память", 1),
        CharacterStat("Мудрость", "Восприимчивость и ментальная устойчивость", 1),
        CharacterStat("Харизма", "Уверенность, самообладание и обаяние", 1)
    ),
    skills = listOf(
        CharacterSkill("Лесоруб", 1, 0.0f),
        CharacterSkill("Плотник", 1, 0.0f),
        CharacterSkill("Архивариус", 1, 0.0f)
    )
)

const val CHARACTER_MAX_LEVEL = 100

/** Верхняя граница значения характеристики, выше которой прокачка недоступна. */
const val CHARACTER_STAT_MAX = 99

/**
 * Стоимость поднять характеристику на +1 от текущего значения [value], в монетах.
 * Растёт пропорционально текущему значению (квадратично): cost = value².
 * Минимум 1 монета, чтобы стартовая прокачка не была бесплатной.
 */
fun statUpgradeCost(value: Int): Int {
    val v = value.coerceAtLeast(0)
    return (v.toLong() * v.toLong()).coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
}

/**
 * Награда за повышение уровня: за каждый набранный уровень случайно повышается одна
 * характеристика (+1, не выше [CHARACTER_STAT_MAX]) или один навык (+1 к уровню).
 * Характеристики на максимуме в выбор не попадают. Чистая функция — [random] позволяет
 * детерминированно тестировать выбор.
 */
fun applyLevelUpRewards(
    sheet: CharacterSheet,
    levelsGained: Int,
    random: kotlin.random.Random = kotlin.random.Random.Default
): CharacterSheet {
    if (levelsGained <= 0) return sheet
    var stats = sheet.stats
    var skills = sheet.skills
    repeat(levelsGained) {
        val statTargets = stats.indices.filter { stats[it].value < CHARACTER_STAT_MAX }
        val totalChoices = statTargets.size + skills.size
        if (totalChoices == 0) return@repeat
        val pick = random.nextInt(totalChoices)
        if (pick < statTargets.size) {
            val idx = statTargets[pick]
            stats = stats.toMutableList().also { it[idx] = it[idx].copy(value = it[idx].value + 1) }
        } else {
            val idx = pick - statTargets.size
            skills = skills.toMutableList().also { it[idx] = it[idx].copy(level = it[idx].level + 1) }
        }
    }
    return sheet.copy(stats = stats, skills = skills)
}

/** XP needed to advance FROM [level]: 100 * 2^(level-1), capped at level 100. Mirrors the backend. */
fun characterXpToNext(level: Int): Int {
    if (level >= CHARACTER_MAX_LEVEL) return Int.MAX_VALUE
    val shift = level - 1
    if (shift >= 31) return Int.MAX_VALUE
    val value = 100L shl shift
    return if (value >= Int.MAX_VALUE.toLong()) Int.MAX_VALUE else value.toInt()
}

/** A level with its in-level XP and the 0..1 fill toward the next level. */
data class LeveledProgress(
    val level: Int,
    val xpInLevel: Int,
    val xpToNext: Int,
    val fraction: Float
)

/** Applies [earned] XP on top of a starting level + in-level XP, rolling over level-ups. */
fun applyCharacterXp(startLevel: Int, startXpInLevel: Double, earned: Double): LeveledProgress {
    var level = startLevel
    var xp = (startXpInLevel + earned).coerceAtLeast(0.0)
    while (level < CHARACTER_MAX_LEVEL && xp >= characterXpToNext(level)) {
        xp -= characterXpToNext(level)
        level++
    }
    val toNext = characterXpToNext(level)
    val fraction = if (level >= CHARACTER_MAX_LEVEL) 1f else (xp / toNext).toFloat().coerceIn(0f, 1f)
    return LeveledProgress(level = level, xpInLevel = xp.toInt(), xpToNext = toNext, fraction = fraction)
}

/* ------------------------------------------------------------------ */

@Composable
fun CharacterScreen(
    uiState: CharacterUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRename: (String) -> Unit = {},
    availableCoins: Int = 0,
    onUpgradeStat: (String) -> Unit = {},
    onOpenInventory: () -> Unit = {}
) {
    val scaffoldState = rememberScaffoldState()
    Scaffold(
        scaffoldState = scaffoldState,
        backgroundColor = CozyAuth.Cream,
        topBar = {
            CozyTopBar(title = stringResource(R.string.character_title), onBack = onBack)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(CozyAuth.Cream)
                .pixelScreenFrame()
        ) {
            when (uiState) {
                CharacterUiState.Loading -> NotesLoadingMessage()
                is CharacterUiState.Error -> NotesStatusMessage(
                    message = uiState.message.asString(),
                    actionText = stringResource(R.string.action_retry),
                    onAction = onRetry
                )

                is CharacterUiState.Content -> CharacterContent(
                    sheet = uiState.sheet,
                    onRename = onRename,
                    availableCoins = availableCoins,
                    onUpgradeStat = onUpgradeStat,
                    onOpenInventory = onOpenInventory
                )
            }
        }
    }
}

@Composable
private fun CharacterContent(
    sheet: CharacterSheet,
    onRename: (String) -> Unit,
    availableCoins: Int,
    onUpgradeStat: (String) -> Unit,
    onOpenInventory: () -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    if (showRenameDialog) {
        RenameDialog(
            currentName = sheet.name,
            onConfirm = {
                onRename(it)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        CharacterPortrait(sheet, onEditName = { showRenameDialog = true })
        Spacer(Modifier.height(16.dp))
        XpSection(sheet)
        Spacer(Modifier.height(16.dp))
        PixelOutlineButton(
            text = stringResource(R.string.inventory_open),
            onClick = onOpenInventory,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionLabel(stringResource(R.string.character_section_stats))
            CoinBalanceChip(availableCoins)
        }
        Spacer(Modifier.height(8.dp))
        StatsSection(
            stats = sheet.stats,
            availableCoins = availableCoins,
            onUpgradeStat = onUpgradeStat
        )
        Spacer(Modifier.height(20.dp))

        SectionLabel(stringResource(R.string.character_section_skills))
        Spacer(Modifier.height(8.dp))
        sheet.skills.forEach { skill ->
            SkillCard(skill)
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CharacterPortrait(sheet: CharacterSheet, onEditName: () -> Unit) {
    PixelPanel(
        fill = CozyAuth.CardCream,
        shadowOffset = 6,
        cornerRadius = 8,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CozyAuth.FieldCream)
                    .border(3.dp, CozyAuth.BrownOutline, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = ImageBitmap.imageResource(id = sheet.portraitRes),
                    contentDescription = stringResource(R.string.character_mascot_description),
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.size(78.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sheet.name,
                        fontFamily = CozyAuth.PixelFont,
                        color = CozyAuth.Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    IconButton(
                        onClick = onEditName,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.character_rename_title),
                            tint = CozyAuth.InkSoft
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                LevelBadge(sheet.level)
            }
        }
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = CozyAuth.CardCream,
        title = {
            Text(
                text = stringResource(R.string.character_rename_title),
                color = CozyAuth.Ink,
                fontFamily = CozyAuth.PixelFont
            )
        },
        text = {
            CozyTextField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(R.string.character_name_label)
            )
        },
        confirmButton = {
            PixelPrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = { onConfirm(text) },
                enabled = text.trim().isNotEmpty()
            )
        },
        dismissButton = {
            PixelOutlineButton(text = stringResource(R.string.action_cancel), onClick = onDismiss)
        }
    )
}

@Composable
private fun LevelBadge(level: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CozyAuth.Terracotta)
            .border(2.dp, CozyAuth.TerracottaDark, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.character_level_short, level),
            fontFamily = CozyAuth.PixelFont,
            color = Color(0xFFFFF6EC),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun XpSection(sheet: CharacterSheet) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.character_xp_label),
                fontFamily = CozyAuth.PixelFont,
                color = CozyAuth.InkSoft,
                fontSize = 13.sp
            )
            Text(
                text = "${sheet.xp} / ${sheet.xpToNext} XP",
                fontFamily = CozyAuth.PixelFont,
                color = CozyAuth.InkSoft,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        PixelBar(
            progress = if (sheet.xpToNext > 0) sheet.xp.toFloat() / sheet.xpToNext else 0f,
            fill = CozyAuth.Terracotta,
            height = 16.dp
        )
    }
}

@Composable
private fun StatsSection(
    stats: List<CharacterStat>,
    availableCoins: Int,
    onUpgradeStat: (String) -> Unit
) {
    // Tap a tile to reveal its description; only one is expanded at a time.
    var expandedName by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        stats.chunked(3).forEach { rowStats ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowStats.forEach { stat ->
                    StatTile(
                        stat = stat,
                        expanded = expandedName == stat.name,
                        availableCoins = availableCoins,
                        onClick = {
                            expandedName = if (expandedName == stat.name) null else stat.name
                        },
                        onUpgrade = { onUpgradeStat(stat.key) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowStats.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun StatTile(
    stat: CharacterStat,
    expanded: Boolean,
    availableCoins: Int,
    onClick: () -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mod = stat.modifier
    val modText = if (mod >= 0) "+$mod" else "$mod"
    val atMax = stat.value >= CHARACTER_STAT_MAX
    val cost = statUpgradeCost(stat.value)
    val canAfford = availableCoins >= cost
    PixelPanel(
        fill = CozyAuth.CardCream,
        shadowOffset = 4,
        cornerRadius = 6,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 14.dp, horizontal = 6.dp)
                .animateContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stat.name,
                fontFamily = CozyAuth.PixelFont,
                color = CozyAuth.InkSoft,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stat.value.toString(),
                fontFamily = CozyAuth.PixelFont,
                color = CozyAuth.Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            )
            Text(
                text = modText,
                fontFamily = CozyAuth.PixelFont,
                color = CozyAuth.Terracotta,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(10.dp))
            StatUpgradeButton(
                cost = cost,
                atMax = atMax,
                enabled = canAfford && !atMax,
                onClick = onUpgrade
            )
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stat.description,
                    fontFamily = CozyAuth.PixelFont,
                    color = CozyAuth.InkSoft,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Compact "+ cost🪙" upgrade button under a stat. Shows MAX once the cap is reached. */
@Composable
private fun StatUpgradeButton(
    cost: Int,
    atMax: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val face = when {
        atMax -> CozyAuth.FieldCream
        enabled -> CozyAuth.SoftGreen
        else -> CozyAuth.SoftGreen.copy(alpha = 0.4f)
    }
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(face)
            .border(2.dp, CozyAuth.BrownOutline, shape)
            .then(
                if (enabled && !atMax) Modifier.clickable { onClick() } else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (atMax) {
            Text(
                text = stringResource(R.string.character_stat_max),
                fontFamily = CozyAuth.PixelFont,
                color = CozyAuth.InkSoft,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        } else {
            Text(
                text = "+1",
                fontFamily = CozyAuth.PixelFont,
                color = CozyAuth.Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = cost.toString(),
                fontFamily = CozyAuth.PixelFont,
                color = CozyAuth.Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(Modifier.size(3.dp))
            Image(
                painter = painterResource(id = R.drawable.gold_coin),
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/** Shows how many coins are available to spend on upgrades, in the stats header. */
@Composable
private fun CoinBalanceChip(coins: Int) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(CozyAuth.FieldCream)
            .border(2.dp, CozyAuth.BrownOutline, shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.gold_coin),
            contentDescription = stringResource(R.string.total_coins_description),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = coins.toString(),
            fontFamily = CozyAuth.PixelFont,
            color = CozyAuth.Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun SkillCard(skill: CharacterSkill) {
    PixelPanel(
        fill = CozyAuth.CardCream,
        shadowOffset = 4,
        cornerRadius = 6,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = skill.name,
                    fontFamily = CozyAuth.PixelFont,
                    color = CozyAuth.Ink,
                    style = MaterialTheme.typography.subtitle1
                )
                Text(
                    text = stringResource(R.string.character_skill_level, skill.level),
                    fontFamily = CozyAuth.PixelFont,
                    color = CozyAuth.InkSoft,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            PixelBar(
                progress = skill.progress,
                fill = CozyAuth.SoftGreen,
                height = 12.dp
            )
        }
    }
}

/** Chunky pixel progress bar: outlined track with a flat coloured fill. */
@Composable
private fun PixelBar(
    progress: Float,
    fill: Color,
    height: androidx.compose.ui.unit.Dp,
    track: Color = CozyAuth.FieldCream
) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(track)
            .border(2.dp, CozyAuth.BrownOutline, shape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .padding(2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(fill)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontFamily = CozyAuth.PixelFont,
        color = CozyAuth.Ink,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )
}
