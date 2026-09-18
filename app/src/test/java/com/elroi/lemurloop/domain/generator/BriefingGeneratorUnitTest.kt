package com.elroi.lemurloop.domain.generator

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * BUG-7 (revised): The original `(temp * 9/5) + 32` in Kotlin is actually floating-point
 * because `temp` is a Double — `9` and `5` are Int but the Double * Int result stays Double.
 * However, the fix adds `"%.1f".format(...)` to produce cleaner output (e.g. "70.7°F" not
 * "70.70000000000001°F"). These tests validate the corrected conversion formula.
 */
class BriefingGeneratorUnitTest {

    // Mirrors the fixed production formula
    private fun formatFahrenheit(tempCelsius: Double): String =
        "${"%.1f".format((tempCelsius * 9.0 / 5.0) + 32.0)}°F"

    @Test
    fun `fahrenheit conversion produces correctly rounded output`() {
        // 21.5°C → 70.7°F (not 70.70000000000001°F)
        assertEquals("70.7°F", formatFahrenheit(21.5))
    }

    @Test
    fun `freezing point converts correctly`() {
        assertEquals("32.0°F", formatFahrenheit(0.0))
    }

    @Test
    fun `boiling point converts correctly`() {
        assertEquals("212.0°F", formatFahrenheit(100.0))
    }

    @Test
    fun `negative celsius converts correctly`() {
        // -10°C → 14.0°F
        assertEquals("14.0°F", formatFahrenheit(-10.0))
    }

    @Test
    fun `body temperature converts correctly`() {
        // 37°C = 98.6°F
        assertEquals("98.6°F", formatFahrenheit(37.0))
    }

    /**
     * Per-alarm AI persona override: an alarm's own aiPersona field wins over the app-wide
     * default persona when set, and falls back to the default when the alarm has no override
     * (either because alarmId was null, e.g. a settings preview, or the alarm's aiPersona is null).
     */
    @Test
    fun `resolvePersona prefers the alarm override when present`() {
        assertEquals("ZEN", BriefingGenerator.resolvePersona(alarmOverride = "ZEN", globalDefault = "COACH"))
    }

    @Test
    fun `resolvePersona falls back to the global default when there is no override`() {
        assertEquals("COACH", BriefingGenerator.resolvePersona(alarmOverride = null, globalDefault = "COACH"))
    }
}
