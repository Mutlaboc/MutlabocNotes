package com.example.mutlabocsnotes

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun EditNoteScreenPreview() {
    val sampleNote = Note(
        id = "1",
        title = "Sample",
        content = "Content",
        category = NoteCategory.NOTES,
        coinCount = 3
    )
    EditNoteScreen(
        note = sampleNote,
        onSaveClick = { _ -> },
        onDeleteClick = { }
    )
}
