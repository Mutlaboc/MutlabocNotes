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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun HomeInfoScreen(
    cards: List<HomeInfoCard>,
    isLoading: Boolean,
    errorMessage: String?,
    onAddClick: () -> Unit,
    onCardClick: (String) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedSection by remember { mutableStateOf<HomeSection?>(null) }
    var sectionExpanded by remember { mutableStateOf(false) }
    val filteredCards = cards.filter { card ->
        val matchesSection = selectedSection == null || card.section == selectedSection
        val queryText = query.trim().lowercase()
        val matchesQuery = if (queryText.isBlank()) {
            true
        } else {
            card.title.lowercase().contains(queryText) ||
                    card.note.lowercase().contains(queryText) ||
                    card.fields.any { it.value.lowercase().contains(queryText) }
        }
        matchesSection && matchesQuery
    }

    Scaffold(
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
            if (isLoading) {
                Text("Загрузка...")
            }
            if (!errorMessage.isNullOrBlank()) {
                Text(text = errorMessage, color = Color.Red)
            }
            if (filteredCards.isEmpty() && !isLoading) {
                Text("Нет карточек для отображения")
            } else {
                val uniqueCards = remember(filteredCards) {
                    filteredCards.distinctBy { it.id }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    items(uniqueCards, key = { it.id }) { card ->
                        HomeInfoCardItem(card = card, onClick = { onCardClick(card.id) })
                    }
                }
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
        val initialFields = card?.fields ?: emptyList()
        mutableStateListOf<HomeField>().apply { addAll(initialFields) }
    }
    val links = remember(cardId) {
        val initialLinks = card?.links ?: emptyList()
        mutableStateListOf<String>().apply { addAll(initialLinks) }
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
                    if (!titleError.isNullOrBlank()) {
                        titleError = null
                    }
                },
                label = { Text("Заголовок") },
                modifier = Modifier.fillMaxWidth(),
                isError = !titleError.isNullOrBlank()
            )
            if (!titleError.isNullOrBlank()) {
                Text(text = titleError ?: "", color = Color.Red)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Раздел")
                Spacer(modifier = Modifier.width(12.dp))
                var sectionExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { sectionExpanded = true }) {
                        Text(selectedSection.displayName())
                    }
                    DropdownMenu(
                        expanded = sectionExpanded,
                        onDismissRequest = { sectionExpanded = false }
                    ) {
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
            Text("Поля")
            Spacer(modifier = Modifier.height(6.dp))
            fields.forEachIndexed { index, field ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = field.key,
                        onValueChange = { newKey ->
                            fields[index] = field.copy(key = newKey)
                        },
                        label = { Text("Ключ") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = field.value,
                        onValueChange = { newValue ->
                            fields[index] = field.copy(value = newValue)
                        },
                        label = { Text("Значение") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { fields.removeAt(index) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить поле"
                        )
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
                        onValueChange = { newValue ->
                            links[index] = newValue
                        },
                        label = { Text("URL") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { links.removeAt(index) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить ссылку"
                        )
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
                    val cleanedFields = fields.map {
                        HomeField(key = it.key.trim(), value = it.value.trim())
                    }.filter { it.key.isNotBlank() || it.value.isNotBlank() }
                    val cleanedLinks = links.map { it.trim() }.filter { it.isNotBlank() }
                    val now = System.currentTimeMillis()
                    val updatedCard = HomeInfoCard(
                        id = card?.id ?: "",
                        title = trimmedTitle,
                        section = selectedSection,
                        fields = cleanedFields,
                        note = note.trim(),
                        links = cleanedLinks,
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
            text = { Text("Действие нельзя отменить") },
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

private fun HomeSection.displayName(): String = when (this) {
    HomeSection.METERS -> "Счётчики"
    HomeSection.APPLIANCES -> "Техника"
    HomeSection.LIGHTING -> "Освещение"
    HomeSection.DOCUMENTS -> "Документы"
    HomeSection.CONTACTS -> "Контакты"
    HomeSection.OTHER -> "Другое"
}