package com.example.mutlabocsnotes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun HomeInfoScreen(
    uiState: HomeInfoUiState,
    uiMessage: UiMessage?,
    onRetry: () -> Unit,
    onMessageShown: (Long) -> Unit,
    onAddClick: () -> Unit,
    onCardClick: (String) -> Unit,
    onLinkClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scaffoldState = rememberScaffoldState()
    val snackbarText = uiMessage?.text?.asString()
    val cards = (uiState as? HomeInfoUiState.Content)?.cards.orEmpty()
    var query by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf<HomeSection?>(null) }
    var sectionExpanded by remember { mutableStateOf(false) }
    val filteredCards = cards.filter { card ->
        val queryText = query.trim()
        val sectionText = context.getString(card.section.labelResId())
        val matchesSection = selectedSection == null || card.section == selectedSection
        val matchesQuery = queryText.isBlank() ||
            card.title.contains(queryText, ignoreCase = true) ||
            card.note.contains(queryText, ignoreCase = true) ||
            card.fields.any { field ->
                field.key.contains(queryText, ignoreCase = true) ||
                    field.value.contains(queryText, ignoreCase = true)
            } ||
            card.links.any { it.contains(queryText, ignoreCase = true) } ||
            sectionText.contains(queryText, ignoreCase = true)
        matchesSection && matchesQuery
    }

    LaunchedEffect(uiMessage?.id) {
        val message = uiMessage ?: return@LaunchedEffect
        val text = snackbarText ?: return@LaunchedEffect
        scaffoldState.snackbarHostState.showSnackbar(text)
        onMessageShown(message.id)
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_info_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.home_info_add_card)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(R.string.search_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(HOME_INFO_SEARCH_FIELD_TEST_TAG)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.home_info_section))
                Box {
                    OutlinedButton(onClick = { sectionExpanded = true }) {
                        Text(
                            selectedSection?.let { sectionLabel(it) }
                                ?: stringResource(R.string.home_info_all_sections)
                        )
                    }
                    DropdownMenu(
                        expanded = sectionExpanded,
                        onDismissRequest = { sectionExpanded = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            selectedSection = null
                            sectionExpanded = false
                        }) {
                            Text(stringResource(R.string.home_info_all_sections))
                        }
                        HomeSection.values().forEach { section ->
                            DropdownMenuItem(onClick = {
                                selectedSection = section
                                sectionExpanded = false
                            }) {
                                Text(sectionLabel(section))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            when (uiState) {
                is HomeInfoUiState.Error -> HomeInfoStatusMessage(
                    message = uiState.message.asString(),
                    actionText = stringResource(R.string.action_retry),
                    onAction = onRetry
                )

                HomeInfoUiState.Loading -> LoadingMessage()

                HomeInfoUiState.Empty -> HomeInfoStatusMessage(
                    message = stringResource(R.string.home_info_empty)
                )

                is HomeInfoUiState.Content -> {
                    if (filteredCards.isEmpty()) {
                        HomeInfoStatusMessage(message = stringResource(R.string.home_info_empty))
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                        ) {
                            items(filteredCards, key = { it.id }) { card ->
                                HomeInfoCardItem(
                                    card = card,
                                    onClick = { onCardClick(card.id) },
                                    onLinkClick = onLinkClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingMessage() {
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
private fun HomeInfoStatusMessage(
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
