package com.elroi.lemurloop.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerVoiceCuePlanTest {

    private val now = 1_000_000L
    private val allOn = TimerVoiceCueEffective(start = true, midRemaining = true, lastTen = true)

    private fun input(
        remaining: Long,
        effective: TimerVoiceCueEffective = allOn,
        allowStart: Boolean = true
    ) = TimerVoiceCuePlanInput(
        nowEpochMillis = now,
        endsAtEpochMillis = now + remaining,
        effective = effective,
        allowStart = allowStart
    )

    @Test
    fun plan_thirtySeconds_midAtHalfRemaining() {
        val plan = TimerVoiceCuePlan.plan(input(30_000L))
        val mid = plan.single { it.kind == TimerVoiceCueKind.MidRemaining }
        assertEquals(now + 15_000L, mid.fireAtEpochMillis)
    }

    @Test
    fun plan_omitMid_whenInsideLastTenPlusClearance() {
        // remaining 20s: mid at +10s; last-10 starts at +10s; clearance 3s → mid omitted
        val plan = TimerVoiceCuePlan.plan(input(20_000L))
        assertTrue(plan.none { it.kind == TimerVoiceCueKind.MidRemaining })
        assertTrue(plan.any { it.kind == TimerVoiceCueKind.EnterLastTen })
    }

    @Test
    fun plan_shortTimer_lastTenWins_startSuppressed() {
        val plan = TimerVoiceCuePlan.plan(input(8_000L, allowStart = true))
        assertTrue(plan.none { it.kind == TimerVoiceCueKind.Start })
        assertEquals(
            listOf(TimerVoiceCueKind.EnterLastTen),
            plan.map { it.kind }
        )
        assertEquals(now, plan.single().fireAtEpochMillis)
    }

    @Test
    fun plan_reconcile_allowStartFalse_omitsStart() {
        val plan = TimerVoiceCuePlan.plan(input(120_000L, allowStart = false))
        assertTrue(plan.none { it.kind == TimerVoiceCueKind.Start })
        assertTrue(plan.any { it.kind == TimerVoiceCueKind.MidRemaining })
        assertTrue(plan.any { it.kind == TimerVoiceCueKind.EnterLastTen })
    }

    @Test
    fun plan_createStart_includesStartWhenBudgetAllows() {
        val plan = TimerVoiceCuePlan.plan(input(120_000L, allowStart = true))
        assertTrue(plan.any { it.kind == TimerVoiceCueKind.Start && it.fireAtEpochMillis == now })
    }

    @Test
    fun plan_plusOneReplan_midUsesHalfOfNewRemaining() {
        // Simulate post-+1:00 remaining 5 minutes → mid at +2.5 min from now
        val plan = TimerVoiceCuePlan.plan(input(5 * 60_000L, allowStart = false))
        val mid = plan.single { it.kind == TimerVoiceCueKind.MidRemaining }
        assertEquals(now + 150_000L, mid.fireAtEpochMillis)
    }
}
