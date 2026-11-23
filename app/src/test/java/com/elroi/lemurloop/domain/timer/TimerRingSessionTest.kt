package com.elroi.lemurloop.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerRingSessionTest {

    @Test
    fun beginRing_keepsCreatedAtAndOriginalDuration() {
        val timer = running(id = 4L, createdAt = 15L, original = 5 * 60_000L)
        val ringing = TimerTransitions.beginRing(timer)

        assertEquals(TimerPhase.Ringing, ringing.phase)
        assertEquals(15L, ringing.createdAtEpochMillis)
        assertEquals(5 * 60_000L, ringing.originalDurationMillis)
        assertNull(ringing.pausedRemainingMillis)
    }

    @Test
    fun fire_newSession_readsSettingsOnlyAfterForeground() {
        val rig = Rig()
        rig.handler.onFire(4L)

        assertEquals("foreground", rig.events.first())
        assertTrue(rig.events.indexOf("settings") > rig.events.indexOf("foreground"))
        assertTrue(rig.events.contains("playback:AlarmRingtone"))
        assertTrue(rig.events.contains("ramp:60000"))
        assertTrue(rig.events.contains("vibration:BASIC"))
        assertTrue(rig.events.contains("openScreen"))
        assertEquals(TimerPhase.Ringing, rig.timers.getValue(4L).phase)
    }

    @Test
    fun fire_secondTimer_joinsWithoutRestartingTheSession() {
        val rig = Rig()
        rig.handler.onFire(4L)
        rig.events.clear()
        rig.timers[9L] = running(id = 9L)

        rig.handler.onFire(9L)

        assertEquals(
            listOf("foreground", "cancelVoice:9", "load", "save", "cancel:9"),
            rig.events
        )
        assertEquals(TimerPhase.Ringing, rig.timers.getValue(9L).phase)
        assertTrue(rig.handler.sessionOpen)
    }

    @Test
    fun fire_silentWithVibration_skipsPlaybackAndStillVibrates() {
        val rig = Rig(settings = TimerSettings(sound = TimerSound.Silent, vibration = TimerVibration.Light))
        rig.handler.onFire(4L)

        assertFalse(rig.events.any { it.startsWith("playback:") })
        assertFalse(rig.events.any { it.startsWith("ramp:") })
        assertFalse(rig.events.any { it.startsWith("capture:") })
        assertTrue(rig.events.contains("vibration:RAPID_PULSE"))
        assertTrue(rig.events.contains("openScreen"))
    }

    @Test
    fun fire_silentWithoutVibration_staysQuiet() {
        val rig = Rig(
            settings = TimerSettings(
                sound = TimerSound.Silent,
                vibration = TimerVibration.Off,
                rampMinutes = 1
            )
        )
        rig.handler.onFire(4L)

        assertFalse(rig.events.any { it.startsWith("playback:") || it.startsWith("ramp:") || it.startsWith("vibration:") })
        assertTrue(rig.events.contains("openScreen"))
    }

    @Test
    fun fire_phoneTrackThatWillNotOpen_usesTheAlarmRingtone() {
        val rig = Rig(
            settings = TimerSettings(sound = TimerSound.PhoneTrack, phoneTrackUri = "content://tracks/missing"),
            trackOpens = false
        )
        rig.handler.onFire(4L)

        assertTrue(rig.events.contains("playback:AlarmRingtone"))
        assertFalse(rig.events.any { it.startsWith("playback:PhoneTrack") })
    }

    @Test
    fun fire_phoneTrackThatOpens_usesThatTrack() {
        val rig = Rig(
            settings = TimerSettings(sound = TimerSound.PhoneTrack, phoneTrackUri = "content://tracks/1"),
            trackOpens = true
        )
        rig.handler.onFire(4L)

        assertTrue(rig.events.contains("playback:PhoneTrack:content://tracks/1"))
    }

    @Test
    fun fire_prioritizeVolume_restoresALeftoverLevelBeforeCapturing() {
        val rig = Rig(settings = TimerSettings(volumePercent = 40, capturedAlarmVolume = 7, prioritizeVolume = true))
        rig.handler.onFire(4L)

        val restoreAt = rig.events.indexOf("restoreLeftover:7")
        val captureAt = rig.events.indexOf("capture:40")
        assertTrue(restoreAt >= 0)
        assertTrue(captureAt > restoreAt)
    }

    @Test
    fun fire_prioritizeOff_doesNotCaptureVolume() {
        val rig = Rig(settings = TimerSettings(prioritizeVolume = false, rampMinutes = 0))
        rig.handler.onFire(4L)

        assertFalse(rig.events.any { it.startsWith("capture:") || it.startsWith("restoreLeftover:") })
        assertFalse(rig.events.any { it.startsWith("ramp:") })
        assertTrue(rig.events.contains("playback:AlarmRingtone"))
    }

    @Test
    fun fire_missingTimer_stopsWithoutReadingSettings() {
        val rig = Rig()
        rig.timers.clear()
        rig.handler.onFire(4L)

        assertEquals(listOf("foreground", "cancelVoice:4", "load", "stopService"), rig.events)
        assertFalse(rig.handler.sessionOpen)
    }

    @Test
    fun fire_cancelsVoiceCuesForOnlyTheFiringTimer() {
        val rig = Rig()

        rig.handler.onFire(4L)

        assertTrue(rig.events.contains("cancelVoice:4"))
        assertFalse(rig.events.contains("cancelVoice:9"))
    }

    @Test
    fun dismiss_pausesEveryRingingTimerAndRestoresVolume() {
        val rig = Rig(settings = TimerSettings(capturedAlarmVolume = 7))
        rig.timers[4L] = running(id = 4L, original = 5 * 60_000L).copy(phase = TimerPhase.Ringing, endsAtEpochMillis = null)
        rig.timers[9L] = running(id = 9L, original = 60_000L).copy(phase = TimerPhase.Ringing, endsAtEpochMillis = null)
        rig.timers[3L] = running(id = 3L).copy(
            phase = TimerPhase.Paused,
            endsAtEpochMillis = null,
            pausedRemainingMillis = 2_000L
        )
        rig.handler.sessionOpen = true

        rig.handler.onDismiss()

        assertEquals("foreground", rig.events.first())
        assertTrue(rig.events.indexOf("settings") > 0)
        assertTrue(rig.events.contains("restore:7"))
        assertTrue(rig.events.contains("stopPlayback"))
        assertTrue(rig.events.contains("stopVibration"))
        assertEquals("stopService", rig.events.last())
        assertEquals(TimerPhase.Paused, rig.timers.getValue(4L).phase)
        assertEquals(5 * 60_000L, rig.timers.getValue(4L).pausedRemainingMillis)
        assertEquals(60_000L, rig.timers.getValue(9L).pausedRemainingMillis)
        assertEquals(2_000L, rig.timers.getValue(3L).pausedRemainingMillis)
        assertFalse(rig.handler.sessionOpen)
    }

    @Test
    fun playerGain_rampsFromZeroToFull() {
        assertEquals(0f, TimerPlayback.playerGain(0L, 60_000L))
        assertEquals(0.5f, TimerPlayback.playerGain(30_000L, 60_000L))
        assertEquals(1f, TimerPlayback.playerGain(60_000L, 60_000L))
        assertEquals(1f, TimerPlayback.playerGain(0L, 0L))
    }

    @Test
    fun timerId_readsTheDataUri() {
        assertEquals(4L, TimerAlarmIntents.timerId("lemurloop://timer/4"))
        assertNull(TimerAlarmIntents.timerId("lemurloop://timer/nope"))
        assertNull(TimerAlarmIntents.timerId(null))
    }

    @Test
    fun wantsTimerTab_isTheShowIntentOnly() {
        assertTrue(TimerAlarmIntents.wantsTimerTab(TimerAlarmIntents.SHOW_ACTION, openTimerTabExtra = false))
        assertTrue(TimerAlarmIntents.wantsTimerTab(action = null, openTimerTabExtra = true))
        assertFalse(TimerAlarmIntents.wantsTimerTab(TimerAlarmIntents.FIRE_ACTION, openTimerTabExtra = false))
    }

    @Test
    fun consumes_volumeKeysOnlyWhenChangesAreOff() {
        assertFalse(TimerVolumeKeys.consumes(allowChanges = true, keyCode = TimerVolumeKeys.UP))
        assertTrue(TimerVolumeKeys.consumes(allowChanges = false, keyCode = TimerVolumeKeys.DOWN))
        assertTrue(TimerVolumeKeys.consumes(allowChanges = false, keyCode = TimerVolumeKeys.MUTE))
        assertFalse(TimerVolumeKeys.consumes(allowChanges = false, keyCode = 4))
    }

    @Test
    fun ringNotificationId_isNotTheAlarmSleepOrFinishedId() {
        assertTrue(TimerRingNotification.ID != 111)
        assertTrue(TimerRingNotification.ID != 2)
        assertTrue(TimerRingNotification.ID != TimerFinishedNotification.ID)
    }

    private fun running(
        id: Long,
        createdAt: Long = id,
        original: Long = 60_000L
    ) = CountdownTimer(
        id = id,
        label = "Timer",
        originalDurationMillis = original,
        endsAtEpochMillis = 10_000L,
        pausedRemainingMillis = null,
        phase = TimerPhase.Running,
        createdAtEpochMillis = createdAt
    )

    private inner class Rig(
        settings: TimerSettings = TimerSettings(),
        private val trackOpens: Boolean = true
    ) {
        val events = mutableListOf<String>()
        val timers = mutableMapOf(4L to running(4L))
        var settings = settings
        val handler = TimerRingHandler(
            foreground = { events += "foreground" },
            readSettings = { events += "settings"; settings },
            trackOpens = { uri ->
                events += "track:$uri"
                trackOpens
            },
            loadTimer = { id ->
                events += "load"
                timers[id]
            },
            saveTimers = { updated ->
                events += "save"
                updated.forEach { timers[it.id] = it }
            },
            loadRinging = { timers.values.toList() },
            startPlayback = { choice ->
                events += when (choice) {
                    TimerPlaybackChoice.Silent -> "playback:Silent"
                    TimerPlaybackChoice.AlarmRingtone -> "playback:AlarmRingtone"
                    is TimerPlaybackChoice.PhoneTrack -> "playback:PhoneTrack:${choice.uri}"
                }
            },
            startVibration = { vibration -> events += "vibration:${vibration.patternName}" },
            startRamp = { millis -> events += "ramp:$millis" },
            restoreLeftover = { level -> events += "restoreLeftover:$level" },
            captureVolume = { percent -> events += "capture:$percent" },
            restoreCaptured = { level -> events += "restore:$level" },
            openScreen = { events += "openScreen" },
            stopPlayback = { events += "stopPlayback" },
            stopVibration = { events += "stopVibration" },
            stopService = { events += "stopService" },
            cancelAlarm = { id -> events += "cancel:$id" },
            cancelVoiceCues = { id -> events += "cancelVoice:$id" }
        )
    }
}
