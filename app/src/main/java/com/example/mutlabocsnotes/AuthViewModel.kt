package com.example.mutlabocsnotes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// Хранит UI-состояние и обрабатывает действия пользователя.
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application)

    var uiState by mutableStateOf(AuthUiState())
        private set

    init {
        checkExistingSession()
    }

    // Проверяет предусловия перед продолжением сценария.
    fun checkExistingSession() {
        viewModelScope.launch {
            uiState = uiState.copy(
                isCheckingSession = true,
                errorMessage = null
            )

            repository.restoreSession()
                .onSuccess { session ->
                    uiState = uiState.copy(
                        isCheckingSession = false,
                        isAuthenticated = true,
                        currentEmail = session.email,
                        errorMessage = null
                    )
                }
                .onFailure {
                    uiState = AuthUiState(
                        isCheckingSession = false,
                        isAuthenticated = false
                    )
                }
        }
    }

    // Выполняет аутентификацию пользователя и обновляет локальное состояние авторизации.
    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.length < 6) {
            uiState = uiState.copy(
                errorMessage = "Введите корректный e-mail и пароль не короче 6 символов"
            )
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)

            repository.login(email, password)
                .onSuccess { session ->
                    uiState = uiState.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        currentEmail = session.email,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        errorMessage = error.message ?: "Ошибка входа"
                    )
                }
        }
    }

    // Регистрирует пользователя и обновляет локальное состояние авторизации.
    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.length < 6) {
            uiState = uiState.copy(
                errorMessage = "Введите корректный e-mail и пароль не короче 6 символов"
            )
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)

            repository.register(email, password)
                .onSuccess { session ->
                    uiState = uiState.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        currentEmail = session.email,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        errorMessage = error.message ?: "Ошибка регистрации"
                    )
                }
        }
    }

    // Завершает текущую сессию и очищает данные авторизации.
    fun logout() {
        repository.logout()
        uiState = AuthUiState(
            isCheckingSession = false,
            isAuthenticated = false
        )
    }

    // Обрабатывает входящее событие и соответствующим образом обновляет состояние.
    fun handleSessionExpired() {
        repository.logout()
        uiState = AuthUiState(
            isCheckingSession = false,
            isAuthenticated = false,
            errorMessage = "Сессия истекла. Войдите снова."
        )
    }

    // Очищает временные и сохранённые данные состояния.
    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
    }
}
