package com.elroi.lemurloop.util

import android.content.Context
import android.content.res.Resources
import com.elroi.lemurloop.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Localized previews matching built-in accountability lifecycle SMS templates (SMS footer is omitted; added only when sending). */
object BuddyLifecycleSmsPreview {

    fun builtInHeadsUp(
        context: Context,
        userName: String?,
        alarmLabel: String?,
        time: LocalTime,
        daysOfWeek: List<Int>,
    ): String {
        val res = context.resources
        val who = userName?.trim()?.takeIf { it.isNotBlank() }
            ?: res.getString(R.string.buddy_invite_default_user)
        val label = alarmLabel?.trim()?.takeIf { it.isNotBlank() }
            ?: res.getString(R.string.alarm_default_label)
        val timeStr = formatTime(time)
        val repeatStr = formatRepeat(context, daysOfWeek)
        return res.getString(R.string.buddy_lifecycle_alarm_set, who, label, timeStr, repeatStr)
    }

    fun builtInScheduleChanged(
        context: Context,
        userName: String?,
        alarmLabel: String?,
        time: LocalTime,
        daysOfWeek: List<Int>,
    ): String {
        val res = context.resources
        val who = userName?.trim()?.takeIf { it.isNotBlank() }
            ?: res.getString(R.string.buddy_invite_default_user)
        val label = alarmLabel?.trim()?.takeIf { it.isNotBlank() }
            ?: res.getString(R.string.alarm_default_label)
        val timeStr = formatTime(time)
        val repeatStr = formatRepeat(context, daysOfWeek)
        return res.getString(R.string.buddy_lifecycle_alarm_changed, who, label, timeStr, repeatStr)
    }

    fun builtInDismissed(context: Context, userName: String?, alarmLabel: String?): String {
        val res = context.resources
        val who = userName?.trim()?.takeIf { it.isNotBlank() }
            ?: res.getString(R.string.buddy_invite_default_user)
        val label = alarmLabel?.trim()?.takeIf { it.isNotBlank() }
            ?: res.getString(R.string.alarm_default_label)
        return res.getString(R.string.buddy_lifecycle_alarm_dismissed, who, label)
    }

    private fun formatTime(time: LocalTime): String {
        val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(Locale.getDefault())
        return formatter.format(time)
    }

    private fun formatRepeat(context: Context, daysOfWeek: List<Int>): String {
        val res = context.resources
        if (daysOfWeek.isEmpty()) {
            return res.getString(R.string.buddy_lifecycle_repeat_once)
        }
        return daysOfWeek.sorted().map { isoDay ->
            dayAbbrev(res, isoDay)
        }.joinToString(", ")
    }

    private fun dayAbbrev(res: Resources, isoDay: Int): String = when (isoDay) {
        1 -> res.getString(R.string.day_mon)
        2 -> res.getString(R.string.day_tue)
        3 -> res.getString(R.string.day_wed)
        4 -> res.getString(R.string.day_thu)
        5 -> res.getString(R.string.day_fri)
        6 -> res.getString(R.string.day_sat)
        7 -> res.getString(R.string.day_sun)
        else -> "?"
    }
}
