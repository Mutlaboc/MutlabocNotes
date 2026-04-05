package com.example.mutlabocsnotes

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// Interface that defines a contract for this module.
sealed interface SessionEvent {
    // Data model shared between layers of this module.
    data object SessionExpired : SessionEvent
}

// Singleton event bus for cross-layer session events.
object SessionEventBus {
    private val _events = MutableSharedFlow<SessionEvent>(
        extraBufferCapacity = 1
    )

    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    // Publishes an event to interested subscribers.
    fun emit(event: SessionEvent) {
        _events.tryEmit(event)
    }
}