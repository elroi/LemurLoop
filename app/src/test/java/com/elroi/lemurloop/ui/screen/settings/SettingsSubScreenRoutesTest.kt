package com.elroi.lemurloop.ui.screen.settings

import com.elroi.lemurloop.R
import com.elroi.lemurloop.domain.manager.SettingSearchTarget
import com.elroi.lemurloop.domain.manager.SettingsSearchRegistry
import org.junit.Assert.assertEquals
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

    @Test
    fun timerVoiceCueSearchItems_haveUniqueLabelsAndOpenTimerSettings() {
        val ids = setOf(
            "timer_voice_cues",
            "timer_voice_cue_start",
            "timer_voice_cue_mid_remaining",
            "timer_voice_cue_last_ten",
            "timer_voice_announce_on_resume"
        )
        val items = SettingsSearchRegistry.allItems.filter { it.id in ids }

        assertEquals(ids, items.map { it.id }.toSet())
        assertEquals(
            setOf(SettingSearchTarget.SubScreen("timer_settings")),
            items.map { it.target }.toSet()
        )
        assertEquals(ids.size, items.map { SettingsSearchLabels.titleRes(it.id) }.toSet().size)
        assertEquals(ids.size, items.map { SettingsSearchLabels.descriptionRes(it.id) }.toSet().size)
    }

    private fun assertTrueHasTimer(routes: List<String>) {
        org.junit.Assert.assertTrue(routes.contains("timer_settings"))
    }
}
