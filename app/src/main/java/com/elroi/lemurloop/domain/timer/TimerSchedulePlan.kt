package com.elroi.lemurloop.domain.timer

data class TimerScheduleAt(
    val timerId: Long,
    val triggerAtEpochMillis: Long
)

enum class TimerReconcileKind {
    Schedule,
    Cancel,
    FinishQuietly
}

sealed class TimerArm {
    data class Schedule(val timerId: Long, val triggerAtEpochMillis: Long) : TimerArm()
    data class Cancel(val timerId: Long) : TimerArm()
}

data class TimerSchedulePlan(
    val schedule: List<TimerScheduleAt>,
    val cancel: List<Long>,
    val ringSoon: List<Long>,
    val finishQuietly: List<Long>,
    val startsRingService: Boolean = false
) {
    companion object {
        fun reconcile(
            timers: List<CountdownTimer>,
            nowEpochMillis: Long,
            ringServiceRunning: Boolean
        ): TimerSchedulePlan {
            val schedule = mutableListOf<TimerScheduleAt>()
            val cancel = mutableListOf<Long>()
            val ringSoon = mutableListOf<Long>()
            val finishQuietly = mutableListOf<Long>()

            for (timer in timers) {
                when (timer.phase) {
                    TimerPhase.Paused -> cancel += timer.id
                    TimerPhase.Ringing -> if (!ringServiceRunning) {
                        ringSoon += timer.id
                        schedule += TimerScheduleAt(timer.id, nowEpochMillis)
                    }
                    TimerPhase.Running -> classifyRunning(
                        timer = timer,
                        nowEpochMillis = nowEpochMillis,
                        schedule = schedule,
                        cancel = cancel,
                        ringSoon = ringSoon,
                        finishQuietly = finishQuietly
                    )
                }
            }

            return TimerSchedulePlan(
                schedule = schedule,
                cancel = cancel,
                ringSoon = ringSoon,
                finishQuietly = finishQuietly,
                startsRingService = false
            )
        }

        fun pauseFinished(timer: CountdownTimer): CountdownTimer {
            return timer.copy(
                phase = TimerPhase.Paused,
                pausedRemainingMillis = timer.originalDurationMillis,
                endsAtEpochMillis = null
            )
        }

        /**
         * Undo is not the unattended overdue rule. A restored running timer whose end has
         * already passed is armed at [nowEpochMillis] so it rings, however late it is.
         */
        fun armRestored(timer: CountdownTimer, nowEpochMillis: Long): TimerArm {
            if (timer.phase == TimerPhase.Paused) return TimerArm.Cancel(timer.id)
            val end = timer.endsAtEpochMillis
            return if (timer.phase == TimerPhase.Running && end != null && end > nowEpochMillis) {
                TimerArm.Schedule(timer.id, end)
            } else {
                TimerArm.Schedule(timer.id, nowEpochMillis)
            }
        }

        private fun classifyRunning(
            timer: CountdownTimer,
            nowEpochMillis: Long,
            schedule: MutableList<TimerScheduleAt>,
            cancel: MutableList<Long>,
            ringSoon: MutableList<Long>,
            finishQuietly: MutableList<Long>
        ) {
            val end = timer.endsAtEpochMillis
            if (end != null && end > nowEpochMillis) {
                schedule += TimerScheduleAt(timer.id, end)
                return
            }
            val lateBy = if (end == null) Long.MAX_VALUE else nowEpochMillis - end
            if (end != null && lateBy < TimerLimits.OVERDUE_RING_WINDOW_MILLIS) {
                ringSoon += timer.id
                schedule += TimerScheduleAt(timer.id, nowEpochMillis)
            } else {
                finishQuietly += timer.id
                cancel += timer.id
            }
        }
    }
}

object TimerClockEvents {
    const val BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
    const val TIME_SET = "android.intent.action.TIME_SET"

    fun reconciles(action: String?): Boolean {
        return action == BOOT_COMPLETED || action == TIME_SET
    }
}
