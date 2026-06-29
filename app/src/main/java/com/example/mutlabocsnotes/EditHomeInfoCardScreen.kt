package com.example.mutlabocsnotes

import androidx.compose.foundation.background
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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        backgroundColor = CozyAuth.Cream,
        topBar = {
            CozyTopBar(
                title = if (card == null) {
                    stringResource(R.string.home_info_new_card)
                } else {
                    stringResource(R.string.home_info_edit_card)
                },
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(CozyAuth.Cream)
                .pixelScreenFrame()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            CozyTextField(
                value = title,
                onValueChange = {
                    title = it
                    isTitleError = false
                },
                label = stringResource(R.string.note_title_label),
                isError = isTitleError,
                modifier = Modifier.fillMaxWidth()
            )
            if (isTitleError) {
                Text(
                    text = stringResource(R.string.home_info_title_required),
                    color = CozyAuth.Terracotta,
                    fontFamily = CozyAuth.PixelFont,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            SectionPicker(
                selectedSection = selectedSection,
                onSectionSelected = { selectedSection = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
            EditCardSectionLabel(stringResource(R.string.home_info_fields))
            Spacer(modifier = Modifier.height(6.dp))
            fields.forEachIndexed { index, field ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CozyTextField(
                        value = field.key,
                        onValueChange = { fields[index] = field.copy(key = it) },
                        label = stringResource(R.string.home_info_key_label),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CozyTextField(
                        value = field.value,
                        onValueChange = { fields[index] = field.copy(value = it) },
                        label = stringResource(R.string.home_info_value_label),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { fields.removeAt(index) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.home_info_delete_field),
                            tint = CozyAuth.InkSoft
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            PixelOutlineButton(
                text = stringResource(R.string.home_info_add_field),
                onClick = { fields.add(HomeField()) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            CozyTextField(
                value = note,
                onValueChange = { note = it },
                label = stringResource(R.string.home_info_note_label),
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text)
            )
            Spacer(modifier = Modifier.height(12.dp))
            EditCardSectionLabel(stringResource(R.string.home_info_links))
            Spacer(modifier = Modifier.height(6.dp))
            links.forEachIndexed { index, link ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CozyTextField(
                        value = link,
                        onValueChange = { links[index] = it },
                        label = stringResource(R.string.home_info_link_label),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Uri),
                        modifier = Modifier
                            .weight(1f)
                            .testTag(editHomeInfoLinkFieldTestTag(index))
                    )
                    IconButton(onClick = { links.removeAt(index) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.home_info_delete_link),
                            tint = CozyAuth.InkSoft
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            if (hasLinkWarning) {
                Text(
                    text = stringResource(R.string.home_info_link_warning),
                    color = CozyAuth.Terracotta,
                    fontFamily = CozyAuth.PixelFont,
                    fontSize = 12.sp,
                    modifier = Modifier.testTag(HOME_INFO_LINK_WARNING_TEST_TAG)
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            PixelOutlineButton(
                text = stringResource(R.string.home_info_add_link),
                onClick = { links.add("") }
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PixelPrimaryButton(
                    text = stringResource(R.string.action_save),
                    onClick = {
                        val trimmedTitle = title.trim()
                        if (trimmedTitle.isBlank()) {
                            isTitleError = true
                            return@PixelPrimaryButton
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
                )
                if (card != null && onDeleteClick != null) {
                    PixelOutlineButton(
                        text = stringResource(R.string.action_delete),
                        onClick = { showDeleteDialog = true }
                    )
                }
            }
        }
    }

    if (showDeleteDialog && onDeleteClick != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            backgroundColor = CozyAuth.CardCream,
            title = {
                Text(
                    text = stringResource(R.string.home_info_delete_card_title),
                    color = CozyAuth.Ink,
                    fontFamily = CozyAuth.PixelFont
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.home_info_delete_card_message),
                    color = CozyAuth.InkSoft,
                    fontFamily = CozyAuth.PixelFont
                )
            },
            confirmButton = {
                PixelPrimaryButton(
                    text = stringResource(R.string.action_delete),
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick()
                    }
                )
            },
            dismissButton = {
                PixelOutlineButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = { showDeleteDialog = false }
                )
            }
        )
    }
}

@Composable
private fun EditCardSectionLabel(text: String) {
    Text(
        text = text,
        color = CozyAuth.Ink,
        fontFamily = CozyAuth.PixelFont,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
    )
}
