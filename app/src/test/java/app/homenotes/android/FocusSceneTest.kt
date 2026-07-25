package app.homenotes.android

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusSceneTest {

    @Test
    fun `every biome scene has activity points in increasing x order`() {
        for (scene in FOCUS_BIOME_SCENES) {
            assertTrue(scene.points.isNotEmpty())
            val xs = scene.points.map { it.xFrac }
            assertEquals("points should be visited left-to-right", xs.sorted(), xs)
            for (x in xs) assertTrue(x in 0f..1f)
        }
    }

    @Test
    fun `loop is continuous within a pass - no teleports at segment boundaries`() {
        // Boundaries inside a pass must be continuous. The mascot may only jump where one
        // pass ends and the next begins — that happens beyond both edges of the scene, so
        // it is never visible (same accepted pattern as FocusActivityStrip's single-stop
        // loop, now repeated once per pass).
        for (scene in FOCUS_BIOME_SCENES) {
            val loop = buildFocusLoop(scene.points)
            val totalMs = loop.sumOf { it.durationMs }
            var boundary = 0L
            for (segment in loop.dropLast(1)) {
                boundary += segment.durationMs
                val before = focusFrameAt(loop, totalMs, boundary - 1)
                val after = focusFrameAt(loop, totalMs, boundary)
                val offScreenWrap = before.xFrac !in 0f..1f && after.xFrac !in 0f..1f
                assertTrue(
                    "jump of ${abs(after.xFrac - before.xFrac)} at boundary $boundary",
                    offScreenWrap || abs(after.xFrac - before.xFrac) < 0.05f
                )
            }
        }
    }

    @Test
    fun `the mascot only stops once every FOCUS_PASSES_PER_ACTIVITY passes`() {
        for (scene in FOCUS_BIOME_SCENES) {
            val loop = buildFocusLoop(scene.points)
            // A pass is a walk that ends beyond the right edge; every segment chain in the
            // loop finishes there, so counting those counts the crossings.
            val passes = loop.count { it.toFrac == FOCUS_OFF_RIGHT_FRAC }
            val activities = loop.count { it.anim != MascotAnim.WALK }
            assertEquals(scene.points.size, activities)
            assertEquals(activities * FOCUS_PASSES_PER_ACTIVITY, passes)
        }
    }

    @Test
    fun `every pass starts beyond the left edge`() {
        for (scene in FOCUS_BIOME_SCENES) {
            val loop = buildFocusLoop(scene.points)
            for ((index, segment) in loop.withIndex()) {
                val startsPass = index == 0 || loop[index - 1].toFrac == FOCUS_OFF_RIGHT_FRAC
                if (startsPass) {
                    assertEquals(FOCUS_OFF_LEFT_FRAC, segment.fromFrac, 0f)
                }
            }
        }
    }

    @Test
    fun `the loop wrap point is off-screen on both sides`() {
        for (scene in FOCUS_BIOME_SCENES) {
            val loop = buildFocusLoop(scene.points)
            val totalMs = loop.sumOf { it.durationMs }
            val beforeWrap = focusFrameAt(loop, totalMs, totalMs - 1)
            val afterWrap = focusFrameAt(loop, totalMs, totalMs)
            assertTrue(beforeWrap.xFrac !in 0f..1f)
            assertTrue(afterWrap.xFrac !in 0f..1f)
        }
    }

    @Test
    fun `loop wraps around cleanly`() {
        for (scene in FOCUS_BIOME_SCENES) {
            val loop = buildFocusLoop(scene.points)
            val totalMs = loop.sumOf { it.durationMs }
            assertEquals(focusFrameAt(loop, totalMs, 0), focusFrameAt(loop, totalMs, totalMs))
        }
    }

    @Test
    fun `every scripted activity point is visited stationary`() {
        for (scene in FOCUS_BIOME_SCENES) {
            val loop = buildFocusLoop(scene.points)
            val stationary = loop.filter { it.anim != MascotAnim.WALK }
            assertEquals(scene.points.map { it.anim }, stationary.map { it.anim })
            for (segment in stationary) {
                assertEquals(segment.fromFrac, segment.toFrac, 0f)
            }
        }
    }

    @Test
    fun `frame index stays within the animation frame count`() {
        for (scene in FOCUS_BIOME_SCENES) {
            val loop = buildFocusLoop(scene.points)
            val totalMs = loop.sumOf { it.durationMs }
            for (t in 0 until totalMs step 41) {
                val frame = focusFrameAt(loop, totalMs, t)
                val frameCount = MASCOT_FRAME_TIMINGS_MS.getValue(frame.anim).size
                assertTrue(frame.frameIndex in 0 until frameCount)
            }
        }
    }
}
