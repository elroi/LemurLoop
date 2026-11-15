package com.elroi.lemurloop.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerAlarmIntentsTest {

    @Test
    fun fire_eachTimerHasItsOwnDataUri() {
        assertEquals("lemurloop://timer/4", TimerAlarmIntents.fire(4L).dataUri)
        assertEquals("lemurloop://timer/9", TimerAlarmIntents.fire(9L).dataUri)
        assertNotEquals(TimerAlarmIntents.fire(4L).dataUri, TimerAlarmIntents.fire(9L).dataUri)
    }

    @Test
    fun requestCode_isSharedAndIsNotAHashOfTheId() {
        assertNotEquals(1L.hashCode(), TimerAlarmIntents.fire(1L).requestCode)
        assertEquals(TimerAlarmIntents.fire(1L).requestCode, TimerAlarmIntents.fire(2L).requestCode)
        assertEquals(TimerAlarmIntents.fire(4L).requestCode, TimerAlarmIntents.show(4L).requestCode)
    }

    @Test
    fun cancel_matchesTheFireIntent() {
        assertEquals(TimerAlarmIntents.fire(4L), TimerAlarmIntents.cancel(4L))
    }

    @Test
    fun show_opensTheTimerTabAndIsNotTheFireBroadcast() {
        val show = TimerAlarmIntents.show(4L)
        val fire = TimerAlarmIntents.fire(4L)

        assertTrue(show.opensTimerTab)
        assertFalse(show.isFireBroadcast)
        assertEquals("com.elroi.lemurloop.MainActivity", show.componentClassName)
        assertTrue(fire.isFireBroadcast)
        assertFalse(fire.opensTimerTab)
        assertEquals("com.elroi.lemurloop.receiver.TimerFiredReceiver", fire.componentClassName)
        assertNotEquals(fire.action, show.action)
        assertEquals(fire.dataUri, show.dataUri)
    }

    @Test
    fun timerAlarmScheduleMode_usesSetAlarmClockUnlessExactAlarmsAreDenied() {
        assertEquals(
            TimerAlarmScheduleMode.SetAlarmClock,
            timerAlarmScheduleMode(sdkInt = 30, canScheduleExactAlarms = false)
        )
        assertEquals(
            TimerAlarmScheduleMode.SetAlarmClock,
            timerAlarmScheduleMode(sdkInt = 31, canScheduleExactAlarms = true)
        )
        assertEquals(
            TimerAlarmScheduleMode.SetAndAllowWhileIdle,
            timerAlarmScheduleMode(sdkInt = 31, canScheduleExactAlarms = false)
        )
    }
}
