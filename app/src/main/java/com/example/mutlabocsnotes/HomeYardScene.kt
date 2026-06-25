package com.example.mutlabocsnotes

import android.graphics.BitmapFactory
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.unit.dp
import com.lottiefiles.dotlottie.core.compose.ui.DotLottieAnimation
import com.lottiefiles.dotlottie.core.util.DotLottieSource

/*
 * Shared "yard" scene: the dotLottie house on the left, the tree + bench on the
 * right, and the peasant who walks from the house to the bench and sits down.
 *
 * Everything is positioned in one scene coordinate space (SCENE_W x SCENE_H, the
 * house's native 1254 grid extended to the right for the tree). The space is scaled
 * to fit the host height and centred horizontally. All pixel-art is drawn with
 * FilterQuality.None so nothing is blurred.
 */

private const val SCENE_W = 2120f
private const val SCENE_H = 1254f
private const val GROUND_Y = 1165f

// House occupies its native 1254 square at the scene origin.
private const val HOUSE = 1254f

// Tree layers (scene offsets + sizes, measured from the source art).
private const val SWAY_X = 1170f; private const val SWAY_Y = 70f
private const val SWAY_W = 920f;  private const val SWAY_H = 780f
private const val TRUNK_X = 1292f; private const val TRUNK_Y = 850f
private const val TRUNK_W = 353f;  private const val TRUNK_H = 315f
private const val BENCH_X = 1628f; private const val BENCH_Y = 925f
private const val BENCH_W = 343f;  private const val BENCH_H = 240f
private const val BENCH_CENTER_X = BENCH_X + BENCH_W / 2f // ~1800

// Peasant standing sprite: 180x340 canvas shown ~320px tall (keeps 180:340 aspect).
private const val STAND_W = 224f; private const val STAND_H = 423f
// Seated sprite: 286x254 canvas scaled by the same factor (~1.244).
private const val SEAT_W = 356f; private const val SEAT_H = 316f

private const val WALK_START_X = 320f
private const val WALK_END_X = 1780f

// Timeline (ms).
private const val WALK_MS = 4_000L
private const val TURN_MS = 300L
private const val SIT_MS = 500L
private const val SEATED_MS = 5_000L
private const val SEAM_MS = 400L
private const val LOOP_MS = WALK_MS + TURN_MS + SIT_MS + SEATED_MS + SEAM_MS

@Composable
internal fun HomeYardScene(
    animationRestartKey: Any,
    modifier: Modifier = Modifier
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val elapsed = rememberLoopMillis(animationsEnabled)

    val walk = remember {
        intArrayOf(
            R.drawable.man_walk_01, R.drawable.man_walk_02, R.drawable.man_walk_03,
            R.drawable.man_walk_04, R.drawable.man_walk_05, R.drawable.man_walk_06,
            R.drawable.man_walk_07, R.drawable.man_walk_08
        )
    }
    val turn = remember {
        intArrayOf(R.drawable.man_turn_01, R.drawable.man_turn_02, R.drawable.man_turn_03)
    }
    val sit = remember {
        intArrayOf(
            R.drawable.man_sit_01, R.drawable.man_sit_02, R.drawable.man_sit_03,
            R.drawable.man_sit_04, R.drawable.man_sit_05, R.drawable.man_sit_06
        )
    }

    val state = if (animationsEnabled) peasantState(elapsed, walk, turn, sit)
    else PeasantState(walk[0], WALK_START_X, false, standAlpha = 0f, seatAlpha = 1f,
        seatDrawable = sit[0], benchAlpha = 1f)

    // Subtle, continuous canopy sway.
    val sway = rememberInfiniteTransition(label = "tree_sway")
    val swayAngle by sway.animateFloat(
        initialValue = -1.2f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway_angle"
    )
    val angle = if (animationsEnabled) swayAngle else 0f

    BoxWithConstraints(modifier = modifier) {
        val s = maxHeight.value / SCENE_H                 // dp per scene unit
        val ox = (maxWidth.value - SCENE_W * s) / 2f      // centre horizontally
        fun x(u: Float) = (ox + u * s).dp
        fun y(u: Float) = (u * s).dp
        fun d(u: Float) = (u * s).dp

        // House (dotLottie) at scene origin.
        key(animationRestartKey) {
            DotLottieAnimation(
                source = DotLottieSource.Res(R.raw.home),
                autoplay = true,
                loop = true,
                speed = 0.6f,
                modifier = Modifier.offset(x(0f), y(0f)).size(d(HOUSE), d(HOUSE))
            )
        }

        // Tree: static trunk, then swaying canopy.
        Image(
            bitmap = pixelBmp(R.drawable.tree_trunk),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier.offset(x(TRUNK_X), y(TRUNK_Y)).size(d(TRUNK_W), d(TRUNK_H))
        )
        Image(
            bitmap = pixelBmp(R.drawable.tree_sway),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .offset(x(SWAY_X), y(SWAY_Y))
                .size(d(SWAY_W), d(SWAY_H))
                .graphicsLayer {
                    rotationZ = angle
                    transformOrigin = TransformOrigin(0.5f, 1f) // pivot at trunk top
                }
        )

        // Empty bench (visible until the man sits).
        if (state.benchAlpha > 0f) {
            Image(
                bitmap = pixelBmp(R.drawable.tree_bench),
                contentDescription = null,
                filterQuality = FilterQuality.None,
                modifier = Modifier
                    .offset(x(BENCH_X), y(BENCH_Y))
                    .size(d(BENCH_W), d(BENCH_H))
                    .graphicsLayer { alpha = state.benchAlpha }
            )
        }

        // Seated man (with his own bench) — crossfades in as he sits.
        if (state.seatAlpha > 0f) {
            Image(
                bitmap = pixelBmp(state.seatDrawable),
                contentDescription = null,
                filterQuality = FilterQuality.None,
                modifier = Modifier
                    .offset(x(BENCH_CENTER_X - SEAT_W / 2f), y(GROUND_Y - SEAT_H))
                    .size(d(SEAT_W), d(SEAT_H))
                    .graphicsLayer { alpha = state.seatAlpha }
            )
        }

        // Walking / turning man — crossfades out as he sits.
        if (state.standAlpha > 0f) {
            Image(
                bitmap = pixelBmp(state.drawableId),
                contentDescription = null,
                filterQuality = FilterQuality.None,
                modifier = Modifier
                    .offset(x(state.anchorX - STAND_W / 2f), y(GROUND_Y - STAND_H))
                    .size(d(STAND_W), d(STAND_H))
                    .graphicsLayer {
                        alpha = state.standAlpha
                        scaleX = if (state.facingLeft) -1f else 1f
                    }
            )
        }
    }
}

