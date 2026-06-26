package com.example.mutlabocsnotes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val filteredCards = filterHomeInfoCards(
        cards = cards,
        query = query,
        selectedSection = selectedSection,
        sectionLabel = { section -> context.getString(section.labelResId()) }
    )

    LaunchedEffect(uiMessage?.id) {
        val message = uiMessage ?: return@LaunchedEffect
        val text = snackbarText ?: return@LaunchedEffect
        scaffoldState.snackbarHostState.showSnackbar(text)
        onMessageShown(message.id)
    }

    Scaffold(
        scaffoldState = scaffoldState,
        backgroundColor = CozyAuth.Cream,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.home_info_title),
                            color = CozyAuth.Ink,
                            fontFamily = CozyAuth.PixelFont,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                                tint = CozyAuth.Ink
                            )
                        }
                    },
                    backgroundColor = CozyAuth.CardCream,
                    contentColor = CozyAuth.Ink,
                    elevation = 0.dp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(CozyAuth.BrownOutline.copy(alpha = 0.35f))
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                backgroundColor = CozyAuth.Terracotta,
                contentColor = CozyAuth.Sky
            ) {
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
                .background(CozyAuth.Cream)
                .pixelScreenFrame()
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            HomeDeskHeader()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = {
                        Text(
                            text = stringResource(R.string.search_label),
                            fontFamily = CozyAuth.PixelFont
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp),
                    textStyle = TextStyle(
                        fontFamily = CozyAuth.PixelFont,
                        color = CozyAuth.Ink
                    ),
                    colors = cozyTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(HOME_INFO_SEARCH_FIELD_TEST_TAG)
                )
                Spacer(modifier = Modifier.height(8.dp))
                HomeInfoFilterRow(
                    selectedSection = selectedSection,
                    expanded = sectionExpanded,
                    onExpandedChange = { sectionExpanded = it },
                    onSectionSelected = { selectedSection = it }
                )
                Spacer(modifier = Modifier.height(12.dp))
                when (uiState) {
                    is HomeInfoUiState.Error -> HomeInfoStatusMessage(
                        message = uiState.message.asString(),
                        actionText = stringResource(R.string.action_retry),
                        onAction = onRetry
                    )

                    HomeInfoUiState.Loading -> HomeInfoLoadingMessage()

                    HomeInfoUiState.Empty -> HomeInfoStatusMessage(
                        message = stringResource(R.string.home_info_empty)
                    )

                    is HomeInfoUiState.Content -> {
                        if (filteredCards.isEmpty()) {
                            HomeInfoStatusMessage(message = stringResource(R.string.home_info_empty))
                        } else {
                            HomeInfoCardsList(
                                cards = filteredCards,
                                onCardClick = onCardClick,
                                onLinkClick = onLinkClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun cozyTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    textColor = CozyAuth.Ink,
    backgroundColor = CozyAuth.FieldCream,
    focusedBorderColor = CozyAuth.Terracotta,
    unfocusedBorderColor = CozyAuth.InputBorder,
    cursorColor = CozyAuth.TerracottaDark,
    focusedLabelColor = CozyAuth.TerracottaDark,
    unfocusedLabelColor = CozyAuth.Hint
)
