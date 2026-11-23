package com.elroi.lemurloop.domain.timer

import com.elroi.lemurloop.domain.repository.TimerRepository
import com.elroi.lemurloop.domain.scheduler.TimerScheduler

sealed class TimerCommandResult {
    data object Applied : TimerCommandResult()
    data object LimitReached : TimerCommandResult()
    data object Refused : TimerCommandResult()
    data object UndoExpired : TimerCommandResult()
}

class TimerListController(
    private val repository: TimerRepository,
    private val scheduler: TimerScheduler,
    private val ringStop: TimerRingStop,
    private val voiceCues: TimerVoiceCueCoordinatorPort,
    private val now: () -> Long
) {
    private var pendingUndo: CountdownTimer? = null
    private var undoUntilEpochMillis: Long = 0L

    fun undoAvailable(atEpochMillis: Long): Boolean {
        return pendingUndo != null && atEpochMillis < undoUntilEpochMillis
    }

    fun peekUndo(): CountdownTimer? = pendingUndo

    fun clearUndo() {
        pendingUndo = null
    }

    suspend fun start(durationMillis: Long, label: String): TimerCommandResult {
        return when (val created = TimerCreation.tryCreate(
            existingCount = repository.count(),
            id = 0L,
            label = label,
            durationMillis = durationMillis,
            nowEpochMillis = now()
        )) {
            is TimerCreation.Created -> {
                val id = repository.upsert(created.timer)
                val end = created.timer.endsAtEpochMillis ?: return TimerCommandResult.Refused
                scheduler.schedule(id, end)
                voiceCues.onTimerChanged(id, TimerVoiceCueRebuildReason.CreateOrDuplicate)
                TimerCommandResult.Applied
            }
            TimerCreation.LimitReached -> TimerCommandResult.LimitReached
            TimerCreation.DurationNotAllowed -> TimerCommandResult.Refused
        }
    }

    suspend fun pause(id: Long): TimerCommandResult =
        edit(id, TimerVoiceCueRebuildReason.CancelOnly) { TimerTransitions.pause(it, now()) }

    suspend fun play(id: Long): TimerCommandResult =
        edit(id, TimerVoiceCueRebuildReason.Resume) { TimerTransitions.play(it, now()) }

    suspend fun addOneMinute(id: Long): TimerCommandResult =
        edit(id, TimerVoiceCueRebuildReason.SilentReplan) {
            TimerTransitions.addOneMinute(it, now())
        }

    suspend fun refresh(id: Long): TimerCommandResult =
        edit(id, TimerVoiceCueRebuildReason.CancelOnly) { TimerTransitions.refresh(it) }

    suspend fun rename(id: Long, label: String): TimerCommandResult {
        val timer = repository.getById(id) ?: return TimerCommandResult.Refused
        repository.upsert(TimerTransitions.rename(timer, label))
        return TimerCommandResult.Applied
    }

    suspend fun setVoiceCueOverride(
        id: Long,
        kind: TimerVoiceCueKind,
        value: Boolean?
    ): TimerCommandResult {
        val timer = repository.getById(id) ?: return TimerCommandResult.Refused
        val updated = when (kind) {
            TimerVoiceCueKind.Start -> timer.copy(voiceCueStartOverride = value)
            TimerVoiceCueKind.MidRemaining -> timer.copy(voiceCueMidRemainingOverride = value)
            TimerVoiceCueKind.EnterLastTen -> timer.copy(voiceCueLastTenOverride = value)
        }
        repository.upsert(updated)
        voiceCues.onTimerChanged(
            id,
            if (updated.phase == TimerPhase.Running) {
                TimerVoiceCueRebuildReason.SilentReplan
            } else {
                TimerVoiceCueRebuildReason.CancelOnly
            }
        )
        return TimerCommandResult.Applied
    }

    suspend fun duplicate(id: Long): TimerCommandResult {
        if (repository.count() >= TimerLimits.MAX_TIMERS) return TimerCommandResult.LimitReached
        val source = repository.getById(id) ?: return TimerCommandResult.Refused
        val copy = TimerTransitions.duplicate(source, newId = 0L, nowEpochMillis = now())
        val newId = repository.upsert(copy)
        val end = copy.endsAtEpochMillis ?: return TimerCommandResult.Refused
        scheduler.schedule(newId, end)
        voiceCues.onTimerChanged(newId, TimerVoiceCueRebuildReason.CreateOrDuplicate)
        return TimerCommandResult.Applied
    }

    suspend fun delete(id: Long): TimerCommandResult {
        val timer = repository.getById(id) ?: return TimerCommandResult.Refused
        val anotherRinging = repository.getAll().any { it.id != id && it.phase == TimerPhase.Ringing }
        repository.delete(id)
        scheduler.cancel(id)
        voiceCues.onTimerChanged(id, TimerVoiceCueRebuildReason.CancelOnly)
        pendingUndo = timer
        undoUntilEpochMillis = now() + UNDO_WINDOW_MILLIS
        if (timer.phase == TimerPhase.Ringing && !anotherRinging) {
            ringStop.stop()
        }
        return TimerCommandResult.Applied
    }

    suspend fun undo(): TimerCommandResult {
        val snapshot = pendingUndo ?: return TimerCommandResult.Refused
        if (now() >= undoUntilEpochMillis) {
            pendingUndo = null
            return TimerCommandResult.UndoExpired
        }
        if (repository.count() >= TimerLimits.MAX_TIMERS) return TimerCommandResult.LimitReached
        repository.upsert(snapshot)
        when (val arm = TimerSchedulePlan.armRestored(snapshot, now())) {
            is TimerArm.Schedule -> scheduler.schedule(arm.timerId, arm.triggerAtEpochMillis)
            is TimerArm.Cancel -> scheduler.cancel(arm.timerId)
        }
        voiceCues.onTimerChanged(
            snapshot.id,
            if (snapshot.phase == TimerPhase.Running) {
                TimerVoiceCueRebuildReason.SilentReplan
            } else {
                TimerVoiceCueRebuildReason.CancelOnly
            }
        )
        pendingUndo = null
        return TimerCommandResult.Applied
    }

    private suspend fun edit(
        id: Long,
        voiceCueReason: TimerVoiceCueRebuildReason,
        change: (CountdownTimer) -> TimerEdit
    ): TimerCommandResult {
        val timer = repository.getById(id) ?: return TimerCommandResult.Refused
        return when (val edit = change(timer)) {
            is TimerEdit.Applied -> {
                repository.upsert(edit.timer)
                val end = edit.timer.endsAtEpochMillis
                if (edit.timer.phase == TimerPhase.Running && end != null) {
                    scheduler.schedule(edit.timer.id, end)
                } else {
                    scheduler.cancel(edit.timer.id)
                }
                voiceCues.onTimerChanged(edit.timer.id, voiceCueReason)
                TimerCommandResult.Applied
            }
            TimerEdit.Refused -> TimerCommandResult.Refused
        }
    }

    private companion object {
        const val UNDO_WINDOW_MILLIS = 5_000L
    }
}
