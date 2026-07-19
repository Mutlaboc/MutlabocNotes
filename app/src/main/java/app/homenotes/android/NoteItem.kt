package app.homenotes.android

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val BURST_MS = 380

// Entrance sequence: the list first frees vertical space, then the card unrolls
// left-to-right like a sheet of parchment, then the coins pop in one after another.
private const val ENTRANCE_EXPAND_MS = 260
private const val ENTRANCE_UNFOLD_MS = 2000
private const val ENTRANCE_COIN_POP_MS = 340
private const val ENTRANCE_COIN_STAGGER_MS = 110L

private const val SPARK_COUNT = 6
private val GoldSparkLight = Color(0xFFFFE082)
private val GoldSpark = Color(0xFFFFC94D)
private val GoldSparkDeep = Color(0xFFD99A2B)

@Composable
fun NoteItem(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCompletionChange: (Boolean) -> Unit,
    // When supplied (notes screen), checking the box bursts the card and emits the source
    // centre + coin count + card colour so the screen can spawn shards and fly coins to
    // the counter. The actual data completion is committed by the caller once coins arrive.
    onBurstComplete: ((burstCenterRoot: Offset, coinSourceRoot: Offset, coinCount: Int, color: Color) -> Unit)? = null,
    // Accumulated stopwatch value to show in the footer (notes screen only).
    timerMillis: Long = 0L,
    // When supplied (notes screen), a Start button is shown; pressing it reports the
    // card's bounds so the screen can float this note to the centre and run the timer.
    onStartTimer: ((originRect: Rect) -> Unit)? = null,
    // True while this note is the one floated to the centre — hide it in its list slot.
    isExpanded: Boolean = false,
    showCompletion: Boolean = true,
    // True for a just-created note: plays a one-shot entrance (space opens up, the card
    // unrolls left-to-right, coins pop in sequentially). [onEntranceShown] fires once
    // the entrance has been handled so the caller can clear the "new note" flag.
    animateEntrance: Boolean = false,
    onEntranceShown: () -> Unit = {}
) {
    val categoryColors = noteCategoryColors(note.category)
    val density = LocalDensity.current

    var cardBounds by remember { mutableStateOf(Rect.Zero) }
    var coinRowBounds by remember { mutableStateOf(Rect.Zero) }
    var bursting by remember { mutableStateOf(false) }
    val burst = remember { Animatable(0f) }
    LaunchedEffect(bursting) {
        if (bursting) burst.animateTo(1f, animationSpec = tween(BURST_MS))
    }

    // --- One-shot entrance. All values start settled (1f) unless this composition was
    // created for a brand-new note with system animations enabled.
    val animationsEnabled = rememberAnimationsEnabled()
    val runEntrance = remember { animateEntrance && animationsEnabled }
    var entrancePlayed by remember { mutableStateOf(!runEntrance) }
    val expandIn = remember { Animatable(if (runEntrance) 0f else 1f) }   // height fraction
    val unfold = remember { Animatable(if (runEntrance) 0f else 1f) }     // left-to-right reveal
    val coinCount = note.coinCount.coerceAtLeast(0)
    val coinPops = remember(coinCount) {
        List(coinCount) { Animatable(if (!entrancePlayed) 0f else 1f) }
    }
    val currentOnEntranceShown by rememberUpdatedState(onEntranceShown)
    LaunchedEffect(coinPops) {
        if (entrancePlayed) {
            // Reduced motion: render statically but still consume the "new note" flag.
            if (animateEntrance && !runEntrance) currentOnEntranceShown()
            return@LaunchedEffect
        }
        expandIn.animateTo(1f, animationSpec = tween(ENTRANCE_EXPAND_MS, easing = FastOutSlowInEasing))
        unfold.animateTo(1f, animationSpec = tween(ENTRANCE_UNFOLD_MS, easing = LinearOutSlowInEasing))
        coinPops.forEach { pop ->
            launch {
                pop.animateTo(1f, animationSpec = tween(ENTRANCE_COIN_POP_MS, easing = LinearOutSlowInEasing))
            }
            delay(ENTRANCE_COIN_STAGGER_MS)
        }
        entrancePlayed = true
        currentOnEntranceShown()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            // Entrance phase 1: the item grows from zero height, so neighbours slide
            // apart smoothly before anything is drawn. Read in the layout phase only.
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val h = (placeable.height * expandIn.value).roundToInt()
                layout(placeable.width, h) { placeable.placeRelative(0, 0) }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .onGloballyPositioned { cardBounds = it.boundsInRoot() }
            .graphicsLayer {
                val p = burst.value
                // Anticipation: snap a bit bigger, then blow apart — shrink to nothing
                // while fading and giving a quick shudder.
                val s = if (p < 0.30f) {
                    1f + 0.22f * (p / 0.30f)
                } else {
                    val q = (p - 0.30f) / 0.70f
                    1.22f - 0.95f * q
                }
                scaleX = s
                scaleY = s
                alpha = when {
                    isExpanded -> 0f
                    p < 0.30f -> 1f
                    else -> (1f - (p - 0.30f) / 0.70f).coerceIn(0f, 1f)
                }
                rotationZ = 6f * sin(PI * 3 * p).toFloat() * (1f - p)
                transformOrigin = TransformOrigin.Center
            }
            // Entrance phase 2: unroll the card left-to-right like parchment. Read in
            // the draw phase only; a no-op once the entrance has settled.
            .drawWithContent {
                val reveal = unfold.value
                when {
                    reveal >= 1f -> drawContent()
                    reveal > 0f -> clipRect(right = size.width * reveal) {
                        this@drawWithContent.drawContent()
                    }
                }
            }
    ) {
        PixelPanel(
            fill = categoryColors.container,
            border = CozyAuth.BrownOutline,
            shadow = CozyAuth.BrownShadow,
            shadowOffset = 4,
            cornerRadius = 6,
            modifier = Modifier.fillMaxWidth()
        ) {
            CompositionLocalProvider(LocalContentColor provides categoryColors.content) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick() }
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (showCompletion) Checkbox(
                            checked = note.isCompleted,
                            colors = cozyCheckboxColors(),
                            onCheckedChange = { checked ->
                                if (checked && onBurstComplete != null) {
                                    if (!bursting) {
                                        bursting = true
                                        val inset = with(density) { 14.dp.toPx() }
                                        val coinSource = if (coinRowBounds != Rect.Zero) {
                                            Offset(
                                                coinRowBounds.right - inset,
                                                (coinRowBounds.top + coinRowBounds.bottom) / 2f
                                            )
                                        } else {
                                            cardBounds.center
                                        }
                                        onBurstComplete(
                                            cardBounds.center,
                                            coinSource,
                                            note.coinCount,
                                            categoryColors.container
                                        )
                                    }
                                } else {
                                    onCompletionChange(checked)
                                }
                            }
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        ) {
                            Text(
                                text = note.title,
                                fontFamily = CozyAuth.PixelFont,
                                style = MaterialTheme.typography.subtitle1
                            )
                            NoteDetails(note)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (onStartTimer != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onStartTimer(cardBounds) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = stringResource(R.string.start_timer_description),
                                        tint = LocalContentColor.current
                                    )
                                }
                                Text(
                                    text = formatTimer(timerMillis),
                                    fontFamily = CozyAuth.PixelFont,
                                    style = MaterialTheme.typography.body2,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(1.dp))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.onGloballyPositioned { coinRowBounds = it.boundsInRoot() }
                        ) {
                            repeat(coinCount) { i ->
                                // Entrance phase 3: each coin pops in with a slight
                                // overshoot and a small golden spark burst behind it.
                                val pop = coinPops.getOrNull(i)
                                Image(
                                    painter = painterResource(id = R.drawable.gold_coin),
                                    contentDescription = stringResource(R.string.coin_description),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .drawBehind { pop?.let { drawCoinSparks(it.value) } }
                                        .graphicsLayer {
                                            val p = pop?.value ?: 1f
                                            val s = if (p < 0.55f) {
                                                1.25f * (p / 0.55f)
                                            } else {
                                                1.25f - 0.25f * ((p - 0.55f) / 0.45f)
                                            }
                                            scaleX = s
                                            scaleY = s
                                            alpha = (p / 0.3f).coerceIn(0f, 1f)
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A small "golden burst": [SPARK_COUNT] pixel squares radiating out from the coin's
 * centre, decelerating and fading as [progress] runs 0..1. Drawn behind the coin and
 * outside its pop-scale layer, so the sparks fly at full size from the first frame.
 */
private fun DrawScope.drawCoinSparks(progress: Float) {
    if (progress <= 0f || progress >= 1f) return
    val ease = 1f - (1f - progress) * (1f - progress)
    val alpha = (1f - progress).coerceIn(0f, 1f)
    val reach = size.minDimension * (0.45f + 0.75f * ease)
    val spark = size.minDimension * 0.14f
    repeat(SPARK_COUNT) { i ->
        val angle = (i.toFloat() / SPARK_COUNT) * 2f * PI.toFloat() - PI.toFloat() / 2f
        val color = when (i % 3) {
            0 -> GoldSparkLight
            1 -> GoldSpark
            else -> GoldSparkDeep
        }
        drawRect(
            color = color,
            topLeft = Offset(
                center.x + cos(angle) * reach - spark / 2f,
                center.y + sin(angle) * reach - spark / 2f
            ),
            size = Size(spark, spark),
            alpha = alpha
        )
    }
}

@Composable
internal fun NoteDetails(note: Note) {
    when (note.category) {
        NoteCategory.SHOPPING -> {
            if (note.checklist.isNotEmpty()) {
                note.checklist.take(3).forEach { item ->
                    Text(
                        text = stringResource(R.string.checklist_preview_item, item.text),
                        fontFamily = CozyAuth.PixelFont,
                        style = MaterialTheme.typography.body2
                    )
                }
                if (note.checklist.size > 3) {
                    Text(
                        text = stringResource(R.string.more_items_indicator),
                        fontFamily = CozyAuth.PixelFont,
                        style = MaterialTheme.typography.body2
                    )
                }
            } else if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    fontFamily = CozyAuth.PixelFont,
                    style = MaterialTheme.typography.body2
                )
            }
        }

        NoteCategory.RECURRING_TASKS -> {
            if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    fontFamily = CozyAuth.PixelFont,
                    style = MaterialTheme.typography.body2
                )
            }
            note.startAtMillis?.let { millis ->
                Text(
                    text = stringResource(R.string.note_start_value, formatTaskDateTime(millis)),
                    fontFamily = CozyAuth.PixelFont,
                    style = MaterialTheme.typography.caption
                )
            }
            note.durationMinutes?.let { minutes ->
                Text(
                    text = stringResource(
                        R.string.note_duration_value,
                        minutes / 1_440,
                        (minutes % 1_440) / 60
                    ),
                    fontFamily = CozyAuth.PixelFont,
                    style = MaterialTheme.typography.caption
                )
            }
            if (note.repeatRule != RepeatRule.NONE) {
                Text(
                    text = stringResource(
                        R.string.note_repeating,
                        stringResource(note.repeatRule.labelRes())
                    ),
                    fontFamily = CozyAuth.PixelFont,
                    style = MaterialTheme.typography.caption
                )
            }
        }

        NoteCategory.TASKS -> {
            if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    fontFamily = CozyAuth.PixelFont,
                    style = MaterialTheme.typography.body2
                )
            }
            note.deadlineMillis?.let { millis ->
                Text(
                    text = stringResource(R.string.note_deadline_value, formatDeadlineDate(millis)),
                    fontFamily = CozyAuth.PixelFont,
                    style = MaterialTheme.typography.caption
                )
            }
            note.durationMinutes?.let { minutes ->
                Text(
                    text = stringResource(
                        R.string.note_duration_value,
                        minutes / 1_440,
                        (minutes % 1_440) / 60
                    ),
                    fontFamily = CozyAuth.PixelFont,
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}
