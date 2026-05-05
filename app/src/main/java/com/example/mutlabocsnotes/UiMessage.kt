package com.example.mutlabocsnotes

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.util.concurrent.atomic.AtomicLong

sealed interface UiText {
    data class StringResource(
        @StringRes val resId: Int,
        val args: List<Any> = emptyList()
    ) : UiText
}

data class UiMessage(
    val id: Long,
    val text: UiText
)

object UiMessageId {
    private val nextId = AtomicLong(0L)

    fun next(): Long = nextId.incrementAndGet()
}

@Composable
fun UiText.asString(): String {
    return when (this) {
        is UiText.StringResource -> stringResource(resId, *args.toTypedArray())
    }
}
