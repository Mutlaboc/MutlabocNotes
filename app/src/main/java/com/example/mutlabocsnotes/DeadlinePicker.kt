package com.example.mutlabocsnotes

import android.app.DatePickerDialog
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.Calendar

@Composable
internal fun DeadlinePicker(
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
        formatDeadlineDate(selectedDeadlineMillis)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = deadlineText,
            onValueChange = {},
            label = { Text(stringResource(R.string.deadline_label)) },
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
