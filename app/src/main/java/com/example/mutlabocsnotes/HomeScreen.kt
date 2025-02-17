package com.example.mutlabocsnotes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


data class Note(
    val id: Int,
    val title: String,
    val content: String
)

@Composable
fun HomeScreen(
    onAddNoteClick: () -> Unit,
    onNoteClick: (Int) -> Unit
) {
    // Пример: список заметок в локальном состоянии
    // В реальном приложении стоит подключить ViewModel, LiveData/Flow
    var notes by remember { mutableStateOf(listOf<Note>()) }

    // Scaffold даёт “каркас” с Fab, TopAppBar, Snackbar и т.д.
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Мои заметки") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNoteClick) {
                Text("+")
            }
        }
    ) { paddingValues ->
        // Содержимое экрана с учетом отступов от Scaffold
        LazyColumn(contentPadding = paddingValues) {
            items(notes) { note ->
                NoteItem(
                    note = note,
                    onClick = { onNoteClick(note.id) }
                )
            }
        }
    }
}

// Простой composable для отображения одной заметки
@Composable
fun NoteItem(note: Note, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Text(text = note.title, style = MaterialTheme.typography.subtitle1)
        Text(text = note.content, style = MaterialTheme.typography.body2)
    }
}
