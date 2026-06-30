package app.homenotes.android

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

private const val BURST_MS = 380

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
    isExpanded: Boolean = false
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

    Column(
        modifier = modifier
            .fillMaxWidth()
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
                        Checkbox(
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
                            repeat(note.coinCount.coerceAtLeast(0)) {
                                Image(
                                    painter = painterResource(id = R.drawable.gold_coin),
                                    contentDescription = stringResource(R.string.coin_description),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
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

        NoteCategory.NOTES -> {
            if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    fontFamily = CozyAuth.PixelFont,
                    style = MaterialTheme.typography.body2
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
    }
}
