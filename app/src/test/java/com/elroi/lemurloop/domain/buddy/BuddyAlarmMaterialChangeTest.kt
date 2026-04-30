package com.elroi.lemurloop.domain.buddy

import com.elroi.lemurloop.domain.model.Alarm
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class BuddyAlarmMaterialChangeTest {

    private fun base(): Alarm = Alarm(
        time = LocalTime.of(7, 30),
        daysOfWeek = listOf(1, 2),
        isEnabled = true
    )

    @Test
    fun `time change is material`() {
        val before = base()
        val after = before.copy(time = LocalTime.of(8, 0))
        assertTrue(hasMaterialBuddyRelevantScheduleChange(before, after))
    }

    @Test
    fun `days order normalization not material`() {
        val before = base().copy(daysOfWeek = listOf(2, 1))
        val after = base().copy(daysOfWeek = listOf(1, 2))
        assertFalse(hasMaterialBuddyRelevantScheduleChange(before, after))
    }

    @Test
    fun `days content change is material`() {
        val before = base()
        val after = before.copy(daysOfWeek = listOf(1, 2, 3))
        assertTrue(hasMaterialBuddyRelevantScheduleChange(before, after))
    }

    @Test
    fun `enabled toggle is material`() {
        val before = base()
        val after = before.copy(isEnabled = false)
        assertTrue(hasMaterialBuddyRelevantScheduleChange(before, after))
    }

    @Test
    fun `label only not material`() {
        val before = base().copy(label = "A")
        val after = base().copy(label = "B")
        assertFalse(hasMaterialBuddyRelevantScheduleChange(before, after))
    }

    @Test
    fun `buddy delay only not material`() {
        val before = base().copy(buddyAlertDelayMinutes = 5)
        val after = base().copy(buddyAlertDelayMinutes = 10)
        assertFalse(hasMaterialBuddyRelevantScheduleChange(before, after))
    }
}
