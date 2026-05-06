package com.example.mutlabocsnotes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences()
)

class SettingsViewModel(
    application: Application,
    private val repository: SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    var uiState by mutableStateOf(SettingsUiState())
        private set

    init {
        viewModelScope.launch(ioDispatcher) {
            repository.preferences.collect { preferences ->
                launch(Dispatchers.Main) {
                    uiState = SettingsUiState(preferences)
                }
            }
        }
    }

    fun setDarkTheme(isDarkTheme: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            repository.setDarkTheme(isDarkTheme)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch(ioDispatcher) {
            repository.setLanguage(language)
        }
    }
}
