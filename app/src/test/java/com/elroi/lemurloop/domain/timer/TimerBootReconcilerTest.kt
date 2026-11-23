package com.elroi.lemurloop.domain.timer

import com.elroi.lemurloop.domain.repository.TimerRepository
import com.elroi.lemurloop.domain.scheduler.TimerScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerBootReconcilerTest {

    private val now = 1_000_000L

    @Test
    fun reconcile_dueSoon_rearmsAtNowAndDoesNotFinishIt() = runBlocking {
        val dueSoon = running(id = 2L, endsAt = now - 1_000L)
        val repository = MemoryTimers(listOf(dueSoon))
        val scheduler = MemoryScheduler()
        val notifier = MemoryNotifier()
        reconciler(repository, scheduler, notifier, ringRunning = false).reconcile(now)

        assertEquals(listOf(Scheduled(2L, now)), scheduler.scheduled)
        assertTrue(scheduler.cancelled.isEmpty())
        assertTrue(notifier.finished.isEmpty())
        assertEquals(TimerPhase.Running, repository.getById(2L)!!.phase)
    }

    @Test
    fun reconcile_dueBy60SecondsOrMore_pausesAndNotifiesWithoutScheduling() = runBlocking {
        val dueLong = running(id = 3L, endsAt = now - 60_000L, original = FIVE_MINUTES, createdAt = 42L)
        val repository = MemoryTimers(listOf(dueLong))
        val scheduler = MemoryScheduler()
        val notifier = MemoryNotifier()
        reconciler(repository, scheduler, notifier, ringRunning = false).reconcile(now)

        assertTrue(scheduler.scheduled.isEmpty())
        assertEquals(listOf(3L), scheduler.cancelled)
        assertEquals(listOf(3L), notifier.finished)
        val stored = repository.getById(3L)!!
        assertEquals(TimerPhase.Paused, stored.phase)
        assertEquals(FIVE_MINUTES, stored.pausedRemainingMillis)
        assertEquals(null, stored.endsAtEpochMillis)
        assertEquals(42L, stored.createdAtEpochMillis)
    }

    @Test
    fun reconcile_futureAndPausedAndRinging_followThePlan() = runBlocking {
        val future = running(id = 1L, endsAt = now + 10_000L)
        val paused = running(id = 4L).copy(
            phase = TimerPhase.Paused,
            endsAtEpochMillis = null,
            pausedRemainingMillis = 1_000L
        )
        val ringing = running(id = 5L, endsAt = now - 3_600_000L).copy(phase = TimerPhase.Ringing)
        val repository = MemoryTimers(listOf(future, paused, ringing))
        val scheduler = MemoryScheduler()
        val notifier = MemoryNotifier()
        val voiceCues = RecordingVoiceCues()
        reconciler(
            repository,
            scheduler,
            notifier,
            ringRunning = false,
            voiceCues = voiceCues
        ).reconcile(now)

        assertEquals(
            listOf(Scheduled(1L, now + 10_000L), Scheduled(5L, now)),
            scheduler.scheduled
        )
        assertEquals(listOf(4L), scheduler.cancelled)
        assertTrue(notifier.finished.isEmpty())
        assertEquals(TimerPhase.Ringing, repository.getById(5L)!!.phase)
        assertEquals(TimerPhase.Running, repository.getById(1L)!!.phase)
        assertEquals(
            listOf(VoiceCueChange(1L, TimerVoiceCueRebuildReason.SilentReplan)),
            voiceCues.changes
        )
    }

    @Test
    fun reconcile_ringingWhileTheServiceIsRunning_doesNotRearm() = runBlocking {
        val ringing = running(id = 5L, endsAt = now - 1_000L).copy(phase = TimerPhase.Ringing)
        val repository = MemoryTimers(listOf(ringing))
        val scheduler = MemoryScheduler()
        val notifier = MemoryNotifier()
        reconciler(repository, scheduler, notifier, ringRunning = true).reconcile(now)

        assertTrue(scheduler.scheduled.isEmpty())
        assertTrue(scheduler.cancelled.isEmpty())
        assertTrue(notifier.finished.isEmpty())
        assertEquals(TimerPhase.Ringing, repository.getById(5L)!!.phase)
    }

    @Test
    fun finishedNotificationId_isNotTheAlarmOrSleepId() {
        assertTrue(TimerFinishedNotification.ID != 111)
        assertTrue(TimerFinishedNotification.ID != 2)
    }

    private fun reconciler(
        repository: TimerRepository,
        scheduler: TimerScheduler,
        notifier: TimerFinishedNotifier,
        ringRunning: Boolean,
        voiceCues: TimerVoiceCueCoordinatorPort = RecordingVoiceCues()
    ) = TimerBootReconciler(
        repository = repository,
        scheduler = scheduler,
        notifier = notifier,
        ringRunning = { ringRunning },
        voiceCues = voiceCues
    )

    private fun running(
        id: Long,
        endsAt: Long = now + 5_000L,
        original: Long = FIVE_MINUTES,
        createdAt: Long = id
    ) = CountdownTimer(
        id = id,
        label = "Timer",
        originalDurationMillis = original,
        endsAtEpochMillis = endsAt,
        pausedRemainingMillis = null,
        phase = TimerPhase.Running,
        createdAtEpochMillis = createdAt
    )

    private class MemoryTimers(seed: List<CountdownTimer>) : TimerRepository {
        private val rows = seed.associateBy { it.id }.toMutableMap()
        override fun observeAll(): Flow<List<CountdownTimer>> = flowOf(rows.values.toList())
        override suspend fun getAll(): List<CountdownTimer> = rows.values.toList()
        override suspend fun getById(id: Long): CountdownTimer? = rows[id]
        override suspend fun upsert(timer: CountdownTimer): Long {
            rows[timer.id] = timer
            return timer.id
        }
        override suspend fun delete(id: Long) {
            rows.remove(id)
        }
        override suspend fun deleteAll() {
            rows.clear()
        }
        override suspend fun count(): Int = rows.size
    }

    private class MemoryScheduler : TimerScheduler {
        val scheduled = mutableListOf<Scheduled>()
        val cancelled = mutableListOf<Long>()
        override fun schedule(timerId: Long, triggerAtEpochMillis: Long) {
            scheduled += Scheduled(timerId, triggerAtEpochMillis)
        }
        override fun cancel(timerId: Long) {
            cancelled += timerId
        }
    }

    private class MemoryNotifier : TimerFinishedNotifier {
        val finished = mutableListOf<Long>()
        override fun notifyFinished(timer: CountdownTimer) {
            finished += timer.id
        }
    }

    private class RecordingVoiceCues : TimerVoiceCueCoordinatorPort {
        val changes = mutableListOf<VoiceCueChange>()
        override fun onTimerChanged(timerId: Long, reason: TimerVoiceCueRebuildReason) {
            changes += VoiceCueChange(timerId, reason)
        }
        override fun onVoiceGlobalsChanged() = Unit
        override fun cancel(timerId: Long) = Unit
        override fun cancelAll() = Unit
    }

    private data class Scheduled(val timerId: Long, val triggerAtEpochMillis: Long)
    private data class VoiceCueChange(
        val timerId: Long,
        val reason: TimerVoiceCueRebuildReason
    )

    private companion object {
        const val FIVE_MINUTES = 5 * 60_000L
    }
}
