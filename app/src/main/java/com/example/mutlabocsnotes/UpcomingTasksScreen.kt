package com.example.mutlabocsnotes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun UpcomingTasksScreen(
    uiState: NotesUiState,
    onRetryNotes: () -> Unit,
    onAddNoteClick: () -> Unit,
    onNoteClick: (String) -> Unit,
    onCompletedNotesClick: () -> Unit
) {
    val notes = (uiState as? NotesUiState.Content)?.notes.orEmpty()
    val nowMillis = rememberTaskClock(notes)
    val upcoming = upcomingNotes(notes, nowMillis)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomBar(
                selectedAction = BottomBarAction.UpcomingTasks,
                onCompletedNotesClick = onCompletedNotesClick,
                onAddClick = onAddNoteClick,
                onHomeInfoClick = {},
                onUpcomingClick = {}
            )
        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues).fillMaxSize()) {
            Text(
                text = stringResource(R.string.upcoming_tasks_title),
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            when (uiState) {
                is NotesUiState.Error -> NotesStatusMessage(
                    message = uiState.message.asString(),
                    actionText = stringResource(R.string.action_retry),
                    onAction = onRetryNotes
                )
                NotesUiState.Loading -> NotesLoadingMessage()
                NotesUiState.Empty -> UpcomingEmptyMessage()
                is NotesUiState.Content -> if (upcoming.isEmpty()) {
                    UpcomingEmptyMessage()
                } else {
                    LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                        items(upcoming, key = { it.id }) { note ->
                            NoteItem(
                                note = note,
                                onClick = { onNoteClick(note.id) },
                                onCompletionChange = {},
                                showCompletion = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingEmptyMessage() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(stringResource(R.string.upcoming_tasks_empty))
    }
}
