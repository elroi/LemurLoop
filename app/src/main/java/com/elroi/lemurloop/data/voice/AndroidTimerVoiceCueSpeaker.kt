package com.elroi.lemurloop.data.voice

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.elroi.lemurloop.domain.timer.TimerVoiceCueKind
import com.elroi.lemurloop.domain.timer.TimerVoiceCueSpeaker
import com.elroi.lemurloop.service.TimerVoiceCueService

class AndroidTimerVoiceCueSpeaker(
    private val context: Context
) : TimerVoiceCueSpeaker {

    override fun deliver(
        timerId: Long,
        kind: TimerVoiceCueKind,
        endsAtEpochMillis: Long,
        label: String
    ) {
        val action = when (kind) {
            TimerVoiceCueKind.Start,
            TimerVoiceCueKind.MidRemaining -> TimerVoiceCueService.ACTION_SPEAK_ONCE
            TimerVoiceCueKind.EnterLastTen -> TimerVoiceCueService.ACTION_LAST_TEN
        }
        startSafely(
            TimerVoiceCueService.intent(
                context = context,
                action = action,
                timerId = timerId,
                kind = kind,
                scheduledFireEpochMillis = System.currentTimeMillis(),
                expectedEndsAtEpochMillis = endsAtEpochMillis,
                label = label
            )
        )
    }

    override fun cancelSpeech(timerId: Long) {
        startSafely(
            TimerVoiceCueService.intent(
                context = context,
                action = TimerVoiceCueService.ACTION_CANCEL_TIMER,
                timerId = timerId
            )
        )
    }

    private fun startSafely(intent: Intent) {
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (error: Exception) {
            Log.w(TAG, "Timer voice cue foreground-service start refused", error)
        }
    }

    private companion object {
        const val TAG = "TimerVoiceCueSpeaker"
    }
}
