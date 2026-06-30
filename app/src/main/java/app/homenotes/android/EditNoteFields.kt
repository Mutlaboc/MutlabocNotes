package app.homenotes.android

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp

@Composable
internal fun NoteTitleField(
    title: String,
    isError: Boolean,
    onTitleChange: (String) -> Unit
) {
    CozyTextField(
        value = title,
        onValueChange = onTitleChange,
        label = stringResource(R.string.note_title_label),
        isError = isError,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(EDIT_NOTE_TITLE_FIELD_TEST_TAG)
    )
    if (isError) {
        Text(
            text = stringResource(R.string.note_title_required),
            color = CozyAuth.Terracotta,
            fontFamily = CozyAuth.PixelFont,
            fontSize = 13.sp,
            modifier = Modifier.testTag(EDIT_NOTE_TITLE_ERROR_TEST_TAG)
        )
    }
}

@Composable
internal fun NoteContentField(
    content: String,
    onContentChange: (String) -> Unit
) {
    CozyTextField(
        value = content,
        onValueChange = onContentChange,
        label = stringResource(R.string.note_content_label),
        singleLine = false,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.4f)
            .testTag(EDIT_NOTE_CONTENT_FIELD_TEST_TAG)
    )
}

@Composable
internal fun DeleteNoteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = CozyAuth.CardCream,
        title = {
            Text(
                text = stringResource(R.string.note_delete_title),
                color = CozyAuth.Ink,
                fontFamily = CozyAuth.PixelFont
            )
        },
        text = {
            Text(
                text = stringResource(R.string.note_delete_message),
                color = CozyAuth.InkSoft,
                fontFamily = CozyAuth.PixelFont
            )
        },
        confirmButton = {
            PixelPrimaryButton(
                text = stringResource(R.string.action_delete),
                onClick = onConfirm,
                modifier = Modifier.testTag(EDIT_NOTE_DELETE_CONFIRM_BUTTON_TEST_TAG)
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
