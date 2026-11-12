package com.elroi.lemurloop.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import com.elroi.lemurloop.R

enum class HomeDestination {
    AlarmList,
    Unavailable,
}

data class HomeTab(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
    val destination: HomeDestination,
)

val homeTabs: List<HomeTab> = listOf(
    HomeTab(Screen.AlarmList.route, R.string.home_tab_alarms, Icons.Filled.Alarm, HomeDestination.AlarmList),
    HomeTab(Screen.Timer.route, R.string.home_tab_timer, Icons.Filled.Timer, HomeDestination.Unavailable),
    HomeTab(Screen.Stopwatch.route, R.string.home_tab_stopwatch, Icons.Filled.AvTimer, HomeDestination.Unavailable),
    HomeTab(Screen.SleepTab.route, R.string.home_tab_sleep, Icons.Filled.Bedtime, HomeDestination.Unavailable),
)

fun selectedHomeTab(route: String?): HomeTab? = homeTabs.firstOrNull { it.route == route }

fun showsHomeBar(route: String?): Boolean = selectedHomeTab(route) != null
