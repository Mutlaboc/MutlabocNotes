package com.example.homenotes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * Хранит прогресс первичного обучения текущего аккаунта и отдаёт его экранам.
 * Прогресс привязан к пользователю, поэтому каждый новый аккаунт проходит обучение заново.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModel(
    application: Application,
    private val repository: OnboardingRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    // Пока аккаунт не определён, считаем обучение пройденным, чтобы ничего не мелькало.
    var uiState by mutableStateOf(OnboardingState.Completed)
        private set

    private val userKey = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch(ioDispatcher) {
            userKey
                .filterNotNull()
                .distinctUntilChanged()
                .flatMapLatest { key -> repository.state(key) }
                .collect { state ->
                    launch(Dispatchers.Main) {
                        uiState = state
                    }
                }
        }
    }

    /** Переключает прогресс на указанный аккаунт. null/пусто — обучение скрыто. */
    fun setUser(key: String?) {
        val normalized = key?.trim()?.takeIf { it.isNotEmpty() }
        if (normalized == null) {
            uiState = OnboardingState.Completed
        }
        userKey.value = normalized
    }

    fun markWelcomeSeen() {
        val key = userKey.value ?: return
        viewModelScope.launch(ioDispatcher) {
            repository.markWelcomeSeen(key)
        }
    }

    fun markHintSeen(step: OnboardingHintStep) {
        val key = userKey.value ?: return
        viewModelScope.launch(ioDispatcher) {
            repository.markHintSeen(key, step)
        }
    }
}
