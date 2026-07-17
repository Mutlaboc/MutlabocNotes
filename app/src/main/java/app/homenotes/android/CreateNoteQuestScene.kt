package app.homenotes.android

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
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

private const val QUEST_FRAME_MS = 320L

/** Decorative tavern-yard footer shown only while creating a note. */
@Composable
internal fun CreateNoteQuestScene(modifier: Modifier = Modifier) {
    val animationsEnabled = rememberAnimationsEnabled()
    val elapsed = rememberElapsedMillis(animationsEnabled)
    val scribeFrames = rememberPixelBmps(
        R.drawable.create_note_scribe_01,
        R.drawable.create_note_scribe_02,
        R.drawable.create_note_scribe_03,
        R.drawable.create_note_scribe_04,
        R.drawable.create_note_scribe_05,
        R.drawable.create_note_scribe_06
    )
    val frameIndex by remember(scribeFrames) {
        derivedStateOf { ((elapsed.value / QUEST_FRAME_MS) % scribeFrames.size).toInt() }
    }

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .testTag(CREATE_NOTE_QUEST_SCENE_TEST_TAG)
    ) {
        val compact = maxHeight < 140.dp
        val groundHeight = if (compact) 56.dp else 64.dp
        val tavernHeight = if (compact) 102.dp else 148.dp
        val scribeHeight = if (compact) 104.dp else 150.dp

        Image(
            bitmap = pixelImageBitmap(R.drawable.create_note_ground_01),
            contentDescription = null,
            filterQuality = FilterQuality.None,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(groundHeight)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .fillMaxHeight()
        ) {
            Image(
                bitmap = pixelImageBitmap(R.drawable.create_note_tavern_01),
                contentDescription = null,
                filterQuality = FilterQuality.None,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(
                        x = if (compact) (-34).dp else (-28).dp,
                        y = if (compact) (-30).dp else (-34).dp
                    )
                    .height(tavernHeight)
            )
            Image(
                bitmap = scribeFrames[frameIndex],
                contentDescription = null,
                filterQuality = FilterQuality.None,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(
                        x = if (compact) (-2).dp else (-6).dp,
                        y = if (compact) (-22).dp else (-26).dp
                    )
                    .height(scribeHeight)
            )
        }
    }
}
