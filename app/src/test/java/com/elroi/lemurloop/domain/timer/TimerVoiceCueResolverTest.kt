package com.elroi.lemurloop.domain.timer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerVoiceCueResolverTest {

    private val globalsOn = TimerVoiceCueGlobals(
        masterEnabled = true,
        startEnabled = true,
        midRemainingEnabled = true,
        lastTenEnabled = true,
        announceOnResume = true
    )

    @Test
    fun effective_masterOff_overrideOn_isStillFalse() {
        val effective = TimerVoiceCueResolver.effective(
            globals = globalsOn.copy(masterEnabled = false),
            overrides = TimerVoiceCueOverrides(start = true, midRemaining = true, lastTen = true)
        )
        assertFalse(effective.start)
        assertFalse(effective.midRemaining)
        assertFalse(effective.lastTen)
    }

    @Test
    fun effective_masterOn_overrideOff_disablesOnlyThatCue() {
        val effective = TimerVoiceCueResolver.effective(
            globals = globalsOn,
            overrides = TimerVoiceCueOverrides(start = false, midRemaining = null, lastTen = null)
        )
        assertFalse(effective.start)
        assertTrue(effective.midRemaining)
        assertTrue(effective.lastTen)
    }

    @Test
    fun effective_masterOn_inherit_usesGlobalToggles() {
        val effective = TimerVoiceCueResolver.effective(
            globals = globalsOn.copy(startEnabled = false, midRemainingEnabled = true, lastTenEnabled = false),
            overrides = TimerVoiceCueOverrides()
        )
        assertFalse(effective.start)
        assertTrue(effective.midRemaining)
        assertFalse(effective.lastTen)
    }

    @Test
    fun announceOnResume_isNotAnEffectiveCueFlag() {
        // Documented contract: resume gating is coordinator-side via allowStart.
        val a = TimerVoiceCueResolver.effective(globalsOn.copy(announceOnResume = true), TimerVoiceCueOverrides())
        val b = TimerVoiceCueResolver.effective(globalsOn.copy(announceOnResume = false), TimerVoiceCueOverrides())
        assertTrue(a.start && b.start && a.midRemaining && b.midRemaining && a.lastTen && b.lastTen)
    }
}
