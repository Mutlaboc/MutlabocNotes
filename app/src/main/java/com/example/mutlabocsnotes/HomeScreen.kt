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
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Add
import java.util.Calendar
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext


// Composable-функция для отображения главного экрана.
@Composable
fun HomeScreen(
    notes: List<Note>,
    userEmail: String,
    totalCoins: Int,
    onAddNoteClick: () -> Unit,
    onNoteClick: (noteId: String) -> Unit,
    onOtherCellClick: (index: Int) -> Unit,
    onCompletionChange: (noteId: String, Boolean) -> Unit,
    onSwitchUser: () -> Unit,
    onOpenSettings: () -> Unit,

) {
    // Зачатки поиска
    var search by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomRowWithFiveCells(
                selectedIndex = 0,
                onAddClick = onAddNoteClick,
                onCellClick = onOtherCellClick
            )
        }
    ) { paddingValues ->
        Column (modifier = Modifier.padding(paddingValues) ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.BottomCenter
            ){
                //TODO надо будет интерактивную картинку сделать.
                Image (
                    painter = painterResource(id = R.drawable.background_country_home),
                    contentDescription = "Home background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Row (
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(
                            color = MaterialTheme.colors.surface.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.gold_coin),
                        contentDescription = "Всего монет",
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = totalCoins.toString(),
                        style = MaterialTheme.typography.subtitle1,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

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
                            DropdownMenuItem (onClick = {
                                userMenuExtended = false
                                onOpenSettings()

                            }) {
                                Text("Настройки")
                            }
                            DropdownMenuItem (onClick = {

                            }) {

                            }
                        }
                    }
/*                    if (userEmail.isNotEmpty()) {
                        Text(userEmail, modifier = Modifier.padding(start = 4.dp))
                    }*/
                }

            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(notes.filter { !it.isCompleted }, key = { it.id }) { note ->
                    NoteItem(
                        note = note,
                        onClick = { onNoteClick(note.id)},
                        onCompletionChange = { isCompleted ->
                            onCompletionChange(note.id, isCompleted)
                        }
                    )
                }
            }
            Column(modifier = Modifier) {

            }
        }


    }
}
// базовая геймофикация, пока не реализовано
private enum class  BuildingStage {
    Foundation,
    Walls,
    Roof,
    Details,
    Lively
}

// Сопоставляет общее количество собранных монет со стадией прогресса строительства дома.
private fun stageForCoins(totalCoins: Int): BuildingStage = when {
    totalCoins >= 400 -> BuildingStage.Lively
    totalCoins >= 300 -> BuildingStage.Details
    totalCoins >= 200 -> BuildingStage.Roof
    totalCoins >= 100 -> BuildingStage.Walls
    else -> BuildingStage.Foundation
}



