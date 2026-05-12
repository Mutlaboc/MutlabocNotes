package com.example.mutlabocsnotes

import android.app.DatePickerDialog
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Calendar

const val EDIT_NOTE_TITLE_FIELD_TEST_TAG = "edit_note_title_field"
const val EDIT_NOTE_TITLE_ERROR_TEST_TAG = "edit_note_title_error"
const val EDIT_NOTE_CONTENT_FIELD_TEST_TAG = "edit_note_content_field"
const val EDIT_NOTE_COIN_COUNT_FIELD_TEST_TAG = "edit_note_coin_count_field"
const val EDIT_NOTE_SAVE_BUTTON_TEST_TAG = "edit_note_save_button"
const val EDIT_NOTE_DELETE_BUTTON_TEST_TAG = "edit_note_delete_button"
const val EDIT_NOTE_DELETE_CONFIRM_BUTTON_TEST_TAG = "edit_note_delete_confirm_button"

fun editNoteCategoryChipTestTag(category: NoteCategory): String = "edit_note_category_${category.name}"

fun editNoteChecklistItemFieldTestTag(index: Int): String = "edit_note_checklist_item_field_$index"

fun editNoteChecklistItemDeleteButtonTestTag(index: Int): String =
    "edit_note_checklist_item_delete_$index"

