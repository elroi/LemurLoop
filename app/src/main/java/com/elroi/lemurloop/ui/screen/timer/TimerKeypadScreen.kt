package com.elroi.lemurloop.ui.screen.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.hilt.navigation.compose.hiltViewModel
import com.elroi.lemurloop.R
import com.elroi.lemurloop.domain.timer.TimerDuration
import com.elroi.lemurloop.ui.navigation.Screen
import com.elroi.lemurloop.ui.viewmodel.TimerViewModel

private enum class KeypadField { Hours, Minutes, Seconds }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerKeypadScreen(
    navController: NavHostController,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val parent = remember(navController) { navController.getBackStackEntry(Screen.Timer.route) }
    val viewModel: TimerViewModel = hiltViewModel(parent)
    var hours by remember { mutableStateOf(0) }
    var minutes by remember { mutableStateOf(0) }
    var seconds by remember { mutableStateOf(0) }
    var field by remember { mutableStateOf(KeypadField.Minutes) }
    var label by remember { mutableStateOf("") }
    val total = TimerDuration.millis(hours, minutes, seconds)
    fun applyDigit(digit: Int) {
        val next = when (field) {
            KeypadField.Hours -> TimerDuration.parts(TimerDuration.millis(shiftDigit(hours, digit), minutes, seconds))
            KeypadField.Minutes -> TimerDuration.parts(TimerDuration.millis(hours, shiftDigit(minutes, digit), seconds))
            KeypadField.Seconds -> TimerDuration.parts(TimerDuration.millis(hours, minutes, shiftDigit(seconds, digit)))
        }
        hours = next.hours
        minutes = next.minutes
        seconds = next.seconds
    }
    fun start(duration: Long) {
        if (!TimerDuration.canStart(duration)) return
        viewModel.start(duration, label)
        onBack()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.timer_add)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_cancel))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.content_desc_timer_settings))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TimerDigits(formatParts(hours, minutes, seconds), MaterialTheme.typography.displayMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = field == KeypadField.Hours, onClick = { field = KeypadField.Hours }, label = { Text(stringResource(R.string.timer_hours)) })
                FilterChip(selected = field == KeypadField.Minutes, onClick = { field = KeypadField.Minutes }, label = { Text(stringResource(R.string.timer_minutes)) })
                FilterChip(selected = field == KeypadField.Seconds, onClick = { field = KeypadField.Seconds }, label = { Text(stringResource(R.string.timer_seconds)) })
            }
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.timer_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            DigitPad(onDigit = ::applyDigit)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimerDuration.presets.forEach { preset ->
                    TextButton(onClick = { start(preset) }) {
                        TimerDigits(formatPreset(preset), MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Button(
                onClick = { start(total) },
                enabled = TimerDuration.canStart(total),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.timer_start))
            }
        }
    }
}

@Composable
private fun DigitPad(onDigit: (Int) -> Unit) {
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9), listOf(0))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { digit ->
                    TextButton(onClick = { onDigit(digit) }, modifier = Modifier.weight(1f)) {
                        TimerDigits(digit.toString(), MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
    }
}

private fun shiftDigit(current: Int, digit: Int): Int = (current % 10) * 10 + digit

private fun formatParts(hours: Int, minutes: Int, seconds: Int): String {
    return "%02d:%02d:%02d".format(java.util.Locale.US, hours, minutes, seconds)
}

private fun formatPreset(millis: Long): String {
    val parts = TimerDuration.parts(millis)
    return formatParts(parts.hours, parts.minutes, parts.seconds)
}
