package com.elroi.lemurloop.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.elroi.lemurloop.domain.repository.TimerSettingsRepository
import com.elroi.lemurloop.domain.timer.TimerSettings
import com.elroi.lemurloop.domain.timer.TimerSound
import com.elroi.lemurloop.domain.timer.TimerVibration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TimerSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : TimerSettingsRepository {

    override suspend fun get(): TimerSettings = read(dataStore.data.first())

    override fun observe(): Flow<TimerSettings> = dataStore.data.map { read(it) }

    override suspend fun update(settings: TimerSettings) {
        val normalized = settings.normalized()
        dataStore.edit { prefs ->
            prefs[SOUND] = normalized.sound.name
            if (normalized.phoneTrackUri == null) {
                prefs.remove(PHONE_TRACK_URI)
            } else {
                prefs[PHONE_TRACK_URI] = normalized.phoneTrackUri
            }
            prefs[VOLUME_PERCENT] = normalized.volumePercent
            prefs[PRIORITIZE_VOLUME] = normalized.prioritizeVolume
            prefs[ALLOW_VOLUME_KEY_CHANGES] = normalized.allowVolumeKeyChanges
            prefs[RAMP_MINUTES] = normalized.rampMinutes
            prefs[VIBRATION] = normalized.vibration.storedValue()
            prefs[KEEP_SCREEN_ON] = normalized.keepScreenOn
            if (normalized.capturedAlarmVolume == null) {
                prefs.remove(CAPTURED_ALARM_VOLUME)
            } else {
                prefs[CAPTURED_ALARM_VOLUME] = normalized.capturedAlarmVolume
            }
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun read(prefs: Preferences): TimerSettings {
        val sound = prefs[SOUND]?.let { raw ->
            runCatching { TimerSound.valueOf(raw) }.getOrNull()
        } ?: TimerSound.DeviceAlarmRingtone
        return TimerSettings(
            sound = sound,
            phoneTrackUri = prefs[PHONE_TRACK_URI],
            volumePercent = prefs[VOLUME_PERCENT] ?: 100,
            prioritizeVolume = prefs[PRIORITIZE_VOLUME] ?: true,
            allowVolumeKeyChanges = prefs[ALLOW_VOLUME_KEY_CHANGES] ?: true,
            rampMinutes = prefs[RAMP_MINUTES] ?: 1,
            vibration = TimerVibration.fromStored(prefs[VIBRATION]),
            keepScreenOn = prefs[KEEP_SCREEN_ON] ?: true,
            capturedAlarmVolume = prefs[CAPTURED_ALARM_VOLUME]
        ).normalized()
    }

    companion object {
        const val VIBRATION_KEY = "vibration"
        private val SOUND = stringPreferencesKey("sound")
        private val PHONE_TRACK_URI = stringPreferencesKey("phone_track_uri")
        private val VOLUME_PERCENT = intPreferencesKey("volume_percent")
        private val PRIORITIZE_VOLUME = booleanPreferencesKey("prioritize_volume")
        private val ALLOW_VOLUME_KEY_CHANGES = booleanPreferencesKey("allow_volume_key_changes")
        private val RAMP_MINUTES = intPreferencesKey("ramp_minutes")
        private val VIBRATION = stringPreferencesKey(VIBRATION_KEY)
        private val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        private val CAPTURED_ALARM_VOLUME = intPreferencesKey("captured_alarm_volume")
    }
}
