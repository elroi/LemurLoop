package com.elroi.lemurloop.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.elroi.lemurloop.R
import com.elroi.lemurloop.domain.manager.SettingsManager
import com.elroi.lemurloop.domain.repository.TimerRepository
import com.elroi.lemurloop.domain.repository.TimerSettingsRepository
import com.elroi.lemurloop.domain.timer.CountdownTimer
import com.elroi.lemurloop.domain.timer.TimerPhase
import com.elroi.lemurloop.domain.timer.TimerVoiceCueBudget
import com.elroi.lemurloop.domain.timer.TimerVoiceCueFireGate
import com.elroi.lemurloop.domain.timer.TimerVoiceCueKind
import com.elroi.lemurloop.domain.timer.TimerVoiceCueNotification
import com.elroi.lemurloop.domain.timer.TimerVoiceCueResolver
import com.elroi.lemurloop.domain.timer.voiceOverrides
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Speaks short timer progress cues while the app is backgrounded or the screen is off.
 *
 * `mediaPlayback` matches the audible, alarm-adjacent purpose of [TimerRingService], but this
 * service never changes the alarm stream volume. Platform mute and Do Not Disturb rules still apply.
 */
@AndroidEntryPoint
class TimerVoiceCueService : Service() {

    @Inject lateinit var timerRepository: TimerRepository
    @Inject lateinit var timerSettingsRepository: TimerSettingsRepository
    @Inject lateinit var settingsManager: SettingsManager

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(serviceJob + Dispatchers.Main.immediate)
    private val sessions = mutableMapOf<Long, Job>()
    private val utterances = ConcurrentHashMap<String, CompletableDeferred<Unit>>()
    private val speechMutex = Mutex()
    private val ttsReady = CompletableDeferred<Boolean>()
    private var tts: TextToSpeech? = null
    private var latestStartId = 0
    private var foregroundStarted = false
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this) { status ->
            val engine = tts
            val ready = status == TextToSpeech.SUCCESS && engine != null
            if (ready) {
                engine?.setAudioAttributes(SPEECH_AUDIO_ATTRIBUTES)
                engine?.setOnUtteranceProgressListener(utteranceListener)
            } else {
                Log.w(TAG, "Dedicated timer TextToSpeech initialization failed: $status")
            }
            ttsReady.complete(ready)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestStartId = maxOf(latestStartId, startId)
        promoteToForeground()

        val timerId = intent?.getLongExtra(EXTRA_TIMER_ID, INVALID_TIMER_ID) ?: INVALID_TIMER_ID
        when (intent?.action) {
            ACTION_CANCEL_TIMER -> if (timerId != INVALID_TIMER_ID) cancelTimer(timerId)
            ACTION_SPEAK_ONCE,
            ACTION_LAST_TEN -> if (timerId != INVALID_TIMER_ID) startSession(intent, timerId)
            else -> finishIfIdle()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        sessions.values.forEach(Job::cancel)
        sessions.clear()
        utterances.values.forEach { it.cancel() }
        utterances.clear()
        abandonAudioFocus()
        tts?.stop()
        tts?.shutdown()
        tts = null
        scope.cancel()
        super.onDestroy()
    }

    private fun startSession(intent: Intent, timerId: Long) {
        sessions.remove(timerId)?.cancel()
        val action = intent.action
        val kind = intent.getStringExtra(EXTRA_KIND)?.let {
            runCatching { TimerVoiceCueKind.valueOf(it) }.getOrNull()
        } ?: return finishIfIdle()
        val scheduledFire = intent.getLongExtra(EXTRA_SCHEDULED_FIRE_EPOCH_MILLIS, 0L)
        val expectedEndsAt = intent.getLongExtra(EXTRA_EXPECTED_ENDS_AT_EPOCH_MILLIS, 0L)
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()

        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val timer = timerRepository.getById(timerId) ?: return@launch
                val endsAt = timer.endsAtEpochMillis ?: return@launch
                val effective = TimerVoiceCueResolver.effective(
                    timerSettingsRepository.get().toVoiceCueGlobals(),
                    timer.voiceOverrides()
                )
                val cueStillEffective = when (kind) {
                    TimerVoiceCueKind.Start -> effective.start
                    TimerVoiceCueKind.MidRemaining -> effective.midRemaining
                    TimerVoiceCueKind.EnterLastTen -> effective.lastTen
                }
                val now = System.currentTimeMillis()
                if (!TimerVoiceCueFireGate.shouldSpeak(
                        nowEpochMillis = now,
                        scheduledFireEpochMillis = scheduledFire.takeIf { it > 0L } ?: now,
                        endsAtEpochMillis = endsAt,
                        phaseIsRunning = timer.phase == TimerPhase.Running,
                        endsAtUnchanged = expectedEndsAt <= 0L || expectedEndsAt == endsAt,
                        cueStillEffective = cueStillEffective
                    )
                ) {
                    return@launch
                }

                speechMutex.withLock {
                    if (!ttsReady.await()) return@withLock
                    requestAudioFocus()
                    when (action) {
                        ACTION_LAST_TEN -> speakLastTen(timer, endsAt)
                        ACTION_SPEAK_ONCE -> speakOnce(kind, endsAt, label.ifBlank { timer.label })
                    }
                }
            } catch (error: Exception) {
                Log.w(TAG, "Timer voice cue failed for timer=$timerId", error)
            } finally {
                completeSession(timerId, coroutineContext.job)
            }
        }
        sessions[timerId] = job
        job.start()
    }

    private suspend fun speakOnce(
        kind: TimerVoiceCueKind,
        endsAtEpochMillis: Long,
        label: String
    ) {
        val localized = localizedSpeech() ?: return
        val body = when (kind) {
            TimerVoiceCueKind.Start -> localized.context.getString(R.string.timer_voice_cue_started)
            TimerVoiceCueKind.MidRemaining -> remainingCopy(
                localized.context,
                endsAtEpochMillis - System.currentTimeMillis()
            )
            TimerVoiceCueKind.EnterLastTen -> return
        }
        val text = if (label.isBlank()) {
            body
        } else {
            localized.context.getString(R.string.timer_voice_cue_labeled, label, body)
        }
        speakAndAwait(text)
    }

    private suspend fun speakLastTen(timer: CountdownTimer, endsAtEpochMillis: Long) {
        val powerManager = getSystemService(PowerManager::class.java) ?: return
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "LemurLoop:TimerVoiceCue:${timer.id}"
        )
        wakeLock.acquire(LAST_TEN_WAKE_LOCK_TIMEOUT_MILLIS)
        try {
            val localized = localizedSpeech() ?: return
            val initialDigits = TimerVoiceCueFireGate.lastTenDigitsToSpeak(
                System.currentTimeMillis(),
                endsAtEpochMillis
            )
            for (digit in initialDigits) {
                val idealEpoch = endsAtEpochMillis - digit * 1_000L
                val waitMillis = idealEpoch - System.currentTimeMillis()
                if (waitMillis > 0L) delay(waitMillis)
                val stillValid = TimerVoiceCueFireGate.lastTenDigitsToSpeak(
                    System.currentTimeMillis(),
                    endsAtEpochMillis
                ).contains(digit)
                if (stillValid) {
                    speakAndAwait(localized.context.getString(R.string.timer_voice_cue_digit, digit))
                }
            }
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private suspend fun localizedSpeech(): LocalizedSpeech? {
        val requested = settingsManager.appLanguageFlow.first()
        val current = resources.configuration.locales[0]?.language.orEmpty()
        val language = when (requested.lowercase()) {
            "he", "iw" -> "he"
            "en" -> "en"
            else -> if (current == "he" || current == "iw") "he" else "en"
        }
        val requestedLocale = if (language == "he") Locale("he", "IL") else Locale.US
        val languageResult = tts?.setLanguage(requestedLocale) ?: TextToSpeech.ERROR
        if (languageResult != TextToSpeech.LANG_MISSING_DATA &&
            languageResult != TextToSpeech.LANG_NOT_SUPPORTED &&
            languageResult != TextToSpeech.ERROR
        ) {
            return LocalizedSpeech(localizedContext(requestedLocale))
        }
        if (language == "he") {
            Log.w(TAG, "Hebrew timer TextToSpeech unavailable; falling back to English")
            val fallbackResult = tts?.setLanguage(Locale.US) ?: TextToSpeech.ERROR
            if (fallbackResult != TextToSpeech.LANG_MISSING_DATA &&
                fallbackResult != TextToSpeech.LANG_NOT_SUPPORTED &&
                fallbackResult != TextToSpeech.ERROR
            ) {
                return LocalizedSpeech(localizedContext(Locale.US))
            }
        }
        Log.w(TAG, "No supported TextToSpeech language for timer cue")
        return null
    }

    private fun localizedContext(locale: Locale): Context {
        val configuration = Configuration(resources.configuration).apply { setLocale(locale) }
        return createConfigurationContext(configuration)
    }

    private fun remainingCopy(context: Context, remainingMillis: Long): String {
        if (remainingMillis >= 60_000L) {
            val minutes = remainingMillis / 60_000L
            val resource = if (minutes == 1L) {
                R.string.timer_voice_cue_one_minute_left
            } else {
                R.string.timer_voice_cue_minutes_left
            }
            return context.getString(resource, minutes)
        }
        val seconds = kotlin.math.ceil(remainingMillis.coerceAtLeast(0L) / 1_000.0)
            .toLong()
            .coerceAtLeast(1L)
        val resource = if (seconds == 1L) {
            R.string.timer_voice_cue_one_second_left
        } else {
            R.string.timer_voice_cue_seconds_left
        }
        return context.getString(resource, seconds)
    }

    private suspend fun speakAndAwait(text: String) {
        val engine = tts ?: return
        val utteranceId = UUID.randomUUID().toString()
        val completion = CompletableDeferred<Unit>()
        utterances[utteranceId] = completion
        val result = engine.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            utterances.remove(utteranceId)
            return
        }
        completion.await()
    }

    private fun cancelTimer(timerId: Long) {
        val removed = sessions.remove(timerId)
        removed?.cancel()
        if (removed != null && sessions.isEmpty()) {
            tts?.stop()
            utterances.values.forEach { it.cancel() }
            utterances.clear()
        }
        finishIfIdle()
    }

    private fun completeSession(timerId: Long, completed: Job) {
        if (sessions[timerId] === completed) {
            sessions.remove(timerId)
        }
        finishIfIdle()
    }

    private fun finishIfIdle() {
        if (sessions.isNotEmpty()) return
        abandonAudioFocus()
        if (foregroundStarted) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        stopSelfResult(latestStartId)
    }

    private fun promoteToForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    TimerVoiceCueNotification.CHANNEL_ID,
                    getString(R.string.timer_voice_cue_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    setSound(null, null)
                    enableVibration(false)
                }
            )
        }
        val notification = NotificationCompat.Builder(this, TimerVoiceCueNotification.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(getString(R.string.timer_voice_cue_notification_title))
            .setContentText(getString(R.string.timer_voice_cue_notification_body))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                TimerVoiceCueNotification.ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(TimerVoiceCueNotification.ID, notification)
        }
        foregroundStarted = true
    }

    private fun requestAudioFocus() {
        val audioManager = getSystemService(AudioManager::class.java) ?: return
        if (audioFocusRequest == null) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(SPEECH_AUDIO_ATTRIBUTES)
                .setOnAudioFocusChangeListener { }
                .build()
        }
        audioFocusRequest?.let(audioManager::requestAudioFocus)
    }

    private fun abandonAudioFocus() {
        val request = audioFocusRequest ?: return
        getSystemService(AudioManager::class.java)?.abandonAudioFocusRequest(request)
        audioFocusRequest = null
    }

    private val utteranceListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) = Unit

        override fun onDone(utteranceId: String?) {
            utteranceId?.let { utterances.remove(it)?.complete(Unit) }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            utteranceId?.let { utterances.remove(it)?.complete(Unit) }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            onError(utteranceId)
        }
    }

    private data class LocalizedSpeech(val context: Context)

    companion object {
        const val ACTION_SPEAK_ONCE = "com.elroi.lemurloop.action.TIMER_VOICE_CUE_SPEAK_ONCE"
        const val ACTION_LAST_TEN = "com.elroi.lemurloop.action.TIMER_VOICE_CUE_LAST_TEN"
        const val ACTION_CANCEL_TIMER = "com.elroi.lemurloop.action.TIMER_VOICE_CUE_CANCEL_TIMER"

        private const val EXTRA_TIMER_ID = "com.elroi.lemurloop.extra.TIMER_VOICE_CUE_TIMER_ID"
        private const val EXTRA_KIND = "com.elroi.lemurloop.extra.TIMER_VOICE_CUE_KIND"
        private const val EXTRA_SCHEDULED_FIRE_EPOCH_MILLIS =
            "com.elroi.lemurloop.extra.TIMER_VOICE_CUE_SCHEDULED_FIRE"
        private const val EXTRA_EXPECTED_ENDS_AT_EPOCH_MILLIS =
            "com.elroi.lemurloop.extra.TIMER_VOICE_CUE_EXPECTED_END"
        private const val EXTRA_LABEL = "com.elroi.lemurloop.extra.TIMER_VOICE_CUE_LABEL"
        private const val INVALID_TIMER_ID = -1L
        private const val LAST_TEN_WAKE_LOCK_TIMEOUT_MILLIS =
            TimerVoiceCueBudget.LAST_TEN_WINDOW_MILLIS +
                TimerVoiceCueBudget.LAST_TEN_WARMUP_LEAD_MILLIS +
                2_000L
        private const val TAG = "TimerVoiceCueService"

        private val SPEECH_AUDIO_ATTRIBUTES = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        fun intent(
            context: Context,
            action: String,
            timerId: Long,
            kind: TimerVoiceCueKind? = null,
            scheduledFireEpochMillis: Long = 0L,
            expectedEndsAtEpochMillis: Long = 0L,
            label: String = ""
        ): Intent {
            return Intent(context, TimerVoiceCueService::class.java).apply {
                this.action = action
                putExtra(EXTRA_TIMER_ID, timerId)
                kind?.let { putExtra(EXTRA_KIND, it.name) }
                putExtra(EXTRA_SCHEDULED_FIRE_EPOCH_MILLIS, scheduledFireEpochMillis)
                putExtra(EXTRA_EXPECTED_ENDS_AT_EPOCH_MILLIS, expectedEndsAtEpochMillis)
                putExtra(EXTRA_LABEL, label)
            }
        }
    }
}
