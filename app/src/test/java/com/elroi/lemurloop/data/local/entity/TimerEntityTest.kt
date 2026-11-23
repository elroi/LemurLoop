package com.elroi.lemurloop.data.local.entity

import com.elroi.lemurloop.domain.timer.CountdownTimer
import com.elroi.lemurloop.domain.timer.TimerPhase
import com.elroi.lemurloop.domain.timer.TimerVoiceCueOverrides
import com.elroi.lemurloop.domain.timer.voiceOverrides
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerEntityTest {

    @Test
    fun fromDomain_toDomain_preservesCreatedAtAndANullEnd() {
        val timer = CountdownTimer(
            id = 4L,
            label = "",
            originalDurationMillis = 5_000L,
            endsAtEpochMillis = null,
            pausedRemainingMillis = 5_000L,
            phase = TimerPhase.Paused,
            createdAtEpochMillis = 99L
        )
        assertEquals(timer, TimerEntity.fromDomain(timer).toDomain())
    }

    @Test
    fun fromDomain_toDomain_preservesEachPhase() {
        for (phase in TimerPhase.entries) {
            val timer = CountdownTimer(
                id = 1L,
                label = "Phase",
                originalDurationMillis = 60_000L,
                endsAtEpochMillis = if (phase == TimerPhase.Running) 10L else null,
                pausedRemainingMillis = if (phase == TimerPhase.Paused) 10L else null,
                phase = phase,
                createdAtEpochMillis = 15L
            )
            assertEquals(phase, TimerEntity.fromDomain(timer).toDomain().phase)
            assertEquals(15L, TimerEntity.fromDomain(timer).createdAtEpochMillis)
        }
    }

    @Test
    fun fromDomain_toDomain_preservesVoiceCueOverrides() {
        val timer = CountdownTimer(
            id = 8L,
            label = "Tea",
            originalDurationMillis = 60_000L,
            endsAtEpochMillis = 70_000L,
            pausedRemainingMillis = null,
            phase = TimerPhase.Running,
            createdAtEpochMillis = 10_000L,
            voiceCueStartOverride = true,
            voiceCueMidRemainingOverride = false,
            voiceCueLastTenOverride = null
        )

        val restored = TimerEntity.fromDomain(timer).toDomain()

        assertEquals(timer, restored)
        assertEquals(
            TimerVoiceCueOverrides(start = true, midRemaining = false, lastTen = null),
            restored.voiceOverrides()
        )
    }
}
