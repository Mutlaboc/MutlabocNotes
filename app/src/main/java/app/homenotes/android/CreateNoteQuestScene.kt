package app.homenotes.android

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

const val CREATE_NOTE_QUEST_SCENE_TEST_TAG = "create_note_quest_scene"

private const val QUEST_FRAME_MS = 180L

/**
 * Decorative footer shown only while creating a note: the mascot writes a
 * quest scroll at a desk on a grass strip, with a quest board in the back.
 *
 * All element heights are derived from the host height, so nothing is ever
 * clipped regardless of the scene height chosen by the caller.
 */
@Composable
internal fun CreateNoteQuestScene(modifier: Modifier = Modifier) {
    val animationsEnabled = rememberAnimationsEnabled()
    val elapsed = rememberElapsedMillis(animationsEnabled)
    val deskFrames = rememberPixelBmps(
        R.drawable.create_note_desk_01,
        R.drawable.create_note_desk_02,
        R.drawable.create_note_desk_03,
        R.drawable.create_note_desk_04,
        R.drawable.create_note_desk_05,
        R.drawable.create_note_desk_06,
        R.drawable.create_note_desk_07,
        R.drawable.create_note_desk_08
    )
    val frameIndex by remember(deskFrames) {
        derivedStateOf { ((elapsed.value / QUEST_FRAME_MS) % deskFrames.size).toInt() }
    }

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .testTag(CREATE_NOTE_QUEST_SCENE_TEST_TAG)
    ) {
        val compact = maxHeight < 140.dp
        val grassHeight = if (compact) 36.dp else 44.dp
        // How far above the bottom edge the props stand (inside the grass).
        val standOffset = if (compact) 10.dp else 14.dp
        val topMargin = 6.dp
        // Fit-to-host: content can never extend past the top edge.
        val deskHeight = (maxHeight - standOffset - topMargin).coerceAtMost(150.dp)
        val boardHeight = deskHeight * 0.62f

        // Grass strip pinned to the bottom, same asset as the auth screen.
        Image(
            bitmap = pixelImageBitmap(R.drawable.auth_grass_strip),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(grassHeight)
        )

        // Quest board in the back-left; skipped on narrow hosts.
        if (maxWidth >= 300.dp) {
            Image(
                bitmap = pixelImageBitmap(R.drawable.create_note_board_01),
                contentDescription = null,
                filterQuality = FilterQuality.None,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 20.dp, y = -(standOffset + 4.dp))
                    .height(boardHeight)
            )
        }

        // Focal point: mascot writing a quest scroll.
        Image(
            bitmap = deskFrames[frameIndex],
            contentDescription = null,
            filterQuality = FilterQuality.None,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-16).dp, y = -standOffset)
                .height(deskHeight)
        )
    }
}
