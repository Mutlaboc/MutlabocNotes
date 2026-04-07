package com.example.mutlabocsnotes

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// Интерфейс, задающий контракт этого модуля.
sealed interface SessionEvent {
    // Модель данных, общая для слоёв этого модуля.
    data object SessionExpired : SessionEvent
}

// Event bus-одиночка для межслойных событий сессии.
object SessionEventBus {
    private val _events = MutableSharedFlow<SessionEvent>(
        extraBufferCapacity = 1
    )

    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    // Публикует событие для заинтересованных подписчиков.
    fun emit(event: SessionEvent) {
        _events.tryEmit(event)
    }
}
