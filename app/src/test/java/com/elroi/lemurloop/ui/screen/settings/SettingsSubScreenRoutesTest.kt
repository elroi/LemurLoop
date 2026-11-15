package com.elroi.lemurloop.ui.screen.settings

import com.elroi.lemurloop.R
import com.elroi.lemurloop.domain.manager.SettingSearchTarget
import com.elroi.lemurloop.domain.manager.SettingsSearchRegistry
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SettingsSubScreenRoutesTest {

    @Test
    fun everySubScreenRoute_isHandled() {
        val routes = SettingsSearchRegistry.allItems.mapNotNull { item ->
            (item.target as? SettingSearchTarget.SubScreen)?.route
        }
        assertTrueHasTimer(routes)
        routes.forEach { route ->
            assertNotNull(settingsSubScreen(route))
        }
    }

    @Test
    fun everySearchItem_hasItsOwnTitle() {
        SettingsSearchRegistry.allItems.forEach { item ->
            assertNotEquals(R.string.settings_screen_title, SettingsSearchLabels.titleRes(item.id))
        }
    }

    private fun assertTrueHasTimer(routes: List<String>) {
        org.junit.Assert.assertTrue(routes.contains("timer_settings"))
    }
}
