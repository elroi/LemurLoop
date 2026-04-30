package com.elroi.lemurloop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elroi.lemurloop.domain.buddy.AlarmBuddyLifecycleNotifier
import com.elroi.lemurloop.domain.model.Alarm
import com.elroi.lemurloop.domain.manager.SettingsManager
import com.elroi.lemurloop.domain.manager.AlarmDefaults
import com.elroi.lemurloop.domain.repository.AlarmRepository
import com.elroi.lemurloop.domain.scheduler.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val settingsManager: SettingsManager,
    private val accountabilityManager: com.elroi.lemurloop.domain.manager.AccountabilityManager,
    private val buddyLifecycleNotifier: AlarmBuddyLifecycleNotifier
) : ViewModel() {

    val confirmedBuddyNumbers: StateFlow<Set<String>> = settingsManager.confirmedBuddyNumbersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val pendingBuddyCodes: StateFlow<Set<String>> = settingsManager.pendingBuddyCodesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val globalBuddies: StateFlow<Set<String>> = settingsManager.globalBuddiesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val alarms: StateFlow<List<Alarm>> = repository.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val defaultAlarmSettings: StateFlow<AlarmDefaults> = settingsManager.alarmDefaultsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlarmDefaults())

    val alarmCreationStyle: StateFlow<String> = settingsManager.alarmCreationStyleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "WIZARD")

    val isCloudAiEnabled: StateFlow<Boolean> = settingsManager.isCloudAiEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val geminiApiKey: StateFlow<String> = settingsManager.geminiApiKeyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun toggleAlarm(alarm: Alarm, isEnabled: Boolean) {
        viewModelScope.launch {
            val previous = repository.getAlarmById(alarm.id) ?: alarm
            repository.updateAlarmToggle(alarm.id, isEnabled)
            val updated = previous.copy(isEnabled = isEnabled)
            if (isEnabled) {
                scheduler.schedule(updated)
            } else {
                scheduler.cancel(updated)
            }
            buddyLifecycleNotifier.onAlarmSaved(previous, updated)
        }
    }

    fun addAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val previous = repository.getAlarmById(alarm.id)
            repository.insertAlarm(alarm)
            if (alarm.isEnabled) {
                scheduler.schedule(alarm)
            }
            buddyLifecycleNotifier.onAlarmSaved(previous, alarm)
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            scheduler.cancel(alarm)
            repository.deleteAlarm(alarm)
        }
    }
    
    suspend fun getAlarm(id: String): Alarm? {
        return repository.getAlarmById(id)
    }

    fun sendBuddyOptInRequest(phoneNumber: String, buddyName: String?, userName: String?) {
        viewModelScope.launch {
            accountabilityManager.sendBuddyOptInRequest(phoneNumber, buddyName, userName)
        }
    }

    fun addGlobalBuddy(name: String, phoneNumber: String) {
        viewModelScope.launch {
            settingsManager.addGlobalBuddy(name, phoneNumber)
        }
    }

    fun updateAlarmDefaults(defaults: AlarmDefaults) {
        viewModelScope.launch {
            settingsManager.saveAlarmDefaults(defaults)
        }
    }

    fun updateAlarmCreationStyle(style: String) {
        viewModelScope.launch {
            settingsManager.saveAlarmCreationStyle(style)
        }
    }

    fun updateCloudAiEnabled(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsManager.saveIsCloudAiEnabled(isEnabled)
        }
    }

    fun updateGeminiApiKey(apiKey: String) {
        viewModelScope.launch {
            settingsManager.saveGeminiApiKey(apiKey.trim())
        }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            settingsManager.saveBriefingUserName(name.trim())
        }
    }
}
