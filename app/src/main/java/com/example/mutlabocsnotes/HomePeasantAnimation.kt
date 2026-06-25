package com.example.mutlabocsnotes

import android.graphics.BitmapFactory
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

private const val SCENE_SIZE = 1254f
// The PNG canvas is 180x340 with the figure's feet on the bottom edge. The on-screen
// box keeps that 180:340 aspect but is enlarged so the visible man reads ~320px tall
// in the 1254 scene (proportional to the house). No bitmap resampling is added here;
// Compose scales the nearest-neighbour bitmap to this box at runtime.
private const val SPRITE_WIDTH = 224f
private const val SPRITE_HEIGHT = 423f
private const val GROUND_Y = 1150f
private const val WALK_START_X = 150f
private const val WALK_END_X = 1100f
private const val LOOP_MS = 14_200L
private const val WALK_MS = 4_000L
private const val SETTLE_MS = 300L
private const val THINK_MS = 2_500L
private const val TURN_MS = 300L

private data class PeasantFrame(
    val drawableId: Int,
    val anchorX: Float,
    val facingLeft: Boolean
)

@Composable
internal fun HomePeasantAnimation(modifier: Modifier = Modifier) {
    val animationsEnabled = rememberSystemAnimationsEnabled()
    val elapsedMs = rememberPeasantLoopMillis(animationsEnabled)
    val walkFrames = remember {
        intArrayOf(
            R.drawable.man_walk_01,
            R.drawable.man_walk_02,
            R.drawable.man_walk_03,
            R.drawable.man_walk_04,
            R.drawable.man_walk_05,
            R.drawable.man_walk_06,
            R.drawable.man_walk_07,
            R.drawable.man_walk_08
        )
    }
    val turnFrames = remember {
        intArrayOf(
            R.drawable.man_turn_01,
            R.drawable.man_turn_02,
            R.drawable.man_turn_03
        )
    }
    val thinkFrames = remember {
        intArrayOf(
            R.drawable.man_think_01,
            R.drawable.man_think_02,
            R.drawable.man_think_03,
            R.drawable.man_think_04,
            R.drawable.man_think_05,
            R.drawable.man_think_06
        )
    }
    val frame = if (animationsEnabled) {
        peasantFrameAt(elapsedMs, walkFrames, turnFrames, thinkFrames)
    } else {
        PeasantFrame(
            drawableId = R.drawable.man_think_01,
            anchorX = WALK_START_X,
            facingLeft = false
        )
    }

    BoxWithConstraints(modifier = modifier) {
        val sceneSize = minOf(maxWidth, maxHeight)
        val frameWidth = sceneSize * (SPRITE_WIDTH / SCENE_SIZE)
        val frameHeight = sceneSize * (SPRITE_HEIGHT / SCENE_SIZE)
        val left = sceneSize * ((frame.anchorX - SPRITE_WIDTH / 2f) / SCENE_SIZE)
        val top = sceneSize * ((GROUND_Y - SPRITE_HEIGHT) / SCENE_SIZE)

        Image(
            bitmap = pixelBitmap(frame.drawableId),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .offset(x = left, y = top)
                .size(width = frameWidth, height = frameHeight)
                .graphicsLayer {
                    scaleX = if (frame.facingLeft) -1f else 1f
                    transformOrigin = TransformOrigin.Center
                }
        )
    }
}

@Composable
private fun pixelBitmap(drawableId: Int): ImageBitmap {
    val resources = LocalContext.current.resources
    return remember(drawableId, resources) {
        BitmapFactory.decodeResource(resources, drawableId).asImageBitmap()
    }
}

@Composable
private fun rememberSystemAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) != 0f
        }.getOrDefault(true)
    }
}

@Composable
private fun rememberPeasantLoopMillis(enabled: Boolean): Long {
    var elapsedMs by remember { mutableStateOf(0L) }
    LaunchedEffect(enabled) {
        if (!enabled) {
            elapsedMs = 0L
            return@LaunchedEffect
        }
        val startMs = withFrameMillis { it }
        while (true) {
            withFrameMillis { frameMs ->
                elapsedMs = (frameMs - startMs) % LOOP_MS
            }
        }
    }
    return elapsedMs
}

private fun peasantFrameAt(
    elapsedMs: Long,
    walkFrames: IntArray,
    turnFrames: IntArray,
    thinkFrames: IntArray
): PeasantFrame {
    var time = elapsedMs

    if (time < WALK_MS) {
        val progress = time / WALK_MS.toFloat()
        return PeasantFrame(
            drawableId = walkFrames[((progress * 16).toInt()).coerceAtMost(15) % walkFrames.size],
            anchorX = lerp(WALK_START_X, WALK_END_X, progress),
            facingLeft = false
        )
    }
    time -= WALK_MS

    if (time < SETTLE_MS) {
        return PeasantFrame(thinkFrames.first(), WALK_END_X, false)
    }
    time -= SETTLE_MS

    if (time < THINK_MS) {
        val progress = time / THINK_MS.toFloat()
        return PeasantFrame(
            drawableId = thinkFrames[((progress * 18).toInt()).coerceAtMost(17) % thinkFrames.size],
            anchorX = WALK_END_X,
            facingLeft = false
        )
    }
    time -= THINK_MS

    if (time < TURN_MS) {
        val progress = time / TURN_MS.toFloat()
        return PeasantFrame(
            drawableId = turnFrames[((progress * turnFrames.size).toInt()).coerceAtMost(turnFrames.lastIndex)],
            anchorX = WALK_END_X,
            facingLeft = false
        )
    }
    time -= TURN_MS

    if (time < WALK_MS) {
        val progress = time / WALK_MS.toFloat()
        return PeasantFrame(
            drawableId = walkFrames[((progress * 16).toInt()).coerceAtMost(15) % walkFrames.size],
            anchorX = lerp(WALK_END_X, WALK_START_X, progress),
            facingLeft = true
        )
    }
    time -= WALK_MS

    if (time < SETTLE_MS) {
        return PeasantFrame(thinkFrames.first(), WALK_START_X, true)
    }
    time -= SETTLE_MS

    if (time < THINK_MS) {
        val progress = time / THINK_MS.toFloat()
        return PeasantFrame(
            drawableId = thinkFrames[((progress * 18).toInt()).coerceAtMost(17) % thinkFrames.size],
            anchorX = WALK_START_X,
            facingLeft = true
        )
    }
    time -= THINK_MS

    val progress = time / TURN_MS.toFloat()
    return PeasantFrame(
        drawableId = turnFrames[((progress * turnFrames.size).toInt()).coerceAtMost(turnFrames.lastIndex)],
        anchorX = WALK_START_X,
        facingLeft = true
    )
}

private fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction.coerceIn(0f, 1f)
