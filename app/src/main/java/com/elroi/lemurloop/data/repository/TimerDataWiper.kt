package com.elroi.lemurloop.data.repository

import com.elroi.lemurloop.domain.repository.TimerRepository
import com.elroi.lemurloop.domain.repository.TimerSettingsRepository
import com.elroi.lemurloop.domain.scheduler.TimerScheduler
import com.elroi.lemurloop.domain.timer.TimerRingStop
import com.elroi.lemurloop.domain.timer.TimerTrackGrantReleaser
import com.elroi.lemurloop.domain.timer.TimerVoiceCueCoordinatorPort

class TimerDataWiper(
    private val timerRepository: TimerRepository,
    private val timerSettingsRepository: TimerSettingsRepository,
    private val timerScheduler: TimerScheduler,
    private val timerVoiceCues: TimerVoiceCueCoordinatorPort,
    private val timerRingStop: TimerRingStop,
    private val timerTrackGrantReleaser: TimerTrackGrantReleaser
) {
    suspend fun wipe() {
        val settings = timerSettingsRepository.get()
        val timers = timerRepository.getAll()
        timerVoiceCues.cancelAll()
        for (timer in timers) {
            timerScheduler.cancel(timer.id)
        }
        timerRingStop.stop()
        timerRepository.deleteAll()
        timerSettingsRepository.clear()
        timerTrackGrantReleaser.release(settings.phoneTrackUri)
    }
}
