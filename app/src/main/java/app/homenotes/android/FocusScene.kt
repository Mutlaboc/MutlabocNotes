package app.homenotes.android

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource

/*
 * Biome backgrounds for the focus screen (ExpandedNoteOverlay): the meadow scene keeps
 * its existing HomeYardScene rendering untouched (shared with the home screen script).
 * The four new biomes below are lighter-weight, fraction-positioned scenes: a full-bleed
 * background plus a mascot that strolls left-to-right, stopping at activity points baked
 * into the art to play an anim (existing tool anims + the new GATHER/REST/DIG), then exits
 * and loops. Purely ambient, decoupled from real progress — same spirit as
 * FocusActivityStrip, generalized from one work stop to several per scene.
 */

internal data class FocusActivityPoint(
    val anim: MascotAnim,
    val xFrac: Float,
    val loops: Int = 2
)

internal data class FocusScene(
    val backgroundRes: Int,
    val groundFrac: Float,
    val points: List<FocusActivityPoint>
)

internal val FOCUS_BIOME_SCENES: List<FocusScene> = listOf(
    FocusScene(
        backgroundRes = R.drawable.focus_bg_forest,
        groundFrac = 0.80f,
        points = listOf(
            FocusActivityPoint(MascotAnim.CHOP, xFrac = 0.21f),
            FocusActivityPoint(MascotAnim.GATHER, xFrac = 0.49f),
            FocusActivityPoint(MascotAnim.REST, xFrac = 0.76f)
        )
    ),
    FocusScene(
        backgroundRes = R.drawable.focus_bg_desert,
        groundFrac = 0.82f,
        points = listOf(
            FocusActivityPoint(MascotAnim.GATHER, xFrac = 0.15f),
            FocusActivityPoint(MascotAnim.DIG, xFrac = 0.48f),
            FocusActivityPoint(MascotAnim.REST, xFrac = 0.75f)
        )
    ),
    FocusScene(
        backgroundRes = R.drawable.focus_bg_ruins,
        groundFrac = 0.80f,
        points = listOf(
            FocusActivityPoint(MascotAnim.HAMMER, xFrac = 0.27f),
            FocusActivityPoint(MascotAnim.DIG, xFrac = 0.50f),
            FocusActivityPoint(MascotAnim.GATHER, xFrac = 0.73f)
        )
    ),
    FocusScene(
        backgroundRes = R.drawable.focus_bg_swamp,
        groundFrac = 0.83f,
        points = listOf(
            FocusActivityPoint(MascotAnim.CHOP, xFrac = 0.19f),
            FocusActivityPoint(MascotAnim.GATHER, xFrac = 0.47f),
            FocusActivityPoint(MascotAnim.REST, xFrac = 0.73f)
        )
    )
)

internal const val FOCUS_OFF_LEFT_FRAC = -0.10f
internal const val FOCUS_OFF_RIGHT_FRAC = 1.10f

/** Time to walk across the full scene width once; stop durations follow from anim timings. */
internal const val FOCUS_WALK_MS_PER_FULL_WIDTH = 3_000L

/**
 * Сколько проходов сцены приходится на одну рабочую остановку: маскот сначала просто
 * пересекает кадр, и только на следующем проходе останавливается поработать. Раньше
 * все точки отыгрывались за один проход подряд, и он стоял три четверти цикла.
 */
internal const val FOCUS_PASSES_PER_ACTIVITY = 2

internal data class FocusSegment(
    val anim: MascotAnim,
    val durationMs: Long,
    val fromFrac: Float,
    val toFrac: Float
)

private fun walkMs(fromFrac: Float, toFrac: Float): Long =
    (kotlin.math.abs(toFrac - fromFrac) * FOCUS_WALK_MS_PER_FULL_WIDTH).toLong()

/**
 * Цикл сцены — последовательность проходов слева направо. Каждый начинается за левым
 * краем и заканчивается за правым, поэтому «прыжок» между проходами всегда за кадром
 * (тот же приём, что и на склейке цикла). Остановка есть только в каждом
 * [FOCUS_PASSES_PER_ACTIVITY]-м проходе.
 */
internal fun buildFocusLoop(points: List<FocusActivityPoint>): List<FocusSegment> {
    val segments = mutableListOf<FocusSegment>()
    val crossingMs = walkMs(FOCUS_OFF_LEFT_FRAC, FOCUS_OFF_RIGHT_FRAC)
    for (point in points) {
        // Холостые проходы: просто прогулка через весь кадр.
        repeat(FOCUS_PASSES_PER_ACTIVITY - 1) {
            segments += FocusSegment(
                MascotAnim.WALK, crossingMs, FOCUS_OFF_LEFT_FRAC, FOCUS_OFF_RIGHT_FRAC
            )
        }
        // Рабочий проход: вход слева, остановка в точке, выход направо.
        val workMs = MASCOT_FRAME_TIMINGS_MS.getValue(point.anim).sum() * point.loops
        segments += FocusSegment(
            MascotAnim.WALK, walkMs(FOCUS_OFF_LEFT_FRAC, point.xFrac), FOCUS_OFF_LEFT_FRAC, point.xFrac
        )
        segments += FocusSegment(point.anim, workMs, point.xFrac, point.xFrac)
        segments += FocusSegment(
            MascotAnim.WALK, walkMs(point.xFrac, FOCUS_OFF_RIGHT_FRAC), point.xFrac, FOCUS_OFF_RIGHT_FRAC
        )
    }
    return segments
}

