package com.example.mutlabocsnotes

import android.app.Application
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeSettingsRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        repository = FakeSettingsRepository()
        viewModel = SettingsViewModel(
            application = Application(),
            repository = repository,
            ioDispatcher = mainDispatcherRule.dispatcher
        )
    }

    @Test
    fun initialPreferencesAreCollectedIntoUiState() = runTest(mainDispatcherRule.dispatcher) {
        repository.preferencesState.value = UserPreferences(
            isDarkTheme = true,
            language = AppLanguage.EN
        )

        advanceUntilIdle()

        assertEquals(
            UserPreferences(isDarkTheme = true, language = AppLanguage.EN),
            viewModel.uiState.preferences
        )
    }

    @Test
    fun setDarkThemeDelegatesToRepositoryAndUpdatesState() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.setDarkTheme(true)
        advanceUntilIdle()

        assertEquals(listOf(true), repository.darkThemeCalls)
        assertEquals(true, viewModel.uiState.preferences.isDarkTheme)
    }

    @Test
    fun setLanguageDelegatesToRepositoryAndUpdatesState() = runTest(mainDispatcherRule.dispatcher) {
        viewModel.setLanguage(AppLanguage.DE)
        advanceUntilIdle()

        assertEquals(listOf(AppLanguage.DE), repository.languageCalls)
        assertEquals(AppLanguage.DE, viewModel.uiState.preferences.language)
    }
}

private class FakeSettingsRepository : SettingsRepository {
    val preferencesState = MutableStateFlow(UserPreferences())
    val darkThemeCalls = mutableListOf<Boolean>()
    val languageCalls = mutableListOf<AppLanguage>()

    override val preferences: Flow<UserPreferences> = preferencesState

    override suspend fun setDarkTheme(isDarkTheme: Boolean) {
        darkThemeCalls.add(isDarkTheme)
        preferencesState.value = preferencesState.value.copy(isDarkTheme = isDarkTheme)
    }

    override suspend fun setLanguage(language: AppLanguage) {
        languageCalls.add(language)
        preferencesState.value = preferencesState.value.copy(language = language)
    }
}
