package com.elroi.lemurloop.domain.timer

data class TimerVoiceCueOverrides(
    val start: Boolean? = null,
    val midRemaining: Boolean? = null,
    val lastTen: Boolean? = null
)

data class TimerVoiceCueGlobals(
    val masterEnabled: Boolean,
    val startEnabled: Boolean,
    val midRemainingEnabled: Boolean,
    val lastTenEnabled: Boolean,
    val announceOnResume: Boolean
)

data class TimerVoiceCueEffective(
    val start: Boolean,
    val midRemaining: Boolean,
    val lastTen: Boolean
)

object TimerVoiceCueResolver {
    fun effective(
        globals: TimerVoiceCueGlobals,
        overrides: TimerVoiceCueOverrides
    ): TimerVoiceCueEffective {
        if (!globals.masterEnabled) {
            return TimerVoiceCueEffective(start = false, midRemaining = false, lastTen = false)
        }
        return TimerVoiceCueEffective(
            start = overrides.start ?: globals.startEnabled,
            midRemaining = overrides.midRemaining ?: globals.midRemainingEnabled,
            lastTen = overrides.lastTen ?: globals.lastTenEnabled
        )
    }
}
