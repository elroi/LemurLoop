package com.elroi.lemurloop.ui.navigation

sealed class Screen(val route: String) {
    object AlarmList : Screen("alarm_list")
    object AlarmDetail : Screen("alarm_detail?alarmId={alarmId}") {
        fun createRoute(alarmId: String) = "alarm_detail?alarmId=$alarmId"
        val arguments = listOf(
            androidx.navigation.navArgument("alarmId") {
                type = androidx.navigation.NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    }
    object SleepTracking : Screen("sleep_tracking")
    object Settings : Screen("settings")
    object Help : Screen("help")
    /** Route for initial app load (no args). Use this as startDestination. */
    object Onboarding : Screen("onboarding") {
        /** Route with isReplay for replay-from-settings. Use navigate(OnboardingReplay) for replay. */
        object OnboardingReplay : Screen("onboarding_replay")
        fun createRoute(isReplay: Boolean = false) =
            if (isReplay) OnboardingReplay.route else Onboarding.route
    }
    object AlarmWizard : Screen("alarm_wizard")
    object LemurChat : Screen("lemur_chat")
    object About : Screen("about")
    object DiagnosticLogs : Screen("diagnostic_logs")
}
