package com.example.mutlabocsnotes

import java.util.Calendar

fun formatDeadlineDate(millis: Long): String {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = millis
    }
    return "%02d.%02d.%04d".format(
        calendar.get(Calendar.DAY_OF_MONTH),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.YEAR)
    )
}

fun formatTaskDateTime(millis: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = millis }
    return "%02d.%02d.%04d %02d:%02d".format(
        calendar.get(Calendar.DAY_OF_MONTH),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE)
    )
}
