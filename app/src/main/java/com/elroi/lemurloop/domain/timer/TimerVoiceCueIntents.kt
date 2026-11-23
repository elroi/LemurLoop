package com.elroi.lemurloop.domain.timer

data class TimerVoiceCueIntentSpec(
    val dataUri: String,
    val action: String,
    val requestCode: Int,
    val componentClassName: String
)

object TimerVoiceCueIntents {
    const val FIRE_ACTION = "com.elroi.lemurloop.action.TIMER_VOICE_CUE"
    const val FIRE_COMPONENT = "com.elroi.lemurloop.receiver.TimerVoiceCueReceiver"
    private const val DATA_PREFIX = "lemurloop://timer-cue/"

    fun dataUri(timerId: Long, kind: TimerVoiceCueKind): String =
        "$DATA_PREFIX$timerId/${kind.name}"

    fun fire(timerId: Long, kind: TimerVoiceCueKind) = TimerVoiceCueIntentSpec(
        dataUri = dataUri(timerId, kind),
        action = FIRE_ACTION,
        requestCode = kind.ordinal,
        componentClassName = FIRE_COMPONENT
    )

    fun timerId(uri: String?): Long? {
        if (uri == null || !uri.startsWith(DATA_PREFIX)) return null
        return uri.removePrefix(DATA_PREFIX).substringBefore('/').toLongOrNull()
    }

    fun kind(uri: String?): TimerVoiceCueKind? {
        if (uri == null || !uri.startsWith(DATA_PREFIX)) return null
        val name = uri.removePrefix(DATA_PREFIX).substringAfter('/', missingDelimiterValue = "")
        return runCatching { TimerVoiceCueKind.valueOf(name) }.getOrNull()
    }
}

enum class TimerVoiceCueScheduleMode {
    SetExactAndAllowWhileIdle,
    SetAndAllowWhileIdle
}

fun timerVoiceCueScheduleMode(
    sdkInt: Int,
    canScheduleExactAlarms: Boolean
): TimerVoiceCueScheduleMode {
    return if (sdkInt < 31 || canScheduleExactAlarms) {
        TimerVoiceCueScheduleMode.SetExactAndAllowWhileIdle
    } else {
        TimerVoiceCueScheduleMode.SetAndAllowWhileIdle
    }
}
