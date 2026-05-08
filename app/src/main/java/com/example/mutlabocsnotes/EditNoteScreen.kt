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
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Calendar

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
    val categories = listOf(
        NoteCategory.SHOPPING to R.string.note_category_shopping,
        NoteCategory.TASKS to R.string.note_category_tasks,
        NoteCategory.NOTES to R.string.note_category_notes
    )

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
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.note_title_label)) },
                textStyle = TextStyle(color = Color.Black),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.Black,
                    focusedLabelColor = Color.Black,
                    unfocusedLabelColor = Color.Gray,
                    cursorColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                categories.forEach { (category, labelResId) ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(stringResource(labelResId)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFBBDEFB)
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            when (selectedCategory) {
                NoteCategory.SHOPPING -> {
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
                                    onCheckedChange = { checked ->
                                        checklistItems[index] = item.copy(isChecked = checked)
                                    }
                                )
                                OutlinedTextField(
                                    value = item.text,
                                    onValueChange = { text ->
                                        checklistItems[index] = item.copy(text = text)
                                    },
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
                                )
                            }
                        }
                        Button(onClick = { checklistItems.add(ChecklistItem()) }) {
                            Text(stringResource(R.string.checklist_add_item))
                        }
                    }
                }

                NoteCategory.TASKS -> {
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
                                selectedDeadlineMillis = pickedCalendar.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).apply {
                            datePicker.minDate = todayCalendar.timeInMillis
                        }
                    }

                    val dateFieldInteractionSource = remember { MutableInteractionSource() }
                    LaunchedEffect(dateFieldInteractionSource) {
                        dateFieldInteractionSource.interactions.collect { interaction ->
                            if (interaction is PressInteraction.Release) {
                                val cal = Calendar.getInstance().apply {
                                    timeInMillis = selectedDeadlineMillis
                                }
                                datePickerDialog.updateDate(
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
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
                                onCheckedChange = { isRepeating = it }
                            )
                            Text(stringResource(R.string.repeat_label))
                        }
                    }
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
                OutlinedTextField(
                    value = coinCountText,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() }) {
                            coinCountText = value
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
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        val coinCount = coinCountText.toIntOrNull()
                            ?: defaultCoinCount
                        val cleanedChecklist = if (selectedCategory == NoteCategory.SHOPPING) {
                            checklistItems
                                .map { it.copy(text = it.text.trim()) }
                                .filter { it.text.isNotEmpty() || it.isChecked }
                        } else {
                            emptyList()
                        }
                        val preparedContent = when (selectedCategory) {
                            NoteCategory.SHOPPING -> note?.content ?: ""
                            else -> content
                        }
                        val preparedNote = Note(
                            id = noteId,
                            title = title,
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
                            coinCount = coinCount,
                            isCompleted = note?.isCompleted ?: false
                        )
                        onSaveClick(preparedNote)
                    },
                ) {
                    Text(stringResource(R.string.action_save))
                }
                Spacer(Modifier.weight(1f))
                onDeleteClick?.let {
                    Button(
                        onClick = { it() },
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
            }
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
            .fillMaxHeight(0.4f),
        maxLines = Int.MAX_VALUE
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
