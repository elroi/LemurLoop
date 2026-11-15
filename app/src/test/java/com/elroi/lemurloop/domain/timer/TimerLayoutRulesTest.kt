package com.elroi.lemurloop.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerLayoutRulesTest {

    @Test
    fun swipeTowardEnd_english_isRightward() {
        assertTrue(deletesWhenSwipingTowardEnd(isRtl = false, deltaX = 12f))
        assertFalse(deletesWhenSwipingTowardEnd(isRtl = false, deltaX = -12f))
    }

    @Test
    fun swipeTowardEnd_hebrew_isLeftward() {
        assertTrue(deletesWhenSwipingTowardEnd(isRtl = true, deltaX = -12f))
        assertFalse(deletesWhenSwipingTowardEnd(isRtl = true, deltaX = 12f))
    }

    @Test
    fun keepTimerScreenOn_onlyWhileATimerIsRunning() {
        val running = CountdownTimer(
            id = 1L,
            label = "",
            originalDurationMillis = 1_000L,
            endsAtEpochMillis = 2_000L,
            pausedRemainingMillis = null,
            phase = TimerPhase.Running,
            createdAtEpochMillis = 1L
        )
        val paused = running.copy(phase = TimerPhase.Paused, endsAtEpochMillis = null, pausedRemainingMillis = 1_000L)
        val ringing = running.copy(phase = TimerPhase.Ringing)
        assertTrue(keepTimerScreenOn(listOf(running, paused)))
        assertFalse(keepTimerScreenOn(listOf(paused, ringing)))
        assertFalse(keepTimerScreenOn(emptyList()))
    }

    @Test
    fun presets_areOneFiveAndTenMinutes() {
        assertEquals(listOf(60_000L, 5 * 60_000L, 10 * 60_000L), TimerDuration.presets)
    }

    @Test
    fun canStart_rejectsZeroAndAllowsOneSecond() {
        assertFalse(TimerDuration.canStart(0L))
        assertFalse(TimerDuration.canStart(999L))
        assertTrue(TimerDuration.canStart(1_000L))
    }
}
