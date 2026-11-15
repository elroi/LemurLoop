package com.elroi.lemurloop.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerVibrationRhythmTest {

    @Test
    fun cycle_lightMediumStrong_differByTimingNotByStrength() {
        val light = TimerVibrationRhythm.steps(TimerVibration.Light)
        val medium = TimerVibrationRhythm.steps(TimerVibration.Medium)
        val strong = TimerVibrationRhythm.steps(TimerVibration.Strong)

        assertEquals(2, light.size)
        assertEquals(1, medium.size)
        assertEquals(3, strong.size)
        assertTrue(onMillis(strong) < onMillis(medium))
        assertEquals("RAPID_PULSE", TimerVibration.Light.patternName)
        assertEquals("BASIC", TimerVibration.Medium.patternName)
        assertEquals("STACCATO", TimerVibration.Strong.patternName)
    }

    @Test
    fun waveform_off_isEmpty() {
        assertEquals(0, TimerVibrationRhythm.waveform(TimerVibration.Off).size)
    }

    @Test
    fun waveform_patterns_keepTheirOwnTimings() {
        assertEquals(longArrayOf(0, 200, 100, 200, 400).asList(), TimerVibrationRhythm.waveform(TimerVibration.Light).asList())
        assertEquals(longArrayOf(0, 800, 400).asList(), TimerVibrationRhythm.waveform(TimerVibration.Medium).asList())
        assertEquals(longArrayOf(0, 80, 80, 80, 80, 80, 400).asList(), TimerVibrationRhythm.waveform(TimerVibration.Strong).asList())
    }

    private fun onMillis(steps: List<TimerVibrationStep>): Long = steps.sumOf { it.onMillis }
}
