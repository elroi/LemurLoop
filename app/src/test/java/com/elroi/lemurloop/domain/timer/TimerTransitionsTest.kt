package com.elroi.lemurloop.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerTransitionsTest {

    @Test
    fun start_fiveMinutes_isRunningUntilThatWallClockEnd() {
        val now = 1_000_000L
        val timer = TimerTransitions.start(
            id = 7L,
            label = "Tea",
            durationMillis = FIVE_MINUTES,
            nowEpochMillis = now
        )

        assertEquals(7L, timer.id)
        assertEquals("Tea", timer.label)
        assertEquals(FIVE_MINUTES, timer.originalDurationMillis)
        assertEquals(now + FIVE_MINUTES, timer.endsAtEpochMillis)
        assertEquals(TimerPhase.Running, timer.phase)
        assertEquals(now, timer.createdAtEpochMillis)
        assertNull(timer.pausedRemainingMillis)
        assertEquals(FIVE_MINUTES, TimerTransitions.remainingMillis(timer, now))
    }

    @Test
    fun remainingMillis_afterTheEnd_isZero() {
        val timer = running(endsAt = 1_000L)
        assertEquals(0L, TimerTransitions.remainingMillis(timer, 5_000L))
    }

    @Test
    fun remainingMillis_whilePaused_staysAtTheSnapshot() {
        val paused = paused(remaining = 4_000L)
        assertEquals(4_000L, TimerTransitions.remainingMillis(paused, 50_000L))
        assertEquals(4_000L, TimerTransitions.remainingMillis(paused, 90_000L))
    }

    @Test
    fun pause_whileRunning_freezesTimeLeftAndKeepsCreatedAt() {
        val now = 10_000L
        val timer = running(endsAt = now + 5_000L, createdAt = 42L)
        val applied = TimerTransitions.pause(timer, now) as TimerEdit.Applied

        assertEquals(TimerPhase.Paused, applied.timer.phase)
        assertEquals(5_000L, applied.timer.pausedRemainingMillis)
        assertNull(applied.timer.endsAtEpochMillis)
        assertEquals(42L, applied.timer.createdAtEpochMillis)
        assertEquals(timer.originalDurationMillis, applied.timer.originalDurationMillis)
        assertEquals(5_000L, TimerTransitions.remainingMillis(applied.timer, now + 80_000L))
    }

    @Test
    fun pause_whenTimeLeftIsZero_isRefused() {
        val now = 10_000L
        val timer = running(endsAt = now)
        assertEquals(TimerEdit.Refused, TimerTransitions.pause(timer, now))
    }

    @Test
    fun pause_whilePaused_isRefused() {
        val paused = paused(remaining = 5_000L)
        assertEquals(TimerEdit.Refused, TimerTransitions.pause(paused, 10_000L))
    }

    @Test
    fun play_whilePaused_continuesFromTimeLeftAndKeepsCreatedAt() {
        val now = 20_000L
        val paused = paused(remaining = 30_000L, createdAt = 11L, original = 90_000L)
        val applied = TimerTransitions.play(paused, now) as TimerEdit.Applied

        assertEquals(TimerPhase.Running, applied.timer.phase)
        assertEquals(now + 30_000L, applied.timer.endsAtEpochMillis)
        assertNull(applied.timer.pausedRemainingMillis)
        assertEquals(11L, applied.timer.createdAtEpochMillis)
        assertEquals(90_000L, applied.timer.originalDurationMillis)
    }

    @Test
    fun play_whileRunning_isRefused() {
        assertEquals(TimerEdit.Refused, TimerTransitions.play(running(), 10_000L))
    }

    @Test
    fun addOneMinute_whileRunning_addsSixtySecondsToOriginalAndTimeLeft() {
        val now = 100_000L
        val timer = running(original = TEN_MINUTES, endsAt = now + FIVE_MINUTES, createdAt = 3L)
        val applied = TimerTransitions.addOneMinute(timer, now) as TimerEdit.Applied

        assertEquals(TEN_MINUTES + ONE_MINUTE, applied.timer.originalDurationMillis)
        assertEquals(now + FIVE_MINUTES + ONE_MINUTE, applied.timer.endsAtEpochMillis)
        assertEquals(TimerPhase.Running, applied.timer.phase)
        assertEquals(3L, applied.timer.createdAtEpochMillis)
    }

    @Test
    fun addOneMinute_atTheDurationCap_clampsOriginalAndTimeLeft() {
        val now = 100_000L
        val almostMax = TimerLimits.MAX_DURATION_MILLIS - 10_000L
        val timer = running(original = almostMax, endsAt = now + almostMax)
        val applied = TimerTransitions.addOneMinute(timer, now) as TimerEdit.Applied

        assertEquals(TimerLimits.MAX_DURATION_MILLIS, applied.timer.originalDurationMillis)
        assertEquals(now + TimerLimits.MAX_DURATION_MILLIS, applied.timer.endsAtEpochMillis)
    }

    @Test
    fun addOneMinute_whenOriginalIsAlreadyMax_stillAddsToTimeLeftUntilMax() {
        val now = 100_000L
        val timer = running(original = TimerLimits.MAX_DURATION_MILLIS, endsAt = now + 10_000L)
        val applied = TimerTransitions.addOneMinute(timer, now) as TimerEdit.Applied

        assertEquals(TimerLimits.MAX_DURATION_MILLIS, applied.timer.originalDurationMillis)
        assertEquals(now + 70_000L, applied.timer.endsAtEpochMillis)
    }

    @Test
    fun addOneMinute_whilePaused_isRefused() {
        val paused = paused(remaining = 5_000L)
        assertEquals(TimerEdit.Refused, TimerTransitions.addOneMinute(paused, 10_000L))
    }

    @Test
    fun refresh_whilePaused_restoresOriginalDurationAndStaysPaused() {
        val paused = paused(remaining = 2_000L, original = ELEVEN_MINUTES, createdAt = 8L)
        val applied = TimerTransitions.refresh(paused) as TimerEdit.Applied

        assertEquals(TimerPhase.Paused, applied.timer.phase)
        assertEquals(ELEVEN_MINUTES, applied.timer.pausedRemainingMillis)
        assertNull(applied.timer.endsAtEpochMillis)
        assertEquals(8L, applied.timer.createdAtEpochMillis)
        assertEquals(ELEVEN_MINUTES, applied.timer.originalDurationMillis)
    }

    @Test
    fun refresh_whileRunning_isRefused() {
        assertEquals(TimerEdit.Refused, TimerTransitions.refresh(running()))
    }

    @Test
    fun dismiss_whileRinging_pausesAtOriginalDuration() {
        val ringing = running(original = FIVE_MINUTES, endsAt = 500L, createdAt = 9L).copy(
            phase = TimerPhase.Ringing
        )
        val applied = TimerTransitions.dismiss(ringing) as TimerEdit.Applied

        assertEquals(TimerPhase.Paused, applied.timer.phase)
        assertEquals(FIVE_MINUTES, applied.timer.pausedRemainingMillis)
        assertNull(applied.timer.endsAtEpochMillis)
        assertEquals(9L, applied.timer.createdAtEpochMillis)
        assertEquals(FIVE_MINUTES, applied.timer.originalDurationMillis)
        assertEquals(ringing.label, applied.timer.label)
    }

    @Test
    fun dismiss_whilePaused_isRefused() {
        assertEquals(TimerEdit.Refused, TimerTransitions.dismiss(paused(remaining = 1_000L)))
    }

    @Test
    fun duplicate_copiesLabelAndOriginalDurationAndStartsANewTimer() {
        val source = paused(id = 3L, label = "Eggs", remaining = 10_000L, original = 120_000L, createdAt = 50L)
        val copy = TimerTransitions.duplicate(source, newId = 9L, nowEpochMillis = 800L)

        assertEquals(9L, copy.id)
        assertEquals("Eggs", copy.label)
        assertEquals(120_000L, copy.originalDurationMillis)
        assertEquals(TimerPhase.Running, copy.phase)
        assertEquals(800L + 120_000L, copy.endsAtEpochMillis)
        assertEquals(800L, copy.createdAtEpochMillis)
        assertNull(copy.pausedRemainingMillis)
    }

    @Test
    fun duplicate_copiesVoiceOverrides() {
        val source = paused(
            voiceCueStartOverride = true,
            voiceCueMidRemainingOverride = false,
            voiceCueLastTenOverride = null
        )

        val copy = TimerTransitions.duplicate(source, newId = 9L, nowEpochMillis = 800L)

        assertEquals(true, copy.voiceCueStartOverride)
        assertEquals(false, copy.voiceCueMidRemainingOverride)
        assertNull(copy.voiceCueLastTenOverride)
    }

    @Test
    fun rename_trimsToFortyCharactersAndKeepsTheTimer() {
        val timer = running(label = "old", createdAt = 15L, endsAt = 77_000L)
        val renamed = TimerTransitions.rename(timer, "  " + "a".repeat(50) + "  ")

        assertEquals("a".repeat(40), renamed.label)
        assertEquals(15L, renamed.createdAtEpochMillis)
        assertEquals(77_000L, renamed.endsAtEpochMillis)
        assertEquals(timer.phase, renamed.phase)
        assertEquals(timer.id, renamed.id)
    }

    @Test
    fun rename_blank_isAllowed() {
        val renamed = TimerTransitions.rename(running(label = "old"), "   ")
        assertEquals("", renamed.label)
    }

    @Test
    fun newestFirst_ordersByCreatedAtDescending() {
        val older = running(id = 1L, createdAt = 10L)
        val newer = running(id = 2L, createdAt = 20L)
        val ordered = TimerOrdering.newestFirst(listOf(older, newer))
        assertEquals(listOf(2L, 1L), ordered.map { it.id })
    }

    @Test
    fun tryCreate_atTwentyTimers_isRefused() {
        val result = TimerCreation.tryCreate(
            existingCount = TimerLimits.MAX_TIMERS,
            id = 21L,
            label = "Late",
            durationMillis = ONE_MINUTE,
            nowEpochMillis = 100L
        )
        assertEquals(TimerCreation.LimitReached, result)
    }

    @Test
    fun tryCreate_belowTheCap_startsRunning() {
        val result = TimerCreation.tryCreate(
            existingCount = TimerLimits.MAX_TIMERS - 1,
            id = 21L,
            label = "Ok",
            durationMillis = ONE_MINUTE,
            nowEpochMillis = 100L
        ) as TimerCreation.Created

        assertEquals(TimerPhase.Running, result.timer.phase)
        assertEquals(100L + ONE_MINUTE, result.timer.endsAtEpochMillis)
        assertEquals(100L, result.timer.createdAtEpochMillis)
        assertEquals("Ok", result.timer.label)
    }

    @Test
    fun tryCreate_atZero_isRefused() {
        val result = TimerCreation.tryCreate(
            existingCount = 0,
            id = 1L,
            label = "",
            durationMillis = 0L,
            nowEpochMillis = 100L
        )
        assertEquals(TimerCreation.DurationNotAllowed, result)
    }

    @Test
    fun tryCreate_belowOneSecond_isRefused() {
        val result = TimerCreation.tryCreate(
            existingCount = 0,
            id = 1L,
            label = "",
            durationMillis = 999L,
            nowEpochMillis = 100L
        )
        assertEquals(TimerCreation.DurationNotAllowed, result)
    }

    @Test
    fun fromFields_ninetySeconds_carriesToOneMinuteThirty() {
        val millis = TimerDuration.millis(hours = 0, minutes = 0, seconds = 90)
        assertEquals(90_000L, millis)
        assertEquals(TimerDuration.Parts(hours = 0, minutes = 1, seconds = 30), TimerDuration.parts(millis))
    }

    @Test
    fun fromFields_pastTheCap_clampsAtNinetyNineHours() {
        val millis = TimerDuration.millis(hours = 99, minutes = 59, seconds = 60)
        assertEquals(TimerLimits.MAX_DURATION_MILLIS, millis)
        assertEquals(
            TimerDuration.Parts(hours = 99, minutes = 59, seconds = 59),
            TimerDuration.parts(millis)
        )
    }

    @Test
    fun restore_runningTimer_keepsTheOldEndTime() {
        val deleted = running(id = 4L, endsAt = 5_000L, createdAt = 7L, label = "Back")
        val result = TimerUndo.restore(existingCount = 1, deleted) as TimerUndo.Restored

        assertEquals(deleted, result.timer)
        assertEquals(5_000L, result.timer.endsAtEpochMillis)
        assertEquals(TimerPhase.Running, result.timer.phase)
        assertEquals(7L, result.timer.createdAtEpochMillis)
    }

    @Test
    fun restore_whenTheListIsFull_isRefused() {
        val result = TimerUndo.restore(existingCount = TimerLimits.MAX_TIMERS, running())
        assertEquals(TimerUndo.LimitReached, result)
        assertTrue(result is TimerUndo.LimitReached)
    }

    private fun running(
        id: Long = 1L,
        label: String = "Tea",
        original: Long = FIVE_MINUTES,
        endsAt: Long = 1_000_000L,
        createdAt: Long = 10L
    ) = CountdownTimer(
        id = id,
        label = label,
        originalDurationMillis = original,
        endsAtEpochMillis = endsAt,
        pausedRemainingMillis = null,
        phase = TimerPhase.Running,
        createdAtEpochMillis = createdAt
    )

    private fun paused(
        id: Long = 1L,
        label: String = "Tea",
        original: Long = FIVE_MINUTES,
        remaining: Long = FIVE_MINUTES,
        createdAt: Long = 10L,
        voiceCueStartOverride: Boolean? = null,
        voiceCueMidRemainingOverride: Boolean? = null,
        voiceCueLastTenOverride: Boolean? = null
    ) = CountdownTimer(
        id = id,
        label = label,
        originalDurationMillis = original,
        endsAtEpochMillis = null,
        pausedRemainingMillis = remaining,
        phase = TimerPhase.Paused,
        createdAtEpochMillis = createdAt,
        voiceCueStartOverride = voiceCueStartOverride,
        voiceCueMidRemainingOverride = voiceCueMidRemainingOverride,
        voiceCueLastTenOverride = voiceCueLastTenOverride
    )

    private companion object {
        const val ONE_MINUTE = 60_000L
        const val FIVE_MINUTES = 5 * ONE_MINUTE
        const val TEN_MINUTES = 10 * ONE_MINUTE
        const val ELEVEN_MINUTES = 11 * ONE_MINUTE
    }
}
