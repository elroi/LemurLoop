package com.elroi.lemurloop.ui.screen.settings

import com.elroi.lemurloop.R

enum class SettingsSubScreen {
    About,
    Onboarding,
    Logs,
    TimerSettings
}

fun settingsSubScreen(route: String): SettingsSubScreen? {
    return when (route) {
        "about" -> SettingsSubScreen.About
        "onboarding" -> SettingsSubScreen.Onboarding
        "logs" -> SettingsSubScreen.Logs
        "timer_settings" -> SettingsSubScreen.TimerSettings
        else -> null
    }
}

object SettingsSearchLabels {
    fun titleRes(id: String): Int = when (id) {
        "morning_personality" -> R.string.search_morning_personality_title
        "briefing_tts" -> R.string.search_briefing_tts_title
        "briefing_content" -> R.string.search_briefing_content_title
        "alarm_creation_style" -> R.string.search_alarm_creation_style_title
        "alarm_sound" -> R.string.search_alarm_sound_title
        "vibration" -> R.string.search_vibration_title
        "gentle_wake" -> R.string.search_gentle_wake_title
        "snooze_duration" -> R.string.search_snooze_duration_title
        "math_challenge" -> R.string.search_math_challenge_title
        "face_game" -> R.string.search_face_game_title
        "weekend_days" -> R.string.search_weekend_days_title
        "accountability_buddies" -> R.string.search_accountability_buddies_title
        "cloud_ai" -> R.string.search_cloud_ai_title
        "gemini_api_key" -> R.string.search_gemini_api_key_title
        "intelligence_health" -> R.string.search_intelligence_health_title
        "about" -> R.string.search_about_title
        "replay_tutorial" -> R.string.search_replay_tutorial_title
        "demo_alarms" -> R.string.search_demo_alarms_title
        "privacy_policy" -> R.string.search_privacy_policy_title
        "diagnostic_logs" -> R.string.search_diagnostic_logs_title
        "danger_zone" -> R.string.search_danger_zone_title
        "timer_settings" -> R.string.search_timer_settings_title
        "timer_voice_cues" -> R.string.search_timer_voice_cues_title
        "timer_voice_cue_start" -> R.string.search_timer_voice_cue_start_title
        "timer_voice_cue_mid_remaining" -> R.string.search_timer_voice_cue_mid_remaining_title
        "timer_voice_cue_last_ten" -> R.string.search_timer_voice_cue_last_ten_title
        "timer_voice_announce_on_resume" -> R.string.search_timer_voice_announce_on_resume_title
        else -> R.string.settings_screen_title
    }

    fun descriptionRes(id: String): Int = when (id) {
        "morning_personality" -> R.string.search_morning_personality_desc
        "briefing_tts" -> R.string.search_briefing_tts_desc
        "briefing_content" -> R.string.search_briefing_content_desc
        "alarm_creation_style" -> R.string.search_alarm_creation_style_desc
        "alarm_sound" -> R.string.search_alarm_sound_desc
        "vibration" -> R.string.search_vibration_desc
        "gentle_wake" -> R.string.search_gentle_wake_desc
        "snooze_duration" -> R.string.search_snooze_duration_desc
        "math_challenge" -> R.string.search_math_challenge_desc
        "face_game" -> R.string.search_face_game_desc
        "weekend_days" -> R.string.search_weekend_days_desc
        "accountability_buddies" -> R.string.search_accountability_buddies_desc
        "cloud_ai" -> R.string.search_cloud_ai_desc
        "gemini_api_key" -> R.string.search_gemini_api_key_desc
        "intelligence_health" -> R.string.search_intelligence_health_desc
        "about" -> R.string.search_about_desc
        "replay_tutorial" -> R.string.search_replay_tutorial_desc
        "demo_alarms" -> R.string.search_demo_alarms_desc
        "privacy_policy" -> R.string.search_privacy_policy_desc
        "diagnostic_logs" -> R.string.search_diagnostic_logs_desc
        "danger_zone" -> R.string.search_danger_zone_desc
        "timer_settings" -> R.string.search_timer_settings_desc
        "timer_voice_cues" -> R.string.search_timer_voice_cues_desc
        "timer_voice_cue_start" -> R.string.search_timer_voice_cue_start_desc
        "timer_voice_cue_mid_remaining" -> R.string.search_timer_voice_cue_mid_remaining_desc
        "timer_voice_cue_last_ten" -> R.string.search_timer_voice_cue_last_ten_desc
        "timer_voice_announce_on_resume" -> R.string.search_timer_voice_announce_on_resume_desc
        else -> R.string.settings_search_hint
    }
}
