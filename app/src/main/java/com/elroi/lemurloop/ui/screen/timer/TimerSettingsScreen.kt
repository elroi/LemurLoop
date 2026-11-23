package com.elroi.lemurloop.ui.screen.timer

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elroi.lemurloop.R
import com.elroi.lemurloop.domain.timer.TimerSound
import com.elroi.lemurloop.domain.timer.TimerVibration
import com.elroi.lemurloop.ui.viewmodel.TimerSettingsViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSettingsScreen(
    onBack: () -> Unit,
    viewModel: TimerSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
        }
        viewModel.setTrack(uri.toString())
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.timer_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_cancel))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.timer_sound), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoundChip(TimerSound.DeviceAlarmRingtone, R.string.timer_sound_ringtone, settings.sound, viewModel::setSound)
                SoundChip(TimerSound.PhoneTrack, R.string.timer_sound_track, settings.sound, viewModel::setSound)
                SoundChip(TimerSound.Silent, R.string.timer_sound_silent, settings.sound, viewModel::setSound)
            }
            if (settings.sound == TimerSound.PhoneTrack) {
                Button(onClick = { picker.launch(arrayOf("audio/*")) }) {
                    Text(stringResource(R.string.timer_choose_track))
                }
                if (settings.phoneTrackUri != null) {
                    Text(Uri.parse(settings.phoneTrackUri).lastPathSegment ?: settings.phoneTrackUri!!)
                    TextButton(onClick = viewModel::clearTrack) {
                        Text(stringResource(R.string.timer_clear_track))
                    }
                }
            }
            Text(stringResource(R.string.timer_volume, settings.volumePercent))
            Slider(
                value = settings.volumePercent.toFloat(),
                onValueChange = { viewModel.setVolume(it.roundToInt()) },
                valueRange = 0f..100f
            )
            SwitchRow(stringResource(R.string.timer_prioritize_volume), settings.prioritizeVolume, viewModel::setPrioritizeVolume)
            SwitchRow(stringResource(R.string.timer_volume_keys), settings.allowVolumeKeyChanges, viewModel::setAllowVolumeKeys)
            Text(
                if (settings.rampMinutes == 0) stringResource(R.string.timer_ramp_off)
                else stringResource(R.string.timer_ramp_minutes, settings.rampMinutes)
            )
            Slider(
                value = settings.rampMinutes.toFloat(),
                onValueChange = { viewModel.setRampMinutes(it.roundToInt()) },
                valueRange = 0f..10f,
                steps = 9
            )
            Text(stringResource(R.string.timer_vibration), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VibrationChip(TimerVibration.Off, R.string.timer_vibration_off, settings.vibration, viewModel::setVibration)
                VibrationChip(TimerVibration.Light, R.string.timer_vibration_light, settings.vibration, viewModel::setVibration)
                VibrationChip(TimerVibration.Medium, R.string.timer_vibration_medium, settings.vibration, viewModel::setVibration)
                VibrationChip(TimerVibration.Strong, R.string.timer_vibration_strong, settings.vibration, viewModel::setVibration)
            }
            Text(stringResource(R.string.timer_strong_is_rhythm), style = MaterialTheme.typography.bodySmall)
            SwitchRow(stringResource(R.string.timer_keep_screen_on), settings.keepScreenOn, viewModel::setKeepScreenOn)
            if (settings.sound == TimerSound.Silent && settings.vibration == TimerVibration.Off) {
                Text(stringResource(R.string.timer_quiet_ring), color = MaterialTheme.colorScheme.error)
            }
            Text(
                stringResource(R.string.timer_voice_cues),
                style = MaterialTheme.typography.titleMedium
            )
            SwitchRow(
                label = stringResource(R.string.timer_voice_cues_master),
                checked = settings.voiceCuesMasterEnabled,
                onChange = viewModel::setVoiceCuesMasterEnabled
            )
            SwitchRow(
                label = stringResource(R.string.timer_voice_cue_start),
                checked = settings.voiceCueStartEnabled,
                onChange = viewModel::setVoiceCueStartEnabled,
                enabled = settings.voiceCuesMasterEnabled
            )
            SwitchRow(
                label = stringResource(R.string.timer_voice_cue_mid_remaining),
                checked = settings.voiceCueMidRemainingEnabled,
                onChange = viewModel::setVoiceCueMidRemainingEnabled,
                enabled = settings.voiceCuesMasterEnabled
            )
            SwitchRow(
                label = stringResource(R.string.timer_voice_cue_last_ten),
                checked = settings.voiceCueLastTenEnabled,
                onChange = viewModel::setVoiceCueLastTenEnabled,
                enabled = settings.voiceCuesMasterEnabled
            )
            SwitchRow(
                label = stringResource(R.string.timer_voice_announce_on_resume),
                checked = settings.voiceCueAnnounceOnResume,
                onChange = viewModel::setVoiceCueAnnounceOnResume,
                enabled = settings.voiceCuesMasterEnabled
            )
        }
    }
}

@Composable
private fun SoundChip(
    sound: TimerSound,
    labelRes: Int,
    selected: TimerSound,
    onSelect: (TimerSound) -> Unit
) {
    FilterChip(selected = selected == sound, onClick = { onSelect(sound) }, label = { Text(stringResource(labelRes)) })
}

@Composable
private fun VibrationChip(
    vibration: TimerVibration,
    labelRes: Int,
    selected: TimerVibration,
    onSelect: (TimerVibration) -> Unit
) {
    FilterChip(
        selected = selected == vibration,
        onClick = { onSelect(vibration) },
        label = { Text(stringResource(labelRes)) }
    )
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled
        )
    }
}
