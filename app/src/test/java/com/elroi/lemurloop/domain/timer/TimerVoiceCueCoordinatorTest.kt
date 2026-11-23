package com.elroi.lemurloop.domain.timer

import com.elroi.lemurloop.domain.scheduler.TimerVoiceCueScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerVoiceCueCoordinatorTest {

    private val now = 1_000_000L

    @Test
    fun onTimerChanged_masterOffWithOverridesOn_schedulesNothing() {
        val rig = Rig(
            timer = running(
                voiceCueStartOverride = true,
                voiceCueMidRemainingOverride = true,
                voiceCueLastTenOverride = true
            ),
            globals = globals(masterEnabled = false)
        )

        rig.coordinator.onTimerChanged(7L, TimerVoiceCueRebuildReason.CreateOrDuplicate)

        assertTrue(rig.scheduler.scheduled.isEmpty())
        assertTrue(rig.speaker.delivered.isEmpty())
    }

    @Test
    fun onTimerChanged_createLongTimer_deliversStartAndSchedulesLaterCues() {
        val rig = Rig(timer = running())

        rig.coordinator.onTimerChanged(7L, TimerVoiceCueRebuildReason.CreateOrDuplicate)

        assertEquals(
            listOf(Delivered(7L, TimerVoiceCueKind.Start, now + 120_000L, "Tea")),
            rig.speaker.delivered
        )
        assertEquals(
            listOf(
                Scheduled(7L, TimerVoiceCueKind.MidRemaining, now + 60_000L),
                Scheduled(7L, TimerVoiceCueKind.EnterLastTen, now + 110_000L)
            ),
            rig.scheduler.scheduled
        )
    }

    @Test
    fun onTimerChanged_resumeWithAnnounceOff_omitsStart() {
        val rig = Rig(timer = running(), globals = globals(announceOnResume = false))

        rig.coordinator.onTimerChanged(7L, TimerVoiceCueRebuildReason.Resume)

        assertTrue(rig.speaker.delivered.isEmpty())
        assertEquals(
            listOf(TimerVoiceCueKind.MidRemaining, TimerVoiceCueKind.EnterLastTen),
            rig.scheduler.scheduled.map { it.kind }
        )
    }

    @Test
    fun onTimerChanged_resumeWithAnnounceOn_deliversStart() {
        val rig = Rig(timer = running(), globals = globals(announceOnResume = true))

        rig.coordinator.onTimerChanged(7L, TimerVoiceCueRebuildReason.Resume)

        assertEquals(listOf(TimerVoiceCueKind.Start), rig.speaker.delivered.map { it.kind })
    }

    @Test
    fun onTimerChanged_silentReplan_omitsStart() {
        val rig = Rig(timer = running())

        rig.coordinator.onTimerChanged(7L, TimerVoiceCueRebuildReason.SilentReplan)

        assertTrue(rig.speaker.delivered.isEmpty())
        assertEquals(
            listOf(TimerVoiceCueKind.MidRemaining, TimerVoiceCueKind.EnterLastTen),
            rig.scheduler.scheduled.map { it.kind }
        )
    }

    @Test
    fun onTimerChanged_shortCreate_lastTenWins() {
        val rig = Rig(timer = running(endsAt = now + 8_000L))

        rig.coordinator.onTimerChanged(7L, TimerVoiceCueRebuildReason.CreateOrDuplicate)

        assertTrue(rig.speaker.delivered.isEmpty())
        assertEquals(
            listOf(Scheduled(7L, TimerVoiceCueKind.EnterLastTen, now)),
            rig.scheduler.scheduled
        )
    }

    @Test
    fun onTimerChanged_rebuild_cancelsPriorAlarmAndSpeechBeforeScheduling() {
        val rig = Rig(timer = running())

        rig.coordinator.onTimerChanged(7L, TimerVoiceCueRebuildReason.SilentReplan)

        assertEquals(listOf(7L), rig.scheduler.cancelled)
        assertEquals(listOf(7L), rig.speaker.cancelled)
        assertEquals("cancelAlarm:7", rig.events.first())
        assertEquals("cancelSpeech:7", rig.events[1])
        assertEquals("schedule:MidRemaining", rig.events[2])
    }

    @Test
    fun onVoiceGlobalsChanged_replansEveryRunningTimerWithoutStart() {
        val other = running(id = 9L, endsAt = now + 60_000L, label = "Eggs")
        val rig = Rig(timer = running(), otherTimers = listOf(other))

        rig.coordinator.onVoiceGlobalsChanged()

        assertEquals(listOf(7L, 9L), rig.scheduler.cancelled)
        assertTrue(rig.speaker.delivered.isEmpty())
    }

    private fun running(
        id: Long = 7L,
        endsAt: Long = now + 120_000L,
        label: String = "Tea",
        voiceCueStartOverride: Boolean? = null,
        voiceCueMidRemainingOverride: Boolean? = null,
        voiceCueLastTenOverride: Boolean? = null
    ) = CountdownTimer(
        id = id,
        label = label,
        originalDurationMillis = 120_000L,
        endsAtEpochMillis = endsAt,
        pausedRemainingMillis = null,
        phase = TimerPhase.Running,
        createdAtEpochMillis = 1L,
        voiceCueStartOverride = voiceCueStartOverride,
        voiceCueMidRemainingOverride = voiceCueMidRemainingOverride,
        voiceCueLastTenOverride = voiceCueLastTenOverride
    )

    private fun globals(
        masterEnabled: Boolean = true,
        announceOnResume: Boolean = true
    ) = TimerVoiceCueGlobals(
        masterEnabled = masterEnabled,
        startEnabled = true,
        midRemainingEnabled = true,
        lastTenEnabled = true,
        announceOnResume = announceOnResume
    )

    private inner class Rig(
        timer: CountdownTimer,
        otherTimers: List<CountdownTimer> = emptyList(),
        globals: TimerVoiceCueGlobals = globals()
    ) {
        val events = mutableListOf<String>()
        val timers = (listOf(timer) + otherTimers).associateBy { it.id }
        val scheduler = RecordingScheduler(events)
        val speaker = RecordingSpeaker(events)
        val coordinator = TimerVoiceCueCoordinator(
            settings = { globals },
            loadTimer = { timers[it] },
            loadRunningTimers = { timers.values.filter { it.phase == TimerPhase.Running } },
            scheduler = scheduler,
            speaker = speaker,
            now = { now }
        )
    }

    private class RecordingScheduler(
        private val events: MutableList<String>
    ) : TimerVoiceCueScheduler {
        val scheduled = mutableListOf<Scheduled>()
        val cancelled = mutableListOf<Long>()

        override fun schedule(
            timerId: Long,
            kind: TimerVoiceCueKind,
            triggerAtEpochMillis: Long
        ) {
            events += "schedule:$kind"
            scheduled += Scheduled(timerId, kind, triggerAtEpochMillis)
        }

        override fun cancel(timerId: Long, kind: TimerVoiceCueKind) = Unit

        override fun cancelAllForTimer(timerId: Long) {
            events += "cancelAlarm:$timerId"
            cancelled += timerId
        }
    }

    private class RecordingSpeaker(
        private val events: MutableList<String>
    ) : TimerVoiceCueSpeaker {
        val delivered = mutableListOf<Delivered>()
        val cancelled = mutableListOf<Long>()

        override fun deliver(
            timerId: Long,
            kind: TimerVoiceCueKind,
            endsAtEpochMillis: Long,
            label: String
        ) {
            events += "deliver:$kind"
            delivered += Delivered(timerId, kind, endsAtEpochMillis, label)
        }

        override fun cancelSpeech(timerId: Long) {
            events += "cancelSpeech:$timerId"
            cancelled += timerId
        }
    }

    private data class Scheduled(
        val timerId: Long,
        val kind: TimerVoiceCueKind,
        val triggerAtEpochMillis: Long
    )

    private data class Delivered(
        val timerId: Long,
        val kind: TimerVoiceCueKind,
        val endsAtEpochMillis: Long,
        val label: String
    )
}