internal data class FocusFrame(val anim: MascotAnim, val frameIndex: Int, val xFrac: Float)

internal fun focusFrameAt(segments: List<FocusSegment>, totalMs: Long, elapsedMs: Long): FocusFrame {
    var t = ((elapsedMs % totalMs) + totalMs) % totalMs
    var segment = segments.last()
    for (s in segments) {
        if (t < s.durationMs) { segment = s; break }
        t -= s.durationMs
    }
    val progress = if (segment.durationMs == 0L) 0f else t.toFloat() / segment.durationMs
    val xFrac = segment.fromFrac + (segment.toFrac - segment.fromFrac) * progress
    return FocusFrame(segment.anim, MascotScript.frameAt(segment.anim, t), xFrac)
}

/** Decodes once, shared by every rotating biome scene (they all draw from the same set). */
@Composable
internal fun rememberFocusBiomeMascotFrames(): Map<MascotAnim, List<ImageBitmap>> = mapOf(
    MascotAnim.WALK to rememberPixelBmps(
        R.drawable.mascot_walk_01, R.drawable.mascot_walk_02, R.drawable.mascot_walk_03,
        R.drawable.mascot_walk_04, R.drawable.mascot_walk_05, R.drawable.mascot_walk_06,
        R.drawable.mascot_walk_07, R.drawable.mascot_walk_08
    ),
    MascotAnim.CHOP to rememberPixelBmps(
        R.drawable.mascot2_chop_01, R.drawable.mascot2_chop_02, R.drawable.mascot2_chop_03,
        R.drawable.mascot2_chop_04, R.drawable.mascot2_chop_05, R.drawable.mascot2_chop_06
    ),
    MascotAnim.HAMMER to rememberPixelBmps(
        R.drawable.mascot2_hammer_01, R.drawable.mascot2_hammer_02, R.drawable.mascot2_hammer_03,
        R.drawable.mascot2_hammer_04, R.drawable.mascot2_hammer_05, R.drawable.mascot2_hammer_06
    ),
    MascotAnim.GATHER to rememberPixelBmps(
        R.drawable.mascot2_gather_01, R.drawable.mascot2_gather_02, R.drawable.mascot2_gather_03,
        R.drawable.mascot2_gather_04, R.drawable.mascot2_gather_05, R.drawable.mascot2_gather_06
    ),
    MascotAnim.REST to rememberPixelBmps(
        R.drawable.mascot2_rest_01, R.drawable.mascot2_rest_02, R.drawable.mascot2_rest_03,
        R.drawable.mascot2_rest_04, R.drawable.mascot2_rest_05, R.drawable.mascot2_rest_06
    ),
    MascotAnim.DIG to rememberPixelBmps(
        R.drawable.mascot2_dig_01, R.drawable.mascot2_dig_02, R.drawable.mascot2_dig_03,
        R.drawable.mascot2_dig_04, R.drawable.mascot2_dig_05, R.drawable.mascot2_dig_06
    )
)

/** One rotating biome scene: full-bleed background + a mascot looping between activity points. */
@Composable
internal fun FocusBiomeScene(
    scene: FocusScene,
    elapsed: State<Long>,
    animationsEnabled: Boolean,
    frames: Map<MascotAnim, List<ImageBitmap>>,
    modifier: Modifier = Modifier
) {
    val loop = remember(scene) { buildFocusLoop(scene.points) }
    val totalMs = remember(loop) { loop.sumOf { it.durationMs } }

    Image(
        painter = painterResource(id = scene.backgroundRes),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize()
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize().clipToBounds()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val mascotHeightPx = heightPx * 0.34f
        val mascotWidthPx = mascotHeightPx * (MascotScript.MASCOT_W / MascotScript.MASCOT_H)
        val mascotHeightDp = with(density) { mascotHeightPx.toDp() }
        val mascotWidthDp = with(density) { mascotWidthPx.toDp() }

        // Only the (anim, frameIndex) pair recomposes this node (a few Hz); xFrac is read
        // again inside the graphicsLayer draw lambda below, so the 60fps clock never does.
        val frameKey by remember(loop, animationsEnabled) {
            derivedStateOf {
                if (!animationsEnabled) {
                    scene.points.first().anim to 0
                } else {
                    val f = focusFrameAt(loop, totalMs, elapsed.value)
                    f.anim to f.frameIndex
                }
            }
        }
        val (anim, frameIndex) = frameKey
        val bitmap = frames.getValue(anim)[frameIndex]
        val staticXFrac = scene.points.first().xFrac

        Image(
            bitmap = bitmap,
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .size(mascotWidthDp, mascotHeightDp)
                .graphicsLayer {
                    val xFrac = if (animationsEnabled) focusFrameAt(loop, totalMs, elapsed.value).xFrac else staticXFrac
                    translationX = xFrac * widthPx - mascotWidthPx / 2f
                    translationY = scene.groundFrac * heightPx - mascotHeightPx
                }
        )
    }
}
