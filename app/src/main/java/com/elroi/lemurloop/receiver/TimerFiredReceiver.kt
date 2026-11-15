package com.elroi.lemurloop.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.elroi.lemurloop.domain.timer.TimerAlarmIntents
import com.elroi.lemurloop.service.TimerRingService

/**
 * Alarm broadcast for one timer. Starts the ring service. Boot and clock changes do not.
 */
class TimerFiredReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timerId = TimerAlarmIntents.timerId(intent.dataString) ?: return
        val service = Intent(context, TimerRingService::class.java).apply {
            action = TimerRingService.ACTION_FIRE
            putExtra(TimerRingService.EXTRA_TIMER_ID, timerId)
        }
        context.startForegroundService(service)
    }
}
