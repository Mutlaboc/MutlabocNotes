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
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/*
 * Shared "yard" scene: the dotLottie house on the left, the tree (trunk + swaying
 * canopy) on the right with leaves drifting down, and the mascot who strolls back and
 * forth along the grass.
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
private const val WALK_LEFT_X = 300f    // centre x at the left turn
private const val WALK_RIGHT_X = 1700f  // centre x at the right turn
private const val FRAME_MS = 110L
private const val CROSS_MS = 9_000L      // one length of the yard
private const val FOOT_PAD = 8f          // sprite has a few empty px below the feet



@Composable
internal fun HomeYardScene(
    animationRestartKey: Any,
    modifier: Modifier = Modifier
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val elapsed = rememberElapsedMillis(animationsEnabled)

    val walk = remember {
        intArrayOf(
            R.drawable.mascot_walk_01, R.drawable.mascot_walk_02, R.drawable.mascot_walk_03,
            R.drawable.mascot_walk_04, R.drawable.mascot_walk_05, R.drawable.mascot_walk_06,
            R.drawable.mascot_walk_07, R.drawable.mascot_walk_08
        )
    }
    val leafDrawables = remember {
        intArrayOf(
            R.drawable.tree2_leaf_01, R.drawable.tree2_leaf_02, R.drawable.tree2_leaf_03,
            R.drawable.tree2_leaf_04, R.drawable.tree2_leaf_05, R.drawable.tree2_leaf_06
        )
    }

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

    // Mascot position + frame (ping-pong across the yard).
    val mascot = mascotState(elapsed, walk, animationsEnabled)

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

        // Tree trunk (static).
        Image(
            bitmap = pixelBmp(R.drawable.tree2_trunk),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier.offset(x(TRUNK_X), y(TRUNK_Y)).size(d(TRUNK_W), d(TRUNK_H))
        )
        // Tree canopy (sways from its lower edge, where it meets the trunk).
        Image(
            bitmap = pixelBmp(R.drawable.tree2_canopy),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .offset(x(CANOPY_X), y(CANOPY_Y))
                .size(d(CANOPY_W), d(CANOPY_H))
                .graphicsLayer {
                    rotationZ = angle
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
        )

        // Mascot strolling along the grass.
        Image(
            bitmap = pixelBmp(mascot.drawableId),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .offset(x(mascot.centerX - MASCOT_W / 2f), y(GROUND_Y - MASCOT_H + FOOT_PAD))
                .size(d(MASCOT_W), d(MASCOT_H))
                .graphicsLayer {
                    scaleX = if (mascot.facingLeft) -1f else 1f
                    transformOrigin = TransformOrigin.Center
                }
        )

    }
}

private data class MascotState(val drawableId: Int, val centerX: Float, val facingLeft: Boolean)

private fun mascotState(elapsed: Long, walk: IntArray, enabled: Boolean): MascotState {
    if (!enabled) return MascotState(walk[0], WALK_LEFT_X, facingLeft = false)
    val period = CROSS_MS * 2
    val t = (elapsed % period).toFloat() / CROSS_MS  // 0..2
    val goingRight = t <= 1f
    val p = if (goingRight) t else 2f - t            // 0..1 along the path
    val centerX = lerp(WALK_LEFT_X, WALK_RIGHT_X, p)
    val frame = walk[((elapsed / FRAME_MS) % walk.size).toInt()]
    // sprite faces right; flip when walking left
    return MascotState(frame, centerX, facingLeft = !goingRight)
}

private data class Leaf(
    val drawableId: Int,
    val baseX: Float,
    val height: Float,
    val durationMs: Long,
    val offsetMs: Long,
    val swayAmp: Float,
    val swayCycles: Float,
    val phase: Float,
    val rotSpeed: Float,
    val rotPhase: Float
)


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
private fun rememberElapsedMillis(enabled: Boolean): Long {
    var elapsed by remember { mutableStateOf(0L) }
    LaunchedEffect(enabled) {
        if (!enabled) { elapsed = 0L; return@LaunchedEffect }
        val start = withFrameMillis { it }
        while (true) {
            withFrameMillis { frameMs -> elapsed = frameMs - start }
        }
    }
    return elapsed
}
