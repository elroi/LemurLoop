package com.elroi.lemurloop.ui.screen.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.View
import com.elroi.lemurloop.R
import com.elroi.lemurloop.domain.timer.CountdownTimer
import com.elroi.lemurloop.domain.timer.TimerPhase
import com.elroi.lemurloop.domain.timer.formatCountdown

@Composable
fun KeepTimerScreenOn(enabled: Boolean) {
    if (!enabled) return
    AndroidView(
        modifier = Modifier.size(0.dp),
        factory = { context -> View(context).apply { keepScreenOn = true } }
    )
}

@Composable
fun TimerDigits(text: String, style: TextStyle) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(text = text, style = style)
    }
}

@Composable
fun TimerActions(
    timer: CountdownTimer,
    onPause: () -> Unit,
    onPlay: () -> Unit,
    onAddMinute: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        when (timer.phase) {
            TimerPhase.Running -> {
                TextButton(onClick = onPause) { Text(stringResource(R.string.timer_pause)) }
                TextButton(onClick = onAddMinute) { Text(stringResource(R.string.timer_add_minute)) }
            }
            TimerPhase.Paused -> {
                TextButton(onClick = onPlay) { Text(stringResource(R.string.timer_play)) }
                TextButton(onClick = onRefresh) { Text(stringResource(R.string.timer_refresh)) }
            }
            TimerPhase.Ringing -> {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.timer_ring_dismiss)) }
            }
        }
    }
}

fun countdownText(timer: CountdownTimer, nowEpochMillis: Long): String {
    val left = com.elroi.lemurloop.domain.timer.TimerTransitions.remainingMillis(timer, nowEpochMillis)
    return formatCountdown(left)
}
