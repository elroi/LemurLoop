package com.elroi.lemurloop.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TimerVoiceCueNotificationTest {

    @Test
    fun id_isDedicatedAndDoesNotCollideWithExistingNotifications() {
        assertEquals(222, TimerVoiceCueNotification.ID)
        assertFalse(TimerVoiceCueNotification.ID in setOf(2, 111, 220, 221))
    }
}
