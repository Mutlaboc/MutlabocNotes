package com.example.mutlabocsnotes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview




@Composable
fun HomeScreen(
    notes: List<Note>,
    onAddNoteClick: () -> Unit,
    onNoteClick: (Int) -> Unit
) {
    // Простейший список категорий, пока статичный
    val categories = listOf("Все", "Работа", "Личное")

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
        // Размещаем категории слева и список заметок справа
        Row(modifier = Modifier.padding(paddingValues)) {
            LazyColumn(modifier = Modifier.width(120.dp)) {
                items(categories) { category ->
                    Text(
                        text = category,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(notes) { note ->
                    NoteItem(
                        note = note,
                        onClick = { onNoteClick(note.id)}
                    )
                }
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

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val sampleNotes = listOf(
        Note(id = 1, title = "Заметка 1", content = "Содержание заметки")
    )
    HomeScreen(
        notes = sampleNotes,
        onAddNoteClick = {},
        onNoteClick = {}
    )
}