package com.elroi.lemurloop.service

/**
 * Pure mapping from alarm state to the intent extras a snoozed alarm must carry, so it keeps
 * every per-alarm setting instead of resetting some to defaults (BUG-8). Android-free so it's
 * testable in a plain JVM test; the key set must match what AlarmReceiver forwards on initial
 * fire (see AlarmSnoozeExtrasTest).
 */
object AlarmSnoozeExtras {

    fun build(
        alarmId: String,
        alarmLabel: String,
        snoozeMins: Int,
        snoozeCount: Int,
        mathDifficulty: Int,
        mathProblemCount: Int,
        mathGradualDifficulty: Boolean,
        smileToDismiss: Boolean,
        ttsEnabled: Boolean,
        isEvasiveSnooze: Boolean,
        evasiveSnoozesBeforeMoving: Int,
        soundUri: String?,
        daysOfWeek: String?,
        smileFallbackMethod: String,
        isSmoothFadeOut: Boolean,
        isVibrate: Boolean,
        isSoundEnabled: Boolean,
        isSnoozeEnabled: Boolean,
        isSmartWakeupEnabled: Boolean,
        wakeupCheckDelayMinutes: Int,
        wakeupCheckTimeoutSeconds: Int,
        briefingEnabled: Boolean,
        briefingTimeoutSeconds: Int,
        vibrationPattern: String,
        vibrationStartGapSeconds: Int
    ): Map<String, Any?> = mapOf(
        AlarmIntentExtras.EXTRA_ALARM_ID to alarmId,
        AlarmIntentExtras.EXTRA_ALARM_LABEL to alarmLabel,
        AlarmIntentExtras.EXTRA_SNOOZE_DURATION to snoozeMins,
        AlarmIntentExtras.EXTRA_SNOOZE_COUNT to snoozeCount,
        AlarmIntentExtras.EXTRA_MATH_DIFFICULTY to mathDifficulty,
        AlarmIntentExtras.EXTRA_MATH_PROBLEM_COUNT to mathProblemCount,
        AlarmIntentExtras.EXTRA_MATH_GRADUAL_DIFFICULTY to mathGradualDifficulty,
        AlarmIntentExtras.EXTRA_SMILE_TO_DISMISS to smileToDismiss,
        AlarmIntentExtras.EXTRA_TTS_ENABLED to ttsEnabled,
        AlarmIntentExtras.EXTRA_IS_EVASIVE_SNOOZE to isEvasiveSnooze,
        AlarmIntentExtras.EXTRA_EVASIVE_SNOOZES_BEFORE_MOVING to evasiveSnoozesBeforeMoving,
        AlarmIntentExtras.EXTRA_SOUND_URI to soundUri,
        AlarmIntentExtras.EXTRA_DAYS_OF_WEEK to daysOfWeek,
        AlarmIntentExtras.EXTRA_SMILE_FALLBACK_METHOD to smileFallbackMethod,
        AlarmIntentExtras.EXTRA_IS_SMOOTH_FADE_OUT to isSmoothFadeOut,
        AlarmIntentExtras.EXTRA_IS_VIBRATE to isVibrate,
        AlarmIntentExtras.EXTRA_IS_SOUND_ENABLED to isSoundEnabled,
        AlarmIntentExtras.EXTRA_IS_SNOOZE_ENABLED to isSnoozeEnabled,
        AlarmIntentExtras.EXTRA_IS_SMART_WAKEUP_ENABLED to isSmartWakeupEnabled,
        AlarmIntentExtras.EXTRA_WAKEUP_CHECK_DELAY to wakeupCheckDelayMinutes,
        AlarmIntentExtras.EXTRA_WAKEUP_CHECK_TIMEOUT to wakeupCheckTimeoutSeconds,
        AlarmIntentExtras.EXTRA_BRIEFING_ENABLED to briefingEnabled,
        AlarmIntentExtras.EXTRA_BRIEFING_TIMEOUT to briefingTimeoutSeconds,
        AlarmIntentExtras.EXTRA_VIBRATION_PATTERN to vibrationPattern,
        AlarmIntentExtras.EXTRA_VIBRATION_START_GAP to vibrationStartGapSeconds
    )
}
