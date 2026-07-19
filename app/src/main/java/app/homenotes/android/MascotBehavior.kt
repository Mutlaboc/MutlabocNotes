package app.homenotes.android

/*
 * Deterministic behaviour script for the yard mascot. Everything is a pure function of
 * the elapsed-milliseconds clock, matching the scene's no-recomposition design: the
 * caller reads position in the layout lambda and facing in the draw lambda, while only
 * the integer frame key recomposes the sprite node.
 *
 * The mascot loops one fixed route: enters from the left, hammers at the house wall,
 * chops at the tree, thinks and writes a note mid-yard, strolls off to the right and
 * walks back across. A scripted loop (rather than randomness) keeps the automaton unit
 * testable and the clock the single source of truth.
 */

internal enum class MascotAnim { WALK, IDLE, NOTES, CHOP, HAMMER }

/** Per-frame hold times; non-uniform timings make the tool swings read naturally. */
internal val MASCOT_FRAME_TIMINGS_MS: Map<MascotAnim, LongArray> = mapOf(
    MascotAnim.WALK to LongArray(8) { 110L },
    MascotAnim.IDLE to longArrayOf(500, 200, 600, 600, 200, 500),
    MascotAnim.NOTES to longArrayOf(450, 250, 250, 250, 450, 400),
    MascotAnim.CHOP to longArrayOf(400, 220, 220, 110, 150, 260),
    MascotAnim.HAMMER to longArrayOf(400, 220, 220, 110, 150, 260)
)

internal data class MascotSegment(
    val anim: MascotAnim,
    val durationMs: Long,
    val fromX: Float,
    val toX: Float,
    val facingLeft: Boolean
)

internal data class MascotState(
    val anim: MascotAnim,
    val frameIndex: Int,
    val centerX: Float,
    val facingLeft: Boolean
)

internal object MascotScript {
    // Sprite frame geometry (320x485 canvas, drawn at 430 scene units tall). The body is
    // feet-anchored at the canvas centre; extended tools reach ~141 scene units from it.
    const val MASCOT_H = 430f
    const val MASCOT_W = MASCOT_H * (320f / 485f)

    private const val SCENE_W = 2120f
    private const val WALK_SPEED = 2324f / 9_000f  // scene units per ms, the old cross pace

    private const val OFF_LEFT = -MASCOT_W / 2f
    private const val OFF_RIGHT = SCENE_W + MASCOT_W / 2f
    private const val HAMMER_X = 1200f  // hammer (flipped left) lands on the house wall at x=1060
    private const val CHOP_X = 1290f    // axe lands on the tree trunk edge at x=1430
    private const val IDLE_X = 600f     // open grass for thinking
    private const val NOTES_X = 900f    // open grass for note writing

    private fun loopMs(anim: MascotAnim) = MASCOT_FRAME_TIMINGS_MS.getValue(anim).sum()

    private fun walk(fromX: Float, toX: Float) = MascotSegment(
        MascotAnim.WALK,
        (Math.abs(toX - fromX) / WALK_SPEED).toLong(),
        fromX, toX,
        facingLeft = toX < fromX
    )

    private fun stay(anim: MascotAnim, x: Float, loops: Int, facingLeft: Boolean) =
        MascotSegment(anim, loopMs(anim) * loops, x, x, facingLeft)

    // One activity per yard pass, with full walking crossings in between, so the mascot
    // spends most of the loop strolling (~75% walking over a ~70s cycle).
    val segments: List<MascotSegment> = listOf(
        walk(OFF_LEFT, OFF_RIGHT),
        walk(OFF_RIGHT, HAMMER_X),
        stay(MascotAnim.HAMMER, HAMMER_X, loops = 3, facingLeft = true),
        walk(HAMMER_X, OFF_LEFT),
        walk(OFF_LEFT, OFF_RIGHT),
        walk(OFF_RIGHT, CHOP_X),
        stay(MascotAnim.CHOP, CHOP_X, loops = 3, facingLeft = false),
        walk(CHOP_X, OFF_LEFT),
        walk(OFF_LEFT, IDLE_X),
        stay(MascotAnim.IDLE, IDLE_X, loops = 2, facingLeft = false),
        walk(IDLE_X, OFF_RIGHT),
        walk(OFF_RIGHT, NOTES_X),
        stay(MascotAnim.NOTES, NOTES_X, loops = 2, facingLeft = true),
        walk(NOTES_X, OFF_LEFT)
    )

    val totalMs: Long = segments.sumOf { it.durationMs }

    fun stateAt(elapsed: Long): MascotState {
        var t = ((elapsed % totalMs) + totalMs) % totalMs
        var segment = segments.last()
        for (s in segments) {
            if (t < s.durationMs) { segment = s; break }
            t -= s.durationMs
        }
        val progress = if (segment.durationMs == 0L) 0f else t.toFloat() / segment.durationMs
        return MascotState(
            anim = segment.anim,
            frameIndex = frameAt(segment.anim, t),
            centerX = segment.fromX + (segment.toX - segment.fromX) * progress,
            facingLeft = segment.facingLeft
        )
    }

    /** Compact recomposition key: changes only when the visible bitmap changes. */
    fun frameKeyAt(elapsed: Long): Int {
        val s = stateAt(elapsed)
        return (s.anim.ordinal shl 8) or s.frameIndex
    }

    fun animOf(frameKey: Int): MascotAnim = MascotAnim.entries[frameKey shr 8]

    fun frameOf(frameKey: Int): Int = frameKey and 0xFF

    /** Frame index for [anim] at [localMs] within its own loop (shared with other scripted scenes). */
    internal fun frameAt(anim: MascotAnim, localMs: Long): Int {
        val timings = MASCOT_FRAME_TIMINGS_MS.getValue(anim)
        var t = localMs % timings.sum()
        for (i in timings.indices) {
            if (t < timings[i]) return i
            t -= timings[i]
        }
        return timings.size - 1
    }
}
