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
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/*
 * Shared "yard" scene: the dotLottie house on the left, the tree (trunk + swaying
 * canopy) on the right, and the mascot who strolls back and forth along the grass.
 *
 * Everything is positioned in one scene coordinate space (SCENE_W x SCENE_H, the
 * house's native 1254 grid extended to the right for the tree). The space is scaled
 * to fit the host height and centred horizontally. All pixel-art is drawn with
 * FilterQuality.None so nothing is blurred.
 *
 * Performance notes: the per-frame timer (`elapsed`) and the canopy sway are read only
 * inside deferred layout/draw lambdas (offset {} / graphicsLayer {}), so ticking the
 * animation does NOT recompose the whole scene — it only re-lays-out / re-draws the
 * single node that depends on it. Mascot frames are decoded once up front instead of
 * per frame.
 */

private const val SCENE_W = 2120f
private const val SCENE_H = 1254f
private const val GROUND_Y = 1165f

// House occupies its native 1254 square at the scene origin.
private const val HOUSE = 1254f

// Tree v2 layers (scene offsets + sizes). The trunk's base sits on the ground line;
// the canopy overlaps the upper trunk and sways from its lower edge.
private const val TRUNK_X = 1250f; private const val TRUNK_Y = 242f
private const val TRUNK_W = 760f;  private const val TRUNK_H = 923f
private const val CANOPY_X = 1144f; private const val CANOPY_Y = 90f
private const val CANOPY_W = 971f;  private const val CANOPY_H = 878f

// Mascot strolls along the grass (same walk cycle as the auth screen).
private const val MASCOT_ASPECT = 230f / 485f
private const val MASCOT_H = 430f
private const val MASCOT_W = MASCOT_H * MASCOT_ASPECT
private const val WALK_LEFT_X = -MASCOT_W / 2f    // centre x when fully off the left edge
private const val WALK_RIGHT_X = SCENE_W + MASCOT_W / 2f  // centre x when fully off the right edge
private const val FRAME_MS = 110L
private const val CROSS_MS = 9_000L      // one length of the yard
private const val FOOT_PAD = 8f          // sprite has a few empty px below the feet
private const val ANIMATION_START_DELAY_MS = 5_000L  // hold a static scene before animating
private const val HOUSE_FRAME_MS = 700L  // per-frame hold for the house idle sprite loop



