package com.elroi.lemurloop.data.scheduler

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.elroi.lemurloop.domain.scheduler.TimerScheduler
import com.elroi.lemurloop.domain.timer.TimerAlarmIntents
import com.elroi.lemurloop.domain.timer.TimerAlarmScheduleMode
import com.elroi.lemurloop.domain.timer.TimerIntentSpec
import com.elroi.lemurloop.domain.timer.timerAlarmScheduleMode

class AndroidTimerScheduler(
    private val context: Context
) : TimerScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    @SuppressLint("ScheduleExactAlarm")
    override fun schedule(timerId: Long, triggerAtEpochMillis: Long) {
        val fire = pendingBroadcast(TimerAlarmIntents.fire(timerId))
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        when (timerAlarmScheduleMode(Build.VERSION.SDK_INT, canExact)) {
            TimerAlarmScheduleMode.SetAlarmClock -> {
                val show = pendingActivity(TimerAlarmIntents.show(timerId))
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtEpochMillis, show),
                    fire
                )
            }
            TimerAlarmScheduleMode.SetAndAllowWhileIdle -> {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtEpochMillis,
                    fire
                )
            }
        }
    }

    override fun cancel(timerId: Long) {
        alarmManager.cancel(pendingBroadcast(TimerAlarmIntents.cancel(timerId)))
    }

    private fun pendingBroadcast(spec: TimerIntentSpec): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            spec.requestCode,
            intentFor(spec),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun pendingActivity(spec: TimerIntentSpec): PendingIntent {
        val intent = intentFor(spec).apply {
            putExtra(TimerAlarmIntents.EXTRA_OPEN_TIMER_TAB, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            context,
            spec.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun intentFor(spec: TimerIntentSpec): Intent {
        return Intent(spec.action).apply {
            setClassName(context, spec.componentClassName)
            data = Uri.parse(spec.dataUri)
        }
    }
}
