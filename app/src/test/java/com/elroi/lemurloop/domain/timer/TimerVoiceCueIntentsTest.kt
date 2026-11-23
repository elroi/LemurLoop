package com.elroi.lemurloop.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerVoiceCueIntentsTest {

    @Test
    fun dataUri_isDistinctFromEndRingPrefix() {
        val uri = TimerVoiceCueIntents.dataUri(7L, TimerVoiceCueKind.MidRemaining)

        assertEquals("lemurloop://timer-cue/7/MidRemaining", uri)
        assertNull(TimerAlarmIntents.timerId(uri))
    }

    @Test
    fun parse_roundTripsIdAndKind() {
        val uri = TimerVoiceCueIntents.dataUri(42L, TimerVoiceCueKind.EnterLastTen)

        assertEquals(42L, TimerVoiceCueIntents.timerId(uri))
        assertEquals(TimerVoiceCueKind.EnterLastTen, TimerVoiceCueIntents.kind(uri))
    }

    @Test
    fun fire_targetsVoiceCueReceiver() {
        val spec = TimerVoiceCueIntents.fire(3L, TimerVoiceCueKind.MidRemaining)

        assertEquals("com.elroi.lemurloop.receiver.TimerVoiceCueReceiver", spec.componentClassName)
        assertEquals(TimerVoiceCueIntents.FIRE_ACTION, spec.action)
        assertEquals("lemurloop://timer-cue/3/MidRemaining", spec.dataUri)
    }

    @Test
    fun scheduleMode_neverUsesSetAlarmClock() {
        assertEquals(
            TimerVoiceCueScheduleMode.SetExactAndAllowWhileIdle,
            timerVoiceCueScheduleMode(sdkInt = 34, canScheduleExactAlarms = true)
        )
        assertEquals(
            TimerVoiceCueScheduleMode.SetAndAllowWhileIdle,
            timerVoiceCueScheduleMode(sdkInt = 34, canScheduleExactAlarms = false)
        )
        assertTrue(
            TimerVoiceCueScheduleMode.entries.none {
                it.name.contains("AlarmClock", ignoreCase = true)
            }
        )
    }
}
