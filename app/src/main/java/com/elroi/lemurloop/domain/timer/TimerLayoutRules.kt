package com.elroi.lemurloop.domain.timer

import java.util.Locale

fun deletesWhenSwipingTowardEnd(isRtl: Boolean, deltaX: Float): Boolean {
    if (deltaX == 0f) return false
    return if (isRtl) deltaX < 0f else deltaX > 0f
}

fun keepTimerScreenOn(timers: List<CountdownTimer>): Boolean {
    return timers.any { it.phase == TimerPhase.Running }
}

fun formatCountdown(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d:%02d".format(Locale.US, hours, minutes, seconds)
}
