package com.elroi.lemurloop.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Timer
import com.elroi.lemurloop.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeTabsTest {

    private val otherRoutes = listOf(
        null,
        Screen.AlarmDetail.route,
        Screen.Settings.route,
        Screen.Onboarding.route,
        Screen.Onboarding.OnboardingReplay.route,
        Screen.LemurChat.route,
        Screen.AlarmWizard.route,
        Screen.Help.route,
        Screen.About.route,
        Screen.DiagnosticLogs.route,
        Screen.SleepTracking.route,
    )

    @Test
    fun homeTabs_orderAndDestination_matchesShell() {
        assertEquals(4, homeTabs.size)
        assertEquals(Screen.AlarmList.route, homeTabs[0].route)
        assertEquals(Screen.Timer.route, homeTabs[1].route)
        assertEquals(Screen.Stopwatch.route, homeTabs[2].route)
        assertEquals(Screen.SleepTab.route, homeTabs[3].route)
        assertEquals(HomeDestination.AlarmList, homeTabs[0].destination)
        assertEquals(HomeDestination.Unavailable, homeTabs[1].destination)
        assertEquals(HomeDestination.Unavailable, homeTabs[2].destination)
        assertEquals(HomeDestination.Unavailable, homeTabs[3].destination)
        assertEquals(R.string.home_tab_alarms, homeTabs[0].labelRes)
        assertEquals(R.string.home_tab_timer, homeTabs[1].labelRes)
        assertEquals(R.string.home_tab_stopwatch, homeTabs[2].labelRes)
        assertEquals(R.string.home_tab_sleep, homeTabs[3].labelRes)
        assertSame(Icons.Filled.Alarm, homeTabs[0].icon)
        assertSame(Icons.Filled.Timer, homeTabs[1].icon)
        assertSame(Icons.Filled.AvTimer, homeTabs[2].icon)
        assertSame(Icons.Filled.Bedtime, homeTabs[3].icon)
        assertTrue(homeTabs[3].route != Screen.SleepTracking.route)
    }

    @Test
    fun showsHomeBar_tabRoute_true() {
        homeTabs.forEach { tab ->
            assertTrue(showsHomeBar(tab.route))
        }
    }

    @Test
    fun showsHomeBar_otherRoute_false() {
        otherRoutes.forEach { route ->
            assertFalse(showsHomeBar(route))
        }
    }

    @Test
    fun selectedHomeTab_route_matchesThatTab() {
        homeTabs.forEach { tab ->
            assertSame(tab, selectedHomeTab(tab.route))
        }
    }

    @Test
    fun selectedHomeTab_otherRoute_null() {
        otherRoutes.forEach { route ->
            assertNull(selectedHomeTab(route))
        }
    }
}
