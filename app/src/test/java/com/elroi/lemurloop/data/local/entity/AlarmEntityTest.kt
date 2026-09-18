package com.elroi.lemurloop.data.local.entity

import com.elroi.lemurloop.domain.model.Alarm
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Migration 24->25 added the aiPersona column (per-alarm persona override, null = follow the
 * global default). These tests pin the AlarmEntity <-> Alarm round trip for that field.
 */
class AlarmEntityTest {

    @Test
    fun `toDomain and fromDomain round trip a non-null aiPersona`() {
        val alarm = Alarm(time = LocalTime.of(7, 0), aiPersona = "ZEN")

        val roundTripped = AlarmEntity.fromDomain(alarm).toDomain()

        assertEquals("ZEN", roundTripped.aiPersona)
    }

    @Test
    fun `toDomain and fromDomain round trip a null aiPersona`() {
        val alarm = Alarm(time = LocalTime.of(7, 0), aiPersona = null)

        val roundTripped = AlarmEntity.fromDomain(alarm).toDomain()

        assertNull(roundTripped.aiPersona)
    }
}
