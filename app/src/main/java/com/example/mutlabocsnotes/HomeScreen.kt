package com.example.mutlabocsnotes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

const val HOME_SEARCH_FIELD_TEST_TAG = "home_search_field"
const val BOTTOM_BAR_TEST_TAG = "bottom_bar"
const val USER_MENU_BUTTON_TEST_TAG = "user_menu_button"
const val USER_MENU_SETTINGS_ITEM_TEST_TAG = "user_menu_settings_item"

fun noteItemTestTag(noteId: String): String = "note_item_$noteId"

enum class BottomBarAction {
    CompletedNotes,
    AddNote,
    HomeInfo
}

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
    onCompletedNotesClick: () -> Unit,
    onHomeInfoClick: () -> Unit,
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
            BottomBar(
                selectedAction = null,
                onCompletedNotesClick = onCompletedNotesClick,
                onAddClick = onAddNoteClick,
                onHomeInfoClick = onHomeInfoClick
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
                        .testTag(HOME_SEARCH_FIELD_TEST_TAG)
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
                        NotesLoadingMessage()
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
                                modifier = Modifier.testTag(noteItemTestTag(note.id)),
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
        onCompletedNotesClick = {},
        onHomeInfoClick = {},
        onCompletionChange = { _, _ -> },
        onSwitchUser = {},
        onOpenSettings = {}
    )
}
