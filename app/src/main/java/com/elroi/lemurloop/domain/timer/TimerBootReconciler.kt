package com.elroi.lemurloop.domain.timer

import com.elroi.lemurloop.domain.repository.TimerRepository
import com.elroi.lemurloop.domain.scheduler.TimerScheduler

class TimerBootReconciler(
    private val repository: TimerRepository,
    private val scheduler: TimerScheduler,
    private val notifier: TimerFinishedNotifier,
    private val ringRunning: () -> Boolean,
    private val voiceCues: TimerVoiceCueCoordinatorPort
) {
    suspend fun reconcile(nowEpochMillis: Long) {
        val timers = repository.getAll()
        val plan = TimerSchedulePlan.reconcile(
            timers = timers,
            nowEpochMillis = nowEpochMillis,
            ringServiceRunning = ringRunning()
        )
        check(!plan.startsRingService) { "Timer reconcile must not start the ring service." }
        for (item in plan.schedule) {
            scheduler.schedule(item.timerId, item.triggerAtEpochMillis)
        }
        for (id in plan.cancel) {
            scheduler.cancel(id)
        }
        val byId = timers.associateBy { it.id }
        for (id in plan.finishQuietly) {
            val timer = byId[id] ?: continue
            val paused = TimerSchedulePlan.pauseFinished(timer)
            repository.upsert(paused)
            notifier.notifyFinished(paused)
        }
        repository.getAll()
            .filter { it.phase == TimerPhase.Running }
            .forEach {
                voiceCues.onTimerChanged(it.id, TimerVoiceCueRebuildReason.SilentReplan)
            }
    }
}
