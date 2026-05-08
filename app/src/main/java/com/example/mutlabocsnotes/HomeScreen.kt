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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.primarySurface
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun HomeScreen(
    uiState: NotesUiState,
    uiMessage: UiMessage?,
    userEmail: String,
    onRetryNotes: () -> Unit,
    onMessageShown: (Long) -> Unit,
    onMessageAction: (UiMessageAction) -> Unit,
    onAddNoteClick: () -> Unit,
    onNoteClick: (noteId: String) -> Unit,
    onOtherCellClick: (index: Int) -> Unit,
    onCompletionChange: (noteId: String, Boolean) -> Unit,
    onSwitchUser: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val snackbarText = uiMessage?.text?.asString()
    val snackbarActionText = uiMessage?.actionText?.asString()
    val contentState = uiState as? NotesUiState.Content
    val notes = contentState?.notes.orEmpty()
    val totalCoins = contentState?.totalCoins ?: 0
    val activeNotes = notes.filter { !it.isCompleted }
    var search by remember { mutableStateOf("") }

    LaunchedEffect(uiMessage?.id) {
        val message = uiMessage ?: return@LaunchedEffect
        val text = snackbarText ?: return@LaunchedEffect
        val result = scaffoldState.snackbarHostState.showSnackbar(
            message = text,
            actionLabel = snackbarActionText
        )
        if (result == SnackbarResult.ActionPerformed) {
            message.action?.let(onMessageAction)
        }
        onMessageShown(message.id)
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomRowWithFiveCells(
                selectedIndex = 0,
                onAddClick = onAddNoteClick,
                onCellClick = onOtherCellClick
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            HomeHeader(totalCoins = totalCoins)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text(stringResource(R.string.search_label)) },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f)
                )
                UserMenu(
                    userEmail = userEmail,
                    onSwitchUser = onSwitchUser,
                    onOpenSettings = onOpenSettings
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (uiState) {
                    is NotesUiState.Error -> item {
                        NotesStatusMessage(
                            message = uiState.message.asString(),
                            actionText = stringResource(R.string.action_retry),
                            onAction = onRetryNotes
                        )
                    }

                    NotesUiState.Loading -> item {
                        LoadingMessage()
                    }

                    NotesUiState.Empty -> item {
                        NotesStatusMessage(message = stringResource(R.string.notes_empty))
                    }

                    is NotesUiState.Content -> {
                        val query = search.trim()
                        val visibleNotes = activeNotes.filter { note ->
                            query.isBlank() ||
                                note.title.contains(query, ignoreCase = true) ||
                                note.content.contains(query, ignoreCase = true)
                        }
                        if (visibleNotes.isEmpty()) {
                            item {
                                NotesStatusMessage(message = stringResource(R.string.notes_empty))
                            }
                        }
                        items(visibleNotes, key = { it.id }) { note ->
                            NoteItem(
                                note = note,
                                onClick = { onNoteClick(note.id) },
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
}

@Composable
private fun HomeHeader(totalCoins: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(id = R.drawable.background_country_home),
            contentDescription = stringResource(R.string.home_background_description),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Row(
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
                contentDescription = stringResource(R.string.total_coins_description),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = totalCoins.toString(),
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun UserMenu(
    userEmail: String,
    onSwitchUser: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = stringResource(R.string.user_description)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(onClick = {
                    expanded = false
                    onSwitchUser()
                }) {
                    Text(stringResource(R.string.switch_user))
                }
                DropdownMenuItem(onClick = {
                    expanded = false
                    onOpenSettings()
                }) {
                    Text(stringResource(R.string.action_settings))
                }
                if (userEmail.isNotBlank()) {
                    DropdownMenuItem(onClick = { expanded = false }) {
                        Text(userEmail)
                    }
                }
            }
        }
    }
}

@Composable
fun CompletedNotesScreen(
    uiState: NotesUiState,
    uiMessage: UiMessage?,
    onRetryNotes: () -> Unit,
    onMessageShown: (Long) -> Unit,
    onMessageAction: (UiMessageAction) -> Unit,
    onaddNoteClick: () -> Unit,
    onNoteClick: (noteId: String) -> Unit,
    onCompletionChange: (noteId: String, Boolean) -> Unit,
    onNavigateHome: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val snackbarText = uiMessage?.text?.asString()
    val snackbarActionText = uiMessage?.actionText?.asString()
    val completedNotes = (uiState as? NotesUiState.Content)
        ?.notes
        .orEmpty()
        .filter { it.isCompleted }

    LaunchedEffect(uiMessage?.id) {
        val message = uiMessage ?: return@LaunchedEffect
        val text = snackbarText ?: return@LaunchedEffect
        val result = scaffoldState.snackbarHostState.showSnackbar(
            message = text,
            actionLabel = snackbarActionText
        )
        if (result == SnackbarResult.ActionPerformed) {
            message.action?.let(onMessageAction)
        }
        onMessageShown(message.id)
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomRowWithFiveCells(
                selectedIndex = 2,
                onAddClick = onaddNoteClick,
                onCellClick = { index ->
                    when (index) {
                        0 -> onNavigateHome()
                        2 -> Unit
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Text(
                text = stringResource(R.string.completed_notes_title),
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            when (uiState) {
                is NotesUiState.Error -> NotesStatusMessage(
                    message = uiState.message.asString(),
                    actionText = stringResource(R.string.action_retry),
                    onAction = onRetryNotes
                )

                NotesUiState.Loading -> LoadingMessage()

                NotesUiState.Empty -> EmptyCompletedNotesMessage()

                is NotesUiState.Content -> {
                    if (completedNotes.isEmpty()) {
                        EmptyCompletedNotesMessage()
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(completedNotes, key = { it.id }) { note ->
                                NoteItem(
                                    note = note,
                                    onClick = { onNoteClick(note.id) },
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
    }
}

@Composable
private fun EmptyCompletedNotesMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(stringResource(R.string.completed_notes_empty))
    }
}

@Composable
private fun LoadingMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun NotesStatusMessage(
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.body1
        )
        if (actionText != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(actionText)
            }
        }
    }
}

@Composable
fun NoteItem(
    note: Note,
    onClick: () -> Unit,
    onCompletionChange: (Boolean) -> Unit
) {
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
                    horizontalArrangement = Arrangement.End
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
                    text = stringResource(R.string.note_deadline_value, formatDeadline(millis)),
                    style = MaterialTheme.typography.caption
                )
            }
            if (note.isRepeating) {
                Text(
                    text = stringResource(R.string.note_repeating),
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}

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
            ) {
                when (index) {
                    0 -> Icon(
                        imageVector = Icons.Default.Task,
                        contentDescription = stringResource(R.string.active_tasks),
                        tint = if (isSelected) MaterialTheme.colors.secondary else MaterialTheme.colors.onPrimary
                    )

                    1 -> Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.action_add),
                        tint = MaterialTheme.colors.onPrimary
                    )

                    2 -> Icon(
                        imageVector = Icons.Default.QuestionMark,
                        contentDescription = stringResource(R.string.home_info_title),
                        tint = MaterialTheme.colors.onPrimary
                    )

                    3 -> Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = stringResource(R.string.completed_notes_title),
                        tint = if (isSelected) MaterialTheme.colors.secondary else MaterialTheme.colors.onPrimary
                    )

                    4 -> Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.action_settings),
                        tint = MaterialTheme.colors.onPrimary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    val sampleNotes = listOf(
        Note(id = "1", title = "Note 1", content = "Note content"),
        Note(id = "2", title = "Note 2", content = "Note content", category = NoteCategory.SHOPPING),
        Note(id = "3", title = "Note 3", content = "Note content", category = NoteCategory.TASKS)
    )
    HomeScreen(
        uiState = NotesUiState.Content(sampleNotes, totalCoins = 12),
        uiMessage = null,
        userEmail = "user@example.com",
        onRetryNotes = {},
        onMessageShown = {},
        onMessageAction = {},
        onAddNoteClick = {},
        onNoteClick = {},
        onOtherCellClick = {},
        onCompletionChange = { _, _ -> },
        onSwitchUser = {},
        onOpenSettings = {}
    )
}
