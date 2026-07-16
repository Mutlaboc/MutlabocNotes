package com.example.mutlabocsnotes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun BottomBar(
    selectedAction: BottomBarAction?,
    onCompletedNotesClick: () -> Unit,
    onAddClick: () -> Unit,
    onHomeInfoClick: () -> Unit,
    onUpcomingClick: (() -> Unit)? = null
) {
    val actions = listOf(
        BottomBarAction.CompletedNotes,
        BottomBarAction.AddNote,
        if (onUpcomingClick == null) BottomBarAction.HomeInfo else BottomBarAction.UpcomingTasks
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colors.primarySurface)
            .testTag(BOTTOM_BAR_TEST_TAG),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEach { action ->
            val isSelected = action == selectedAction
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable {
                        when (action) {
                            BottomBarAction.CompletedNotes -> onCompletedNotesClick()
                            BottomBarAction.AddNote -> onAddClick()
                            BottomBarAction.HomeInfo -> onHomeInfoClick()
                            BottomBarAction.UpcomingTasks -> onUpcomingClick?.invoke()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                BottomBarIcon(action = action, isSelected = isSelected)
            }
        }
    }
}

@Composable
private fun BottomBarIcon(
    action: BottomBarAction,
    isSelected: Boolean
) {
    when (action) {
        BottomBarAction.CompletedNotes -> Icon(
            imageVector = if (isSelected) Icons.Default.AddTask else Icons.Default.DoneAll,
            contentDescription = if (isSelected) {
                stringResource(R.string.notes_title)
            } else {
                stringResource(R.string.completed_notes_title)
            },
            tint = MaterialTheme.colors.onPrimary
        )

        BottomBarAction.AddNote -> Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.action_add),
            tint = if (isSelected) MaterialTheme.colors.secondary else MaterialTheme.colors.onPrimary
        )

        BottomBarAction.HomeInfo -> Icon(
            imageVector = Icons.Default.QuestionMark,
            contentDescription = stringResource(R.string.home_info_title),
            tint = if (isSelected) MaterialTheme.colors.secondary else MaterialTheme.colors.onPrimary
        )

        BottomBarAction.UpcomingTasks -> Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = stringResource(R.string.upcoming_tasks_title),
            tint = if (isSelected) MaterialTheme.colors.secondary else MaterialTheme.colors.onPrimary
        )
    }
}
