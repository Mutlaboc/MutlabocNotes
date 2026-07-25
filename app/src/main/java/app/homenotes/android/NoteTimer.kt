package app.homenotes.android

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Formats milliseconds as mm:ss (or h:mm:ss past an hour). */
fun formatTimer(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private const val SPLIT_SLIDE_DP = 64f

/** Интервал ролла случайного события во время работы таймера. */
private const val FOCUS_EVENT_INTERVAL_MS = 60_000L

/** Как часто меняется фон/биом сцены фокуса. */
private const val FOCUS_SCENE_ROTATION_MS = 5 * 60_000L

/**
 * Доля экрана под сценой (нижней части достаётся `2 - FOCUS_SCENE_WEIGHT`).
 * 0.85 вместо ровной половины — картинка поднята примерно на 15 %, чтобы под ней
 * помещались и лента событий, и модуль выполнения.
 */
private const val FOCUS_SCENE_WEIGHT = 0.85f

/**
 * Full-screen "focus" overlay shown while a note's timer runs. The screen splits at the
 * centre: the top half is the yard animation (meadow + tree + strolling mascot, no house),
 * the bottom half is the focus-event feed (a random event rolls in once per minute of
 * running time) and the timer window with the controls.
 */
@Composable
fun ExpandedNoteOverlay(
    note: Note,
    accumulatedMs: Long,
    running: Boolean,
    runStartUptime: Long,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onComplete: () -> Unit,
    onReturnHome: () -> Unit,
    onClosed: () -> Unit,
    focusEvents: List<FocusFeedEntry> = emptyList(),
    onFocusMinuteTick: () -> Unit = {},
    onChecklistItemToggle: (index: Int, checked: Boolean) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    DisposableEffect(view) {
        val previousKeepScreenOn = view.keepScreenOn
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = previousKeepScreenOn
        }
    }

    // Tick locally so only this overlay recomposes while the stopwatch runs.
    var now by remember { mutableStateOf(SystemClock.uptimeMillis()) }
    LaunchedEffect(running) {
        while (running) {
            now = SystemClock.uptimeMillis()
            delay(200)
        }
    }
    val elapsed = if (running) {
        accumulatedMs + (now - runStartUptime).coerceAtLeast(0L)
    } else {
        accumulatedMs
    }
    val timerText = formatTimer(elapsed)

    // Раз в минуту работы таймера роллим случайное событие. Пауза останавливает
    // отсчёт; после возобновления минута отсчитывается заново.
    LaunchedEffect(running) {
        while (running) {
            delay(FOCUS_EVENT_INTERVAL_MS)
            onFocusMinuteTick()
        }
    }

    val progress = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(320, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(closing) {
        if (closing) {
            progress.animateTo(0f, animationSpec = tween(240, easing = FastOutSlowInEasing))
            onClosed()
        }
    }
    fun closeWith(action: () -> Unit) {
        if (!closing) {
            action()
            closing = true
        }
    }

    // Back press returns to the notes screen (instead of the exit dialog).
    BackHandler(enabled = !closing) { closeWith(onReturnHome) }

    Column(modifier = modifier.fillMaxSize()) {
        val focusAnimationsEnabled = rememberAnimationsEnabled()
        val focusSceneElapsed = rememberElapsedMillis(focusAnimationsEnabled)
        val focusBiomeFrames = rememberFocusBiomeMascotFrames()
        // Scene 0 is the existing meadow (unchanged); 1..N are the rotating biomes. Random
        // start so two overlapping focus sessions don't all open on the same scene.
        var sceneIndex by remember { mutableStateOf(if (focusAnimationsEnabled) (0..FOCUS_BIOME_SCENES.size).random() else 0) }
        LaunchedEffect(focusAnimationsEnabled) {
            if (focusAnimationsEnabled) {
                while (true) {
                    delay(FOCUS_SCENE_ROTATION_MS)
                    sceneIndex = (sceneIndex + 1) % (FOCUS_BIOME_SCENES.size + 1)
                }
            }
        }

        // TOP — animated yard scene (no house), rotating through the meadow + biomes.
        // Сцена занимает чуть меньше половины экрана (0.85 против 1.15 снизу): так
        // картинка поднимается вверх, а лента событий и модуль таймера помещаются
        // целиком. Фоны биомов широкие (2048x768), при Crop высоту задаёт контейнер —
        // вертикально они по-прежнему видны полностью, groundFrac не съезжает.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(FOCUS_SCENE_WEIGHT)
                .clipToBounds()
                .graphicsLayer {
                    translationY = SPLIT_SLIDE_DP.dp.toPx() * (1f - progress.value)
                    alpha = progress.value
                }
                .background(CozyAuth.Cream)
        ) {
            Crossfade(targetState = sceneIndex, animationSpec = tween(700), label = "focus_scene") { index ->
                if (index == 0) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.home_meadow_background),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        HomeYardScene(
                            animationRestartKey = Unit,
                            showHouse = false,
                            startDelayMs = 0L,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    FocusBiomeScene(
                        scene = FOCUS_BIOME_SCENES[index - 1],
                        elapsed = focusSceneElapsed,
                        animationsEnabled = focusAnimationsEnabled,
                        frames = focusBiomeFrames,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // BOTTOM — event feed + timer window, parts downward from the centre seam.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f - FOCUS_SCENE_WEIGHT)
                .graphicsLayer {
                    translationY = -SPLIT_SLIDE_DP.dp.toPx() * (1f - progress.value)
                    alpha = progress.value
                }
                .background(CozyAuth.Cream)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            FocusEventFeed(entries = focusEvents)
            Spacer(Modifier.height(16.dp))
            TimerWindow(
                note = note,
                timerText = timerText,
                running = running,
                onPauseResume = { if (running) onPause() else onResume() },
                onComplete = { closeWith(onComplete) },
                onReturnHome = { closeWith(onReturnHome) },
                onChecklistItemToggle = onChecklistItemToggle
            )
        }
    }
}

/* ------------------------------------------------------------------ *
 * Полоса событий: случайные события сессии, новые сверху. Типы событий
 * различаются цветом пиксельного маркера и строки награды.
 * ------------------------------------------------------------------ */

/** Цвет маркера и строки награды для типа события. */
internal fun focusEventColor(type: FocusEventType): Color = when (type) {
    FocusEventType.TEXT -> CozyAuth.InkSoft
    FocusEventType.CHARACTER_XP -> CozyAuth.Terracotta
    FocusEventType.SKILL_XP -> CozyAuth.SoftGreen
    FocusEventType.ITEM -> CozyAuth.MutedYellow
    FocusEventType.NEW_SKILL -> FocusNewSkillViolet
}

/** Фиолетовый для самого редкого события — нового навыка (в палитре CozyAuth его нет). */
private val FocusNewSkillViolet = Color(0xFF8A6FB8)

@Composable
private fun FocusEventFeed(entries: List<FocusFeedEntry>, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CozyAuth.FieldCream)
            .border(2.dp, CozyAuth.BrownOutline, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.focus_events_title),
            fontFamily = CozyAuth.PixelFont,
            color = CozyAuth.Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(6.dp))
        if (entries.isEmpty()) {
            Text(
                text = stringResource(R.string.focus_events_empty),
                fontFamily = CozyAuth.PixelFont,
                color = CozyAuth.Hint,
                fontSize = 12.sp
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 148.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Новые события сверху — прокручивать не нужно.
                entries.asReversed().forEach { entry ->
                    FocusEventRow(entry)
                }
            }
        }
    }
}

