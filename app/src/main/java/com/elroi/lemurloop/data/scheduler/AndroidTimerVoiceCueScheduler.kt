package com.elroi.lemurloop.data.scheduler

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.elroi.lemurloop.domain.scheduler.TimerVoiceCueScheduler
import com.elroi.lemurloop.domain.timer.TimerVoiceCueIntentSpec
import com.elroi.lemurloop.domain.timer.TimerVoiceCueIntents
import com.elroi.lemurloop.domain.timer.TimerVoiceCueKind
import com.elroi.lemurloop.domain.timer.TimerVoiceCueScheduleMode
import com.elroi.lemurloop.domain.timer.timerVoiceCueScheduleMode
import com.elroi.lemurloop.receiver.TimerVoiceCueReceiver

class AndroidTimerVoiceCueScheduler(
    private val context: Context
) : TimerVoiceCueScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    @SuppressLint("ScheduleExactAlarm")
    override fun schedule(
        timerId: Long,
        kind: TimerVoiceCueKind,
        triggerAtEpochMillis: Long
    ) {
        val cue = pendingBroadcast(
            spec = TimerVoiceCueIntents.fire(timerId, kind),
            scheduledFireEpochMillis = triggerAtEpochMillis
        )
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        when (timerVoiceCueScheduleMode(Build.VERSION.SDK_INT, canExact)) {
            TimerVoiceCueScheduleMode.SetExactAndAllowWhileIdle -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtEpochMillis,
                    cue
                )
            }
            TimerVoiceCueScheduleMode.SetAndAllowWhileIdle -> {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtEpochMillis,
                    cue
                )
            }
        }
    }

    override fun cancel(timerId: Long, kind: TimerVoiceCueKind) {
        alarmManager.cancel(pendingBroadcast(TimerVoiceCueIntents.fire(timerId, kind)))
    }

    override fun cancelAllForTimer(timerId: Long) {
        ALARM_MANAGER_CUE_KINDS.forEach { kind -> cancel(timerId, kind) }
    }

    private fun pendingBroadcast(
        spec: TimerVoiceCueIntentSpec,
        scheduledFireEpochMillis: Long? = null
    ): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            spec.requestCode,
            intentFor(spec).apply {
                scheduledFireEpochMillis?.let {
                    putExtra(TimerVoiceCueReceiver.EXTRA_SCHEDULED_FIRE_EPOCH_MILLIS, it)
                }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun intentFor(spec: TimerVoiceCueIntentSpec): Intent {
        return Intent(spec.action).apply {
            setClassName(context, spec.componentClassName)
            data = Uri.parse(spec.dataUri)
        }
    }

    private companion object {
        val ALARM_MANAGER_CUE_KINDS = listOf(
            TimerVoiceCueKind.MidRemaining,
            TimerVoiceCueKind.EnterLastTen
        )
    }
}
