package com.elroi.lemurloop.ui.activity

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.elroi.lemurloop.R
import com.elroi.lemurloop.domain.repository.TimerRepository
import com.elroi.lemurloop.domain.repository.TimerSettingsRepository
import com.elroi.lemurloop.domain.timer.TimerPhase
import com.elroi.lemurloop.domain.timer.TimerVolumeKeys
import com.elroi.lemurloop.service.TimerRingService
import com.elroi.lemurloop.ui.theme.LemurLoopTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class TimerRingActivity : ComponentActivity() {

    @Inject lateinit var timerRepository: TimerRepository
    @Inject lateinit var timerSettingsRepository: TimerSettingsRepository

    private var allowVolumeKeyChanges = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndShowWhenLocked()
        setContent {
            LemurLoopTheme {
                TimerRingContent(
                    repository = timerRepository,
                    onDismiss = { dismissRing() },
                    onEmpty = { finish() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            val settings = withContext(Dispatchers.IO) { timerSettingsRepository.get() }
            allowVolumeKeyChanges = settings.allowVolumeKeyChanges
            if (allowVolumeKeyChanges) {
                volumeControlStream = AudioManager.STREAM_ALARM
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (TimerVolumeKeys.consumes(allowVolumeKeyChanges, keyCode)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (TimerVolumeKeys.consumes(allowVolumeKeyChanges, keyCode)) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun dismissRing() {
        startService(
            Intent(this, TimerRingService::class.java).setAction(TimerRingService.ACTION_DISMISS)
        )
        finish()
    }

    private fun turnScreenOnAndShowWhenLocked() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)
                ?.requestDismissKeyguard(this, null)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }
}

@Composable
private fun TimerRingContent(
    repository: TimerRepository,
    onDismiss: () -> Unit,
    onEmpty: () -> Unit
) {
    var labels by remember { mutableStateOf<List<String>>(emptyList()) }
    var seenRinging by remember { mutableStateOf(false) }
    LaunchedEffect(repository) {
        repository.observeAll().collect { timers ->
            val ringing = timers.filter { it.phase == TimerPhase.Ringing }
            labels = ringing.map { it.label }
            if (ringing.isNotEmpty()) seenRinging = true
            if (seenRinging && ringing.isEmpty()) onEmpty()
        }
    }
    BackHandler(onBack = onDismiss)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = labels.firstOrNull { it.isNotBlank() } ?: stringResource(R.string.timer_ring_title),
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onDismiss,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text(stringResource(R.string.timer_ring_dismiss))
        }
    }
}
