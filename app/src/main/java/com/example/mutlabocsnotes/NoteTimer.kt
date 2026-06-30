package com.example.mutlabocsnotes

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.random.Random
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
private const val PARTICLE_EMIT_MS = 700L
private const val PARTICLE_FALL_MS = 1300

private data class XpParticle(val id: Long, val xFrac: Float)

/**
 * Full-screen "focus" overlay shown while a note's timer runs. The screen splits at the
 * centre: the top half is the yard animation (meadow + tree + strolling mascot, no house),
 * the bottom half is the timer window with an XP level bar (filled by particles drifting
 * down from the animation), a skill bar, and the controls.
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
    characterSheet: CharacterSheet? = null,
    timerSkill: CharacterSkill? = null,
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

    // Character data drives the bars: real sheet from the backend if available, else a
    // local sample as a fallback while it loads. The bars are the real XP source — XP
    // earned this running segment (1 per 2s for the character, 1 per 10s for the skill)
    // fills them live, rolling the level over when full. The same amounts are persisted
    // to the backend when the session settles (pause / done / home).
    val fallbackSheet = remember { sampleCharacterSheet() }
    val sheet = characterSheet ?: fallbackSheet
    val fallbackSkill = remember(sheet) { sheet.skills.randomOrNull() }
    val skill = timerSkill ?: fallbackSkill

    val runMsLive = if (running) (now - runStartUptime).coerceAtLeast(0L) else 0L
    val charLive = applyCharacterXp(sheet.level, sheet.xp.toDouble(), runMsLive / 2000.0)
    val skillLive = skill?.let {
        val startXpInLevel = it.progress.toDouble() * characterXpToNext(it.level)
        applyCharacterXp(it.level, startXpInLevel, runMsLive / 10000.0)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // TOP — animated yard scene (no house) + XP particles drifting down into the bar.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clipToBounds()
                .graphicsLayer {
                    translationY = SPLIT_SLIDE_DP.dp.toPx() * (1f - progress.value)
                    alpha = progress.value
                }
                .background(CozyAuth.Cream)
        ) {
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
            XpParticles(running = running && !closing)
        }

        // BOTTOM — bars + timer window, parts downward from the centre seam.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .graphicsLayer {
                    translationY = -SPLIT_SLIDE_DP.dp.toPx() * (1f - progress.value)
                    alpha = progress.value
                }
                .background(CozyAuth.Cream)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            val charValue = if (charLive.level >= CHARACTER_MAX_LEVEL) {
                stringResource(R.string.character_xp_max)
            } else {
                "${charLive.xpInLevel} / ${charLive.xpToNext}"
            }
            BarLabel(stringResource(R.string.character_level_xp_label, charLive.level), charValue)
            StatBar(progress = charLive.fraction, fill = CozyAuth.Terracotta)
            if (skill != null && skillLive != null) {
                Spacer(Modifier.height(10.dp))
                BarLabel(skill.name, stringResource(R.string.character_skill_level, skillLive.level))
                StatBar(progress = skillLive.fraction, fill = CozyAuth.SoftGreen)
            }
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

@Composable
private fun XpParticles(running: Boolean) {
    val particles = remember { mutableStateListOf<XpParticle>() }
    var nextId by remember { mutableStateOf(0L) }
    LaunchedEffect(running) {
        while (running) {
            delay(PARTICLE_EMIT_MS)
            particles.add(XpParticle(nextId++, Random.nextFloat()))
        }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = maxWidth
        val travel = maxHeight
        particles.forEach { particle ->
            key(particle.id) {
                FallingParticle(
                    xFrac = particle.xFrac,
                    width = width,
                    travel = travel,
                    onDone = { particles.remove(particle) }
                )
            }
        }
    }
}

@Composable
private fun FallingParticle(xFrac: Float, width: Dp, travel: Dp, onDone: () -> Unit) {
    val prog = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        prog.animateTo(1f, animationSpec = tween(PARTICLE_FALL_MS, easing = LinearEasing))
        onDone()
    }
    Box(
        modifier = Modifier
            .offset {
                val sizePx = 8.dp.toPx()
                val x = width.toPx() * xFrac - sizePx / 2f
                val y = travel.toPx() * prog.value - sizePx / 2f
                IntOffset(x.roundToInt(), y.roundToInt())
            }
            .size(8.dp)
            .graphicsLayer { alpha = (1f - prog.value * 0.25f).coerceIn(0f, 1f) }
            .clip(CircleShape)
            .background(CozyAuth.MutedYellow)
            .border(1.dp, CozyAuth.TerracottaDark, CircleShape)
    )
}

@Composable
private fun BarLabel(name: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = name, fontFamily = CozyAuth.PixelFont, color = CozyAuth.InkSoft, fontSize = 12.sp)
        Text(text = value, fontFamily = CozyAuth.PixelFont, color = CozyAuth.InkSoft, fontSize = 12.sp)
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun StatBar(progress: Float, fill: Color) {
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(shape)
            .background(CozyAuth.FieldCream)
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
