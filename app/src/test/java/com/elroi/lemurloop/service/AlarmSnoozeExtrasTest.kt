package com.elroi.lemurloop.service

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * BUG-8: handleSnooze() must forward every extra AlarmReceiver forwards when an alarm first
 * fires, so a snoozed alarm keeps its per-alarm settings instead of some resetting to defaults.
 * This is the canonical set AlarmReceiver.kt forwards to AlarmService (ALARM_ID/ALARM_LABEL
 * included); if a new extra is added to one side and not the other, this test must fail.
 */
class AlarmSnoozeExtrasTest {

    private val canonicalKeys = setOf(
        AlarmIntentExtras.EXTRA_ALARM_ID,
        AlarmIntentExtras.EXTRA_ALARM_LABEL,
        AlarmIntentExtras.EXTRA_MATH_DIFFICULTY,
        AlarmIntentExtras.EXTRA_MATH_PROBLEM_COUNT,
        AlarmIntentExtras.EXTRA_MATH_GRADUAL_DIFFICULTY,
        AlarmIntentExtras.EXTRA_SNOOZE_DURATION,
        AlarmIntentExtras.EXTRA_SNOOZE_COUNT,
        AlarmIntentExtras.EXTRA_SMILE_TO_DISMISS,
        AlarmIntentExtras.EXTRA_BRIEFING_ENABLED,
        AlarmIntentExtras.EXTRA_TTS_ENABLED,
        AlarmIntentExtras.EXTRA_IS_EVASIVE_SNOOZE,
        AlarmIntentExtras.EXTRA_EVASIVE_SNOOZES_BEFORE_MOVING,
        AlarmIntentExtras.EXTRA_SOUND_URI,
        AlarmIntentExtras.EXTRA_IS_SMOOTH_FADE_OUT,
        AlarmIntentExtras.EXTRA_IS_VIBRATE,
        AlarmIntentExtras.EXTRA_IS_SOUND_ENABLED,
        AlarmIntentExtras.EXTRA_IS_SNOOZE_ENABLED,
        AlarmIntentExtras.EXTRA_IS_SMART_WAKEUP_ENABLED,
        AlarmIntentExtras.EXTRA_WAKEUP_CHECK_DELAY,
        AlarmIntentExtras.EXTRA_WAKEUP_CHECK_TIMEOUT,
        AlarmIntentExtras.EXTRA_BRIEFING_TIMEOUT,
        AlarmIntentExtras.EXTRA_VIBRATION_PATTERN,
        AlarmIntentExtras.EXTRA_VIBRATION_START_GAP,
        AlarmIntentExtras.EXTRA_DAYS_OF_WEEK,
        AlarmIntentExtras.EXTRA_SMILE_FALLBACK_METHOD
    )

    private fun sampleExtras() = AlarmSnoozeExtras.build(
        alarmId = "alarm-1",
        alarmLabel = "Wake up",
        snoozeMins = 9,
        snoozeCount = 1,
        mathDifficulty = 2,
        mathProblemCount = 3,
        mathGradualDifficulty = true,
        smileToDismiss = false,
        ttsEnabled = true,
        isEvasiveSnooze = true,
        evasiveSnoozesBeforeMoving = 2,
        soundUri = "content://sound",
        daysOfWeek = "MON,TUE",
        smileFallbackMethod = "MATH",
        isSmoothFadeOut = true,
        isVibrate = true,
        isSoundEnabled = true,
        isSnoozeEnabled = true,
        isSmartWakeupEnabled = true,
        wakeupCheckDelayMinutes = 5,
        wakeupCheckTimeoutSeconds = 90,
        briefingEnabled = true,
        briefingTimeoutSeconds = 45,
        vibrationPattern = "HEARTBEAT",
        vibrationStartGapSeconds = 20
    )

    @Test
    fun `build forwards every extra AlarmReceiver forwards on initial fire`() {
        assertEquals(canonicalKeys, sampleExtras().keys)
    }

    @Test
    fun `build preserves the configured briefing timeout instead of resetting to default`() {
        // BUG-8: a non-default briefing timeout must survive a snooze, not reset to 30s.
        assertEquals(45, sampleExtras()[AlarmIntentExtras.EXTRA_BRIEFING_TIMEOUT])
    }
}
