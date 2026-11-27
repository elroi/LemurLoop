package com.elroi.lemurloop.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TimerVoiceCueRemainingCopyTest {

    @Test
    fun english_fiveMinutes_left() {
        assertEquals("5 minutes left", TimerVoiceCueRemainingCopy.english(5 * 60_000L))
    }

    @Test
    fun english_oneMinute_singular() {
        assertEquals("1 minute left", TimerVoiceCueRemainingCopy.english(60_000L))
    }

    @Test
    fun english_thirtySeconds_left() {
        assertEquals("30 seconds left", TimerVoiceCueRemainingCopy.english(30_000L))
    }

    @Test
    fun english_oneSecond_singular() {
        assertEquals("1 second left", TimerVoiceCueRemainingCopy.english(1_000L))
    }

    @Test
    fun english_neverSaysHalfway() {
        val text = TimerVoiceCueRemainingCopy.english(120_000L)
        assertFalse(text.contains("halfway", ignoreCase = true))
        assertEquals("2 minutes left", text)
    }

    @Test
    fun budget_constants_matchPlanLocks() {
        assertEquals(10_000L, TimerVoiceCueBudget.LAST_TEN_WINDOW_MILLIS)
        assertEquals(2_500L, TimerVoiceCueBudget.START_SPEECH_BUDGET_MILLIS)
        assertEquals(3_000L, TimerVoiceCueBudget.MID_CLEARANCE_BEFORE_LAST_TEN_MILLIS)
        assertEquals(1_500L, TimerVoiceCueBudget.FIRE_LATE_SKEW_MILLIS)
        assertEquals(400L, TimerVoiceCueBudget.LAST_TEN_DIGIT_SKEW_MILLIS)
        assertEquals(1_500L, TimerVoiceCueBudget.LAST_TEN_WARMUP_LEAD_MILLIS)
    }
}
