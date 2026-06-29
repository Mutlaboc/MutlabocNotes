package com.example.mutlabocsnotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
internal fun CategoryPicker(
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
            val categoryColors = noteCategoryColors(category)
            val modifier = Modifier
                .padding(end = 8.dp)
                .testTag(editNoteCategoryChipTestTag(category))
            if (selectedCategory == category) {
                Button(
                    onClick = { onCategorySelected(category) },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = categoryColors.container,
                        contentColor = categoryColors.content
                    ),
                    modifier = modifier
                ) {
                    Text(stringResource(labelResId), fontFamily = CozyAuth.PixelFont)
                }
            } else {
                OutlinedButton(
                    onClick = { onCategorySelected(category) },
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = categoryColors.content
                    ),
                    modifier = modifier
                ) {
                    Text(stringResource(labelResId), fontFamily = CozyAuth.PixelFont)
                }
            }
        }
    }
}

@Composable
internal fun ChecklistEditor(
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
                    onCheckedChange = { checked -> onItemCheckedChange(index, checked) },
                    colors = cozyCheckboxColors()
                )
                OutlinedTextField(
                    value = item.text,
                    onValueChange = { text -> onItemTextChange(index, text) },
                    singleLine = true,
                    shape = RoundedCornerShape(4.dp),
                    textStyle = TextStyle(fontFamily = CozyAuth.PixelFont, color = CozyAuth.Ink),
                    colors = cozyTextFieldColors(),
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
                        contentDescription = stringResource(R.string.checklist_delete_item),
                        tint = CozyAuth.InkSoft
                    )
                }
            }
        }
        PixelPrimaryButton(
            text = stringResource(R.string.checklist_add_item),
            onClick = onAddItem,
            modifier = Modifier.testTag(EDIT_NOTE_CHECKLIST_ADD_BUTTON_TEST_TAG)
        )
    }
}

@Composable
internal fun cozyCheckboxColors() = CheckboxDefaults.colors(
    checkedColor = CozyAuth.Terracotta,
    uncheckedColor = CozyAuth.InputBorder,
    checkmarkColor = CozyAuth.FieldCream
)
