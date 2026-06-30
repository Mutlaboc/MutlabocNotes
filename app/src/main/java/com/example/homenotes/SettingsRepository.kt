package com.example.homenotes

import android.content.Context
import androidx.annotation.StringRes
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class AppLanguage(
    val code: String,
    @StringRes val displayNameResId: Int
) {
    RU("ru", R.string.language_ru),
    EN("en", R.string.language_en);

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull { it.code == code } ?: RU
        }
    }
}

data class UserPreferences(
    val isDarkTheme: Boolean = false,
    val language: AppLanguage = AppLanguage.RU
)

interface SettingsRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setDarkTheme(isDarkTheme: Boolean)
    suspend fun setLanguage(language: AppLanguage)
}

private val Context.userPreferencesDataStore by preferencesDataStore(
    name = DataStoreSettingsRepository.DATA_STORE_NAME
)

class DataStoreSettingsRepository internal constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    constructor(context: Context) : this(context.userPreferencesDataStore)

    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            UserPreferences(
                isDarkTheme = preferences[Keys.DarkTheme] ?: false,
                language = AppLanguage.fromCode(preferences[Keys.LanguageCode])
            )
        }

    override suspend fun setDarkTheme(isDarkTheme: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.DarkTheme] = isDarkTheme
        }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[Keys.LanguageCode] = language.code
        }
    }

    internal companion object {
        const val DATA_STORE_NAME = "user_preferences"
    }

    private object Keys {
        val DarkTheme = booleanPreferencesKey("dark_theme")
        val LanguageCode = stringPreferencesKey("language_code")
    }
}
