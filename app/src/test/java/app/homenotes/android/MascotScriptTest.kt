package app.homenotes.android

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MascotScriptTest {

    @Test
    fun `total duration is the sum of segment durations`() {
        assertEquals(MascotScript.segments.sumOf { it.durationMs }, MascotScript.totalMs)
        assertTrue(MascotScript.totalMs > 0)
    }

    @Test
    fun `route is continuous - no teleports at segment boundaries`() {
        var boundary = 0L
        for (segment in MascotScript.segments) {
            boundary += segment.durationMs
            val before = MascotScript.stateAt(boundary - 1)
            val after = MascotScript.stateAt(boundary)
            // One ms of walking covers well under 1 scene unit; anything larger is a jump.
            assertTrue(
                "jump of ${abs(after.centerX - before.centerX)} at boundary $boundary",
                abs(after.centerX - before.centerX) < 2f
            )
        }
    }

    @Test
    fun `script wraps around the loop`() {
        val atStart = MascotScript.stateAt(0)
        val atLoop = MascotScript.stateAt(MascotScript.totalMs)
        assertEquals(atStart, atLoop)
    }

    @Test
    fun `frame index stays within the animation frame count`() {
        for (t in 0 until MascotScript.totalMs step 37) {
            val state = MascotScript.stateAt(t)
            val frameCount = MASCOT_FRAME_TIMINGS_MS.getValue(state.anim).size
            assertTrue(state.frameIndex in 0 until frameCount)
        }
    }

    @Test
    fun `walking segments face the direction of travel`() {
        for (segment in MascotScript.segments) {
            if (segment.anim == MascotAnim.WALK && segment.fromX != segment.toX) {
                assertEquals(segment.toX < segment.fromX, segment.facingLeft)
            }
        }
    }

    @Test
    fun `stationary activities happen at their scripted spots`() {
        val stationary = MascotScript.segments.filter { it.anim != MascotAnim.WALK }
        assertTrue(stationary.map { it.anim }.containsAll(
            listOf(MascotAnim.HAMMER, MascotAnim.CHOP, MascotAnim.IDLE, MascotAnim.NOTES)
        ))
        for (segment in stationary) {
            assertEquals(segment.fromX, segment.toX, 0f)
        }
    }

    @Test
    fun `frame key round-trips animation and frame index`() {
        for (t in 0 until MascotScript.totalMs step 113) {
            val state = MascotScript.stateAt(t)
            val key = MascotScript.frameKeyAt(t)
            assertEquals(state.anim, MascotScript.animOf(key))
            assertEquals(state.frameIndex, MascotScript.frameOf(key))
        }
    }
}
