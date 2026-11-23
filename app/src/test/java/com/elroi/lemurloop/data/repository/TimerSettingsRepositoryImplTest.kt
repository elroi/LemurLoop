package com.elroi.lemurloop.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.elroi.lemurloop.domain.timer.TimerSettings
import com.elroi.lemurloop.domain.timer.TimerSound
import com.elroi.lemurloop.domain.timer.TimerVibration
import com.elroi.lemurloop.domain.timer.TimerVoiceCueGlobals
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimerSettingsRepositoryImplTest {

    @Test
    fun patternName_lightMediumStrong_areDistinctRhythms() {
        assertEquals("RAPID_PULSE", TimerVibration.Light.patternName)
        assertEquals("BASIC", TimerVibration.Medium.patternName)
        assertEquals("STACCATO", TimerVibration.Strong.patternName)
        assertNull(TimerVibration.Off.patternName)
        assertFalse(TimerVibration.entries.any { it.patternName == "PULSE" })
        assertFalse(TimerVibration.entries.any { it.patternName == "HEARTBEAT" })
    }

    @Test
    fun fromStored_pulse_isNotKept() {
        assertEquals(TimerVibration.Medium, TimerVibration.fromStored("PULSE"))
        assertEquals(TimerVibration.Medium, TimerVibration.fromStored("HEARTBEAT"))
    }

    @Test
    fun get_emptyStore_usesTheTimerDefaults() = runBlocking {
        withRepository { repository, _ ->
            val settings = repository.get()
            assertEquals(TimerSettings(), settings)
            assertFalse(settings.voiceCuesMasterEnabled)
            assertEquals(true, settings.voiceCueStartEnabled)
            assertEquals(true, settings.voiceCueMidRemainingEnabled)
            assertEquals(true, settings.voiceCueLastTenEnabled)
            assertEquals(true, settings.voiceCueAnnounceOnResume)
        }
    }

    @Test
    fun update_thenGet_roundTripsVoiceCueFlags() = runBlocking {
        withRepository { repository, store ->
            val written = TimerSettings(
                voiceCuesMasterEnabled = true,
                voiceCueStartEnabled = false,
                voiceCueMidRemainingEnabled = false,
                voiceCueLastTenEnabled = false,
                voiceCueAnnounceOnResume = false
            )

            repository.update(written)

            assertEquals(written, repository.get())
            val stored = store.data.first()
            assertEquals(true, stored[booleanPreferencesKey("voice_cues_master_enabled")])
            assertEquals(false, stored[booleanPreferencesKey("voice_cue_start_enabled")])
            assertEquals(false, stored[booleanPreferencesKey("voice_cue_mid_remaining_enabled")])
            assertEquals(false, stored[booleanPreferencesKey("voice_cue_last_ten_enabled")])
            assertEquals(false, stored[booleanPreferencesKey("voice_cue_announce_on_resume")])
        }
    }

    @Test
    fun toVoiceCueGlobals_mapsAllVoiceCueFlags() {
        val settings = TimerSettings(
            voiceCuesMasterEnabled = true,
            voiceCueStartEnabled = false,
            voiceCueMidRemainingEnabled = false,
            voiceCueLastTenEnabled = false,
            voiceCueAnnounceOnResume = false
        )

        assertEquals(
            TimerVoiceCueGlobals(
                masterEnabled = true,
                startEnabled = false,
                midRemainingEnabled = false,
                lastTenEnabled = false,
                announceOnResume = false
            ),
            settings.toVoiceCueGlobals()
        )
    }

    @Test
    fun update_thenGet_roundTripsThePhoneTrack() = runBlocking {
        withRepository { repository, _ ->
            val written = TimerSettings(
                sound = TimerSound.PhoneTrack,
                phoneTrackUri = "content://com.android.providers.media.documents/document/audio%3A1",
                volumePercent = 40,
                prioritizeVolume = false,
                allowVolumeKeyChanges = false,
                rampMinutes = 0,
                vibration = TimerVibration.Strong,
                keepScreenOn = false,
                capturedAlarmVolume = 7
            )
            repository.update(written)
            assertEquals(written, repository.get())
        }
    }

    @Test
    fun update_clampsVolumeAndRamp() = runBlocking {
        withRepository { repository, _ ->
            repository.update(TimerSettings(volumePercent = 140, rampMinutes = 15))
            val high = repository.get()
            assertEquals(100, high.volumePercent)
            assertEquals(10, high.rampMinutes)

            repository.update(TimerSettings(volumePercent = -5, rampMinutes = -1))
            val low = repository.get()
            assertEquals(0, low.volumePercent)
            assertEquals(0, low.rampMinutes)
        }
    }

    @Test
    fun update_afterAStoredPulse_doesNotLeavePulseInTheStore() = runBlocking {
        withRepository { repository, store ->
            store.edit { prefs ->
                prefs[stringPreferencesKey(TimerSettingsRepositoryImpl.VIBRATION_KEY)] = "PULSE"
            }
            val read = repository.get()
            assertEquals(TimerVibration.Medium, read.vibration)
            repository.update(read)
            val stored = store.data.first()[stringPreferencesKey(TimerSettingsRepositoryImpl.VIBRATION_KEY)]
            assertEquals(TimerVibration.Medium.patternName, stored)
            assertNotEquals("PULSE", stored)
        }
    }

    @Test
    fun clear_dropsThePhoneTrack() = runBlocking {
        withRepository { repository, _ ->
            repository.update(
                TimerSettings(
                    sound = TimerSound.Silent,
                    phoneTrackUri = "content://tracks/old"
                )
            )
            repository.clear()
            assertEquals(TimerSettings(), repository.get())
        }
    }

    private suspend fun withRepository(
        block: suspend (TimerSettingsRepositoryImpl, androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>) -> Unit
    ) {
        val directory = kotlin.io.path.createTempDirectory("timer-settings").toFile()
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val store = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(directory, "timer_settings.preferences_pb") }
        )
        try {
            block(TimerSettingsRepositoryImpl(store), store)
        } finally {
            scope.cancel()
            directory.deleteRecursively()
        }
    }
}
