package com.example.mutlabocsnotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

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
    var isTitleError by remember(cardId) { mutableStateOf(false) }
    val fields = remember(cardId) {
        mutableStateListOf<HomeField>().apply { addAll(card?.fields ?: emptyList()) }
    }
    val links = remember(cardId) {
        mutableStateListOf<String>().apply { addAll(card?.links ?: emptyList()) }
    }
    val cleanedLinks = normalizeHomeInfoLinks(links)
    val hasLinkWarning = hasSuspiciousHomeInfoLinks(cleanedLinks)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (card == null) {
                            stringResource(R.string.home_info_new_card)
                        } else {
                            stringResource(R.string.home_info_edit_card)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
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
                    isTitleError = false
                },
                label = { Text(stringResource(R.string.note_title_label)) },
                modifier = Modifier.fillMaxWidth(),
                isError = isTitleError
            )
            if (isTitleError) {
                Text(
                    text = stringResource(R.string.home_info_title_required),
                    color = MaterialTheme.colors.error
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SectionPicker(
                selectedSection = selectedSection,
                onSectionSelected = { selectedSection = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.home_info_fields))
            Spacer(modifier = Modifier.height(6.dp))
            fields.forEachIndexed { index, field ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = field.key,
                        onValueChange = { fields[index] = field.copy(key = it) },
                        label = { Text(stringResource(R.string.home_info_key_label)) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = field.value,
                        onValueChange = { fields[index] = field.copy(value = it) },
                        label = { Text(stringResource(R.string.home_info_value_label)) },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { fields.removeAt(index) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.home_info_delete_field)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            OutlinedButton(onClick = { fields.add(HomeField()) }) {
                Text(stringResource(R.string.home_info_add_field))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.home_info_note_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.home_info_links))
            Spacer(modifier = Modifier.height(6.dp))
            links.forEachIndexed { index, link ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = link,
                        onValueChange = { links[index] = it },
                        label = { Text(stringResource(R.string.home_info_link_label)) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag(editHomeInfoLinkFieldTestTag(index)),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Uri)
                    )
                    IconButton(onClick = { links.removeAt(index) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.home_info_delete_link)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            if (hasLinkWarning) {
                Text(
                    text = stringResource(R.string.home_info_link_warning),
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.testTag(HOME_INFO_LINK_WARNING_TEST_TAG)
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            OutlinedButton(onClick = { links.add("") }) {
                Text(stringResource(R.string.home_info_add_link))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val trimmedTitle = title.trim()
                        if (trimmedTitle.isBlank()) {
                            isTitleError = true
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
                            links = cleanedLinks,
                            createdAt = card?.createdAt ?: now,
                            updatedAt = now
                        )
                        onSaveClick(updatedCard)
                    },
                    modifier = Modifier.testTag(EDIT_HOME_INFO_SAVE_BUTTON_TEST_TAG)
                ) {
                    Text(stringResource(R.string.action_save))
                }
                if (card != null && onDeleteClick != null) {
                    OutlinedButton(onClick = { showDeleteDialog = true }) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
            }
        }
    }

    if (showDeleteDialog && onDeleteClick != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.home_info_delete_card_title)) },
            text = { Text(stringResource(R.string.home_info_delete_card_message)) },
            confirmButton = {
                Button(onClick = {
                    showDeleteDialog = false
                    onDeleteClick()
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
