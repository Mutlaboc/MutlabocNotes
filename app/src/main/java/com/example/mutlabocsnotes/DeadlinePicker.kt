package com.example.mutlabocsnotes

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.Calendar

const val REPEAT_RULE_DROPDOWN_TEST_TAG = "repeat_rule_dropdown"
fun repeatRuleOptionTestTag(rule: RepeatRule): String = "repeat_rule_option_${rule.name}"

@Composable
internal fun DeadlinePicker(
    selectedDeadlineMillis: Long,
    todayMillis: Long,
    repeatRule: RepeatRule,
    onDeadlineSelected: (Long) -> Unit,
    onRepeatRuleChange: (RepeatRule) -> Unit
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
        RepeatRuleDropdown(
            repeatRule = repeatRule,
            onRepeatRuleChange = onRepeatRuleChange
        )
    }
}

@Composable
private fun RepeatRuleDropdown(
    repeatRule: RepeatRule,
    onRepeatRuleChange: (RepeatRule) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag(REPEAT_RULE_DROPDOWN_TEST_TAG)
        ) {
            Text(stringResource(R.string.repeat_label, stringResource(repeatRule.labelRes())))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            RepeatRule.values().forEach { rule ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onRepeatRuleChange(rule)
                    },
                    modifier = Modifier.testTag(repeatRuleOptionTestTag(rule))
                ) {
                    Text(stringResource(rule.labelRes()))
                }
            }
        }
    }
}

internal fun RepeatRule.labelRes(): Int =
    when (this) {
        RepeatRule.NONE -> R.string.repeat_rule_none
        RepeatRule.DAILY -> R.string.repeat_rule_daily
        RepeatRule.WEEKLY -> R.string.repeat_rule_weekly
        RepeatRule.MONTHLY -> R.string.repeat_rule_monthly
    }