@Composable
internal fun HomeYardScene(
    animationRestartKey: Any,
    modifier: Modifier = Modifier,
    showHouse: Boolean = true,
    startDelayMs: Long = ANIMATION_START_DELAY_MS
) {
    val animationsEnabled = rememberAnimationsEnabled()

    // Hold a fully static scene for [startDelayMs] after entering, then start the
    // animation. (The notes screen waits a few seconds; the timer scene starts at once.)
    var animationStarted by remember { mutableStateOf(false) }
    LaunchedEffect(startDelayMs) {
        if (animationsEnabled) {
            delay(startDelayMs)
            animationStarted = true
        }
    }
    val animate = animationsEnabled && animationStarted

    // Frame clock as State, read only inside deferred lambdas so it never recomposes the
    // whole scene.
    val elapsed = rememberElapsedMillis(animate)

    // Mascot walk frames decoded once (not per frame).
    val mascotFrames = rememberPixelBmps(
        R.drawable.mascot_walk_01, R.drawable.mascot_walk_02, R.drawable.mascot_walk_03,
        R.drawable.mascot_walk_04, R.drawable.mascot_walk_05, R.drawable.mascot_walk_06,
        R.drawable.mascot_walk_07, R.drawable.mascot_walk_08
    )

    // Pre-rendered idle frames of the house: only the lantern and chimney smoke animate;
    // the windows are frozen to a steady glow to cut load on the notes screen. Only
    // decoded when the house is actually shown.
    val houseFrames = if (showHouse) {
        // 12-frame loop: lantern flicker + chimney smoke only; windows are static.
        rememberPixelBmps(
            R.drawable.house_anim_01, R.drawable.house_anim_02, R.drawable.house_anim_03,
            R.drawable.house_anim_04, R.drawable.house_anim_05, R.drawable.house_anim_06,
            R.drawable.house_anim_07, R.drawable.house_anim_08, R.drawable.house_anim_09,
            R.drawable.house_anim_10, R.drawable.house_anim_11, R.drawable.house_anim_12
        )
    } else {
        emptyList()
    }

    // Subtle, continuous canopy sway (kept as State, read in the draw lambda).
    val sway = rememberInfiniteTransition(label = "tree_sway")
    val swayAngle = sway.animateFloat(
        initialValue = -1.2f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway_angle"
    )

    BoxWithConstraints(modifier = modifier) {
        val s = maxHeight.value / SCENE_H                 // dp per scene unit
        val ox = (maxWidth.value - SCENE_W * s) / 2f      // centre horizontally
        fun x(u: Float) = (ox + u * s).dp
        fun y(u: Float) = (u * s).dp
        fun d(u: Float) = (u * s).dp

        // House at scene origin. The static pixel-art is drawn immediately so the house
        // is on screen the moment the background is. Once the start delay elapses, the
        // idle sprite loop is overlaid in the exact same slot and begins cycling.
        if (showHouse) {
            val houseModifier = Modifier.offset(x(0f), y(0f)).size(d(HOUSE), d(HOUSE))
            Image(
                bitmap = pixelBmp(R.drawable.house_static),
                contentDescription = null,
                filterQuality = FilterQuality.None,
                modifier = houseModifier
            )
            if (animate) {
                HouseSprite(elapsed = elapsed, frames = houseFrames, modifier = houseModifier)
            }
        }

        // Tree trunk (static).
        Image(
            bitmap = pixelBmp(R.drawable.tree2_trunk),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier.offset(x(TRUNK_X), y(TRUNK_Y)).size(d(TRUNK_W), d(TRUNK_H))
        )
        // Tree canopy (sways from its lower edge, where it meets the trunk). The sway
        // value is read inside graphicsLayer (draw phase) so it never recomposes.
        Image(
            bitmap = pixelBmp(R.drawable.tree2_canopy),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .offset(x(CANOPY_X), y(CANOPY_Y))
                .size(d(CANOPY_W), d(CANOPY_H))
                .graphicsLayer {
                    rotationZ = if (animate) swayAngle.value else 0f
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
        )

        // Mascot strolling along the grass — only once the animation has started.
        if (animate) {
            MascotImage(elapsed = elapsed, frames = mascotFrames, s = s, ox = ox)
        }
    }
}

/**
 * House idle sprite. Cycles through the pre-rendered frames at a slow cadence. Only a
 * change of the integer frame (~1.4fps) recomposes this node; the bitmaps are decoded
 * once up front, so there is no per-frame compositing or decoding.
 */
@Composable
private fun HouseSprite(
    elapsed: State<Long>,
    frames: List<ImageBitmap>,
    modifier: Modifier
) {
    val frameIndex by remember(frames) {
        derivedStateOf { ((elapsed.value / HOUSE_FRAME_MS) % frames.size).toInt() }
    }
    Image(
        bitmap = frames[frameIndex],
        contentDescription = null,
        filterQuality = FilterQuality.None,
        modifier = modifier
    )
}

/**
 * Mascot sprite. Position is computed in the layout-phase `offset {}` lambda and the
 * facing flip in the draw-phase `graphicsLayer {}` lambda, so the 60fps clock never
 * recomposes this node. Only a change of the integer walk-frame (~9fps) recomposes it,
 * and even then the bitmaps are already decoded.
 */
@Composable
private fun MascotImage(
    elapsed: State<Long>,
    frames: List<ImageBitmap>,
    s: Float,
    ox: Float
) {
    val frameIndex by remember(frames) {
        derivedStateOf { ((elapsed.value / FRAME_MS) % frames.size).toInt() }
    }
    Image(
        bitmap = frames[frameIndex],
        contentDescription = null,
        filterQuality = FilterQuality.None,
        modifier = Modifier
            .offset {
                val centerX = mascotCenterX(elapsed.value)
                val left = (ox + (centerX - MASCOT_W / 2f) * s).dp
                val top = ((GROUND_Y - MASCOT_H + FOOT_PAD) * s).dp
                IntOffset(left.roundToPx(), top.roundToPx())
            }
            .size((MASCOT_W * s).dp, (MASCOT_H * s).dp)
            .graphicsLayer {
                scaleX = if (mascotFacingLeft(elapsed.value)) -1f else 1f
                transformOrigin = TransformOrigin.Center
            }
    )
}

// Mascot ping-pongs across the yard; sprite faces right and is flipped when walking left.
private fun mascotCenterX(elapsed: Long): Float {
    val t = (elapsed % (CROSS_MS * 2)).toFloat() / CROSS_MS  // 0..2
    val p = if (t <= 1f) t else 2f - t                       // 0..1 along the path
    return lerp(WALK_LEFT_X, WALK_RIGHT_X, p)
}

private fun mascotFacingLeft(elapsed: Long): Boolean {
    val t = (elapsed % (CROSS_MS * 2)).toFloat() / CROSS_MS
    return t > 1f
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
private fun rememberPixelBmps(vararg drawableIds: Int): List<ImageBitmap> {
    val resources = LocalContext.current.resources
    return remember(resources, drawableIds.contentHashCode()) {
        drawableIds.map { BitmapFactory.decodeResource(resources, it).asImageBitmap() }
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
private fun rememberElapsedMillis(enabled: Boolean): State<Long> {
    val elapsed = remember { mutableStateOf(0L) }
    LaunchedEffect(enabled) {
        if (!enabled) { elapsed.value = 0L; return@LaunchedEffect }
        val start = withFrameMillis { it }
        while (true) {
            withFrameMillis { frameMs -> elapsed.value = frameMs - start }
        }
    }
    return elapsed
}
