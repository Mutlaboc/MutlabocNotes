package com.example.mutlabocsnotes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Checkbox
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Checkbox
import androidx.compose.runtime.mutableStateListOf



@Composable
fun HomeScreen(
    notes: List<Note>,
    userEmail: String,
    onAddNoteClick: () -> Unit,
    onNoteClick: (noteId: String) -> Unit,
    onOtherCellClick: (index: Int) -> Unit,
    onSwitchUser: () -> Unit,

) {
    // Простейший список категорий, пока статичный
    val categories = listOf("Все", "Работа", "Личное")
    var search by remember { mutableStateOf("") }

    val completedNotes = remember { mutableStateListOf<String>() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomRowWithFiveCells(
                onAddClick = onAddNoteClick,
                onCellClick = onOtherCellClick
            )
        }
    ) { paddingValues ->
        Column (modifier = Modifier.padding(paddingValues) ) {
            Image(
                painter = painterResource(id = R.drawable.main_image),
                contentDescription = "Home image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)

            )
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
/*                IconButton(onClick = { TODO() }) {
                    Icon(Icons.Default.Menu, contentDescription = "Меню")
                }*/
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Поиск") },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f)
                )
                var userMenuExtended by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box{
                        IconButton(onClick =  { userMenuExtended = true }) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Пользователь",
                            )
                        }
                        DropdownMenu(
                            expanded = userMenuExtended,
                            onDismissRequest = {userMenuExtended = false}
                        ) {
                            DropdownMenuItem (onClick = {
                                userMenuExtended = false
                                onSwitchUser()
                            }) {
                                Text("Сменить пользователя")
                            }
                        }
                    }
/*                    if (userEmail.isNotEmpty()) {
                        Text(userEmail, modifier = Modifier.padding(start = 4.dp))
                    }*/
                }

            }
            LazyColumn(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(notes.filter { it.id !in completedNotes }, key = {it.id}) { note ->
                    NoteItem(
                        note = note,
                        onClick = { onNoteClick(note.id)},
                        onCompleted = { completedId ->
                            if (completedId !in completedNotes) {
                                completedNotes.add(completedId)
                            }

                        }
                    )
                }
            }
            Column(modifier = Modifier) {

            }
        }


    }
}

// Простой composable для отображения одной заметки
@Composable
fun NoteItem(
    note: Note,
    onClick: () -> Unit,
    onCompleted: (String) -> Unit) {
    var isChecked by remember { mutableStateOf(false) }
    val coinCount by remember(note.id) { mutableStateOf((1..5).random()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ){
            Checkbox(
                checked = isChecked,
                onCheckedChange = { checked ->
                        isChecked = checked
                    if (checked) {
                        onCompleted(note.id)
                    }

                }
            )
            Column (
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClick() }
                    .padding(start = 8.dp)
            ){
                Text(text = note.title, style = MaterialTheme.typography.subtitle1)
                Text(text = note.content, style = MaterialTheme.typography.body2)
            }
        }
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            repeat(coinCount) {
                Image(
                    painter = painterResource(id = R.drawable.gold_coin),
                    contentDescription = "Золотая монета",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

    }
}
@Composable
fun BottomRowWithFiveCells (
    onAddClick: () -> Unit,
    onCellClick: (index: Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colors.primarySurface),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (index in 0 until 3) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable {
                        if (index == 2) {
                            onAddClick()
                        } else {
                            onCellClick(index)
                        }
                    },
                contentAlignment = Alignment.Center
            )
            {
                when (index) {
                    0 -> {
                        Icon(
                            imageVector = Icons.Default.Task,
                            contentDescription = "Ячейка 1",
                            tint = MaterialTheme.colors.onPrimary
                        )
                    }

                    1 -> {
                        Icon(
                            imageVector = Icons.Default.AddHome,
                            contentDescription = "Ячейка 2",
                            tint = MaterialTheme.colors.onPrimary
                        )
                    }

                    2 -> {
                        // Иконка «Добавить»
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Добавить заметку",
                            tint = MaterialTheme.colors.onPrimary
                        )
                    }

                    3 -> {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Ячейка 4",
                            tint = MaterialTheme.colors.onPrimary
                        )
                    }

                    4 -> {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ячейка 5",
                            tint = MaterialTheme.colors.onPrimary
                        )
                    }

                }
            }
        }
    }
    @Composable
    fun SearchRow(
        query: String,
        onQueryChange: (String) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* TODO: открыть боковое меню */ }) {
                Icon(Icons.Default.Menu, contentDescription = "Меню")
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Поиск") },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val sampleNotes = listOf(
        Note(id = "1", title = "Заметка 1", content = "Содержание заметки"),
        Note(id = "2", title = "Заметка 2", content = "Содержание заметки"),
        Note(id = "3", title = "Заметка 3", content = "Содержание заметки")
    )
    HomeScreen(
        notes = sampleNotes,
        userEmail = "user@example.com",
        onAddNoteClick = {},
        onNoteClick = {},
        onOtherCellClick = {},
        onSwitchUser = {}

    )
}
