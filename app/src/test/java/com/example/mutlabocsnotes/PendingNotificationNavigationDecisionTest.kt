package com.example.mutlabocsnotes

import org.junit.Assert.assertEquals
import org.junit.Test

class PendingNotificationNavigationDecisionTest {

    @Test
    fun authenticatedLoadedTargetNoteOpensEditRoute() {
        val decision = pendingNotificationNavigationDecision(
            authState = AuthState.Authenticated("user@example.com"),
            notesUiState = NotesUiState.Content(listOf(Note(id = "target")), totalCoins = 0),
            pendingNotificationNoteId = "target",
            isWaitingForNotificationNotes = true
        )

        assertEquals(PendingNotificationNavigationDecision.OpenNote("target"), decision)
    }

    @Test
    fun loadingOrUnauthenticatedStateWaits() {
        assertEquals(
            PendingNotificationNavigationDecision.Wait,
            pendingNotificationNavigationDecision(
                authState = AuthState.Authenticated("user@example.com"),
                notesUiState = NotesUiState.Loading,
                pendingNotificationNoteId = "target",
                isWaitingForNotificationNotes = true
            )
        )
        assertEquals(
            PendingNotificationNavigationDecision.Wait,
            pendingNotificationNavigationDecision(
                authState = AuthState.Unauthenticated,
                notesUiState = NotesUiState.Content(listOf(Note(id = "target")), totalCoins = 0),
                pendingNotificationNoteId = "target",
                isWaitingForNotificationNotes = false
            )
        )
    }

    @Test
    fun deletedOrEmptyTargetClearsPendingId() {
        assertEquals(
            PendingNotificationNavigationDecision.ClearPending("deleted"),
            pendingNotificationNavigationDecision(
                authState = AuthState.Authenticated("user@example.com"),
                notesUiState = NotesUiState.Content(listOf(Note(id = "other")), totalCoins = 0),
                pendingNotificationNoteId = "deleted",
                isWaitingForNotificationNotes = true
            )
        )
        assertEquals(
            PendingNotificationNavigationDecision.ClearPending("deleted"),
            pendingNotificationNavigationDecision(
                authState = AuthState.Authenticated("user@example.com"),
                notesUiState = NotesUiState.Empty,
                pendingNotificationNoteId = "deleted",
                isWaitingForNotificationNotes = true
            )
        )
    }
}
