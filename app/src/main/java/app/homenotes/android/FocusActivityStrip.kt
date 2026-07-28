package app.homenotes.android

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/*
 * Idle-game-style activity lane for the focus/execution screen (ExpandedNoteOverlay,
 * shown next to the event feed while a note's timer runs): the mascot marches left to
 * right across the lane, stops mid-way to work — hammering for tasks, chopping (resource
 * gathering) for shopping — then exits right and re-enters left. Purely ambient, decouple
 * from real progress; reuses the same frame sequences and MotionUtils/MascotBehavior
 * helpers as HomeYardScene, so no new art assets are needed.
 */

private const val STRIP_HEIGHT_DP = 64
private const val MASCOT_ASPECT = MascotScript.MASCOT_W / MascotScript.MASCOT_H
private const val WORK_FRACTION = 0.45f
private const val WALK_TO_WORK_MS = 1_800L
private const val WALK_TO_EXIT_MS = 1_800L
private const val WORK_LOOPS = 3

private fun activityAnimFor(category: NoteCategory): MascotAnim = when (category) {
    NoteCategory.SHOPPING -> MascotAnim.CHOP
    NoteCategory.TASKS, NoteCategory.RECURRING_TASKS -> MascotAnim.HAMMER
}

private data class ActivitySegment(
    val anim: MascotAnim,
    val durationMs: Long,
    val fromFrac: Float,
    val toFrac: Float
)

// Off-lane on both sides so the sprite fully enters/exits before the loop wraps.
private fun buildLoop(activityAnim: MascotAnim): List<ActivitySegment> {
    val workLoopMs = MASCOT_FRAME_TIMINGS_MS.getValue(activityAnim).sum() * WORK_LOOPS
    return listOf(
        ActivitySegment(MascotAnim.WALK, WALK_TO_WORK_MS, -0.15f, WORK_FRACTION),
        ActivitySegment(activityAnim, workLoopMs, WORK_FRACTION, WORK_FRACTION),
        ActivitySegment(MascotAnim.WALK, WALK_TO_EXIT_MS, WORK_FRACTION, 1.15f)
    )
}

private data class ActivityFrame(val anim: MascotAnim, val frameIndex: Int, val xFrac: Float)

private fun frameAt(loop: List<ActivitySegment>, totalMs: Long, elapsedMs: Long): ActivityFrame {
    var t = ((elapsedMs % totalMs) + totalMs) % totalMs
    var segment = loop.last()
    for (s in loop) {
        if (t < s.durationMs) { segment = s; break }
        t -= s.durationMs
    }
    val progress = if (segment.durationMs == 0L) 0f else t.toFloat() / segment.durationMs
    val xFrac = segment.fromFrac + (segment.toFrac - segment.fromFrac) * progress
    return ActivityFrame(segment.anim, MascotScript.frameAt(segment.anim, t), xFrac)
}

/** Category-themed marching/working mascot lane, shown alongside the focus-event feed. */
@Composable
internal fun FocusActivityStrip(
    category: NoteCategory,
    modifier: Modifier = Modifier
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val elapsed = rememberElapsedMillis(animationsEnabled)
    val activityAnim = activityAnimFor(category)
    val loop = remember(activityAnim) { buildLoop(activityAnim) }
    val totalMs = remember(loop) { loop.sumOf { it.durationMs } }

    val frames = mapOf(
        MascotAnim.WALK to rememberPixelBmps(
            R.drawable.mascot_walk_01, R.drawable.mascot_walk_02, R.drawable.mascot_walk_03,
            R.drawable.mascot_walk_04, R.drawable.mascot_walk_05, R.drawable.mascot_walk_06,
            R.drawable.mascot_walk_07, R.drawable.mascot_walk_08
        ),
        MascotAnim.HAMMER to rememberPixelBmps(
            R.drawable.mascot2_hammer_01, R.drawable.mascot2_hammer_02, R.drawable.mascot2_hammer_03,
            R.drawable.mascot2_hammer_04, R.drawable.mascot2_hammer_05, R.drawable.mascot2_hammer_06
        ),
        MascotAnim.CHOP to rememberPixelBmps(
            R.drawable.mascot2_chop_01, R.drawable.mascot2_chop_02, R.drawable.mascot2_chop_03,
            R.drawable.mascot2_chop_04, R.drawable.mascot2_chop_05, R.drawable.mascot2_chop_06
        )
    )

    // Only the (anim, frameIndex) pair recomposes this node (a few Hz); the x position is
    // read inside the graphicsLayer draw lambda below, so the 60fps clock never does.
    val frameKey by remember(loop, animationsEnabled) {
        derivedStateOf {
            if (!animationsEnabled) {
                MascotAnim.WALK to 0
            } else {
                val frame = frameAt(loop, totalMs, elapsed.value)
                frame.anim to frame.frameIndex
            }
        }
    }
    val (anim, frameIndex) = frameKey
    val bitmap = frames.getValue(anim)[frameIndex]

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(STRIP_HEIGHT_DP.dp)
            .clipToBounds()
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val mascotHeightDp = STRIP_HEIGHT_DP.dp
        val mascotWidthDp = mascotHeightDp * MASCOT_ASPECT
        val mascotWidthPx = with(density) { mascotWidthDp.toPx() }

        Image(
            bitmap = bitmap,
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(mascotWidthDp, mascotHeightDp)
                .graphicsLayer {
                    val xFrac = if (animationsEnabled) {
                        frameAt(loop, totalMs, elapsed.value).xFrac
                    } else {
                        WORK_FRACTION
                    }
                    translationX = xFrac * widthPx - mascotWidthPx / 2f
                }
        )
    }
}


@Preview(showBackground = true)
@Composable
fun FocusActivityStripPreview() {

    FocusActivityStrip(
        NoteCategory.RECURRING_TASKS,)
}