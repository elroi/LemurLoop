package com.elroi.lemurloop.domain.timer

import com.elroi.lemurloop.domain.repository.TimerRepository
import com.elroi.lemurloop.domain.scheduler.TimerScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerListControllerTest {

    @Test
    fun start_fiveMinutes_schedulesThatWallClockEnd() = runBlocking {
        val rig = Rig(now = 1_000L)
        val result = rig.controller.start(durationMillis = 5 * 60_000L, label = "Tea")

        assertEquals(TimerCommandResult.Applied, result)
        val timer = rig.repository.rows.values.single()
        assertEquals(TimerPhase.Running, timer.phase)
        assertEquals(1_000L + 5 * 60_000L, timer.endsAtEpochMillis)
        assertEquals(listOf(Scheduled(timer.id, 1_000L + 5 * 60_000L)), rig.scheduler.scheduled)
    }

    @Test
    fun start_atTwentyTimers_isRefused() = runBlocking {
        val rig = Rig(now = 1_000L)
        repeat(TimerLimits.MAX_TIMERS) { index ->
            rig.repository.rows[index + 1L] = running(index + 1L)
        }
        val result = rig.controller.start(durationMillis = 60_000L, label = "")
        assertEquals(TimerCommandResult.LimitReached, result)
        assertEquals(TimerLimits.MAX_TIMERS, rig.repository.rows.size)
        assertTrue(rig.scheduler.scheduled.isEmpty())
    }

    @Test
    fun start_atZero_isRefused() = runBlocking {
        val rig = Rig(now = 1_000L)
        assertEquals(TimerCommandResult.Refused, rig.controller.start(0L, ""))
        assertTrue(rig.repository.rows.isEmpty())
    }

    @Test
    fun delete_runningTimer_keepsTheOldEndForUndo() = runBlocking {
        val rig = Rig(now = 5_000L)
        val timer = running(id = 4L, endsAt = 9_000L, createdAt = 20L)
        rig.repository.rows[4L] = timer
        rig.controller.delete(4L)

        assertTrue(rig.repository.rows.isEmpty())
        assertEquals(listOf(4L), rig.scheduler.cancelled)
        assertTrue(rig.controller.undoAvailable(5_000L))
        val restored = rig.controller.peekUndo()
        assertEquals(9_000L, restored?.endsAtEpochMillis)
        assertEquals(20L, restored?.createdAtEpochMillis)
    }

    @Test
    fun undo_whenTheEndHasPassed_restoresThatTimerAndSchedulesNow() = runBlocking {
        val rig = Rig(now = 20_000L)
        val timer = running(id = 4L, endsAt = 9_000L, createdAt = 20L)
        rig.repository.rows[4L] = timer
        rig.controller.delete(4L)
        rig.scheduler.scheduled.clear()

        val result = rig.controller.undo()

        assertEquals(TimerCommandResult.Applied, result)
        assertEquals(timer, rig.repository.rows[4L])
        assertEquals(listOf(Scheduled(4L, 20_000L)), rig.scheduler.scheduled)
        assertFalse(rig.controller.undoAvailable(20_000L))
    }

    @Test
    fun undo_whenTheListIsFull_isRefused() = runBlocking {
        val rig = Rig(now = 5_000L)
        val deleted = running(id = 99L, endsAt = 9_000L)
        rig.repository.rows[99L] = deleted
        rig.controller.delete(99L)
        repeat(TimerLimits.MAX_TIMERS) { index ->
            rig.repository.rows[index + 1L] = running(index + 1L)
        }

        assertEquals(TimerCommandResult.LimitReached, rig.controller.undo())
        assertNull(rig.repository.rows[99L])
        assertEquals(9_000L, rig.controller.peekUndo()?.endsAtEpochMillis)
    }

    @Test
    fun undo_afterFiveSeconds_expires() = runBlocking {
        val rig = Rig(now = 1_000L)
        rig.repository.rows[4L] = running(4L)
        rig.controller.delete(4L)
        rig.now = 1_000L + 5_000L

        assertEquals(TimerCommandResult.UndoExpired, rig.controller.undo())
        assertFalse(rig.controller.undoAvailable(rig.now))
        assertNull(rig.repository.rows[4L])
    }

    @Test
    fun clearUndo_dropsTheSnapshot() = runBlocking {
        val rig = Rig(now = 1_000L)
        rig.repository.rows[4L] = running(4L)
        rig.controller.delete(4L)
        rig.controller.clearUndo()
        assertEquals(TimerCommandResult.Refused, rig.controller.undo())
    }

    @Test
    fun delete_lastRingingTimer_stopsTheRing() = runBlocking {
        val rig = Rig(now = 1_000L)
        rig.repository.rows[4L] = running(4L).copy(phase = TimerPhase.Ringing)
        rig.controller.delete(4L)
        assertEquals(1, rig.ring.stops)
    }

    @Test
    fun delete_whileAnotherTimerIsRinging_leavesTheRing() = runBlocking {
        val rig = Rig(now = 1_000L)
        rig.repository.rows[4L] = running(4L).copy(phase = TimerPhase.Ringing)
        rig.repository.rows[5L] = running(5L).copy(phase = TimerPhase.Ringing)
        rig.controller.delete(4L)
        assertEquals(0, rig.ring.stops)
        assertEquals(TimerPhase.Ringing, rig.repository.rows.getValue(5L).phase)
    }

    @Test
    fun addOneMinute_whilePaused_doesNotChangeTheTimer() = runBlocking {
        val rig = Rig(now = 1_000L)
        val paused = running(4L).copy(
            phase = TimerPhase.Paused,
            endsAtEpochMillis = null,
            pausedRemainingMillis = 5_000L
        )
        rig.repository.rows[4L] = paused
        assertEquals(TimerCommandResult.Refused, rig.controller.addOneMinute(4L))
        assertEquals(paused, rig.repository.rows[4L])
        assertTrue(rig.scheduler.scheduled.isEmpty())
    }

    private fun running(
        id: Long,
        endsAt: Long = 50_000L,
        createdAt: Long = id
    ) = CountdownTimer(
        id = id,
        label = "Timer",
        originalDurationMillis = 60_000L,
        endsAtEpochMillis = endsAt,
        pausedRemainingMillis = null,
        phase = TimerPhase.Running,
        createdAtEpochMillis = createdAt
    )

    private class Rig(var now: Long) {
        val repository = MemoryTimers()
        val scheduler = MemoryScheduler()
        val ring = MemoryRing()
        val controller = TimerListController(
            repository = repository,
            scheduler = scheduler,
            ringStop = ring,
            now = { now }
        )
    }

    private class MemoryTimers : TimerRepository {
        val rows = linkedMapOf<Long, CountdownTimer>()
        private var nextId = 100L
        override fun observeAll(): Flow<List<CountdownTimer>> = flowOf(rows.values.toList())
        override suspend fun getAll(): List<CountdownTimer> = rows.values.toList()
        override suspend fun getById(id: Long): CountdownTimer? = rows[id]
        override suspend fun upsert(timer: CountdownTimer): Long {
            val id = if (timer.id == 0L) nextId++ else timer.id
            rows[id] = timer.copy(id = id)
            return id
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

    private class MemoryRing : TimerRingStop {
        var stops = 0
        override fun stop() {
            stops += 1
        }
    }

    private data class Scheduled(val timerId: Long, val triggerAtEpochMillis: Long)
}
