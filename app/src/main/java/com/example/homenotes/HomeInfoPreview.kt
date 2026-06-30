package com.example.homenotes

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun HomeInfoScreenPreview() {
    val cards = listOf(
        HomeInfoCard(id = "1", title = "Card 1"),
        HomeInfoCard(id = "2", title = "Card 2"),
        HomeInfoCard(id = "3", title = "Card 3")
    )
    HomeInfoScreen(
        uiState = HomeInfoUiState.Content(cards),
        uiMessage = null,
        onRetry = {},
        onMessageShown = {},
        onAddClick = {},
        onCardClick = {},
        onLinkClick = {},
        onBack = {}
    )
}
