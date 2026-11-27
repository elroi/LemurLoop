package com.elroi.lemurloop.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerLastTenCadenceTest {

    private val ends = 100_000L

    @Test
    fun speakAt_isAnchoredToEndTime_notToPreviousUtterance() {
        // Each digit is due exactly one second after the previous one, however long
        // the previous digit took to say. Waiting for speech to finish caused drift.
        val times = (10 downTo 1).map { TimerLastTenCadence.speakAtEpochMillis(it, ends, 0L) }
        assertEquals((10 downTo 1).map { ends - it * 1_000L }, times)
        assertEquals(List(9) { 1_000L }, times.zipWithNext { a, b -> b - a })
    }

    @Test
    fun speakAt_subtractsOnsetLead_soAudioLandsOnTheSecond() {
        assertEquals(ends - 3_000L - 150L, TimerLastTenCadence.speakAtEpochMillis(3, ends, 150L))
    }

    @Test
    fun nextOnsetLead_smoothsAndClamps() {
        assertEquals(0L, TimerLastTenCadence.nextOnsetLeadMillis(0L, -50L))
        assertEquals(
            TimerLastTenCadence.MAX_ONSET_LEAD_MILLIS,
            TimerLastTenCadence.nextOnsetLeadMillis(
                TimerLastTenCadence.MAX_ONSET_LEAD_MILLIS,
                5_000L
            )
        )
        // Halfway between the previous estimate and the new observation.
        assertEquals(150L, TimerLastTenCadence.nextOnsetLeadMillis(100L, 200L))
    }

    @Test
    fun nextOnsetLead_firstObservationIsTakenAsIs() {
        assertEquals(120L, TimerLastTenCadence.nextOnsetLeadMillis(null, 120L))
    }
}
