package com.elroi.lemurloop.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.elroi.lemurloop.domain.timer.TimerVoiceCueIntents
import com.elroi.lemurloop.domain.timer.TimerVoiceCueKind
import com.elroi.lemurloop.service.TimerVoiceCueService

class TimerVoiceCueReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val timerId = TimerVoiceCueIntents.timerId(intent.dataString) ?: return
        val kind = TimerVoiceCueIntents.kind(intent.dataString) ?: return
        val action = when (kind) {
            TimerVoiceCueKind.MidRemaining -> TimerVoiceCueService.ACTION_SPEAK_ONCE
            TimerVoiceCueKind.EnterLastTen -> TimerVoiceCueService.ACTION_LAST_TEN
            TimerVoiceCueKind.Start -> return
        }
        val serviceIntent = TimerVoiceCueService.intent(
            context = context,
            action = action,
            timerId = timerId,
            kind = kind,
            scheduledFireEpochMillis = intent.getLongExtra(
                EXTRA_SCHEDULED_FIRE_EPOCH_MILLIS,
                System.currentTimeMillis()
            )
        )
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (error: Exception) {
            Log.w(TAG, "Timer voice cue foreground-service start refused", error)
        }
    }

    companion object {
        const val EXTRA_SCHEDULED_FIRE_EPOCH_MILLIS =
            "com.elroi.lemurloop.extra.TIMER_VOICE_CUE_SCHEDULED_FIRE"
        private const val TAG = "TimerVoiceCueReceiver"
    }
}
