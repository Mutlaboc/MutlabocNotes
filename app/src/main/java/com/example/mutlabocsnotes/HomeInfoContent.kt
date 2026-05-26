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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun HomeInfoLoadingMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
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
        Text(text = message, style = MaterialTheme.typography.body1)
        if (actionText != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(actionText)
            }
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

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(homeInfoCardTestTag(card.id))
                .clickable(onClick = onClick)
                .padding(16.dp)
        ) {
            Text(text = card.title, style = MaterialTheme.typography.subtitle1)
            Text(text = sectionLabel(card.section), style = MaterialTheme.typography.caption)
            Spacer(modifier = Modifier.height(6.dp))
            previewFields.forEach { field ->
                Text(
                    text = field.displayText(),
                    style = MaterialTheme.typography.body2
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
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = link,
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        }
    }
}
