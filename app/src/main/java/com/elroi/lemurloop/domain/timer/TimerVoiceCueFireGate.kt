package com.elroi.lemurloop.domain.timer

import kotlin.math.ceil

object TimerVoiceCueFireGate {
    fun shouldSpeak(
        nowEpochMillis: Long,
        scheduledFireEpochMillis: Long,
        endsAtEpochMillis: Long,
        phaseIsRunning: Boolean,
        endsAtUnchanged: Boolean,
        cueStillEffective: Boolean
    ): Boolean {
        if (!phaseIsRunning || !endsAtUnchanged || !cueStillEffective) return false
        if (nowEpochMillis >= endsAtEpochMillis) return false
        return nowEpochMillis <=
            scheduledFireEpochMillis + TimerVoiceCueBudget.FIRE_LATE_SKEW_MILLIS
    }

    fun lastTenDigitsToSpeak(
        nowEpochMillis: Long,
        endsAtEpochMillis: Long
    ): List<Int> {
        val remaining = endsAtEpochMillis - nowEpochMillis
        if (remaining <= 0L) return emptyList()

        val top = ceil(remaining / 1_000.0).toInt().coerceIn(1, 10)
        return (top downTo 1).filter { digit ->
            val idealEpochMillis = endsAtEpochMillis - digit * 1_000L
            nowEpochMillis <=
                idealEpochMillis + TimerVoiceCueBudget.LAST_TEN_DIGIT_SKEW_MILLIS
        }
    }
}
