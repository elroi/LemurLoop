package com.elroi.lemurloop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elroi.lemurloop.domain.repository.TimerSettingsRepository
import com.elroi.lemurloop.domain.timer.TimerSettings
import com.elroi.lemurloop.domain.timer.TimerSound
import com.elroi.lemurloop.domain.timer.TimerTrackGrantReleaser
import com.elroi.lemurloop.domain.timer.TimerVibration
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TimerSettingsViewModel @Inject constructor(
    private val repository: TimerSettingsRepository,
    private val grants: TimerTrackGrantReleaser
) : ViewModel() {
    val settings = repository.observe().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TimerSettings()
    )

    fun setSound(sound: TimerSound) = update { it.copy(sound = sound) }
    fun setVolume(percent: Int) = update { it.copy(volumePercent = percent) }
    fun setPrioritizeVolume(enabled: Boolean) = update { it.copy(prioritizeVolume = enabled) }
    fun setAllowVolumeKeys(enabled: Boolean) = update { it.copy(allowVolumeKeyChanges = enabled) }
    fun setRampMinutes(minutes: Int) = update { it.copy(rampMinutes = minutes) }
    fun setVibration(vibration: TimerVibration) = update { it.copy(vibration = vibration) }
    fun setKeepScreenOn(enabled: Boolean) = update { it.copy(keepScreenOn = enabled) }

    fun setTrack(uri: String) {
        viewModelScope.launch {
            val current = repository.get()
            if (current.phoneTrackUri != null && current.phoneTrackUri != uri) {
                grants.release(current.phoneTrackUri)
            }
            repository.update(current.copy(sound = TimerSound.PhoneTrack, phoneTrackUri = uri))
        }
    }

    fun clearTrack() {
        viewModelScope.launch {
            val current = repository.get()
            grants.release(current.phoneTrackUri)
            repository.update(current.copy(phoneTrackUri = null, sound = TimerSound.DeviceAlarmRingtone))
        }
    }

    private fun update(change: (TimerSettings) -> TimerSettings) {
        viewModelScope.launch {
            repository.update(change(repository.get()))
        }
    }
}
