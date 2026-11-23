package com.elroi.lemurloop.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerVoiceCueFireGateTest {

    @Test
    fun shouldSpeak_falseWhenLaterThanSkew() {
        assertFalse(
            TimerVoiceCueFireGate.shouldSpeak(
                nowEpochMillis = 10_000L,
                scheduledFireEpochMillis = 8_000L,
                endsAtEpochMillis = 20_000L,
                phaseIsRunning = true,
                endsAtUnchanged = true,
                cueStillEffective = true
            )
        )
    }

    @Test
    fun shouldSpeak_trueWithinSkew() {
        assertTrue(
            TimerVoiceCueFireGate.shouldSpeak(
                nowEpochMillis = 8_500L,
                scheduledFireEpochMillis = 8_000L,
                endsAtEpochMillis = 20_000L,
                phaseIsRunning = true,
                endsAtUnchanged = true,
                cueStillEffective = true
            )
        )
    }

    @Test
    fun shouldSpeak_falseWhenCueIsNoLongerValid() {
        val baseline = TimerVoiceCueFireGate.shouldSpeak(
            nowEpochMillis = 8_500L,
            scheduledFireEpochMillis = 8_000L,
            endsAtEpochMillis = 20_000L,
            phaseIsRunning = true,
            endsAtUnchanged = true,
            cueStillEffective = true
        )

        assertTrue(baseline)
        assertFalse(
            TimerVoiceCueFireGate.shouldSpeak(
                nowEpochMillis = 8_500L,
                scheduledFireEpochMillis = 8_000L,
                endsAtEpochMillis = 20_000L,
                phaseIsRunning = false,
                endsAtUnchanged = true,
                cueStillEffective = true
            )
        )
        assertFalse(
            TimerVoiceCueFireGate.shouldSpeak(
                nowEpochMillis = 8_500L,
                scheduledFireEpochMillis = 8_000L,
                endsAtEpochMillis = 20_000L,
                phaseIsRunning = true,
                endsAtUnchanged = false,
                cueStillEffective = true
            )
        )
        assertFalse(
            TimerVoiceCueFireGate.shouldSpeak(
                nowEpochMillis = 8_500L,
                scheduledFireEpochMillis = 8_000L,
                endsAtEpochMillis = 20_000L,
                phaseIsRunning = true,
                endsAtUnchanged = true,
                cueStillEffective = false
            )
        )
        assertFalse(
            TimerVoiceCueFireGate.shouldSpeak(
                nowEpochMillis = 20_000L,
                scheduledFireEpochMillis = 19_000L,
                endsAtEpochMillis = 20_000L,
                phaseIsRunning = true,
                endsAtUnchanged = true,
                cueStillEffective = true
            )
        )
    }

    @Test
    fun lastTenDigits_skipsLateAndStopsAtEnd() {
        val ends = 100_000L

        assertEquals(
            listOf(3, 2, 1),
            TimerVoiceCueFireGate.lastTenDigitsToSpeak(
                nowEpochMillis = ends - 3_200L,
                endsAtEpochMillis = ends
            )
        )
        assertEquals(
            emptyList<Int>(),
            TimerVoiceCueFireGate.lastTenDigitsToSpeak(
                nowEpochMillis = ends,
                endsAtEpochMillis = ends
            )
        )
    }
}
