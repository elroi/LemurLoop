package com.elroi.lemurloop.domain.timer

fun interface TimerRingStop {
    fun stop()
}

fun interface TimerRingControls {
    fun dismiss()
}

fun interface TimerTrackGrantReleaser {
    fun release(uri: String?)
}

fun interface TimerFinishedNotifier {
    fun notifyFinished(timer: CountdownTimer)
}

object TimerFinishedNotification {
    const val ID = 220
    const val CHANNEL_ID = "timer_finished"
}

/**
 * The ring service, once it exists, sets this while playback is running.
 * Until then a ringing row is treated as unattended and re-armed.
 */
object TimerRingSession {
    private val running = java.util.concurrent.atomic.AtomicBoolean(false)

    fun isRunning(): Boolean = running.get()

    fun setRunning(value: Boolean) {
        running.set(value)
    }
}
