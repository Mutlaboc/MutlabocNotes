package app.homenotes.android

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Date

const val REPEAT_RULE_DROPDOWN_TEST_TAG = "repeat_rule_dropdown"
fun repeatRuleOptionTestTag(rule: RepeatRule): String = "repeat_rule_option_${rule.name}"

@Composable
internal fun DeadlinePicker(
    selectedDeadlineMillis: Long,
    todayMillis: Long,
    onDeadlineSelected: (Long) -> Unit
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
            label = {
                Text(
                    text = stringResource(R.string.deadline_label),
                    fontFamily = CozyAuth.PixelFont
                )
            },
            modifier = Modifier.weight(1f),
            readOnly = true,
            singleLine = true,
            shape = RoundedCornerShape(4.dp),
            textStyle = TextStyle(fontFamily = CozyAuth.PixelFont, color = CozyAuth.Ink),
            colors = cozyTextFieldColors(),
            interactionSource = dateFieldInteractionSource
        )
    }
}

@Composable
internal fun RecurringScheduleEditor(
    selectedStartAtMillis: Long,
    todayMillis: Long,
    durationDays: Int,
    durationHours: Int,
    repeatRule: RepeatRule,
    onStartSelected: (Long) -> Unit,
    onDurationDaysChange: (Int) -> Unit,
    onDurationHoursChange: (Int) -> Unit,
    onRepeatRuleChange: (RepeatRule) -> Unit
) {
    val context = LocalContext.current
    fun showDateTimePicker() {
        val initial = Calendar.getInstance().apply { timeInMillis = selectedStartAtMillis }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val selected = Calendar.getInstance().apply {
                            set(year, month, day, hour, minute, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onStartSelected(selected.timeInMillis)
                    },
                    initial.get(Calendar.HOUR_OF_DAY),
                    initial.get(Calendar.MINUTE),
                    true
                ).show()
            },
            initial.get(Calendar.YEAR),
            initial.get(Calendar.MONTH),
            initial.get(Calendar.DAY_OF_MONTH)
        ).apply { datePicker.minDate = todayMillis }.show()
    }

    Column {
        OutlinedButton(onClick = ::showDateTimePicker, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(
                    R.string.first_task_start_value,
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                        .format(Date(selectedStartAtMillis))
                )
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            DurationStepper(
                label = stringResource(R.string.duration_days),
                value = durationDays,
                onDecrease = {
                    if (durationDays > 1 || (durationDays == 1 && durationHours > 0)) {
                        onDurationDaysChange(durationDays - 1)
                    }
                },
                onIncrease = { onDurationDaysChange(durationDays + 1) },
                modifier = Modifier.weight(1f)
            )
            DurationStepper(
                label = stringResource(R.string.duration_hours),
                value = durationHours,
                onDecrease = { if (durationDays > 0 || durationHours > 1) onDurationHoursChange(durationHours - 1) },
                onIncrease = { if (durationHours < 23) onDurationHoursChange(durationHours + 1) },
                modifier = Modifier.weight(1f)
            )
        }
        RepeatRuleDropdown(repeatRule, onRepeatRuleChange)
    }
}

@Composable
private fun DurationStepper(
    label: String,
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrease) { Icon(Icons.Default.Remove, contentDescription = "$label -") }
            Text(value.toString())
            IconButton(onClick = onIncrease) { Icon(Icons.Default.Add, contentDescription = "$label +") }
        }
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
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(2.dp, CozyAuth.InputBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = CozyAuth.FieldCream,
                contentColor = CozyAuth.TerracottaDark
            ),
            modifier = Modifier.testTag(REPEAT_RULE_DROPDOWN_TEST_TAG)
        ) {
            Text(
                text = stringResource(R.string.repeat_label, stringResource(repeatRule.labelRes())),
                fontFamily = CozyAuth.PixelFont,
                fontSize = 14.sp
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(CozyAuth.CardCream)
        ) {
            RepeatRule.values().filter { it != RepeatRule.NONE }.forEach { rule ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onRepeatRuleChange(rule)
                    },
                    modifier = Modifier.testTag(repeatRuleOptionTestTag(rule))
                ) {
                    Text(
                        text = stringResource(rule.labelRes()),
                        color = CozyAuth.Ink,
                        fontFamily = CozyAuth.PixelFont
                    )
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
