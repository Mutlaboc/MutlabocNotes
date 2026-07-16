package com.example.mutlabocsnotes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.delay

internal fun homeNotes(notes: List<Note>, nowMillis: Long): List<Note> = notes.filter { note ->
    !note.isCompleted && (
        note.category != NoteCategory.RECURRING_TASKS ||
            (note.startAtMillis ?: Long.MIN_VALUE) <= nowMillis
        )
}

internal fun upcomingNotes(notes: List<Note>, nowMillis: Long): List<Note> = notes
    .filter { note ->
        !note.isCompleted && note.category == NoteCategory.RECURRING_TASKS &&
            (note.startAtMillis ?: Long.MIN_VALUE) > nowMillis
    }
    .sortedBy { it.startAtMillis }

@Composable
internal fun rememberTaskClock(notes: List<Note>): Long {
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) nowMillis = System.currentTimeMillis()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val nextStart = notes.asSequence()
        .filter { !it.isCompleted && it.category == NoteCategory.RECURRING_TASKS }
        .mapNotNull { it.startAtMillis }
        .filter { it > nowMillis }
        .minOrNull()
    LaunchedEffect(nextStart, notes) {
        if (nextStart != null) {
            delay((nextStart - System.currentTimeMillis()).coerceAtLeast(1L))
            nowMillis = System.currentTimeMillis()
        }
    }
    return nowMillis
}
