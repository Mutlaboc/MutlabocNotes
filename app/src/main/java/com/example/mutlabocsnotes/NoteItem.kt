package com.example.mutlabocsnotes

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun NoteItem(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCompletionChange: (Boolean) -> Unit
) {
    val categoryColors = noteCategoryColors(note.category)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            color = categoryColors.container,
            contentColor = categoryColors.content,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = note.isCompleted,
                        onCheckedChange = onCompletionChange
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onClick() }
                            .padding(start = 8.dp)
                    ) {
                        Text(text = note.title, style = MaterialTheme.typography.subtitle1)
                        NoteDetails(note)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                ) {
                    repeat(note.coinCount.coerceAtLeast(0)) {
                        Image(
                            painter = painterResource(id = R.drawable.gold_coin),
                            contentDescription = stringResource(R.string.coin_description),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteDetails(note: Note) {
    when (note.category) {
        NoteCategory.SHOPPING -> {
            if (note.checklist.isNotEmpty()) {
                note.checklist.take(3).forEach { item ->
                    Text(
                        text = stringResource(R.string.checklist_preview_item, item.text),
                        style = MaterialTheme.typography.body2
                    )
                }
                if (note.checklist.size > 3) {
                    Text(
                        text = stringResource(R.string.more_items_indicator),
                        style = MaterialTheme.typography.body2
                    )
                }
            } else if (note.content.isNotBlank()) {
                Text(text = note.content, style = MaterialTheme.typography.body2)
            }
        }

        NoteCategory.NOTES -> {
            if (note.content.isNotBlank()) {
                Text(text = note.content, style = MaterialTheme.typography.body2)
            }
        }

        NoteCategory.TASKS -> {
            if (note.content.isNotBlank()) {
                Text(text = note.content, style = MaterialTheme.typography.body2)
            }
            note.deadlineMillis?.let { millis ->
                Text(
                    text = stringResource(R.string.note_deadline_value, formatDeadlineDate(millis)),
                    style = MaterialTheme.typography.caption
                )
            }
            if (note.repeatRule != RepeatRule.NONE) {
                Text(
                    text = stringResource(
                        R.string.note_repeating,
                        stringResource(note.repeatRule.labelRes())
                    ),
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}
