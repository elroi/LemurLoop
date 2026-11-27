package com.elroi.lemurloop.domain.timer

enum class TimerVoiceCueKind { Start, MidRemaining, EnterLastTen }

data class TimerVoiceCueMilestone(
    val kind: TimerVoiceCueKind,
    val fireAtEpochMillis: Long
)

data class TimerVoiceCuePlanInput(
    val nowEpochMillis: Long,
    val endsAtEpochMillis: Long,
    val effective: TimerVoiceCueEffective,
    val allowStart: Boolean
)

object TimerVoiceCuePlan {
    fun plan(input: TimerVoiceCuePlanInput): List<TimerVoiceCueMilestone> {
        val remaining = input.endsAtEpochMillis - input.nowEpochMillis
        if (remaining <= 0L) return emptyList()

        val out = ArrayList<TimerVoiceCueMilestone>(3)

        val lastTenWanted = input.effective.lastTen
        val startWanted = input.allowStart && input.effective.start
        val startSuppressedByLastTen = startWanted && lastTenWanted &&
            remaining <= TimerVoiceCueBudget.LAST_TEN_WINDOW_MILLIS +
            TimerVoiceCueBudget.START_SPEECH_BUDGET_MILLIS

        if (startWanted && !startSuppressedByLastTen) {
            out += TimerVoiceCueMilestone(TimerVoiceCueKind.Start, input.nowEpochMillis)
        }

        if (input.effective.midRemaining) {
            val midAt = input.nowEpochMillis + remaining / 2L
            val lastTenStart = input.endsAtEpochMillis - TimerVoiceCueBudget.LAST_TEN_WINDOW_MILLIS
            val midDeadline =
                lastTenStart - TimerVoiceCueBudget.MID_CLEARANCE_BEFORE_LAST_TEN_MILLIS
            if (midAt > input.nowEpochMillis && midAt < midDeadline) {
                out += TimerVoiceCueMilestone(TimerVoiceCueKind.MidRemaining, midAt)
            }
        }

        if (lastTenWanted) {
            // Fire early so the service and TTS engine are warm before "10" is due.
            val lead = TimerVoiceCueBudget.LAST_TEN_WINDOW_MILLIS +
                TimerVoiceCueBudget.LAST_TEN_WARMUP_LEAD_MILLIS
            val enterAt = if (remaining <= lead) {
                input.nowEpochMillis
            } else {
                input.endsAtEpochMillis - lead
            }
            out += TimerVoiceCueMilestone(TimerVoiceCueKind.EnterLastTen, enterAt)
        }

        return out.sortedWith(
            compareBy<TimerVoiceCueMilestone> { it.fireAtEpochMillis }
                .thenBy { it.kind.ordinal }
        )
    }
}
