package com.daedalusapps.echo.ui.screens

/** Preset lookback options shown in the "update from recordings" dialog. */
data class LookbackOption(val hours: Long, val label: String)

val LOOKBACK_OPTIONS = listOf(
    LookbackOption(24L, "최근 24시간"),
    LookbackOption(72L, "최근 3일"),
    LookbackOption(168L, "지난 1주"),
    LookbackOption(-1L, "모든 녹음")
)

const val TODO_LOOKBACK_HOURS_KEY = "todo_lookback_hours"
const val TODO_LOOKBACK_HOURS_DEFAULT = 72L

sealed class LookbackSelection {
    data class Standard(val hours: Long) : LookbackSelection()
    data class Custom(val hours: Long) : LookbackSelection()
}

fun lookbackOptionFor(hours: Long): LookbackSelection {
    return if (LOOKBACK_OPTIONS.any { it.hours == hours }) LookbackSelection.Standard(hours)
    else LookbackSelection.Custom(hours)
}
