package com.elroi.lemurloop.domain.timer

sealed class TimerEdit {
    data class Applied(val timer: CountdownTimer) : TimerEdit()
    data object Refused : TimerEdit()
}

object TimerTransitions {
    fun start(
        id: Long,
        label: String,
        durationMillis: Long,
        nowEpochMillis: Long
    ): CountdownTimer {
        val duration = durationMillis.coerceIn(0L, TimerLimits.MAX_DURATION_MILLIS)
        return CountdownTimer(
            id = id,
            label = label,
            originalDurationMillis = duration,
            endsAtEpochMillis = nowEpochMillis + duration,
            pausedRemainingMillis = null,
            phase = TimerPhase.Running,
            createdAtEpochMillis = nowEpochMillis
        )
    }

    fun remainingMillis(timer: CountdownTimer, nowEpochMillis: Long): Long {
        return when (timer.phase) {
            TimerPhase.Running -> {
                val end = timer.endsAtEpochMillis ?: return 0L
                (end - nowEpochMillis).coerceAtLeast(0L)
            }
            TimerPhase.Paused -> (timer.pausedRemainingMillis ?: 0L).coerceAtLeast(0L)
            TimerPhase.Ringing -> 0L
        }
    }

    fun pause(timer: CountdownTimer, nowEpochMillis: Long): TimerEdit {
        if (timer.phase != TimerPhase.Running) return TimerEdit.Refused
        val left = remainingMillis(timer, nowEpochMillis)
        if (left == 0L) return TimerEdit.Refused
        return TimerEdit.Applied(
            timer.copy(
                phase = TimerPhase.Paused,
                pausedRemainingMillis = left,
                endsAtEpochMillis = null
            )
        )
    }

    fun play(timer: CountdownTimer, nowEpochMillis: Long): TimerEdit {
        if (timer.phase != TimerPhase.Paused) return TimerEdit.Refused
        val left = (timer.pausedRemainingMillis ?: 0L).coerceAtLeast(0L)
        return TimerEdit.Applied(
            timer.copy(
                phase = TimerPhase.Running,
                endsAtEpochMillis = nowEpochMillis + left,
                pausedRemainingMillis = null
            )
        )
    }

    fun addOneMinute(timer: CountdownTimer, nowEpochMillis: Long): TimerEdit {
        if (timer.phase != TimerPhase.Running) return TimerEdit.Refused
        val left = remainingMillis(timer, nowEpochMillis)
        val addedLeft = roomUntilMax(left)
        val addedOriginal = roomUntilMax(timer.originalDurationMillis)
        return TimerEdit.Applied(
            timer.copy(
                originalDurationMillis = timer.originalDurationMillis + addedOriginal,
                endsAtEpochMillis = nowEpochMillis + left + addedLeft
            )
        )
    }

    fun refresh(timer: CountdownTimer): TimerEdit {
        if (timer.phase != TimerPhase.Paused) return TimerEdit.Refused
        return TimerEdit.Applied(
            timer.copy(
                phase = TimerPhase.Paused,
                pausedRemainingMillis = timer.originalDurationMillis,
                endsAtEpochMillis = null
            )
        )
    }

    fun dismiss(timer: CountdownTimer): TimerEdit {
        if (timer.phase != TimerPhase.Ringing) return TimerEdit.Refused
        return TimerEdit.Applied(
            timer.copy(
                phase = TimerPhase.Paused,
                pausedRemainingMillis = timer.originalDurationMillis,
                endsAtEpochMillis = null
            )
        )
    }

    fun duplicate(source: CountdownTimer, newId: Long, nowEpochMillis: Long): CountdownTimer {
        return start(
            id = newId,
            label = source.label,
            durationMillis = source.originalDurationMillis,
            nowEpochMillis = nowEpochMillis
        )
    }

    fun beginRing(timer: CountdownTimer): CountdownTimer {
        return timer.copy(
            phase = TimerPhase.Ringing,
            pausedRemainingMillis = null
        )
    }

    fun rename(timer: CountdownTimer, label: String): CountdownTimer {
        return timer.copy(label = label.trim().take(TimerLimits.MAX_LABEL_LENGTH))
    }

    private fun roomUntilMax(current: Long): Long {
        return (TimerLimits.ONE_MINUTE_MILLIS)
            .coerceAtMost(TimerLimits.MAX_DURATION_MILLIS - current)
            .coerceAtLeast(0L)
    }
}
