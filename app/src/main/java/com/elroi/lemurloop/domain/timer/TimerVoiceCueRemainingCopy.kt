package com.elroi.lemurloop.domain.timer

import kotlin.math.ceil

object TimerVoiceCueRemainingCopy {
    fun english(remainingMillis: Long): String {
        if (remainingMillis <= 0L) return "0 seconds left"
        if (remainingMillis >= 60_000L) {
            val minutes = remainingMillis / 60_000L
            return if (minutes == 1L) "1 minute left" else "$minutes minutes left"
        }
        val seconds = ceil(remainingMillis / 1000.0).toLong().coerceAtLeast(1L)
        return if (seconds == 1L) "1 second left" else "$seconds seconds left"
    }
}
