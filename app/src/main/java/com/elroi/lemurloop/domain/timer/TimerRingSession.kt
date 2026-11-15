package com.elroi.lemurloop.domain.timer

data class TimerVibrationStep(
    val onMillis: Long,
    val offMillis: Long
)

object TimerVibrationRhythm {
    fun steps(vibration: TimerVibration): List<TimerVibrationStep> {
        return when (vibration) {
            TimerVibration.Off -> emptyList()
            TimerVibration.Light -> listOf(
                TimerVibrationStep(onMillis = 200, offMillis = 100),
                TimerVibrationStep(onMillis = 200, offMillis = 400)
            )
            TimerVibration.Medium -> listOf(
                TimerVibrationStep(onMillis = 800, offMillis = 400)
            )
            TimerVibration.Strong -> listOf(
                TimerVibrationStep(onMillis = 80, offMillis = 80),
                TimerVibrationStep(onMillis = 80, offMillis = 80),
                TimerVibrationStep(onMillis = 80, offMillis = 400)
            )
        }
    }

    fun waveform(vibration: TimerVibration): LongArray {
        val beats = steps(vibration)
        if (beats.isEmpty()) return longArrayOf()
        val timings = ArrayList<Long>(1 + beats.size * 2)
        timings += 0L
        for (beat in beats) {
            timings += beat.onMillis
            timings += beat.offMillis
        }
        return timings.toLongArray()
    }
}

sealed class TimerPlaybackChoice {
    data object Silent : TimerPlaybackChoice()
    data object AlarmRingtone : TimerPlaybackChoice()
    data class PhoneTrack(val uri: String) : TimerPlaybackChoice()
}

object TimerPlayback {
    fun resolve(settings: TimerSettings, trackOpens: Boolean): TimerPlaybackChoice {
        return when (settings.sound) {
            TimerSound.Silent -> TimerPlaybackChoice.Silent
            TimerSound.DeviceAlarmRingtone -> TimerPlaybackChoice.AlarmRingtone
            TimerSound.PhoneTrack -> {
                val uri = settings.phoneTrackUri
                if (trackOpens && !uri.isNullOrBlank()) {
                    TimerPlaybackChoice.PhoneTrack(uri)
                } else {
                    TimerPlaybackChoice.AlarmRingtone
                }
            }
        }
    }

    fun playerGain(elapsedMillis: Long, rampMillis: Long): Float {
        if (rampMillis <= 0L) return 1f
        if (elapsedMillis <= 0L) return 0f
        if (elapsedMillis >= rampMillis) return 1f
        return elapsedMillis.toFloat() / rampMillis.toFloat()
    }
}

object TimerVolumeKeys {
    const val UP = 24
    const val DOWN = 25
    const val MUTE = 164

    fun consumes(allowChanges: Boolean, keyCode: Int): Boolean {
        if (allowChanges) return false
        return keyCode == UP || keyCode == DOWN || keyCode == MUTE
    }
}

object TimerRingNotification {
    const val ID = 221
    const val CHANNEL_ID = "timer_ring"
}

object TimerRingDismiss {
    fun paused(timers: List<CountdownTimer>): List<CountdownTimer> {
        return timers.mapNotNull { timer ->
            when (val edit = TimerTransitions.dismiss(timer)) {
                is TimerEdit.Applied -> edit.timer
                TimerEdit.Refused -> null
            }
        }
    }
}

class TimerRingHandler(
    private val foreground: () -> Unit,
    private val readSettings: () -> TimerSettings,
    private val trackOpens: (String?) -> Boolean,
    private val loadTimer: (Long) -> CountdownTimer?,
    private val saveTimers: (List<CountdownTimer>) -> Unit,
    private val loadRinging: () -> List<CountdownTimer>,
    private val startPlayback: (TimerPlaybackChoice) -> Unit,
    private val startVibration: (TimerVibration) -> Unit,
    private val startRamp: (Long) -> Unit,
    private val restoreLeftover: (Int) -> Unit,
    private val captureVolume: (Int) -> Unit,
    private val restoreCaptured: (Int) -> Unit,
    private val openScreen: () -> Unit,
    private val stopPlayback: () -> Unit,
    private val stopVibration: () -> Unit,
    private val stopService: () -> Unit,
    private val cancelAlarm: (Long) -> Unit
) {
    var sessionOpen: Boolean = false

    fun onFire(timerId: Long) {
        foreground()
        if (sessionOpen) {
            val timer = loadTimer(timerId) ?: return
            saveTimers(listOf(TimerTransitions.beginRing(timer)))
            cancelAlarm(timerId)
            return
        }
        val timer = loadTimer(timerId)
        if (timer == null) {
            stopService()
            return
        }
        val settings = readSettings()
        sessionOpen = true
        saveTimers(listOf(TimerTransitions.beginRing(timer)))
        cancelAlarm(timerId)
        val trackAvailable = if (settings.sound == TimerSound.PhoneTrack) {
            trackOpens(settings.phoneTrackUri)
        } else {
            false
        }
        val playback = TimerPlayback.resolve(settings, trackAvailable)
        if (playback !is TimerPlaybackChoice.Silent && settings.prioritizeVolume) {
            settings.capturedAlarmVolume?.let(restoreLeftover)
            captureVolume(settings.volumePercent)
        }
        if (playback !is TimerPlaybackChoice.Silent) {
            startPlayback(playback)
            if (settings.rampMinutes > 0) {
                startRamp(settings.rampMinutes * 60_000L)
            }
        }
        if (settings.vibration != TimerVibration.Off) {
            startVibration(settings.vibration)
        }
        openScreen()
    }

    fun onDismiss() {
        foreground()
        val settings = readSettings()
        saveTimers(TimerRingDismiss.paused(loadRinging()))
        stopPlayback()
        stopVibration()
        settings.capturedAlarmVolume?.let(restoreCaptured)
        sessionOpen = false
        stopService()
    }
}
