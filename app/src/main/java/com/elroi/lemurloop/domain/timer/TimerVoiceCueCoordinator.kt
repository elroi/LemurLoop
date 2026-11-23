package com.elroi.lemurloop.domain.timer

import com.elroi.lemurloop.domain.scheduler.TimerVoiceCueScheduler

enum class TimerVoiceCueRebuildReason {
    CreateOrDuplicate,
    Resume,
    SilentReplan,
    CancelOnly
}

interface TimerVoiceCueSpeaker {
    fun deliver(
        timerId: Long,
        kind: TimerVoiceCueKind,
        endsAtEpochMillis: Long,
        label: String
    )

    fun cancelSpeech(timerId: Long)
}

interface TimerVoiceCueCoordinatorPort {
    fun onTimerChanged(timerId: Long, reason: TimerVoiceCueRebuildReason)
    fun onVoiceGlobalsChanged()
    fun cancel(timerId: Long)
    fun cancelAll()
}

class TimerVoiceCueCoordinator(
    private val settings: () -> TimerVoiceCueGlobals,
    private val loadTimer: (Long) -> CountdownTimer?,
    private val loadRunningTimers: () -> List<CountdownTimer>,
    private val scheduler: TimerVoiceCueScheduler,
    private val speaker: TimerVoiceCueSpeaker,
    private val now: () -> Long
) : TimerVoiceCueCoordinatorPort {
    override fun onTimerChanged(timerId: Long, reason: TimerVoiceCueRebuildReason) {
        cancel(timerId)
        if (reason == TimerVoiceCueRebuildReason.CancelOnly) return

        val timer = loadTimer(timerId) ?: return
        val endsAt = timer.endsAtEpochMillis ?: return
        if (timer.phase != TimerPhase.Running) return

        val globals = settings()
        val effective = TimerVoiceCueResolver.effective(globals, timer.voiceOverrides())
        val allowStart = when (reason) {
            TimerVoiceCueRebuildReason.CreateOrDuplicate -> true
            TimerVoiceCueRebuildReason.Resume -> globals.announceOnResume
            TimerVoiceCueRebuildReason.SilentReplan,
            TimerVoiceCueRebuildReason.CancelOnly -> false
        }
        val plan = TimerVoiceCuePlan.plan(
            TimerVoiceCuePlanInput(
                nowEpochMillis = now(),
                endsAtEpochMillis = endsAt,
                effective = effective,
                allowStart = allowStart
            )
        )
        for (milestone in plan) {
            if (milestone.kind == TimerVoiceCueKind.Start) {
                speaker.deliver(timer.id, milestone.kind, endsAt, timer.label)
            } else {
                scheduler.schedule(timer.id, milestone.kind, milestone.fireAtEpochMillis)
            }
        }
    }

    override fun onVoiceGlobalsChanged() {
        loadRunningTimers().forEach { timer ->
            onTimerChanged(timer.id, TimerVoiceCueRebuildReason.SilentReplan)
        }
    }

    override fun cancel(timerId: Long) {
        scheduler.cancelAllForTimer(timerId)
        speaker.cancelSpeech(timerId)
    }

    override fun cancelAll() {
        loadRunningTimers().map { it.id }.distinct().forEach(::cancel)
    }
}
