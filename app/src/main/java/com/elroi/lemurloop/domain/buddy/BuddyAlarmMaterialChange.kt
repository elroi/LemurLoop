package com.elroi.lemurloop.domain.buddy

import com.elroi.lemurloop.domain.model.Alarm

/**
 * Buddy-facing "alarm changed" SMS should fire only when schedule expectations change.
 * See plan: time, repeat days, and enabled state are material; label and buddy delay are not.
 */
fun hasMaterialBuddyRelevantScheduleChange(before: Alarm, after: Alarm): Boolean {
    if (before.time != after.time) return true
    if (before.isEnabled != after.isEnabled) return true
    return before.daysOfWeek.sorted() != after.daysOfWeek.sorted()
}
