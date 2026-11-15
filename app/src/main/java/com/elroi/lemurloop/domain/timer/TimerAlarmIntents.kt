package com.elroi.lemurloop.domain.timer

data class TimerIntentSpec(
    val dataUri: String,
    val action: String,
    val requestCode: Int,
    val componentClassName: String,
    val opensTimerTab: Boolean,
    val isFireBroadcast: Boolean
)

object TimerAlarmIntents {
    const val REQUEST_CODE = 0
    const val FIRE_ACTION = "com.elroi.lemurloop.action.TIMER_FIRED"
    const val SHOW_ACTION = "com.elroi.lemurloop.action.OPEN_TIMER_TAB"
    const val EXTRA_OPEN_TIMER_TAB = "com.elroi.lemurloop.extra.OPEN_TIMER_TAB"
    const val FIRE_COMPONENT = "com.elroi.lemurloop.receiver.TimerFiredReceiver"
    const val SHOW_COMPONENT = "com.elroi.lemurloop.MainActivity"
    private const val DATA_PREFIX = "lemurloop://timer/"

    fun dataUri(timerId: Long): String = "$DATA_PREFIX$timerId"

    fun fire(timerId: Long): TimerIntentSpec = spec(
        timerId = timerId,
        action = FIRE_ACTION,
        componentClassName = FIRE_COMPONENT,
        opensTimerTab = false,
        isFireBroadcast = true
    )

    fun show(timerId: Long): TimerIntentSpec = spec(
        timerId = timerId,
        action = SHOW_ACTION,
        componentClassName = SHOW_COMPONENT,
        opensTimerTab = true,
        isFireBroadcast = false
    )

    fun cancel(timerId: Long): TimerIntentSpec = fire(timerId)

    fun timerId(uri: String?): Long? {
        if (uri == null || !uri.startsWith(DATA_PREFIX)) return null
        return uri.removePrefix(DATA_PREFIX).toLongOrNull()
    }

    fun wantsTimerTab(action: String?, openTimerTabExtra: Boolean): Boolean {
        return action == SHOW_ACTION || openTimerTabExtra
    }

    private fun spec(
        timerId: Long,
        action: String,
        componentClassName: String,
        opensTimerTab: Boolean,
        isFireBroadcast: Boolean
    ) = TimerIntentSpec(
        dataUri = dataUri(timerId),
        action = action,
        requestCode = REQUEST_CODE,
        componentClassName = componentClassName,
        opensTimerTab = opensTimerTab,
        isFireBroadcast = isFireBroadcast
    )
}

enum class TimerAlarmScheduleMode {
    SetAlarmClock,
    SetAndAllowWhileIdle
}

fun timerAlarmScheduleMode(sdkInt: Int, canScheduleExactAlarms: Boolean): TimerAlarmScheduleMode {
    return if (sdkInt < 31 || canScheduleExactAlarms) {
        TimerAlarmScheduleMode.SetAlarmClock
    } else {
        TimerAlarmScheduleMode.SetAndAllowWhileIdle
    }
}
