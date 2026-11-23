package com.elroi.lemurloop.domain.timer

object TimerVoiceCueBudget {
    const val LAST_TEN_WINDOW_MILLIS = 10_000L
    const val START_SPEECH_BUDGET_MILLIS = 2_500L
    const val MID_CLEARANCE_BEFORE_LAST_TEN_MILLIS = 3_000L
    const val FIRE_LATE_SKEW_MILLIS = 1_500L
    const val LAST_TEN_DIGIT_SKEW_MILLIS = 400L
    const val LAST_TEN_WARMUP_LEAD_MILLIS = 800L
}
