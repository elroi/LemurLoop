package com.elroi.lemurloop.ui.screen.timer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
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
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
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
            }
        }
    }
}
