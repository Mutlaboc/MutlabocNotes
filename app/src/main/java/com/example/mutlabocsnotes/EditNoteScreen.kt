package com.example.mutlabocsnotes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun EditNoteScreen(
    note: Note?,
    onSaveClick: (title: String, content:String) -> Unit
    onDeleteClick: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Редактирование заметки") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Заголовок") },
                textStyle = TextStyle(color = Color.Black),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Gray,
                    cursorColor = Color.Black),

                modifier = Modifier.fillMaxWidth(),

            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Содержимое", color = Color.Black) },
                textStyle = TextStyle(color = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                maxLines = Int.MAX_VALUE

            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    onSaveClick(title, content)
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Сохранить")
            }
            onDeleteClick?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { it() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Удалить")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditNoteScreenPreview() {
    // Для предварительного просмотра редактирования заметки передаём примерную заметку.
    val sampleNote = Note(id = 1, title = "Пример", content = "Содержимое")
    EditNoteScreen(
        note = sampleNote,
        onSaveClick = { _, _ -> },
        onDeleteClick = { }
    )
}
