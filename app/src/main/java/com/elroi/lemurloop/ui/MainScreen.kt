package com.elroi.lemurloop.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.elroi.lemurloop.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.elroi.lemurloop.ui.navigation.LemurLoopNavGraph
import com.elroi.lemurloop.ui.navigation.Screen
import com.elroi.lemurloop.ui.navigation.homeTabs
import com.elroi.lemurloop.ui.navigation.selectedHomeTab
import com.elroi.lemurloop.ui.navigation.showsHomeBar
import com.elroi.lemurloop.ui.viewmodel.OnboardingViewModel

@Composable
fun MainScreen(
    onboardingViewModel: OnboardingViewModel = hiltViewModel()
) {
    val isOnboardingComplete by onboardingViewModel.isOnboardingComplete.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    
    // Lock the start destination once onboarding status is loaded.
    // This prevents the NavHost from resetting if isOnboardingComplete changes during the session.
    val startDestination = remember(isOnboardingComplete != null) {
        if (isOnboardingComplete == true) {
            Screen.AlarmList.route
        } else {
            Screen.Onboarding.route
        }
    }

    if (isOnboardingComplete == null) {
        // Still loading from DataStore, show a simple splash or empty box
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBar = showsHomeBar(currentRoute)

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    homeTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedHomeTab(currentRoute) == tab,
                            onClick = {
                                if (tab.route != currentRoute) {
                                    navController.navigate(tab.route) {
                                        popUpTo(Screen.AlarmList.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(tab.icon, contentDescription = null)
                            },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            LemurLoopNavGraph(
                navController = navController,
                startDestination = startDestination,
            )
        }
    }
}
