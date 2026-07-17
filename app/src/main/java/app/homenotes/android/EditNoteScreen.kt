package app.homenotes.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
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
import androidx.compose.ui.unit.dp
import java.util.Calendar

const val EDIT_NOTE_TITLE_FIELD_TEST_TAG = "edit_note_title_field"
const val EDIT_NOTE_TITLE_ERROR_TEST_TAG = "edit_note_title_error"
const val EDIT_NOTE_CONTENT_FIELD_TEST_TAG = "edit_note_content_field"
const val EDIT_NOTE_SAVE_BUTTON_TEST_TAG = "edit_note_save_button"
const val EDIT_NOTE_DELETE_BUTTON_TEST_TAG = "edit_note_delete_button"
const val EDIT_NOTE_DELETE_CONFIRM_BUTTON_TEST_TAG = "edit_note_delete_confirm_button"
const val EDIT_NOTE_CHECKLIST_ADD_BUTTON_TEST_TAG = "edit_note_checklist_add_button"

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
    selectedStartAtMillis: Long = selectedDeadlineMillis,
    durationDays: Int = 0,
    durationHours: Int = 1,
    repeatRule: RepeatRule
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
        startAtMillis = if (selectedCategory == NoteCategory.RECURRING_TASKS) {
            selectedStartAtMillis
        } else null,
        durationMinutes = if (selectedCategory == NoteCategory.RECURRING_TASKS) {
            (durationDays * 24L + durationHours) * 60L
        } else null,
        repeatRule = if (selectedCategory == NoteCategory.RECURRING_TASKS) {
            repeatRule
        } else {
            RepeatRule.NONE
        },
        coinCount = originalNote?.coinCount ?: 0,
        isCompleted = originalNote?.isCompleted ?: false
    )
}

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
        mutableStateOf(note?.category ?: NoteCategory.TASKS)
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
    var selectedStartAtMillis by remember(noteId) {
        mutableStateOf(note?.startAtMillis ?: defaultDeadline)
    }
    val initialDurationHours = ((note?.durationMinutes ?: 60L) / 60L).coerceAtLeast(1L)
    var durationDays by remember(noteId) { mutableStateOf((initialDurationHours / 24L).toInt()) }
    var durationHours by remember(noteId) { mutableStateOf((initialDurationHours % 24L).toInt()) }
    var repeatRule by remember(noteId) {
        mutableStateOf(note?.repeatRule?.takeIf { it != RepeatRule.NONE } ?: RepeatRule.DAILY)
    }
    var isTitleError by remember(noteId) { mutableStateOf(false) }
    var showDeleteDialog by remember(noteId) { mutableStateOf(false) }

    Scaffold(
        backgroundColor = CozyAuth.Cream,
        topBar = {
            CozyTopBar(
                title = stringResource(
                    if (note == null) R.string.create_note_title else R.string.edit_note_title
                )
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(CozyAuth.Cream)
                .pixelScreenFrame()
        ) {
            val showQuestScene = note == null
            val sceneHeight = if (maxHeight < 720.dp) 112.dp else 164.dp

            if (showQuestScene) {
                CreateNoteQuestScene(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(sceneHeight)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (showQuestScene) {
                            Modifier
                                .imePadding()
                                .navigationBarsPadding()
                        } else {
                            Modifier
                        }
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(
                        top = 16.dp,
                        bottom = if (showQuestScene) sceneHeight + 16.dp else 16.dp
                    )
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
                            onDeadlineSelected = { selectedDeadlineMillis = it },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NoteContentField(
                            content = content,
                            onContentChange = { content = it }
                        )
                    }

                    NoteCategory.RECURRING_TASKS -> {
                        RecurringScheduleEditor(
                            selectedStartAtMillis = selectedStartAtMillis,
                            todayMillis = todayCalendar.timeInMillis,
                            durationDays = durationDays,
                            durationHours = durationHours,
                            repeatRule = repeatRule,
                            onStartSelected = { selectedStartAtMillis = it },
                            onDurationDaysChange = { durationDays = it.coerceAtLeast(0) },
                            onDurationHoursChange = { newHours ->
                                durationHours = newHours.coerceIn(0, 23)
                                if (durationDays == 0 && durationHours == 0) durationHours = 1
                            },
                            onRepeatRuleChange = { repeatRule = it }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        NoteContentField(
                            content = content,
                            onContentChange = { content = it }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    PixelPrimaryButton(
                        text = stringResource(R.string.action_save),
                        onClick = {
                            val preparedNote = prepareNoteForSave(
                                noteId = noteId,
                                originalNote = note,
                                title = title,
                                content = content,
                                selectedCategory = selectedCategory,
                                checklistItems = checklistItems,
                                selectedDeadlineMillis = selectedDeadlineMillis,
                                selectedStartAtMillis = selectedStartAtMillis,
                                durationDays = durationDays,
                                durationHours = durationHours,
                                repeatRule = repeatRule
                            )
                            if (preparedNote == null) {
                                isTitleError = true
                                return@PixelPrimaryButton
                            }
                            onSaveClick(preparedNote)
                        },
                        modifier = Modifier.testTag(EDIT_NOTE_SAVE_BUTTON_TEST_TAG)
                    )
                    Spacer(Modifier.weight(1f))
                    if (note != null && onDeleteClick != null) {
                        PixelOutlineButton(
                            text = stringResource(R.string.action_delete),
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.testTag(EDIT_NOTE_DELETE_BUTTON_TEST_TAG)
                        )
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
