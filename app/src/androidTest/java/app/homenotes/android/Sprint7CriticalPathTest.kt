package app.homenotes.android

import android.app.Application
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class Sprint7CriticalPathTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun authErrorMessage_rendersValidationError() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        composeRule.setContent {
            MaterialTheme {
                AuthScreen(
                    uiState = AuthUiState(
                        authState = AuthState.Unauthenticated,
                        inlineErrorMessage = UiText.StringResource(R.string.auth_error_invalid_credentials)
                    ),
                    onSignIn = { _, _ -> },
                    onSignUp = { _, _ -> },
                    onGoogleIdToken = {},
                    onYandexAccessToken = {},
                    onGoogleTokenEmpty = {},
                    onGoogleSignInFailed = {},
                    onYandexTokenEmpty = {},
                    onYandexSignInFailed = {},
                    onYandexSignInCancelled = {},
                    onMessageShown = {},
                    onClearInlineError = {}
                )
            }
        }

        composeRule.onNodeWithTag(AUTH_ERROR_MESSAGE_TEST_TAG)
            .assertIsDisplayed()
            .assertTextEquals(context.getString(R.string.auth_error_invalid_credentials))
    }

    @Test
    fun invalidLogin_keepsUserOnAuthAndDoesNotCallRepository() {
        val harness = launchApp()

        waitForTag(AUTH_SIGN_IN_BUTTON_TEST_TAG)
        composeRule.onNodeWithTag(AUTH_EMAIL_FIELD_TEST_TAG).performTextInput("user@example.com")
        composeRule.onNodeWithTag(AUTH_PASSWORD_FIELD_TEST_TAG).performTextInput("short")
        composeRule.onNodeWithTag(AUTH_SIGN_IN_BUTTON_TEST_TAG).performClick()

        composeRule.onNodeWithTag(AUTH_SIGN_IN_BUTTON_TEST_TAG).assertIsDisplayed()
        composeRule.onAllNodesWithTag(HOME_SEARCH_FIELD_TEST_TAG).assertCountEquals(0)
        composeRule.runOnIdle {
            assertEquals(0, harness.authRepository.loginCalls)
        }
    }

    @Test
    fun successfulLogin_navigatesHomeAndLoadsSeededNotes() {
        launchApp()

        signIn()

        waitForTag(noteItemTestTag(PASSPORT_NOTE_ID))
        composeRule.onNodeWithTag(noteItemTestTag(PASSPORT_NOTE_ID)).assertIsDisplayed()
        composeRule.onNodeWithTag(noteItemTestTag(GROCERIES_NOTE_ID)).assertIsDisplayed()
    }

    @Test
    fun homeSearch_filtersVisibleNotesByTitle() {
        launchApp()

        signIn()
        waitForTag(noteItemTestTag(PASSPORT_NOTE_ID))
        composeRule.onNodeWithTag(HOME_SEARCH_FIELD_TEST_TAG).performTextInput("passport")

        composeRule.onNodeWithTag(noteItemTestTag(PASSPORT_NOTE_ID)).assertIsDisplayed()
        composeRule.onAllNodesWithTag(noteItemTestTag(GROCERIES_NOTE_ID)).assertCountEquals(0)
    }

    @Test
    fun settingsThemeToggle_persistsInRepositoryState() {
        val harness = launchApp()

        signIn()
        openSettings()

        composeRule.onNodeWithTag(SETTINGS_THEME_SWITCH_TEST_TAG).assertIsOff()
        composeRule.onNodeWithTag(SETTINGS_THEME_SWITCH_TEST_TAG).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            harness.settingsRepository.preferencesState.value.isDarkTheme
        }

        composeRule.onNodeWithTag(SETTINGS_THEME_SWITCH_TEST_TAG).assertIsOn()
        assertEquals(true, harness.settingsRepository.preferencesState.value.isDarkTheme)
    }

    @Test
    fun logout_returnsUserToAuthScreen() {
        launchApp()

        signIn()
        openSettings()
        composeRule.onNodeWithTag(SETTINGS_LOGOUT_BUTTON_TEST_TAG).performClick()

        waitForTag(AUTH_SIGN_IN_BUTTON_TEST_TAG)
        composeRule.onNodeWithTag(AUTH_SIGN_IN_BUTTON_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun smokePath_loginHomeSettingsLogout_passes() {
        launchApp()

        signIn()
        waitForTag(noteItemTestTag(PASSPORT_NOTE_ID))
        openSettings()
        composeRule.onNodeWithTag(SETTINGS_THEME_SWITCH_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SETTINGS_LOGOUT_BUTTON_TEST_TAG).performClick()

        waitForTag(AUTH_SIGN_IN_BUTTON_TEST_TAG)
        composeRule.onNodeWithTag(AUTH_SIGN_IN_BUTTON_TEST_TAG).assertIsDisplayed()
    }

    private fun launchApp(): TestHarness {
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext
            .applicationContext as Application
        val authRepository = FakeAuthSessionRepository()
        val notesRepository = FakeNotesDataSource()
        val settingsRepository = FakeSettingsRepository()
        val authViewModel = AuthViewModel(
            application = application,
            repository = authRepository,
            autoRestore = true
        )
        val notesViewModel = NotesViewModel(
            application = application,
            repository = notesRepository,
            notificationScheduler = NoOpDeadlineScheduler
        )
        val homeInfoViewModel = HomeInfoViewModel(
            application = application,
            repository = FakeHomeInfoDataSource()
        )
        val settingsViewModel = SettingsViewModel(
            application = application,
            repository = settingsRepository,
            localeApplier = NoOpLocaleApplier
        )

        composeRule.setContent {
            MyApp(
                viewModelFactory = ThrowingViewModelFactory,
                authViewModel = authViewModel,
                notesViewModel = notesViewModel,
                homeInfoViewModel = homeInfoViewModel,
                settingsViewModel = settingsViewModel
            )
        }

        return TestHarness(
            authRepository = authRepository,
            settingsRepository = settingsRepository
        )
    }

    private fun signIn() {
        waitForTag(AUTH_SIGN_IN_BUTTON_TEST_TAG)
        composeRule.onNodeWithTag(AUTH_EMAIL_FIELD_TEST_TAG).performTextInput("user@example.com")
        composeRule.onNodeWithTag(AUTH_PASSWORD_FIELD_TEST_TAG).performTextInput("password123")
        composeRule.onNodeWithTag(AUTH_SIGN_IN_BUTTON_TEST_TAG).performClick()
        waitForTag(HOME_SEARCH_FIELD_TEST_TAG)
    }

    private fun openSettings() {
        waitForTag(USER_MENU_BUTTON_TEST_TAG)
        composeRule.onNodeWithTag(USER_MENU_BUTTON_TEST_TAG).performClick()
        waitForTag(USER_MENU_SETTINGS_ITEM_TEST_TAG)
        composeRule.onNodeWithTag(USER_MENU_SETTINGS_ITEM_TEST_TAG, useUnmergedTree = true).performClick()
        waitForTag(SETTINGS_LOGOUT_BUTTON_TEST_TAG)
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private data class TestHarness(
        val authRepository: FakeAuthSessionRepository,
        val settingsRepository: FakeSettingsRepository
    )

    private class FakeAuthSessionRepository : AuthSessionRepository {
        var loginCalls = 0

        override suspend fun login(email: String, password: String): Result<AuthorizedSession> {
            loginCalls += 1
            return Result.success(AuthorizedSession(email))
        }

        override suspend fun register(email: String, password: String): Result<AuthorizedSession> {
            return Result.success(AuthorizedSession(email))
        }

        override suspend fun loginWithGoogle(idToken: String): Result<AuthorizedSession> {
            return Result.success(AuthorizedSession("google@example.com"))
        }

        override suspend fun loginWithYandex(accessToken: String): Result<AuthorizedSession> {
            return Result.success(AuthorizedSession("yandex@example.com"))
        }

        override suspend fun restoreSession(): Result<AuthorizedSession> {
            return Result.failure(IllegalStateException("No saved test session"))
        }

        override suspend fun logout() = Unit

        override fun clearLocalSession() = Unit
    }

    private class FakeNotesDataSource : NotesDataSource {
        private val notes = listOf(
            Note(
                id = PASSPORT_NOTE_ID,
                title = "Passport renewal",
                content = "Collect documents"
            ),
            Note(
                id = GROCERIES_NOTE_ID,
                title = "Groceries",
                content = "Milk"
            )
        )

        override suspend fun getAllNotes(): Result<List<Note>> {
            return Result.success(notes)
        }

        override suspend fun insert(note: Note): Result<String> {
            return Result.success(note.id.ifBlank { "created-note" })
        }

        override suspend fun update(note: Note): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun updateCompletion(noteId: String, isCompleted: Boolean): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun delete(noteId: String): Result<Unit> {
            return Result.success(Unit)
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        val preferencesState = MutableStateFlow(UserPreferences())

        override val preferences: Flow<UserPreferences> = preferencesState

        override suspend fun setDarkTheme(isDarkTheme: Boolean) {
            preferencesState.value = preferencesState.value.copy(isDarkTheme = isDarkTheme)
        }

        override suspend fun setLanguage(language: AppLanguage) {
            preferencesState.value = preferencesState.value.copy(language = language)
        }
    }

    private class FakeHomeInfoDataSource : HomeInfoDataSource {
        override suspend fun getAllCards(): Result<List<HomeInfoCard>> {
            return Result.success(emptyList())
        }

        override suspend fun insert(card: HomeInfoCard): Result<HomeInfoCard> {
            return Result.success(card.copy(id = card.id.ifBlank { "created-card" }))
        }

        override suspend fun update(card: HomeInfoCard): Result<HomeInfoCard> {
            return Result.success(card)
        }

        override suspend fun delete(cardId: String): Result<Unit> {
            return Result.success(Unit)
        }
    }

    private object NoOpDeadlineScheduler : DeadlineScheduler {
        override fun schedule(note: Note): DeadlineScheduleResult {
            return DeadlineScheduleResult.NotScheduled
        }

        override fun cancel(noteId: String) = Unit

        override fun scheduleAll(notes: List<Note>): DeadlineScheduleResult {
            return DeadlineScheduleResult.NotScheduled
        }

        override fun cancelAll() = Unit
    }

    private object NoOpLocaleApplier : LocaleApplier {
        override fun applyLanguage(language: AppLanguage) = Unit
    }

    private object ThrowingViewModelFactory : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            throw AssertionError("Sprint 7 tests inject ViewModels directly")
        }
    }

    private companion object {
        const val PASSPORT_NOTE_ID = "passport"
        const val GROCERIES_NOTE_ID = "groceries"
    }
}
