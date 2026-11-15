package com.elroi.lemurloop.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerSchedulePlanTest {

    private val now = 1_000_000L

    @Test
    fun reconcile_futureEnd_schedulesThatWallClockEnd() {
        val timer = running(id = 1L, endsAt = now + 10_000L)
        val plan = TimerSchedulePlan.reconcile(listOf(timer), now, ringServiceRunning = false)

        assertEquals(listOf(TimerScheduleAt(1L, now + 10_000L)), plan.schedule)
        assertTrue(plan.ringSoon.isEmpty())
        assertTrue(plan.finishQuietly.isEmpty())
        assertTrue(plan.cancel.isEmpty())
        assertFalse(plan.startsRingService)
    }

    @Test
    fun reconcile_dueByLessThan60Seconds_rearmsAtNow() {
        val timer = running(id = 2L, endsAt = now - 59_999L)
        val plan = TimerSchedulePlan.reconcile(listOf(timer), now, ringServiceRunning = false)

        assertEquals(listOf(2L), plan.ringSoon)
        assertEquals(listOf(TimerScheduleAt(2L, now)), plan.schedule)
        assertTrue(plan.finishQuietly.isEmpty())
        assertTrue(plan.cancel.isEmpty())
        assertFalse(plan.startsRingService)
    }

    @Test
    fun reconcile_dueBy60SecondsOrMore_finishesQuietlyAndCancels() {
        val timer = running(id = 3L, endsAt = now - 60_000L)
        val plan = TimerSchedulePlan.reconcile(listOf(timer), now, ringServiceRunning = false)

        assertEquals(listOf(3L), plan.finishQuietly)
        assertEquals(listOf(3L), plan.cancel)
        assertTrue(plan.schedule.isEmpty())
        assertTrue(plan.ringSoon.isEmpty())
        assertFalse(plan.startsRingService)
    }

    @Test
    fun reconcile_paused_isCancelledEvenWhenTheOldEndIsPast() {
        val paused = running(id = 4L, endsAt = now - 5_000L).copy(
            phase = TimerPhase.Paused,
            pausedRemainingMillis = 8_000L
        )
        val plan = TimerSchedulePlan.reconcile(listOf(paused), now, ringServiceRunning = false)

        assertEquals(listOf(4L), plan.cancel)
        assertTrue(plan.schedule.isEmpty())
        assertTrue(plan.ringSoon.isEmpty())
        assertTrue(plan.finishQuietly.isEmpty())
    }

    @Test
    fun reconcile_boot_rearmsWithoutStartingTheRingService() {
        val future = running(id = 1L, endsAt = now + 10_000L)
        val dueSoon = running(id = 2L, endsAt = now - 59_999L)
        val dueLong = running(id = 3L, endsAt = now - 60_000L)
        val paused = running(id = 4L).copy(
            phase = TimerPhase.Paused,
            endsAtEpochMillis = null,
            pausedRemainingMillis = 1_000L
        )
        val ringing = running(id = 5L, endsAt = now - 3_600_000L).copy(phase = TimerPhase.Ringing)

        val plan = TimerSchedulePlan.reconcile(
            timers = listOf(future, dueSoon, dueLong, paused, ringing),
            nowEpochMillis = now,
            ringServiceRunning = false
        )

        assertEquals(
            listOf(
                TimerScheduleAt(1L, now + 10_000L),
                TimerScheduleAt(2L, now),
                TimerScheduleAt(5L, now)
            ),
            plan.schedule
        )
        assertEquals(listOf(2L, 5L), plan.ringSoon)
        assertEquals(listOf(3L), plan.finishQuietly)
        assertEquals(listOf(3L, 4L), plan.cancel)
        assertFalse(plan.startsRingService)
        assertEquals(
            listOf(
                TimerReconcileKind.Schedule,
                TimerReconcileKind.Cancel,
                TimerReconcileKind.FinishQuietly
            ),
            TimerReconcileKind.values().toList()
        )
    }

    @Test
    fun reconcile_ringingWhileTheServiceIsRunning_leavesItAlone() {
        val ringing = running(id = 5L, endsAt = now - 1_000L).copy(phase = TimerPhase.Ringing)
        val plan = TimerSchedulePlan.reconcile(listOf(ringing), now, ringServiceRunning = true)

        assertTrue(plan.schedule.isEmpty())
        assertTrue(plan.cancel.isEmpty())
        assertTrue(plan.ringSoon.isEmpty())
        assertTrue(plan.finishQuietly.isEmpty())
        assertFalse(plan.startsRingService)
    }

    @Test
    fun pauseFinished_pausesAtTheOriginalDuration() {
        val timer = running(id = 3L, original = FIVE_MINUTES, endsAt = now - 120_000L, createdAt = 42L, label = "Soup")
        val paused = TimerSchedulePlan.pauseFinished(timer)

        assertEquals(TimerPhase.Paused, paused.phase)
        assertEquals(FIVE_MINUTES, paused.pausedRemainingMillis)
        assertNull(paused.endsAtEpochMillis)
        assertEquals(42L, paused.createdAtEpochMillis)
        assertEquals(3L, paused.id)
        assertEquals("Soup", paused.label)
        assertEquals(FIVE_MINUTES, paused.originalDurationMillis)
    }

    @Test
    fun armRestored_pastEnd_schedulesAtNowEvenWhenAnHourLate() {
        val timer = running(id = 8L, endsAt = now - 3_600_000L)
        assertEquals(TimerArm.Schedule(8L, now), TimerSchedulePlan.armRestored(timer, now))
    }

    @Test
    fun armRestored_futureEnd_schedulesAtThatEnd() {
        val timer = running(id = 8L, endsAt = now + 4_000L)
        assertEquals(TimerArm.Schedule(8L, now + 4_000L), TimerSchedulePlan.armRestored(timer, now))
    }

    @Test
    fun armRestored_paused_cancels() {
        val paused = running(id = 8L, endsAt = now - 50_000L).copy(
            phase = TimerPhase.Paused,
            pausedRemainingMillis = 2_000L
        )
        assertEquals(TimerArm.Cancel(8L), TimerSchedulePlan.armRestored(paused, now))
    }

    @Test
    fun reconciles_bootAndTimeSetOnly() {
        assertTrue(TimerClockEvents.reconciles("android.intent.action.BOOT_COMPLETED"))
        assertTrue(TimerClockEvents.reconciles("android.intent.action.TIME_SET"))
        assertFalse(TimerClockEvents.reconciles("android.intent.action.TIMEZONE_CHANGED"))
        assertFalse(TimerClockEvents.reconciles("android.intent.action.TIME_TICK"))
        assertFalse(TimerClockEvents.reconciles(null))
    }

    private fun running(
        id: Long,
        endsAt: Long = now + 5_000L,
        original: Long = FIVE_MINUTES,
        createdAt: Long = id,
        label: String = "Timer"
    ) = CountdownTimer(
        id = id,
        label = label,
        originalDurationMillis = original,
        endsAtEpochMillis = endsAt,
        pausedRemainingMillis = null,
        phase = TimerPhase.Running,
        createdAtEpochMillis = createdAt
    )

    private companion object {
        const val FIVE_MINUTES = 5 * 60_000L
    }
}
