package app.homenotes.android

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeInfoScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun search_matchesFieldKeyLinkAndSectionName() {
        setHomeInfoContent()

        composeRule.onNodeWithTag(HOME_INFO_SEARCH_FIELD_TEST_TAG).performTextInput("serial")
        composeRule.onNodeWithTag(homeInfoCardTestTag("meter")).assertIsDisplayed()
        composeRule.onAllNodesWithTag(homeInfoCardTestTag("contact")).assertCountEquals(0)

        composeRule.onNodeWithTag(HOME_INFO_SEARCH_FIELD_TEST_TAG).performTextClearance()
        composeRule.onNodeWithTag(HOME_INFO_SEARCH_FIELD_TEST_TAG).performTextInput("support.example")
        composeRule.onNodeWithTag(homeInfoCardTestTag("contact")).assertIsDisplayed()
        composeRule.onAllNodesWithTag(homeInfoCardTestTag("meter")).assertCountEquals(0)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithTag(HOME_INFO_SEARCH_FIELD_TEST_TAG).performTextClearance()
        composeRule.onNodeWithTag(HOME_INFO_SEARCH_FIELD_TEST_TAG)
            .performTextInput(context.getString(R.string.home_section_documents))
        composeRule.onNodeWithTag(homeInfoCardTestTag("document")).assertIsDisplayed()
        composeRule.onAllNodesWithTag(homeInfoCardTestTag("meter")).assertCountEquals(0)
    }

    @Test
    fun cardFieldPreview_formatsMissingKeyOrValueGracefully() {
        composeRule.setContent {
            MaterialTheme {
                HomeInfoScreen(
                    uiState = HomeInfoUiState.Content(
                        listOf(
                            HomeInfoCard(
                                id = "field-card",
                                title = "Meter",
                                fields = listOf(
                                    HomeField(key = "serial", value = "A-1"),
                                    HomeField(key = "location", value = ""),
                                    HomeField(key = "", value = "kitchen")
                                )
                            )
                        )
                    ),
                    uiMessage = null,
                    onRetry = {},
                    onMessageShown = {},
                    onAddClick = {},
                    onCardClick = {},
                    onLinkClick = {},
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText("serial: A-1").assertIsDisplayed()
        composeRule.onNodeWithText("location").assertIsDisplayed()
        composeRule.onNodeWithText("kitchen").assertIsDisplayed()
    }

    @Test
    fun linkClickOpensLinkWithoutOpeningEditor() {
        var clickedCardId: String? = null
        var clickedLink: String? = null
        composeRule.setContent {
            MaterialTheme {
                HomeInfoScreen(
                    uiState = HomeInfoUiState.Content(
                        listOf(
                            HomeInfoCard(
                                id = "meter",
                                title = "Meter",
                                links = listOf("https://example.com/meter")
                            )
                        )
                    ),
                    uiMessage = null,
                    onRetry = {},
                    onMessageShown = {},
                    onAddClick = {},
                    onCardClick = { clickedCardId = it },
                    onLinkClick = { clickedLink = it },
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithTag(homeInfoLinkTestTag("meter", 0)).performClick()

        composeRule.runOnIdle {
            assertEquals("https://example.com/meter", clickedLink)
            assertEquals(null, clickedCardId)
        }
    }

    @Test
    fun editorAutoPrefixesDomainLinksOnSave() {
        var savedCard: HomeInfoCard? = null
        setEditHomeInfoContent(onSaveClick = { savedCard = it })

        enterTitleAndFirstLink("Meter", "example.com")
        composeRule.onNodeWithTag(EDIT_HOME_INFO_SAVE_BUTTON_TEST_TAG)
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("https://example.com"), savedCard?.links)
        }
    }

    @Test
    fun editorWarnsForSuspiciousLinkButStillSaves() {
        var savedCard: HomeInfoCard? = null
        setEditHomeInfoContent(onSaveClick = { savedCard = it })

        enterTitleAndFirstLink("Meter", "https://bad url")

        composeRule.onNodeWithTag(HOME_INFO_LINK_WARNING_TEST_TAG)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag(EDIT_HOME_INFO_SAVE_BUTTON_TEST_TAG)
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("https://bad url"), savedCard?.links)
        }
    }

    private fun setHomeInfoContent() {
        composeRule.setContent {
            MaterialTheme {
                HomeInfoScreen(
                    uiState = HomeInfoUiState.Content(
                        listOf(
                            HomeInfoCard(
                                id = "meter",
                                title = "Water meter",
                                section = HomeSection.METERS,
                                fields = listOf(HomeField(key = "serial", value = "A-1"))
                            ),
                            HomeInfoCard(
                                id = "contact",
                                title = "Service contact",
                                section = HomeSection.CONTACTS,
                                links = listOf("https://support.example.com")
                            ),
                            HomeInfoCard(
                                id = "document",
                                title = "Warranty",
                                section = HomeSection.DOCUMENTS
                            )
                        )
                    ),
                    uiMessage = null,
                    onRetry = {},
                    onMessageShown = {},
                    onAddClick = {},
                    onCardClick = {},
                    onLinkClick = {},
                    onBack = {}
                )
            }
        }
    }

    private fun setEditHomeInfoContent(onSaveClick: (HomeInfoCard) -> Unit) {
        composeRule.setContent {
            MaterialTheme {
                EditHomeInfoCardScreen(
                    card = null,
                    onSaveClick = onSaveClick,
                    onDeleteClick = null,
                    onBack = {}
                )
            }
        }
    }

    private fun enterTitleAndFirstLink(title: String, link: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithText(context.getString(R.string.note_title_label))
            .performTextInput(title)
        composeRule.onNodeWithText(context.getString(R.string.home_info_add_link))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag(editHomeInfoLinkFieldTestTag(0))
            .performScrollTo()
            .performTextInput(link)
    }
}
