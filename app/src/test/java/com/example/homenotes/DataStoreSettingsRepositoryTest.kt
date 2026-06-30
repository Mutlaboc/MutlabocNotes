package com.example.homenotes

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStoreSettingsRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun defaultPreferencesAreLightThemeAndRussianLanguage() = runTest {
        val file = preferenceFile()
        val store = repository(file)

        val preferences = store.repository.preferences.first()

        assertFalse(preferences.isDarkTheme)
        assertEquals(AppLanguage.RU, preferences.language)

        store.scope.cancel()
    }

    @Test
    fun darkThemePreferenceSurvivesRepositoryRecreation() = runTest {
        val file = preferenceFile()
        var store = repository(file)

        store.repository.setDarkTheme(true)
        assertTrue(store.repository.preferences.first().isDarkTheme)
        store.scope.cancel()

        store = repository(file)
        assertTrue(store.repository.preferences.first().isDarkTheme)

        store.scope.cancel()
    }

    @Test
    fun languagePreferenceSurvivesRepositoryRecreation() = runTest {
        val file = preferenceFile()
        var store = repository(file)

        store.repository.setLanguage(AppLanguage.EN)
        assertEquals(AppLanguage.EN, store.repository.preferences.first().language)
        store.scope.cancel()

        store = repository(file)
        assertEquals(AppLanguage.EN, store.repository.preferences.first().language)

        store.scope.cancel()
    }

    private fun preferenceFile(): File {
        return File(temporaryFolder.root, "user_preferences.preferences_pb")
    }

    private fun repository(file: File): TestSettingsStore {
        val scope = CoroutineScope(UnconfinedTestDispatcher() + SupervisorJob())
        val dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            file
        }
        return TestSettingsStore(
            repository = DataStoreSettingsRepository(dataStore),
            scope = scope
        )
    }

    private data class TestSettingsStore(
        val repository: DataStoreSettingsRepository,
        val scope: CoroutineScope
    )
}
