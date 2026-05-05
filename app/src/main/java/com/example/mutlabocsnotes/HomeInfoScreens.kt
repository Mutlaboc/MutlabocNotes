package com.example.mutlabocsnotes

import android.app.DatePickerDialog
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
fun HomeInfoScreen(
    uiState: HomeInfoUiState,
    uiMessage: UiMessage?,
    onRetry: () -> Unit,
    onMessageShown: (Long) -> Unit,
    onAddClick: () -> Unit,
    onCardClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val snackbarText = uiMessage?.text?.asString()
    val cards = (uiState as? HomeInfoUiState.Content)?.cards.orEmpty()
    var query by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf<HomeSection?>(null) }
    var sectionExpanded by remember { mutableStateOf(false) }
    val filteredCards = cards.filter { card ->
        val queryText = query.trim()
        val matchesSection = selectedSection == null || card.section == selectedSection
        val matchesQuery = queryText.isBlank() ||
            card.title.contains(queryText, ignoreCase = true) ||
            card.note.contains(queryText, ignoreCase = true) ||
            card.fields.any { it.value.contains(queryText, ignoreCase = true) }
        matchesSection && matchesQuery
    }.distinctBy { it.id }

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
                title = { Text("Информация о доме") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Добавить карточку")
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
                label = { Text("Поиск") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Раздел")
                Box {
                    OutlinedButton(onClick = { sectionExpanded = true }) {
                        Text(selectedSection?.displayName() ?: "Все")
                    }
                    DropdownMenu(
                        expanded = sectionExpanded,
                        onDismissRequest = { sectionExpanded = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            selectedSection = null
                            sectionExpanded = false
                        }) {
                            Text("Все")
                        }
                        HomeSection.values().forEach { section ->
                            DropdownMenuItem(onClick = {
                                selectedSection = section
                                sectionExpanded = false
                            }) {
                                Text(section.displayName())
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
                                HomeInfoCardItem(card = card, onClick = { onCardClick(card.id) })
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
private fun HomeInfoCardItem(card: HomeInfoCard, onClick: () -> Unit) {
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
                .padding(16.dp)
                .clickable(onClick = onClick)
        ) {
            Text(text = card.title, style = MaterialTheme.typography.subtitle1)
            Text(text = card.section.displayName(), style = MaterialTheme.typography.caption)
            Spacer(modifier = Modifier.height(6.dp))
            previewFields.forEach { field ->
                Text(text = "${field.key}: ${field.value}", style = MaterialTheme.typography.body2)
            }
            if (card.links.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Ссылок: ${card.links.size}", style = MaterialTheme.typography.caption)
            }
        }
    }
}

@Composable
fun EditHomeInfoCardScreen(
    card: HomeInfoCard?,
    onSaveClick: (HomeInfoCard) -> Unit,
    onDeleteClick: (() -> Unit)?,
    onBack: () -> Unit
) {
    val cardId = card?.id.orEmpty()
    var title by remember(cardId) { mutableStateOf(card?.title ?: "") }
    var selectedSection by remember(cardId) { mutableStateOf(card?.section ?: HomeSection.OTHER) }
    var note by remember(cardId) { mutableStateOf(card?.note ?: "") }
    var showDeleteDialog by remember(cardId) { mutableStateOf(false) }
    var titleError by remember(cardId) { mutableStateOf<String?>(null) }
    val fields = remember(cardId) {
        mutableStateListOf<HomeField>().apply { addAll(card?.fields ?: emptyList()) }
    }
    val links = remember(cardId) {
        mutableStateListOf<String>().apply { addAll(card?.links ?: emptyList()) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (card == null) "Новая карточка" else "Редактирование") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = null
                },
                label = { Text("Заголовок") },
                modifier = Modifier.fillMaxWidth(),
                isError = !titleError.isNullOrBlank()
            )
            titleError?.let {
                Text(text = it, color = Color.Red)
            }
            Spacer(modifier = Modifier.height(8.dp))
            SectionPicker(
                selectedSection = selectedSection,
                onSectionSelected = { selectedSection = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Поля")
            Spacer(modifier = Modifier.height(6.dp))
            fields.forEachIndexed { index, field ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = field.key,
                        onValueChange = { fields[index] = field.copy(key = it) },
                        label = { Text("Ключ") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = field.value,
                        onValueChange = { fields[index] = field.copy(value = it) },
                        label = { Text("Значение") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { fields.removeAt(index) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Удалить поле")
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            OutlinedButton(onClick = { fields.add(HomeField()) }) {
                Text("Добавить поле")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Заметка") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("Ссылки")
            Spacer(modifier = Modifier.height(6.dp))
            links.forEachIndexed { index, link ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = link,
                        onValueChange = { links[index] = it },
                        label = { Text("Ссылка") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { links.removeAt(index) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Удалить ссылку")
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            OutlinedButton(onClick = { links.add("") }) {
                Text("Добавить ссылку")
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    val trimmedTitle = title.trim()
                    if (trimmedTitle.isBlank()) {
                        titleError = "Введите заголовок"
                        return@Button
                    }
                    val now = System.currentTimeMillis()
                    val updatedCard = HomeInfoCard(
                        id = card?.id ?: "",
                        title = trimmedTitle,
                        section = selectedSection,
                        fields = fields.map {
                            HomeField(key = it.key.trim(), value = it.value.trim())
                        }.filter { it.key.isNotBlank() || it.value.isNotBlank() },
                        note = note.trim(),
                        links = links.map { it.trim() }.filter { it.isNotBlank() },
                        createdAt = card?.createdAt ?: now,
                        updatedAt = now
                    )
                    onSaveClick(updatedCard)
                }) {
                    Text("Сохранить")
                }
                if (card != null && onDeleteClick != null) {
                    OutlinedButton(onClick = { showDeleteDialog = true }) {
                        Text("Удалить")
                    }
                }
            }
        }
    }

    if (showDeleteDialog && onDeleteClick != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить карточку?") },
            text = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteDialog = false
                    onDeleteClick()
                }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun SectionPicker(
    selectedSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Раздел")
        Spacer(modifier = Modifier.width(12.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(selectedSection.displayName())
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                HomeSection.values().forEach { section ->
                    DropdownMenuItem(onClick = {
                        onSectionSelected(section)
                        expanded = false
                    }) {
                        Text(section.displayName())
                    }
                }
            }
        }
    }
}

private fun HomeSection.displayName(): String = when (this) {
    HomeSection.METERS -> "Счётчики"
    HomeSection.APPLIANCES -> "Техника"
    HomeSection.LIGHTING -> "Освещение"
    HomeSection.DOCUMENTS -> "Документы"
    HomeSection.CONTACTS -> "Контакты"
    HomeSection.OTHER -> "Другое"
}

@Preview(showBackground = true)
@Composable
fun HomeInfoScreenPreview() {
    val cards = listOf(
        HomeInfoCard(id = "1", title = "Карточка 1"),
        HomeInfoCard(id = "2", title = "Карточка 2"),
        HomeInfoCard(id = "3", title = "Карточка 3")
    )
    HomeInfoScreen(
        uiState = HomeInfoUiState.Content(cards),
        uiMessage = null,
        onRetry = {},
        onMessageShown = {},
        onAddClick = {},
        onCardClick = {},
        onBack = {}
    )
}