internal fun prepareNoteForSave(
    noteId: String,
    originalNote: Note?,
    title: String,
    content: String,
    selectedCategory: NoteCategory,
    checklistItems: List<ChecklistItem>,
    selectedDeadlineMillis: Long,
    isRepeating: Boolean,
    coinCountText: String,
    defaultCoinCount: Int
): Note? {
    val trimmedTitle = title.trim()
    if (trimmedTitle.isBlank()) return null

    val cleanedChecklist = if (selectedCategory == NoteCategory.SHOPPING) {
        checklistItems
            .map { it.copy(text = it.text.trim()) }
            .filter { it.text.isNotBlank() }
    } else {
        emptyList()
    }
    val preparedContent = when (selectedCategory) {
        NoteCategory.SHOPPING -> ""
        else -> content
    }
    return Note(
        id = noteId,
        title = trimmedTitle,
        content = preparedContent,
        category = selectedCategory,
        checklist = cleanedChecklist,
        deadlineMillis = if (selectedCategory == NoteCategory.TASKS) {
            selectedDeadlineMillis
        } else {
            null
        },
        isRepeating = if (selectedCategory == NoteCategory.TASKS) {
            isRepeating
        } else {
            false
        },
        coinCount = coinCountText.toIntOrNull() ?: defaultCoinCount,
        isCompleted = originalNote?.isCompleted ?: false
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteScreen(
    note: Note?,
    onSaveClick: (Note) -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    val noteId = note?.id.orEmpty()
    var title by remember(noteId) { mutableStateOf(note?.title ?: "") }
    var content by remember(noteId) { mutableStateOf(note?.content ?: "") }
    var selectedCategory by remember(noteId) {
        mutableStateOf(note?.category ?: NoteCategory.NOTES)
    }
    val checklistItems = remember(noteId) {
        val initial = note
            ?.takeIf { it.category == NoteCategory.SHOPPING && it.checklist.isNotEmpty() }
            ?.checklist
            ?: listOf(ChecklistItem())
        mutableStateListOf<ChecklistItem>().apply { addAll(initial) }
    }
    val todayCalendar = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val defaultDeadline = remember {
        (todayCalendar.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
    }
    var selectedDeadlineMillis by remember(noteId) {
        mutableStateOf(note?.deadlineMillis ?: defaultDeadline)
    }
    var isRepeating by remember(noteId) {
        mutableStateOf(note?.isRepeating ?: false)
    }
    val defaultCoinCount = remember(noteId) { note?.coinCount ?: (1..5).random() }
    var coinCountText by remember(noteId) {
        mutableStateOf(defaultCoinCount.toString())
    }
    var isTitleError by remember(noteId) { mutableStateOf(false) }
    var showDeleteDialog by remember(noteId) { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_note_title)) }
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
            NoteTitleField(
                title = title,
                isError = isTitleError,
                onTitleChange = {
                    title = it
                    isTitleError = false
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            CategoryPicker(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            Spacer(modifier = Modifier.height(8.dp))
            when (selectedCategory) {
                NoteCategory.SHOPPING -> {
                    ChecklistEditor(
                        checklistItems = checklistItems,
                        onItemTextChange = { index, text ->
                            checklistItems[index] = checklistItems[index].copy(text = text)
                        },
                        onItemCheckedChange = { index, checked ->
                            checklistItems[index] = checklistItems[index].copy(isChecked = checked)
                        },
                        onAddItem = { checklistItems.add(ChecklistItem()) },
                        onRemoveItem = { index ->
                            if (checklistItems.size == 1) {
                                checklistItems[index] = ChecklistItem()
                            } else {
                                checklistItems.removeAt(index)
                            }
                        }
                    )
                }

                NoteCategory.TASKS -> {
                    DeadlinePicker(
                        selectedDeadlineMillis = selectedDeadlineMillis,
                        todayMillis = todayCalendar.timeInMillis,
                        isRepeating = isRepeating,
                        onDeadlineSelected = { selectedDeadlineMillis = it },
                        onRepeatingChange = { isRepeating = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    NoteContentField(
                        content = content,
                        onContentChange = { content = it }
                    )
                }

                NoteCategory.NOTES -> {
                    NoteContentField(
                        content = content,
                        onContentChange = { content = it }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (BuildConfig.DEBUG) {
                CoinCountDebugField(
                    coinCountText = coinCountText,
                    onCoinCountChange = { coinCountText = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val preparedNote = prepareNoteForSave(
                            noteId = noteId,
                            originalNote = note,
                            title = title,
                            content = content,
                            selectedCategory = selectedCategory,
                            checklistItems = checklistItems,
                            selectedDeadlineMillis = selectedDeadlineMillis,
                            isRepeating = isRepeating,
                            coinCountText = coinCountText,
                            defaultCoinCount = defaultCoinCount
                        )
                        if (preparedNote == null) {
                            isTitleError = true
                            return@Button
                        }
                        onSaveClick(preparedNote)
                    },
                    modifier = Modifier.testTag(EDIT_NOTE_SAVE_BUTTON_TEST_TAG)
                ) {
                    Text(stringResource(R.string.action_save))
                }
                Spacer(Modifier.weight(1f))
                if (note != null && onDeleteClick != null) {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.testTag(EDIT_NOTE_DELETE_BUTTON_TEST_TAG)
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
            }
        }
    }

    if (showDeleteDialog && onDeleteClick != null) {
        DeleteNoteDialog(
            onConfirm = {
                showDeleteDialog = false
                onDeleteClick()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun NoteTitleField(
    title: String,
    isError: Boolean,
    onTitleChange: (String) -> Unit
) {
    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        label = { Text(stringResource(R.string.note_title_label)) },
        textStyle = TextStyle(color = Color.Black),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = Color.Black,
            focusedLabelColor = Color.Black,
            unfocusedLabelColor = Color.Gray,
            cursorColor = Color.Black
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(EDIT_NOTE_TITLE_FIELD_TEST_TAG),
        isError = isError
    )
    if (isError) {
        Text(
            text = stringResource(R.string.note_title_required),
            color = MaterialTheme.colors.error,
            modifier = Modifier.testTag(EDIT_NOTE_TITLE_ERROR_TEST_TAG)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPicker(
    selectedCategory: NoteCategory,
    onCategorySelected: (NoteCategory) -> Unit
) {
    val categories = listOf(
        NoteCategory.SHOPPING to R.string.note_category_shopping,
        NoteCategory.TASKS to R.string.note_category_tasks,
        NoteCategory.NOTES to R.string.note_category_notes
    )
    Row {
        categories.forEach { (category, labelResId) ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(stringResource(labelResId)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFBBDEFB)
                ),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .testTag(editNoteCategoryChipTestTag(category))
            )
        }
    }
}

@Composable
private fun ChecklistEditor(
    checklistItems: List<ChecklistItem>,
    onItemTextChange: (Int, String) -> Unit,
    onItemCheckedChange: (Int, Boolean) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        checklistItems.forEachIndexed { index, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Checkbox(
                    checked = item.isChecked,
                    onCheckedChange = { checked -> onItemCheckedChange(index, checked) }
                )
                OutlinedTextField(
                    value = item.text,
                    onValueChange = { text -> onItemTextChange(index, text) },
                    textStyle = TextStyle(color = Color.Black),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                        focusedLabelColor = Color.Black,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color.Black
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                        .testTag(editNoteChecklistItemFieldTestTag(index))
                )
                IconButton(
                    onClick = { onRemoveItem(index) },
                    modifier = Modifier.testTag(editNoteChecklistItemDeleteButtonTestTag(index))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.checklist_delete_item)
                    )
                }
            }
        }
        Button(onClick = onAddItem) {
            Text(stringResource(R.string.checklist_add_item))
        }
    }
}

@Composable
private fun DeadlinePicker(
    selectedDeadlineMillis: Long,
    todayMillis: Long,
    isRepeating: Boolean,
    onDeadlineSelected: (Long) -> Unit,
    onRepeatingChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val datePickerDialog = remember(context) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedDeadlineMillis
        }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val pickedCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onDeadlineSelected(pickedCalendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = todayMillis
        }
    }
    val dateFieldInteractionSource = remember { MutableInteractionSource() }
    LaunchedEffect(dateFieldInteractionSource, selectedDeadlineMillis) {
        dateFieldInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = selectedDeadlineMillis
                }
                datePickerDialog.updateDate(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                datePickerDialog.show()
            }
        }
    }
    val deadlineText = remember(selectedDeadlineMillis) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedDeadlineMillis
        }
        "%02d.%02d.%04d".format(
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.YEAR)
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = deadlineText,
            onValueChange = {},
            label = { Text(stringResource(R.string.deadline_label)) },
            textStyle = TextStyle(color = Color.Black),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = Color.Black,
                focusedLabelColor = Color.Black,
                unfocusedLabelColor = Color.Gray,
                cursorColor = Color.Black
            ),
            modifier = Modifier.weight(1f),
            readOnly = true,
            interactionSource = dateFieldInteractionSource
        )
        Spacer(modifier = Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = isRepeating,
                onCheckedChange = onRepeatingChange
            )
            Text(stringResource(R.string.repeat_label))
        }
    }
}

