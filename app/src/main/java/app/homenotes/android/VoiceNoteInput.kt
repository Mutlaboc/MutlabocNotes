package app.homenotes.android

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.Locale

const val EDIT_NOTE_VOICE_INPUT_BUTTON_TEST_TAG = "edit_note_voice_input_button"
const val EDIT_NOTE_VOICE_DIALOG_FIELD_TEST_TAG = "edit_note_voice_dialog_field"
const val EDIT_NOTE_VOICE_DIALOG_CONFIRM_BUTTON_TEST_TAG = "edit_note_voice_dialog_confirm_button"

/**
 * Starts the device's system speech recognizer (RecognizerIntent). Audio capture and the
 * RECORD_AUDIO permission prompt are owned entirely by that system activity, so dictation
 * needs no extra permission or third-party library in this app. Returns a start function;
 * the best-guess transcript reaches [onResult], a missing recognizer reaches [onUnavailable].
 */
@Composable
private fun rememberVoiceRecognitionLauncher(
    onResult: (String) -> Unit,
    onUnavailable: () -> Unit
): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull { it.isNotBlank() }
                ?.let(onResult)
        }
    }
    return {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,  "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.note_voice_prompt))
        }
        try {
            launcher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            onUnavailable()
        }
    }
}

/**
 * Mic button for dictating a note: starts listening, then shows the transcript in an
 * editable dialog so the user can fix misheard words before it's applied to the note.
 */
@Composable
internal fun VoiceInputButton(
    onTextConfirmed: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var draft by rememberSaveable { mutableStateOf<String?>(null) }
    var unavailable by rememberSaveable { mutableStateOf(false) }

    val startListening = rememberVoiceRecognitionLauncher(
        onResult = { draft = it },
        onUnavailable = { unavailable = true }
    )

    IconButton(
        onClick = startListening,
        modifier = modifier.testTag(EDIT_NOTE_VOICE_INPUT_BUTTON_TEST_TAG)
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = stringResource(R.string.note_voice_input_description),
            tint = CozyAuth.Ink
        )
    }

    val pendingDraft = draft
    if (pendingDraft != null) {
        VoiceTranscriptDialog(
            initialText = pendingDraft,
            onConfirm = { edited ->
                draft = null
                onTextConfirmed(edited)
            },
            onDismiss = { draft = null }
        )
    }

    if (unavailable) {
        VoiceUnavailableDialog(onDismiss = { unavailable = false })
    }
}

/** Lets the user review and fix the recognized text before it lands in the note. */
@Composable
private fun VoiceTranscriptDialog(
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember(initialText) { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = CozyAuth.CardCream,
        title = {
            Text(
                text = stringResource(R.string.note_voice_dialog_title),
                color = CozyAuth.Ink,
                fontFamily = CozyAuth.PixelFont
            )
        },
        text = {
            CozyTextField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(R.string.note_voice_dialog_hint),
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .testTag(EDIT_NOTE_VOICE_DIALOG_FIELD_TEST_TAG)
            )
        },
        confirmButton = {
            PixelPrimaryButton(
                text = stringResource(R.string.note_voice_apply),
                onClick = { onConfirm(text) },
                modifier = Modifier.testTag(EDIT_NOTE_VOICE_DIALOG_CONFIRM_BUTTON_TEST_TAG)
            )
        },
        dismissButton = {
            PixelOutlineButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss
            )
        }
    )
}

@Composable
private fun VoiceUnavailableDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = CozyAuth.CardCream,
        title = {
            Text(
                text = stringResource(R.string.note_voice_unavailable_title),
                color = CozyAuth.Ink,
                fontFamily = CozyAuth.PixelFont
            )
        },
        text = {
            Text(
                text = stringResource(R.string.note_voice_unavailable_message),
                color = CozyAuth.InkSoft,
                fontFamily = CozyAuth.PixelFont
            )
        },
        confirmButton = {
            PixelPrimaryButton(
                text = stringResource(R.string.onboarding_done),
                onClick = onDismiss
            )
        }
    )
}