@Composable
private fun FocusEventRow(entry: FocusFeedEntry) {
    val accent = focusEventColor(entry.type)
    Row(modifier = Modifier.fillMaxWidth()) {
        // Пиксельный маркер типа события.
        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(8.dp)
                .background(accent)
                .border(1.dp, CozyAuth.BrownOutline)
        )
        Spacer(Modifier.size(8.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = entry.text,
                fontFamily = CozyAuth.PixelFont,
                color = CozyAuth.InkSoft,
                fontSize = 12.sp
            )
            focusEventRewardText(entry)?.let { reward ->
                Text(
                    text = reward,
                    fontFamily = CozyAuth.PixelFont,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/** Строка награды под текстом события; null — событие без награды (просто текст). */
@Composable
private fun focusEventRewardText(entry: FocusFeedEntry): String? = when (entry.type) {
    FocusEventType.TEXT -> null
    FocusEventType.CHARACTER_XP ->
        stringResource(R.string.focus_event_xp, entry.characterXp)
    FocusEventType.SKILL_XP ->
        entry.skillName?.let { stringResource(R.string.focus_event_skill, it, entry.skillXp) }
    FocusEventType.ITEM ->
        entry.itemName?.let { name ->
            val icon = entry.itemIcon.orEmpty()
            val display = if (icon.isBlank()) name else "$icon $name"
            stringResource(R.string.focus_event_item, display)
        }
    FocusEventType.NEW_SKILL ->
        entry.newSkillName?.let { stringResource(R.string.focus_event_new_skill, it) }
}

/**
 * Tappable checklist shown for shopping notes while the focus timer runs, letting the
 * user tick items off as they shop. Each toggle is reported to the caller, which persists
 * the change. Ticked items are struck through for clear visual feedback.
 */
@Composable
private fun InteractiveChecklist(
    items: List<ChecklistItem>,
    onToggle: (index: Int, checked: Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(index, !item.isChecked) }
                    .padding(vertical = 2.dp)
            ) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { checked -> onToggle(index, checked) },
                    colors = cozyCheckboxColors()
                )
                Text(
                    text = item.text,
                    fontFamily = CozyAuth.PixelFont,
                    style = MaterialTheme.typography.body2,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TimerWindow(
    note: Note,
    timerText: String,
    running: Boolean,
    onPauseResume: () -> Unit,
    onComplete: () -> Unit,
    onReturnHome: () -> Unit,
    onChecklistItemToggle: (index: Int, checked: Boolean) -> Unit
) {
    val colors = noteCategoryColors(note.category)
    PixelPanel(
        fill = colors.container,
        border = CozyAuth.BrownOutline,
        shadow = CozyAuth.BrownShadow,
        shadowOffset = 8,
        cornerRadius = 10,
        modifier = Modifier.fillMaxWidth()
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.content) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = note.title,
                    fontFamily = CozyAuth.PixelFont,
                    style = MaterialTheme.typography.h6
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (note.category == NoteCategory.SHOPPING && note.checklist.isNotEmpty()) {
                    InteractiveChecklist(
                        items = note.checklist,
                        onToggle = onChecklistItemToggle
                    )
                } else {
                    NoteDetails(note)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = timerText,
                    fontFamily = CozyAuth.PixelFont,
                    color = CozyAuth.Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 42.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                PixelPrimaryButton(
                    text = if (running) stringResource(R.string.timer_stop) else stringResource(R.string.timer_resume),
                    onClick = onPauseResume,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PixelPrimaryButton(
                        text = stringResource(R.string.timer_done),
                        onClick = onComplete,
                        modifier = Modifier.weight(1f)
                    )
                    PixelPrimaryButton(
                        text = stringResource(R.string.timer_home),
                        onClick = onReturnHome,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
