package com.elroi.lemurloop.domain.timer

enum class TimerPhase {
    Running,
    Paused,
    Ringing
}

data class CountdownTimer(
    val id: Long,
    val label: String,
    val originalDurationMillis: Long,
    val endsAtEpochMillis: Long?,
    val pausedRemainingMillis: Long?,
    val phase: TimerPhase,
    val createdAtEpochMillis: Long,
    val voiceCueStartOverride: Boolean? = null,
    val voiceCueMidRemainingOverride: Boolean? = null,
    val voiceCueLastTenOverride: Boolean? = null
)

fun CountdownTimer.voiceOverrides(): TimerVoiceCueOverrides {
    return TimerVoiceCueOverrides(
        start = voiceCueStartOverride,
        midRemaining = voiceCueMidRemainingOverride,
        lastTen = voiceCueLastTenOverride
    )
}

object TimerLimits {
    const val MAX_TIMERS = 20
    const val MAX_LABEL_LENGTH = 40
    const val MIN_DURATION_MILLIS = 1_000L
    const val ONE_MINUTE_MILLIS = 60_000L
    const val OVERDUE_RING_WINDOW_MILLIS = 60_000L

    /** 99:59:59. A timer never stores a longer original duration or time left. */
    const val MAX_DURATION_MILLIS: Long = (99 * 60 * 60 + 59 * 60 + 59) * 1_000L
}
