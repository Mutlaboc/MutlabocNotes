package com.example.mutlabocsnotes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun BottomBar(
    selectedAction: BottomBarAction?,
    onCompletedNotesClick: () -> Unit,
    onAddClick: () -> Unit,
    onHomeInfoClick: () -> Unit
) {
    val actions = listOf(
        BottomBarAction.CompletedNotes,
        BottomBarAction.AddNote,
        BottomBarAction.HomeInfo
    )
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(CozyAuth.BrownOutline.copy(alpha = 0.35f))
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(CozyAuth.CardCream)
                .testTag(BOTTOM_BAR_TEST_TAG),
            horizontalArrangement = Arrangement.Center,
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
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    BottomBarIcon(action = action, isSelected = isSelected)
                }
            }
        }
    }
}

@Composable
private fun BottomBarIcon(
    action: BottomBarAction,
    isSelected: Boolean
) {
    val tint: Color = if (isSelected) CozyAuth.Terracotta else CozyAuth.InkSoft
    when (action) {
        BottomBarAction.CompletedNotes -> Icon(
            imageVector = if (isSelected) Icons.Default.AddTask else Icons.Default.DoneAll,
            contentDescription = if (isSelected) {
                stringResource(R.string.notes_title)
            } else {
                stringResource(R.string.completed_notes_title)
            },
            tint = tint
        )

        BottomBarAction.AddNote -> Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(R.string.action_add),
            tint = tint
        )

        BottomBarAction.HomeInfo -> Icon(
            imageVector = Icons.Default.QuestionMark,
            contentDescription = stringResource(R.string.home_info_title),
            tint = tint
        )
    }
}