@Composable
private fun NoteContentField(
    content: String,
    onContentChange: (String) -> Unit
) {
    OutlinedTextField(
        value = content,
        onValueChange = onContentChange,
        label = { Text(stringResource(R.string.note_content_label), color = Color.Black) },
        textStyle = TextStyle(color = Color.Black),
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.4f)
            .testTag(EDIT_NOTE_CONTENT_FIELD_TEST_TAG),
        maxLines = Int.MAX_VALUE
    )
}

@Composable
private fun CoinCountDebugField(
    coinCountText: String,
    onCoinCountChange: (String) -> Unit
) {
    OutlinedTextField(
        value = coinCountText,
        onValueChange = { value ->
            if (value.all { it.isDigit() }) {
                onCoinCountChange(value)
            }
        },
        label = { Text(stringResource(R.string.coin_count_label)) },
        textStyle = TextStyle(color = Color.Black),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = Color.Black,
            focusedLabelColor = Color.Black,
            unfocusedLabelColor = Color.Gray,
            cursorColor = Color.Black
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(EDIT_NOTE_COIN_COUNT_FIELD_TEST_TAG),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun DeleteNoteDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.note_delete_title)) },
        text = { Text(stringResource(R.string.note_delete_message)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag(EDIT_NOTE_DELETE_CONFIRM_BUTTON_TEST_TAG)
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun EditNoteScreenPreview() {
    val sampleNote = Note(
        id = "1",
        title = "Sample",
        content = "Content",
        category = NoteCategory.NOTES,
        coinCount = 3
    )
    EditNoteScreen(
        note = sampleNote,
        onSaveClick = { _ -> },
        onDeleteClick = { }
    )
}
