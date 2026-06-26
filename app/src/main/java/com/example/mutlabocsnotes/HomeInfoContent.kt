package com.example.mutlabocsnotes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun HomeInfoLoadingMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = CozyAuth.Terracotta)
    }
}

@Composable
internal fun HomeInfoStatusMessage(
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = CozyAuth.InkSoft,
            fontFamily = CozyAuth.PixelFont,
            fontSize = 15.sp
        )
        if (actionText != null && onAction != null) {
            PixelPrimaryButton(
                text = actionText,
                onClick = onAction,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
internal fun HomeInfoCardsList(
    cards: List<HomeInfoCard>,
    onCardClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(cards, key = { it.id }) { card ->
            HomeInfoCardItem(
                card = card,
                onClick = { onCardClick(card.id) },
                onLinkClick = onLinkClick
            )
        }
    }
}

@Composable
private fun HomeInfoCardItem(
    card: HomeInfoCard,
    onClick: () -> Unit,
    onLinkClick: (String) -> Unit
) {
    val previewFields = card.fields
        .filter { it.key.isNotBlank() || it.value.isNotBlank() }
        .take(3)

    PixelPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(homeInfoCardTestTag(card.id))
                .clickable(onClick = onClick)
                .padding(16.dp)
        ) {
            Text(
                text = card.title,
                color = CozyAuth.Ink,
                fontFamily = CozyAuth.PixelFont,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = sectionLabel(card.section),
                color = CozyAuth.Hint,
                fontFamily = CozyAuth.PixelFont,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            previewFields.forEach { field ->
                Text(
                    text = field.displayText(),
                    color = CozyAuth.InkSoft,
                    fontFamily = CozyAuth.PixelFont,
                    fontSize = 14.sp
                )
            }
            card.links.forEachIndexed { index, link ->
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(homeInfoLinkTestTag(card.id, index))
                        .clickable { onLinkClick(link) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = stringResource(R.string.home_info_open_link),
                        tint = CozyAuth.Terracotta,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = link,
                        color = CozyAuth.TerracottaDark,
                        fontFamily = CozyAuth.PixelFont,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
