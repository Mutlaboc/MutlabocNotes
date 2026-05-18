package com.example.mutlabocsnotes

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType

@Composable
internal fun NoteTitleField(
    title: String,
    isError: Boolean,
    onTitleChange: (String) -> Unit
) {
    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        label = { Text(stringResource(R.string.note_title_label)) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(EDIT_NOTE_TITLE_FIELD_TEST_TAG),
        isError = isError
    )
    if (isError) {
        Text(
            text = stringResource(R.string.note_title_required),
            color = MaterialTheme.colors.error,
            modifier = Modifier.testTag(EDIT_NOTE_TITLE_ERROR_TEST_TAG)
        )
    }
}

@Composable
internal fun NoteContentField(
    content: String,
    onContentChange: (String) -> Unit
) {
    OutlinedTextField(
        value = content,
        onValueChange = onContentChange,
        label = { Text(stringResource(R.string.note_content_label)) },
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.4f)
            .testTag(EDIT_NOTE_CONTENT_FIELD_TEST_TAG),
        maxLines = Int.MAX_VALUE
    )
}

@Composable
internal fun CoinCountDebugField(
    coinCountText: String,
    onCoinCountChange: (String) -> Unit
) {
    OutlinedTextField(
        value = coinCountText,
        onValueChange = { value ->
            if (value.all { it.isDigit() }) {
                onCoinCountChange(value)
            }
        },
        label = { Text(stringResource(R.string.coin_count_label)) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(EDIT_NOTE_COIN_COUNT_FIELD_TEST_TAG),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
internal fun DeleteNoteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.note_delete_title)) },
        text = { Text(stringResource(R.string.note_delete_message)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag(EDIT_NOTE_DELETE_CONFIRM_BUTTON_TEST_TAG)
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
