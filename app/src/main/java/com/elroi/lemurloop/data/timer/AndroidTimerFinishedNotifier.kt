package com.elroi.lemurloop.data.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.elroi.lemurloop.R
import com.elroi.lemurloop.domain.timer.CountdownTimer
import com.elroi.lemurloop.domain.timer.TimerFinishedNotification
import com.elroi.lemurloop.domain.timer.TimerFinishedNotifier

class AndroidTimerFinishedNotifier(
    private val context: Context
) : TimerFinishedNotifier {
    override fun notifyFinished(timer: CountdownTimer) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TimerFinishedNotification.CHANNEL_ID,
                context.getString(R.string.timer_finished_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.setSound(null, null)
            channel.enableVibration(false)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, TimerFinishedNotification.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.timer_finished_title))
            .setContentText(context.getString(R.string.timer_finished_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            .build()
        manager.notify(timer.id.toString(), TimerFinishedNotification.ID, notification)
    }
}
