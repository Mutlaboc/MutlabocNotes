package app.homenotes.android

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class Sprint5UxPolishTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeSearch_filtersVisibleNotesByTitle() {
        composeRule.setContent {
            MaterialTheme {
                HomeScreen(
                    uiState = NotesUiState.Content(
                        notes = listOf(
                            Note(id = "groceries", title = "Groceries", content = "Milk"),
                            Note(id = "passport", title = "Passport renewal", content = "Documents")
                        ),
                        totalCoins = 0
                    ),
                    uiMessage = null,
                    userEmail = "user@example.com",
                    onRetryNotes = {},
                    onMessageShown = {},
                    onMessageAction = {},
                    onAddNoteClick = {},
                    onNoteClick = {},
                    onCompletedNotesClick = {},
                    onHomeInfoClick = {},
                    onCompletionChange = { _, _ -> },
                    onSwitchUser = {},
                    onOpenSettings = {},
                    earnedCoins = 0,
                )
            }
        }

        composeRule.onNodeWithTag(noteItemTestTag("groceries")).assertIsDisplayed()
        composeRule.onNodeWithTag(noteItemTestTag("passport")).assertIsDisplayed()

        composeRule.onNodeWithTag(HOME_SEARCH_FIELD_TEST_TAG).performTextInput("passport")

        composeRule.onNodeWithTag(noteItemTestTag("passport")).assertIsDisplayed()
        composeRule.onAllNodesWithTag(noteItemTestTag("groceries")).assertCountEquals(0)
    }

    @Test
    fun authPasswordField_hidesPasswordByDefaultAndTogglesVisibility() {
        val password = "secret123"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val passwordLabel = context.getString(R.string.auth_password_label)
        val showPassword = context.getString(R.string.auth_show_password)
        val hidePassword = context.getString(R.string.auth_hide_password)

        composeRule.setContent {
            MaterialTheme {
                AuthScreen(
                    uiState = AuthUiState(authState = AuthState.Unauthenticated),
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

        composeRule.onNodeWithText(passwordLabel).performTextInput(password)

        composeRule.onAllNodesWithText(password).assertCountEquals(0)
        composeRule.onNodeWithContentDescription(showPassword).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(showPassword).performClick()

        composeRule.onNodeWithText(password).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(hidePassword).assertIsDisplayed()
    }

    @Test
    fun authLoading_disablesButtons() {
        composeRule.setContent {
            MaterialTheme {
                AuthScreen(
                    uiState = AuthUiState(
                        authState = AuthState.Unauthenticated,
                        isLoading = true
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

        composeRule.onNodeWithTag(AUTH_SIGN_IN_BUTTON_TEST_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(AUTH_SIGN_UP_BUTTON_TEST_TAG).assertIsNotEnabled()
        // Google sign-in button is intentionally hidden while its auth flow remains in code.
        // composeRule.onNodeWithTag(AUTH_GOOGLE_SIGN_IN_BUTTON_TEST_TAG).assertIsNotEnabled()
        composeRule.onNodeWithTag(AUTH_YANDEX_SIGN_IN_BUTTON_TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun authSignIn_trimsEmailBeforeCallback() {
        var submittedEmail: String? = null
        var submittedPassword: String? = null

        composeRule.setContent {
            MaterialTheme {
                AuthScreen(
                    uiState = AuthUiState(authState = AuthState.Unauthenticated),
                    onSignIn = { email, password ->
                        submittedEmail = email
                        submittedPassword = password
                    },
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

        composeRule.onNodeWithTag(AUTH_EMAIL_FIELD_TEST_TAG).performTextInput(" user@example.com ")
        composeRule.onNodeWithTag(AUTH_PASSWORD_FIELD_TEST_TAG).performTextInput("secret123")
        composeRule.onNodeWithTag(AUTH_SIGN_IN_BUTTON_TEST_TAG).performClick()

        composeRule.runOnIdle {
            assertEquals("user@example.com", submittedEmail)
            assertEquals("secret123", submittedPassword)
        }
    }

    @Test
    fun authModeSwitch_togglesBetweenSignInAndSignUp() {
        composeRule.setContent {
            MaterialTheme {
                AuthScreen(
                    uiState = AuthUiState(authState = AuthState.Unauthenticated),
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

        composeRule.onNodeWithTag(AUTH_SIGN_IN_BUTTON_TEST_TAG).assertIsDisplayed()
        composeRule.onAllNodesWithTag(AUTH_CONFIRM_PASSWORD_FIELD_TEST_TAG).assertCountEquals(0)

        composeRule.onNodeWithTag(AUTH_SIGN_UP_BUTTON_TEST_TAG).performClick()

        composeRule.onNodeWithTag(AUTH_CONFIRM_PASSWORD_FIELD_TEST_TAG).assertIsDisplayed()

        composeRule.onNodeWithTag(AUTH_SIGN_IN_BUTTON_TEST_TAG).performClick()

        composeRule.onAllNodesWithTag(AUTH_CONFIRM_PASSWORD_FIELD_TEST_TAG).assertCountEquals(0)
        composeRule.onNodeWithTag(AUTH_SIGN_IN_BUTTON_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun authSignUpMode_mismatchedPasswordsShowsErrorAndDoesNotSubmit() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mismatch = context.getString(R.string.auth_error_passwords_do_not_match)
        var signUpCalls = 0

        composeRule.setContent {
            MaterialTheme {
                AuthScreen(
                    uiState = AuthUiState(authState = AuthState.Unauthenticated),
                    onSignIn = { _, _ -> },
                    onSignUp = { _, _ -> signUpCalls += 1 },
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

        composeRule.onNodeWithTag(AUTH_SIGN_UP_BUTTON_TEST_TAG).performClick()
        composeRule.onNodeWithTag(AUTH_EMAIL_FIELD_TEST_TAG).performTextInput("new@example.com")
        composeRule.onNodeWithTag(AUTH_PASSWORD_FIELD_TEST_TAG).performTextInput("secret123")
        composeRule.onNodeWithTag(AUTH_CONFIRM_PASSWORD_FIELD_TEST_TAG).performTextInput("secret124")
        composeRule.onNodeWithTag(AUTH_SIGN_UP_BUTTON_TEST_TAG).performClick()

        composeRule.onNodeWithText(mismatch).assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(0, signUpCalls)
        }
    }

    @Test
    fun authSignUpMode_matchingPasswordsSubmitsTrimmedEmail() {
        var submittedEmail: String? = null
        var submittedPassword: String? = null

        composeRule.setContent {
            MaterialTheme {
                AuthScreen(
                    uiState = AuthUiState(authState = AuthState.Unauthenticated),
                    onSignIn = { _, _ -> },
                    onSignUp = { email, password ->
                        submittedEmail = email
                        submittedPassword = password
                    },
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

        composeRule.onNodeWithTag(AUTH_SIGN_UP_BUTTON_TEST_TAG).performClick()
        composeRule.onNodeWithTag(AUTH_EMAIL_FIELD_TEST_TAG).performTextInput(" new@example.com ")
        composeRule.onNodeWithTag(AUTH_PASSWORD_FIELD_TEST_TAG).performTextInput("secret123")
        composeRule.onNodeWithTag(AUTH_CONFIRM_PASSWORD_FIELD_TEST_TAG).performTextInput("secret123")
        composeRule.onNodeWithTag(AUTH_SIGN_UP_BUTTON_TEST_TAG).performClick()

        composeRule.runOnIdle {
            assertEquals("new@example.com", submittedEmail)
            assertEquals("secret123", submittedPassword)
        }
    }

    @Test
    fun authSnackbar_rendersUiMessage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val message = context.getString(R.string.api_error_network)

        composeRule.setContent {
            MaterialTheme {
                AuthScreen(
                    uiState = AuthUiState(
                        authState = AuthState.Unauthenticated,
                        uiMessage = UiMessage(
                            id = 100L,
                            text = UiText.StringResource(R.string.api_error_network)
                        )
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

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(message)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun bottomBar_containsOnlyCompletedAddAndHomeInfoActions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val add = context.getString(R.string.bottom_bar_create)
        val homeInfo = context.getString(R.string.bottom_bar_home_info)
        val completedTasks = context.getString(R.string.bottom_bar_completed)
        val settings = context.getString(R.string.action_settings)
        var completedClicks = 0

        composeRule.setContent {
            MaterialTheme {
                BottomBar(
                    selectedAction = null,
                    onCompletedNotesClick = { completedClicks += 1 },
                    onAddClick = {},
                    onHomeInfoClick = {},
                    onNavigateHome = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription(completedTasks).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(add).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(homeInfo).assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription(settings).assertCountEquals(0)

        composeRule.onNodeWithContentDescription(completedTasks).performClick()
        composeRule.runOnIdle {
            assertEquals(1, completedClicks)
        }
    }

    @Test
    fun bottomBar_onCompletedScreen_containsNotesAddAndUpcomingActions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val notes = context.getString(R.string.bottom_bar_notes)
        val completed = context.getString(R.string.bottom_bar_completed)
        val upcoming = context.getString(R.string.bottom_bar_upcoming)
        var notesClicks = 0
        var upcomingClicks = 0

        composeRule.setContent {
            MaterialTheme {
                BottomBar(
                    selectedAction = BottomBarAction.CompletedNotes,
                    onCompletedNotesClick = {},
                    onAddClick = {},
                    onHomeInfoClick = {},
                    onNavigateHome = { notesClicks += 1 },
                    onUpcomingClick = { upcomingClicks += 1 }
                )
            }
        }

        composeRule.onAllNodesWithContentDescription(completed).assertCountEquals(0)
        composeRule.onNodeWithContentDescription(notes).performClick()
        composeRule.onNodeWithContentDescription(upcoming).performClick()
        composeRule.runOnIdle {
            assertEquals(1, notesClicks)
            assertEquals(1, upcomingClicks)
        }
    }

    @Test
    fun bottomBar_onUpcomingScreen_containsCompletedAddAndNotesActions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val notes = context.getString(R.string.bottom_bar_notes)
        val completed = context.getString(R.string.bottom_bar_completed)
        val upcoming = context.getString(R.string.bottom_bar_upcoming)
        var completedClicks = 0
        var notesClicks = 0

        composeRule.setContent {
            MaterialTheme {
                BottomBar(
                    selectedAction = BottomBarAction.UpcomingTasks,
                    onCompletedNotesClick = { completedClicks += 1 },
                    onAddClick = {},
                    onHomeInfoClick = {},
                    onNavigateHome = { notesClicks += 1 }
                )
            }
        }

        composeRule.onAllNodesWithContentDescription(upcoming).assertCountEquals(0)
        composeRule.onNodeWithContentDescription(completed).performClick()
        composeRule.onNodeWithContentDescription(notes).performClick()
        composeRule.runOnIdle {
            assertEquals(1, completedClicks)
            assertEquals(1, notesClicks)
        }
    }
}
