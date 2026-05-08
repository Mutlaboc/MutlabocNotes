package com.example.mutlabocsnotes

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
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
                    onOpenSettings = {}
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
                    uiState = AuthUiState(authState = AuthState.Unauthenticated()),
                    onSignIn = { _, _ -> },
                    onSignUp = { _, _ -> },
                    onGoogleIdToken = {},
                    onYandexAccessToken = {},
                    onClearError = {}
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
    fun bottomBar_containsOnlyCompletedAddAndHomeInfoActions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val add = context.getString(R.string.action_add)
        val homeInfo = context.getString(R.string.home_info_title)
        val completedTasks = context.getString(R.string.completed_notes_title)
        val settings = context.getString(R.string.action_settings)
        var completedClicks = 0

        composeRule.setContent {
            MaterialTheme {
                BottomBar(
                    selectedAction = null,
                    onCompletedNotesClick = { completedClicks += 1 },
                    onAddClick = {},
                    onHomeInfoClick = {}
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
}
