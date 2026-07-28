package app.homenotes.android

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun EditNoteScreenPreview() {
    val sampleNote = Note(
        id = "1",
        title = "Sample",
        content = "Content",
        category = NoteCategory.RECURRING_TASKS,
        startAtMillis = System.currentTimeMillis() + 86_400_000L,
        durationMinutes = 60,
        repeatRule = RepeatRule.DAILY,
        coinCount = 3
    )
    EditNoteScreen(
        note = sampleNote,
        onSaveClick = { _ -> },
        onDeleteClick = { }
    )
}
