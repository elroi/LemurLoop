package com.elroi.lemurloop.domain.timer

enum class TimerSound {
    DeviceAlarmRingtone,
    PhoneTrack,
    Silent
}

/**
 * Vibration is a rhythm. Strong is STACCATO, not a heavier buzz.
 * PULSE is not a real pattern and is never stored.
 */
enum class TimerVibration {
    Off,
    Light,
    Medium,
    Strong;

    val patternName: String?
        get() = when (this) {
            Off -> null
            Light -> "RAPID_PULSE"
            Medium -> "BASIC"
            Strong -> "STACCATO"
        }

    fun storedValue(): String = patternName ?: OFF_STORED

    companion object {
        private const val OFF_STORED = "OFF"

        fun fromStored(raw: String?): TimerVibration {
            return when (raw) {
                "RAPID_PULSE" -> Light
                "BASIC" -> Medium
                "STACCATO" -> Strong
                OFF_STORED -> Off
                else -> Medium
            }
        }
    }
}

data class TimerSettings(
    val sound: TimerSound = TimerSound.DeviceAlarmRingtone,
    val phoneTrackUri: String? = null,
    val volumePercent: Int = 100,
    val prioritizeVolume: Boolean = true,
    val allowVolumeKeyChanges: Boolean = true,
    val rampMinutes: Int = 1,
    val vibration: TimerVibration = TimerVibration.Medium,
    val keepScreenOn: Boolean = true,
    val capturedAlarmVolume: Int? = null
) {
    fun normalized(): TimerSettings {
        return copy(
            volumePercent = volumePercent.coerceIn(0, 100),
            rampMinutes = rampMinutes.coerceIn(0, 10),
            phoneTrackUri = phoneTrackUri?.ifBlank { null }
        )
    }
}
