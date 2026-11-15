package com.elroi.lemurloop.domain.timer

object TimerCreation {
    fun tryCreate(
        existingCount: Int,
        id: Long,
        label: String,
        durationMillis: Long,
        nowEpochMillis: Long
    ): Outcome {
        if (existingCount >= TimerLimits.MAX_TIMERS) return LimitReached
        if (durationMillis < TimerLimits.MIN_DURATION_MILLIS) return DurationNotAllowed
        return Created(
            TimerTransitions.start(
                id = id,
                label = label,
                durationMillis = durationMillis.coerceAtMost(TimerLimits.MAX_DURATION_MILLIS),
                nowEpochMillis = nowEpochMillis
            )
        )
    }

    sealed class Outcome
    data class Created(val timer: CountdownTimer) : Outcome()
    data object LimitReached : Outcome()
    data object DurationNotAllowed : Outcome()
}

object TimerUndo {
    fun restore(existingCount: Int, deleted: CountdownTimer): Outcome {
        if (existingCount >= TimerLimits.MAX_TIMERS) return LimitReached
        return Restored(deleted)
    }

    sealed class Outcome
    data class Restored(val timer: CountdownTimer) : Outcome()
    data object LimitReached : Outcome()
}

object TimerDuration {
    fun millis(hours: Int, minutes: Int, seconds: Int): Long {
        val total = hours.coerceAtLeast(0) * 3_600_000L +
            minutes.coerceAtLeast(0) * 60_000L +
            seconds.coerceAtLeast(0) * 1_000L
        return total.coerceAtMost(TimerLimits.MAX_DURATION_MILLIS)
    }

    fun parts(millis: Long): Parts {
        val totalSeconds = millis.coerceIn(0L, TimerLimits.MAX_DURATION_MILLIS) / 1_000L
        val hours = (totalSeconds / 3_600L).toInt()
        val minutes = ((totalSeconds % 3_600L) / 60L).toInt()
        val seconds = (totalSeconds % 60L).toInt()
        return Parts(hours = hours, minutes = minutes, seconds = seconds)
    }

    data class Parts(val hours: Int, val minutes: Int, val seconds: Int)

    val presets: List<Long> = listOf(60_000L, 5 * 60_000L, 10 * 60_000L)

    fun canStart(millis: Long): Boolean = millis >= TimerLimits.MIN_DURATION_MILLIS
}

object TimerOrdering {
    fun newestFirst(timers: List<CountdownTimer>): List<CountdownTimer> {
        return timers.sortedByDescending { it.createdAtEpochMillis }
    }
}
