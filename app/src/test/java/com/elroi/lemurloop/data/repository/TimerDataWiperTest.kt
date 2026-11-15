package com.elroi.lemurloop.data.repository

import com.elroi.lemurloop.domain.timer.CountdownTimer
import com.elroi.lemurloop.domain.timer.TimerPhase
import com.elroi.lemurloop.domain.timer.TimerSettings
import com.elroi.lemurloop.domain.timer.TimerSound
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerDataWiperTest {

    @Test
    fun wipe_cancelsStopsDeletesClearsAndReleasesTheTrack() = runBlocking {
        val events = mutableListOf<String>()
        val track = "content://tracks/1"
        val wiper = TimerDataWiper(
            timerRepository = RecordingTimers(events),
            timerSettingsRepository = RecordingSettings(events, track),
            timerScheduler = RecordingScheduler(events),
            timerRingStop = RecordingRingStop(events),
            timerTrackGrantReleaser = RecordingGrants(events)
        )

        wiper.wipe()

        assertEquals(
            listOf(
                "settings",
                "timers",
                "cancel:4",
                "cancel:9",
                "stopRing",
                "deleteTimers",
                "clearSettings",
                "release:$track"
            ),
            events
        )
    }

    private class RecordingTimers(private val events: MutableList<String>) :
        com.elroi.lemurloop.domain.repository.TimerRepository {
        override fun observeAll(): Flow<List<CountdownTimer>> = flowOf(emptyList())
        override suspend fun getAll(): List<CountdownTimer> {
            events += "timers"
            return listOf(
                CountdownTimer(
                    id = 4L,
                    label = "",
                    originalDurationMillis = 60_000L,
                    endsAtEpochMillis = 5_000L,
                    pausedRemainingMillis = null,
                    phase = TimerPhase.Running,
                    createdAtEpochMillis = 4L
                ),
                CountdownTimer(
                    id = 9L,
                    label = "",
                    originalDurationMillis = 60_000L,
                    endsAtEpochMillis = 5_000L,
                    pausedRemainingMillis = null,
                    phase = TimerPhase.Running,
                    createdAtEpochMillis = 9L
                )
            )
        }
        override suspend fun getById(id: Long): CountdownTimer? = null
        override suspend fun upsert(timer: CountdownTimer): Long = timer.id
        override suspend fun delete(id: Long) = Unit
        override suspend fun deleteAll() {
            events += "deleteTimers"
        }
        override suspend fun count(): Int = 2
    }

    private class RecordingSettings(
        private val events: MutableList<String>,
        private val track: String
    ) : com.elroi.lemurloop.domain.repository.TimerSettingsRepository {
        override suspend fun get(): TimerSettings {
            events += "settings"
            return TimerSettings(sound = TimerSound.PhoneTrack, phoneTrackUri = track)
        }
        override fun observe(): Flow<TimerSettings> = flowOf(TimerSettings())
        override suspend fun update(settings: TimerSettings) = Unit
        override suspend fun clear() {
            events += "clearSettings"
        }
    }

    private class RecordingScheduler(private val events: MutableList<String>) :
        com.elroi.lemurloop.domain.scheduler.TimerScheduler {
        override fun schedule(timerId: Long, triggerAtEpochMillis: Long) = Unit
        override fun cancel(timerId: Long) {
            events += "cancel:$timerId"
        }
    }

    private class RecordingRingStop(private val events: MutableList<String>) :
        com.elroi.lemurloop.domain.timer.TimerRingStop {
        override fun stop() {
            events += "stopRing"
        }
    }

    private class RecordingGrants(private val events: MutableList<String>) :
        com.elroi.lemurloop.domain.timer.TimerTrackGrantReleaser {
        override fun release(uri: String?) {
            events += "release:$uri"
        }
    }
}
