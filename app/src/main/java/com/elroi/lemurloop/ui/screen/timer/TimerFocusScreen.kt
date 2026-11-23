package com.elroi.lemurloop.ui.screen.timer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.elroi.lemurloop.R
import com.elroi.lemurloop.domain.timer.keepTimerScreenOn
import com.elroi.lemurloop.ui.navigation.Screen
import com.elroi.lemurloop.ui.viewmodel.TimerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerFocusScreen(
    timerId: Long,
    navController: NavHostController,
    onBack: () -> Unit
) {
    val parent = remember(navController) { navController.getBackStackEntry(Screen.Timer.route) }
    val viewModel: TimerViewModel = hiltViewModel(parent)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val timer = state.timers.firstOrNull { it.id == timerId }
    LaunchedEffect(state.ready, timer) {
        if (state.ready && timer == null) onBack()
    }
    BackHandler(onBack = onBack)
    val keepOn = keepTimerScreenOn(state.timers)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(timer?.label?.ifBlank { stringResource(R.string.timer_title) } ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_cancel))
                    }
                }
            )
        }
    ) { padding ->
        if (timer != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                KeepTimerScreenOn(keepOn)
                TimerDigits(
                    text = countdownText(timer, state.nowEpochMillis),
                    style = MaterialTheme.typography.displayLarge
                )
                TimerActions(
                    timer = timer,
                    onPause = { viewModel.pause(timer.id) },
                    onPlay = { viewModel.play(timer.id) },
                    onAddMinute = { viewModel.addOneMinute(timer.id) },
                    onRefresh = { viewModel.refresh(timer.id) },
                    onDismiss = viewModel::dismissRinging
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.timer_voice_cue_overrides),
                        style = MaterialTheme.typography.titleMedium
                    )
                    VoiceCueOverrideRow(
                        label = stringResource(R.string.timer_voice_cue_start),
                        value = timer.voiceCueStartOverride,
                        onChange = { viewModel.setVoiceCueStartOverride(timer.id, it) }
                    )
                    VoiceCueOverrideRow(
                        label = stringResource(R.string.timer_voice_cue_mid_remaining),
                        value = timer.voiceCueMidRemainingOverride,
                        onChange = { viewModel.setVoiceCueMidRemainingOverride(timer.id, it) }
                    )
                    VoiceCueOverrideRow(
                        label = stringResource(R.string.timer_voice_cue_last_ten),
                        value = timer.voiceCueLastTenOverride,
                        onChange = { viewModel.setVoiceCueLastTenOverride(timer.id, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceCueOverrideRow(
    label: String,
    value: Boolean?,
    onChange: (Boolean?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = value == null,
                onClick = { onChange(null) },
                label = { Text(stringResource(R.string.timer_voice_cue_inherit)) }
            )
            FilterChip(
                selected = value == true,
                onClick = { onChange(true) },
                label = { Text(stringResource(R.string.timer_voice_cue_on)) }
            )
            FilterChip(
                selected = value == false,
                onClick = { onChange(false) },
                label = { Text(stringResource(R.string.timer_voice_cue_off)) }
            )
        }
    }
}