// Composable-функция для отображения экрана выполненных заметок.
@Composable
fun CompletedNotesScreen (
    notes: List<Note>,
    onaddNoteClick: () -> Unit,
    onNoteClick: (noteId:String) -> Unit,
    onCompletionChange: (noteId: String, Boolean) -> Unit,
    onNavigateHome: () -> Unit
) {
    val completedNotes = notes.filter { it.isCompleted }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomRowWithFiveCells(
                selectedIndex = 2,
                onAddClick = onaddNoteClick,
                onCellClick = { index ->
                    when (index) {
                        0 -> onNavigateHome()
                        2 -> { /* уже на экране выполненных заметок */ }
                    }

                }

            )
        }
    ) {
        paddingValues ->
        Column (
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Text(
                text = "Выполненные задачи",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            if (completedNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Здесь будут отображаться выполненнные задачи")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(completedNotes, key = {it.id}) {note ->
                        NoteItem(
                            note = note,
                            onClick = {onNoteClick(note.id)},
                            onCompletionChange = { isCompleted ->
                                onCompletionChange(note.id, isCompleted)

                            }
                        )
                    }
                }
            }
        }
    }
}
// Простой composable для отображения одной заметки
@Composable
fun NoteItem(
    note: Note,
    onClick: () -> Unit,
    onCompletionChange: (Boolean) -> Unit) {
    val coinCount = note.coinCount
    val backgroundColor = when (note.category) {
        NoteCategory.SHOPPING -> Color(0xFFD9F0FF)
        NoteCategory.TASKS -> Color(0xFFFFE2E2)
        NoteCategory.NOTES -> Color(0xFFE1F5E3)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            color = backgroundColor,
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
                )
                {
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
                        // Отображает детали заметки по-разному в зависимости от категории.
                        when (note.category) {
                            NoteCategory.SHOPPING -> {
                                if (note.checklist.isNotEmpty()) {
                                    note.checklist.take(3).forEach { item ->
                                        Text(
                                            text = "• ${item.text}",
                                            style = MaterialTheme.typography.body2
                                        )
                                    }
                                    if (note.checklist.size > 3) {
                                        Text(
                                            text = "…",
                                            style = MaterialTheme.typography.body2
                                        )
                                    }
                                } else if (note.content.isNotBlank()) {
                                    Text(
                                        text = note.content,
                                        style = MaterialTheme.typography.body2
                                    )
                                }
                            }

                            NoteCategory.NOTES -> {
                                if (note.content.isNotBlank()) {
                                    Text(
                                        text = note.content,
                                        style = MaterialTheme.typography.body2
                                    )
                                }
                            }

                            NoteCategory.TASKS -> {
                                if (note.content.isNotBlank()) {
                                    Text(
                                        text = note.content,
                                        style = MaterialTheme.typography.body2
                                    )
                                }
                                note.deadlineMillis?.let { millis ->
                                    Text(
                                        text = "Дедлайн: ${formatDeadline(millis)}",
                                        style = MaterialTheme.typography.caption
                                    )
                                }
                                if (note.isRepeating) {
                                    Text(
                                        text = "Повторяется",
                                        style = MaterialTheme.typography.caption
                                    )
                                }
                            }
                        }
                    }
                }


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    repeat(coinCount.coerceAtLeast(0)) {
                        Image(
                            painter = painterResource(id = R.drawable.gold_coin),
                            contentDescription = "Золотая монета",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

            }
        }
    }
}

        // Преобразует исходные значения в строку, понятную пользователю.
        private fun formatDeadline(millis: Long): String {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = millis
            }
            return "%02d.%02d.%04d".format(
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.YEAR)
            )
        }

        // Composable-функция для отображения нижней строки из пяти ячеек.
        @Composable
        fun BottomRowWithFiveCells(
            selectedIndex: Int,
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
                    val isSelected = index == selectedIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                if (index == 1) {
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
                                    contentDescription = "Активные задачи",
                                    tint = if (isSelected) MaterialTheme.colors.secondary else MaterialTheme.colors.onPrimary
                                )
                            }

                            1 -> {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Ячейка 2",
                                    tint = MaterialTheme.colors.onPrimary
                                )
                            }

                            2 -> {
                                // Иконка «Добавить»
                                Icon(
                                    imageVector = Icons.Default.QuestionMark,
                                    contentDescription = "Добавить заметку",
                                    tint = MaterialTheme.colors.onPrimary
                                )
                            }

                            3 -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Выполненные задачи",
                                    tint = if (isSelected) MaterialTheme.colors.secondary else MaterialTheme.colors.onPrimary
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
            // Composable-функция для отображения строки поиска.
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

        // Preview-composable для предпросмотра в Android Studio.
        @Preview(showBackground = true)
        @Composable
        fun HomeScreenPreview() {
            val sampleNotes = listOf(
                Note(id = "1", title = "Заметка 1", content = "Содержание заметки"),
                Note(id = "2", title = "Заметка 2", content = "Содержание заметки", NoteCategory.SHOPPING),
                Note(id = "3", title = "Заметка 3", content = "Содержание заметки", NoteCategory.TASKS)
            )
            HomeScreen(
                notes = sampleNotes,
                totalCoins = 12,
                userEmail = "user@example.com",
                onAddNoteClick = {},
                onNoteClick = {},
                onOtherCellClick = {},
                onCompletionChange = { _, _ -> },
                onSwitchUser = {},
                onOpenSettings = {}
            )
        }

