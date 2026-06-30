package com.example.homenotes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    onNavigateHome: () -> Unit,
    onHomeInfoClick: () -> Unit
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
        backgroundColor = CozyAuth.Cream,
        bottomBar = {
            BottomBar(
                selectedAction = BottomBarAction.CompletedNotes,
                onCompletedNotesClick = onNavigateHome,
                onAddClick = onaddNoteClick,
                onHomeInfoClick = onHomeInfoClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CozyAuth.Cream)
                .pixelScreenFrame()
                .padding(paddingValues)
        ) {
            Text(
                text = stringResource(R.string.completed_notes_title),
                color = CozyAuth.Ink,
                fontFamily = CozyAuth.PixelFont,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            when (uiState) {
                is NotesUiState.Error -> NotesStatusMessage(
                    message = uiState.message.asString(),
                    actionText = stringResource(R.string.action_retry),
                    onAction = onRetryNotes
                )

                NotesUiState.Loading -> NotesLoadingMessage()

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
        Text(
            text = stringResource(R.string.completed_notes_empty),
            color = CozyAuth.InkSoft,
            fontFamily = CozyAuth.PixelFont,
            fontSize = 15.sp
        )
    }
}