private data class PeasantState(
    val drawableId: Int,
    val anchorX: Float,
    val facingLeft: Boolean,
    val standAlpha: Float,
    val seatAlpha: Float,
    val seatDrawable: Int,
    val benchAlpha: Float
)

private fun peasantState(elapsed: Long, walk: IntArray, turn: IntArray, sit: IntArray): PeasantState {
    var t = elapsed
    // soft fade at the loop seam so the reset is not a hard pop
    val seamFadeIn = (elapsed.toFloat() / SEAM_MS).coerceIn(0f, 1f)

    if (t < WALK_MS) {
        val p = t / WALK_MS.toFloat()
        return PeasantState(
            drawableId = walk[((p * 16).toInt()).coerceAtMost(15) % walk.size],
            anchorX = lerp(WALK_START_X, WALK_END_X, p),
            facingLeft = false,
            standAlpha = seamFadeIn,
            seatAlpha = 0f,
            seatDrawable = sit[0],
            benchAlpha = seamFadeIn
        )
    }
    t -= WALK_MS

    if (t < TURN_MS) {
        val p = t / TURN_MS.toFloat()
        return PeasantState(
            drawableId = turn[((p * turn.size).toInt()).coerceAtMost(turn.lastIndex)],
            anchorX = WALK_END_X, facingLeft = false,
            standAlpha = 1f, seatAlpha = 0f, seatDrawable = sit[0], benchAlpha = 1f
        )
    }
    t -= TURN_MS

    if (t < SIT_MS) {
        val a = t / SIT_MS.toFloat()
        return PeasantState(
            drawableId = turn.last(), anchorX = WALK_END_X, facingLeft = false,
            standAlpha = 1f - a, seatAlpha = a, seatDrawable = sit[0], benchAlpha = 1f - a
        )
    }
    t -= SIT_MS

    if (t < SEATED_MS) {
        val p = t / SEATED_MS.toFloat()
        return PeasantState(
            drawableId = walk[0], anchorX = WALK_END_X, facingLeft = false,
            standAlpha = 0f, seatAlpha = 1f,
            seatDrawable = sit[((p * 12).toInt()) % sit.size],
            benchAlpha = 0f
        )
    }
    t -= SEATED_MS

    // seam: fade the seated man out before the loop restarts
    val a = (t / SEAM_MS.toFloat()).coerceIn(0f, 1f)
    return PeasantState(
        drawableId = walk[0], anchorX = WALK_END_X, facingLeft = false,
        standAlpha = 0f, seatAlpha = 1f - a, seatDrawable = sit.last(), benchAlpha = a
    )
}

private fun lerp(a: Float, b: Float, f: Float) = a + (b - a) * f.coerceIn(0f, 1f)

@Composable
private fun pixelBmp(drawableId: Int): ImageBitmap {
    val resources = LocalContext.current.resources
    return remember(drawableId, resources) {
        BitmapFactory.decodeResource(resources, drawableId).asImageBitmap()
    }
}

@Composable
private fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
            ) != 0f
        }.getOrDefault(true)
    }
}

@Composable
private fun rememberLoopMillis(enabled: Boolean): Long {
    var elapsed by remember { mutableStateOf(0L) }
    LaunchedEffect(enabled) {
        if (!enabled) { elapsed = 0L; return@LaunchedEffect }
        val start = withFrameMillis { it }
        while (true) {
            withFrameMillis { frameMs -> elapsed = (frameMs - start) % LOOP_MS }
        }
    }
    return elapsed
}
