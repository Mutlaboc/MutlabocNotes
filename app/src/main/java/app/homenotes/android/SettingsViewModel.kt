package app.homenotes.android

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences()
)

interface LocaleApplier {
    fun applyLanguage(language: AppLanguage)
}

object AppCompatLocaleApplier : LocaleApplier {
    override fun applyLanguage(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.code)
        )
    }
}

class SettingsViewModel(
    application: Application,
    private val repository: SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val localeApplier: LocaleApplier = AppCompatLocaleApplier
) : AndroidViewModel(application) {

    var uiState by mutableStateOf(SettingsUiState())
        private set

    init {
        viewModelScope.launch(ioDispatcher) {
            repository.preferences.collect { preferences ->
                launch(Dispatchers.Main) {
                    uiState = SettingsUiState(preferences)
                    localeApplier.applyLanguage(preferences.language)
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
            launch(Dispatchers.Main) {
                localeApplier.applyLanguage(language)
            }
        }
    }
}
