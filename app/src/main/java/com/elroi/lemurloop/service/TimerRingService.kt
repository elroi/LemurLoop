package com.elroi.lemurloop.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.elroi.lemurloop.R
import com.elroi.lemurloop.domain.repository.TimerRepository
import com.elroi.lemurloop.domain.repository.TimerSettingsRepository
import com.elroi.lemurloop.domain.scheduler.TimerScheduler
import com.elroi.lemurloop.domain.timer.TimerPlayback
import com.elroi.lemurloop.domain.timer.TimerPlaybackChoice
import com.elroi.lemurloop.domain.timer.TimerRingHandler
import com.elroi.lemurloop.domain.timer.TimerRingNotification
import com.elroi.lemurloop.domain.timer.TimerRingSession
import com.elroi.lemurloop.domain.timer.TimerVibration
import com.elroi.lemurloop.domain.timer.TimerVibrationRhythm
import com.elroi.lemurloop.ui.activity.TimerRingActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class TimerRingService : Service() {

    @Inject lateinit var timerRepository: TimerRepository
    @Inject lateinit var timerSettingsRepository: TimerSettingsRepository
    @Inject lateinit var timerScheduler: TimerScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var rampJob: Job? = null
    private var vibrator: Vibrator? = null
    private var foregroundStarted = false

    private val handler by lazy {
        TimerRingHandler(
            foreground = { promoteToForeground() },
            readSettings = { runBlocking { timerSettingsRepository.get() } },
            trackOpens = { uri -> trackOpens(uri) },
            loadTimer = { id -> runBlocking { timerRepository.getById(id) } },
            saveTimers = { timers ->
                runBlocking { timers.forEach { timerRepository.upsert(it) } }
            },
            loadRinging = { runBlocking { timerRepository.getAll() } },
            startPlayback = { choice -> startPlayback(choice) },
            startVibration = { vibration -> startVibration(vibration) },
            startRamp = { millis -> startRamp(millis) },
            restoreLeftover = { level -> setAlarmVolume(level) },
            captureVolume = { percent -> captureAlarmVolume(percent) },
            restoreCaptured = { level -> restoreCapturedVolume(level) },
            openScreen = { mainHandler.post { openRingScreen() } },
            stopPlayback = { releasePlayer() },
            stopVibration = { stopVibrator() },
            stopService = { stopRingService() },
            cancelAlarm = { id -> timerScheduler.cancel(id) }
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Foreground first, including silent rings. Settings are read only inside the handler.
        promoteToForeground()
        val action = intent?.action
        val timerId = intent?.getLongExtra(EXTRA_TIMER_ID, -1L) ?: -1L
        scope.launch {
            if (action == ACTION_DISMISS || timerId < 0L) {
                handler.onDismiss()
            } else {
                handler.onFire(timerId)
                if (player != null && rampJob == null) {
                    player?.setVolume(1f, 1f)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        rampJob?.cancel()
        releasePlayer()
        stopVibrator()
        restoreLeftoverCapturedVolume()
        TimerRingSession.setRunning(false)
        scope.cancel()
        super.onDestroy()
    }

    private fun promoteToForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                TimerRingNotification.ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(TimerRingNotification.ID, notification)
        }
        foregroundStarted = true
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TimerRingNotification.CHANNEL_ID,
                getString(R.string.timer_ring_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.setSound(null, null)
            channel.enableVibration(false)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            manager.createNotificationChannel(channel)
        }
        val open = ringActivityIntent()
        val content = PendingIntent.getActivity(
            this,
            2102,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismiss = PendingIntent.getService(
            this,
            2101,
            Intent(this, TimerRingService::class.java).setAction(ACTION_DISMISS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, TimerRingNotification.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.timer_ring_title))
            .setContentText(getString(R.string.timer_ring_body))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(content)
            .setFullScreenIntent(content, true)
            .addAction(0, getString(R.string.timer_ring_dismiss), dismiss)
            .build()
    }

    private fun ringActivityIntent(): Intent {
        return Intent(this, TimerRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    private fun openRingScreen() {
        TimerRingSession.setRunning(true)
        try {
            startActivity(ringActivityIntent())
        } catch (_: Exception) {
            // The full-screen notification is the lock-screen path when this launch is blocked.
        }
    }

    private fun startPlayback(choice: TimerPlaybackChoice) {
        if (choice is TimerPlaybackChoice.Silent) return
        releasePlayer()
        val opened = when (choice) {
            TimerPlaybackChoice.AlarmRingtone -> openAlarmRingtone()
            is TimerPlaybackChoice.PhoneTrack -> openPlayer(Uri.parse(choice.uri))
            TimerPlaybackChoice.Silent -> false
        }
        if (!opened && choice is TimerPlaybackChoice.PhoneTrack) {
            openAlarmRingtone()
        }
    }

    private fun openAlarmRingtone(): Boolean {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: return false
        return openPlayer(uri)
    }

    private fun openPlayer(uri: Uri): Boolean {
        val created = MediaPlayer()
        return try {
            created.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            created.setDataSource(this, uri)
            created.isLooping = true
            created.setVolume(0f, 0f)
            created.prepare()
            created.start()
            player = created
            true
        } catch (_: Exception) {
            try {
                created.release()
            } catch (_: Exception) {
            }
            false
        }
    }

    private fun startRamp(durationMillis: Long) {
        rampJob?.cancel()
        rampJob = scope.launch {
            var elapsed = 0L
            while (isActive && elapsed < durationMillis) {
                val gain = TimerPlayback.playerGain(elapsed, durationMillis)
                player?.setVolume(gain, gain)
                delay(RAMP_STEP_MILLIS)
                elapsed += RAMP_STEP_MILLIS
            }
            player?.setVolume(1f, 1f)
        }
    }

    private fun startVibration(vibration: TimerVibration) {
        val timings = TimerVibrationRhythm.waveform(vibration)
        if (timings.isEmpty()) return
        val motor = getSystemService(Vibrator::class.java) ?: return
        vibrator = motor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            motor.vibrate(VibrationEffect.createWaveform(timings, 0))
        } else {
            @Suppress("DEPRECATION")
            motor.vibrate(timings, 0)
        }
    }

    private fun captureAlarmVolume(percent: Int) {
        val audio = getSystemService(AudioManager::class.java) ?: return
        val current = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        runBlocking {
            val settings = timerSettingsRepository.get()
            timerSettingsRepository.update(settings.copy(capturedAlarmVolume = current))
        }
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        val target = (max * (percent / 100f)).roundToInt().coerceIn(0, max)
        setAlarmVolume(target)
    }

    private fun restoreCapturedVolume(level: Int) {
        setAlarmVolume(level)
        runBlocking {
            val settings = timerSettingsRepository.get()
            timerSettingsRepository.update(settings.copy(capturedAlarmVolume = null))
        }
    }

    private fun restoreLeftoverCapturedVolume() {
        val captured = runBlocking { timerSettingsRepository.get() }.capturedAlarmVolume ?: return
        restoreCapturedVolume(captured)
    }

    private fun setAlarmVolume(level: Int) {
        val audio = getSystemService(AudioManager::class.java) ?: return
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audio.setStreamVolume(AudioManager.STREAM_ALARM, level.coerceIn(0, max), 0)
    }

    private fun trackOpens(uri: String?): Boolean {
        if (uri.isNullOrBlank()) return false
        return try {
            contentResolver.openInputStream(Uri.parse(uri))?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun releasePlayer() {
        rampJob?.cancel()
        rampJob = null
        player?.run {
            try {
                stop()
            } catch (_: Exception) {
            }
            release()
        }
        player = null
    }

    private fun stopVibrator() {
        vibrator?.cancel()
        vibrator = null
    }

    private fun stopRingService() {
        TimerRingSession.setRunning(false)
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        stopSelf()
    }

    companion object {
        const val ACTION_FIRE = "com.elroi.lemurloop.action.TIMER_RING"
        const val ACTION_DISMISS = "com.elroi.lemurloop.action.TIMER_RING_DISMISS"
        const val EXTRA_TIMER_ID = "com.elroi.lemurloop.extra.TIMER_ID"
        private const val RAMP_STEP_MILLIS = 250L
    }
}
