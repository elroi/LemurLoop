# Home shell implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a four-tab home bar so Alarms keeps working and Timer, Stopwatch, and Sleep each open a not-ready screen.

**Architecture:** One `HomeTab` list in `ui/navigation` is the destination contract. `MainScreen` draws the bar from that list, shows it only for those four routes, and consumes scaffold padding. The navigation graph reads each tab's destination. Replay from Settings pops back to the original alarm list.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Navigation Compose 2.5.1 (from `androidx.hilt:hilt-navigation-compose:1.2.0`), JUnit 4 on the Java Virtual Machine (JVM).

**Slug:** grok-4.7

**Spec:** `docs/superpowers/specs/2026-11-11-home-shell-design.md`

## Global Constraints

- Tabs, in order: Alarms (`Screen.AlarmList.route`), Timer (`timer`), Stopwatch (`stopwatch`), Sleep (`sleep`).
- Only Alarms shows `AlarmListScreen`. The other three show `FeatureUnavailableScreen` with title from that tab's label and body `This isn't ready yet.` / `זה עדיין לא מוכן.`
- `showsHomeBar` is true only for those four route strings. Selection is `selectedHomeTab(route)`, not remembered state.
- A tap on the already selected tab does nothing. Do not call `navigate` for that tap. Navigation 2.5.1 runs `popUpTo` before `launchSingleTop`, so a re-tap on Timer would otherwise pop to the alarm list and restore Timer.
- Tab `navigate` uses `launchSingleTop = true`, `restoreState = true`, and `popUpTo(Screen.AlarmList.route)` with `saveState = true`.
- The bar is not animated. Do not use `AnimatedVisibility` for it.
- `MainScreen` applies `Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)`. Tab scaffolds set `contentWindowInsets = WindowInsets(0)`. Do not edit wizard, chat, Settings, alarm detail, or onboarding padding.
- Replay completion calls `popBackStack(Screen.AlarmList.route, inclusive = false)` and does not push another alarm list. First-run onboarding stays as it is.
- Strings go in `values`, `values-he`, and `values-iw`. Hebrew in `values-he` and `values-iw` is identical.
- No new Compose or Navigation test harness. No Room, scheduler, or service changes. Do not link `sleep_tracking`.
- Work on `feature/home-shell` from `origin/main`, not on `main`. Do not stage `.superpowers/`.
- Do not commit a user-visible UI task until the user has seen that shell on a device or emulator and said it looks right.

## Persona review

- A later maintainer needs one list and two pure functions. A second availability flag, or route strings copied beside `Screen`, will drift.
- The person using the app must not see the alarm list flash when they tap the tab they are already on. Each not-ready screen's title is that tab's name. In Hebrew, Alarms is the rightmost tab and the gear sits on the left.
- Replay must leave one alarm list. Consuming insets can shrink padding on screens this pass does not edit. Chat with the keyboard open is the check for that.
- The JVM test locks order, `Screen` routes, labels, icons, destinations, the bar predicate, and selection. The manual matrix is the wiring contract. There is no `androidTest` suite to add.

## File structure

- Create `app/src/main/java/com/elroi/lemurloop/ui/navigation/HomeTab.kt`: tab list, `selectedHomeTab`, `showsHomeBar`.
- Create `app/src/main/java/com/elroi/lemurloop/ui/screen/home/FeatureUnavailableScreen.kt`: the not-ready screen.
- Create `app/src/test/java/com/elroi/lemurloop/ui/navigation/HomeTabsTest.kt`: the JVM contract.
- Modify `Screen.kt`: add `Timer`, `Stopwatch`, `SleepTab`.
- Modify `LemurLoopNavGraph.kt`: read `HomeTab` for the four routes, and fix replay.
- Modify `MainScreen.kt`: bar, consumed padding, re-tap no-op.
- Modify `AlarmListScreen.kt`: `contentWindowInsets = WindowInsets(0)`.
- Modify `values/strings.xml`, `values-he/strings.xml`, `values-iw/strings.xml`.
- Modify `docs/HEBREW-TRANSLATION-GLOSSARY.md` and `docs/RTL-LOCALIZATION.md`.

---

### Task 1: Tab contract

**Files:**
- Create: `app/src/test/java/com/elroi/lemurloop/ui/navigation/HomeTabsTest.kt`
- Create: `app/src/main/java/com/elroi/lemurloop/ui/navigation/HomeTab.kt`
- Modify: `app/src/main/java/com/elroi/lemurloop/ui/navigation/Screen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-he/strings.xml`
- Modify: `app/src/main/res/values-iw/strings.xml`
- Test: `app/src/test/java/com/elroi/lemurloop/ui/navigation/HomeTabsTest.kt`

**Interfaces:**
- Consumes: existing `Screen.AlarmList.route` (`alarm_list`) and `Screen.SleepTracking.route` (`sleep_tracking`).
- Produces: `enum class HomeDestination { AlarmList, Unavailable }`; `data class HomeTab(val route: String, val labelRes: Int, val icon: ImageVector, val destination: HomeDestination)`; `val homeTabs: List<HomeTab>`; `fun selectedHomeTab(route: String?): HomeTab?`; `fun showsHomeBar(route: String?): Boolean`; `Screen.Timer`, `Screen.Stopwatch`, `Screen.SleepTab`.

- [ ] **Step 1: Create the branch**

If `git status` shows uncommitted work you did not make for this shell, use a separate worktree. Otherwise:

```bash
git fetch origin main
git checkout -b feature/home-shell origin/main
```

Bring the spec along if it is still uncommitted: `docs/superpowers/specs/2026-11-11-home-shell-design.md`. Do not add `.superpowers/`.

- [ ] **Step 2: Write the failing test**

Create `app/src/test/java/com/elroi/lemurloop/ui/navigation/HomeTabsTest.kt`:

```kotlin
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
```

- [ ] **Step 3: Run the test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.elroi.lemurloop.ui.navigation.HomeTabsTest"
```

Expected: compile failure. `homeTabs`, `Screen.Timer`, `Screen.SleepTab`, or `R.string.home_tab_alarms` is unresolved. If `Icons.Filled` fails to load on the JVM, stop and report that. Do not delete the `assertSame` lines.

- [ ] **Step 4: Add the strings**

In `app/src/main/res/values/strings.xml`, immediately after `alarm_list_title`:

```xml
    <string name="home_tab_alarms">Alarms</string>
    <string name="home_tab_timer">Timer</string>
    <string name="home_tab_stopwatch">Stopwatch</string>
    <string name="home_tab_sleep">Sleep</string>
    <string name="feature_unavailable_message">This isn\'t ready yet.</string>
```

In both `app/src/main/res/values-he/strings.xml` and `app/src/main/res/values-iw/strings.xml`, in the same place, use the same five keys with these values: `שעון`, `טיימר`, `סטופר`, `שינה`, `זה עדיין לא מוכן.`

- [ ] **Step 5: Add the Screen routes and HomeTab**

In `Screen.kt`, after `object AlarmList`:

```kotlin
    object Timer : Screen("timer")
    object Stopwatch : Screen("stopwatch")
    object SleepTab : Screen("sleep")
```

Create `app/src/main/java/com/elroi/lemurloop/ui/navigation/HomeTab.kt`:

```kotlin
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
```

- [ ] **Step 6: Run the test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.elroi.lemurloop.ui.navigation.HomeTabsTest"
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit the contract**

Commit only when this session was asked to commit. Stage the test, `HomeTab.kt`, `Screen.kt`, the three `strings.xml` files, and the spec if it is still uncommitted. Do not stage `.superpowers/`.

```bash
git commit -m "$(cat <<'EOF'
Add the home tab contract for alarms, timer, stopwatch, and sleep.

EOF
)"
```

---

### Task 2: Not-ready screens and graph destinations

**Files:**
- Create: `app/src/main/java/com/elroi/lemurloop/ui/screen/home/FeatureUnavailableScreen.kt`
- Modify: `app/src/main/java/com/elroi/lemurloop/ui/navigation/LemurLoopNavGraph.kt`
- Modify: `app/src/main/java/com/elroi/lemurloop/ui/screen/alarm/AlarmListScreen.kt` (the `Scaffold(` call near line 90)
- Test: `app/src/test/java/com/elroi/lemurloop/ui/navigation/HomeTabsTest.kt` (must stay green)

**Interfaces:**
- Consumes: `selectedHomeTab`, `HomeDestination`, `Screen.Timer`, `Screen.Stopwatch`, `Screen.SleepTab`, `R.string.feature_unavailable_message`.
- Produces: `FeatureUnavailableScreen(titleRes: Int, onNavigateToSettings: () -> Unit)`. Each of the four tab `composable` blocks calls `HomeTabRoute`.

- [ ] **Step 1: Add the not-ready screen**

Create `app/src/main/java/com/elroi/lemurloop/ui/screen/home/FeatureUnavailableScreen.kt`:

```kotlin
package com.elroi.lemurloop.ui.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.elroi.lemurloop.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureUnavailableScreen(
    titleRes: Int,
    onNavigateToSettings: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(titleRes)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.content_desc_settings),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.feature_unavailable_message))
        }
    }
}
```

- [ ] **Step 2: Branch every tab composable on HomeTab**

In `LemurLoopNavGraph.kt`, add this private composable in the same file:

```kotlin
@Composable
private fun HomeTabRoute(
    route: String,
    onNavigateToSettings: () -> Unit,
    alarmList: @Composable () -> Unit,
) {
    val tab = selectedHomeTab(route) ?: return
    when (tab.destination) {
        HomeDestination.AlarmList -> alarmList()
        HomeDestination.Unavailable -> FeatureUnavailableScreen(
            titleRes = tab.labelRes,
            onNavigateToSettings = onNavigateToSettings,
        )
    }
}
```

Replace the `composable(Screen.AlarmList.route)` block with:

```kotlin
        composable(Screen.AlarmList.route) {
            HomeTabRoute(
                route = Screen.AlarmList.route,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
            ) {
                AlarmListScreen(
                    onNavigateToDetail = { alarmId ->
                        if (alarmId == "WIZARD") {
                            navController.navigate(Screen.AlarmWizard.route)
                        } else {
                            navController.navigate(Screen.AlarmDetail.createRoute(alarmId ?: ""))
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToChat = {
                        navController.navigate(Screen.LemurChat.route)
                    },
                )
            }
        }
```

After that block, add:

```kotlin
        composable(Screen.Timer.route) {
            HomeTabRoute(
                route = Screen.Timer.route,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                alarmList = {},
            )
        }
        composable(Screen.Stopwatch.route) {
            HomeTabRoute(
                route = Screen.Stopwatch.route,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                alarmList = {},
            )
        }
        composable(Screen.SleepTab.route) {
            HomeTabRoute(
                route = Screen.SleepTab.route,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                alarmList = {},
            )
        }
```

Leave the existing `composable(Screen.SleepTracking.route)` block in place. Nothing navigates to it.

- [ ] **Step 3: Stop the alarm list from adding system insets again**

On `AlarmListScreen`'s `Scaffold(`, add `contentWindowInsets = WindowInsets(0)`. Import `androidx.compose.foundation.layout.WindowInsets`.

- [ ] **Step 4: Compile and keep the unit test green**

```bash
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests "com.elroi.lemurloop.ui.navigation.HomeTabsTest"
```

Expected: `BUILD SUCCESSFUL`.

Do not commit yet. The screens are not reachable from the bar until Task 3.

---

### Task 3: Home bar

**Files:**
- Modify: `app/src/main/java/com/elroi/lemurloop/ui/MainScreen.kt`
- Test: `HomeTabsTest` stays green. The re-tap no-op is in this screen and is checked by hand in Task 6.

**Interfaces:**
- Consumes: `homeTabs`, `showsHomeBar`, `selectedHomeTab`, `Screen.AlarmList.route`.
- Produces: the bar in `MainScreen`. `onClick` calls `navigate` only when `tab.route != currentRoute`.

- [ ] **Step 1: Replace MainScreen's scaffold**

Remove the unused imports (`Icons.Filled.Home`, `Icons.Filled.Settings`, `AnimatedVisibility`, `expandVertically`, `shrinkVertically`, `dp`, `sp`, `BuildConfig`, `hierarchy`, `findStartDestination`). Keep the splash `return` before the scaffold.

Replace the scaffold with:

```kotlin
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
```

Import `androidx.compose.foundation.layout.consumeWindowInsets`, `androidx.compose.material3.Icon`, `androidx.compose.material3.NavigationBar`, `androidx.compose.material3.NavigationBarItem`, and `com.elroi.lemurloop.ui.navigation.homeTabs`, `selectedHomeTab`, and `showsHomeBar`.

The `if (showBar)` branch is the whole bar. There is no `AnimatedVisibility`.

- [ ] **Step 2: Compile and re-run the unit test**

```bash
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests "com.elroi.lemurloop.ui.navigation.HomeTabsTest"
```

Expected: `BUILD SUCCESSFUL`.

Do not commit until Task 6 and the user has said the shell looks right.

---

### Task 4: Replay returns to one alarm list

**Files:**
- Modify: `app/src/main/java/com/elroi/lemurloop/ui/navigation/LemurLoopNavGraph.kt` (the `OnboardingReplay` `onFinished` block, lines 40–51)
- Do not modify the first-run `Screen.Onboarding.route` handler.

**Interfaces:**
- Consumes: `Screen.AlarmList.route`, `Screen.AlarmWizard.route`, `navController.graph.id`.
- Produces: replay finish leaves the original alarm list on top, then optionally the wizard.

- [ ] **Step 1: Replace the replay callback**

Replace both branches inside `onFinished` with one path:

```kotlin
                onFinished = { createAlarm ->
                    val popped = navController.popBackStack(Screen.AlarmList.route, inclusive = false)
                    if (!popped) {
                        navController.navigate(Screen.AlarmList.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }
                    if (createAlarm) {
                        navController.navigate(Screen.AlarmWizard.route)
                    }
                }
```

This removes Settings, the tab or chat that opened Settings, and the replay screen. It does not call `navigate(Screen.AlarmList.route)` when the pop succeeds.

- [ ] **Step 2: Compile**

```bash
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests "com.elroi.lemurloop.ui.navigation.HomeTabsTest"
```

Expected: `BUILD SUCCESSFUL`.

---

### Task 5: Glossary and right-to-left checklist

**Files:**
- Modify: `docs/HEBREW-TRANSLATION-GLOSSARY.md`
- Modify: `docs/RTL-LOCALIZATION.md`

**Interfaces:**
- Consumes: the Hebrew strings from Task 1.
- Produces: glossary rows and one checklist row a manual pass can follow.

- [ ] **Step 1: Add glossary rows**

In `docs/HEBREW-TRANSLATION-GLOSSARY.md`, after the Alarm row:

```markdown
| **Timer** (home tab) | **טיימר** | Short tab label. |
| **Stopwatch** (home tab) | **סטופר** | Short tab label. |
| **Sleep** (home tab) | **שינה** | Short tab label. The sleep-tracking screen title stays **מעקב שינה**. |
```

- [ ] **Step 2: Add the checklist row**

In `docs/RTL-LOCALIZATION.md`, add this row after the Alarm list row:

```markdown
| **Home bar** | (1) Alarms, Timer, Stopwatch, and Sleep are in Hebrew. (2) Alarms is the rightmost tab. (3) The settings gear on a not-ready screen is on the left. (4) Titles stay centered. (5) The bar does not animate. |
```

---

### Task 6: Manual check before any UI commit

Install a debug build with `./gradlew :app:installDebug` on a connected device or emulator. Run every item below in English and in Hebrew. Run the whole list on API 35 or newer twice, once with gesture navigation and once with 3-button navigation, and once on API 34 or older.

- [ ] The app opens on the alarm list. Alarms is selected. The other three tabs are not. The add button keeps its normal margin above the bar, with no second gap. The title is not pushed down by a second status-bar inset.
- [ ] Timer, Stopwatch, and Sleep each show that tab's name as the centered title and `This isn't ready yet.` Exactly that tab is selected. Sleep has no start or stop control.
- [ ] From Timer, Stopwatch, and Sleep, the gear opens Settings and the bar is hidden. Back returns to that same tab, selected, with the bar visible.
- [ ] Opening an alarm hides the bar. Back returns to the alarm list with Alarms selected.
- [ ] System Back from Timer returns to the alarm list. System Back from the alarm list leaves the app.
- [ ] Tapping the already selected tab, once per tab, does not stack a second copy and does not flash another screen.
- [ ] From Timer: Settings, replay onboarding, finish. One alarm list is showing. System Back does not open Settings.
- [ ] The same path, choosing to create an alarm, opens the wizard. Back shows one alarm list with the bar.
- [ ] Finish first-run onboarding, open Timer, system Back to the alarm list, system Back again leaves the app. From Timer, tap Alarms, system Back leaves the app. From Timer, open Stopwatch, system Back returns to the alarm list.
- [ ] From chat, open Settings, replay onboarding, and finish. System Back does not open Settings or chat.
- [ ] Wizard, alarm detail, Settings, and onboarding: top content is below the status bar and bottom content is above the navigation bar, with no second copy of that inset. In chat, open the keyboard. The input stays above it.
- [ ] In Hebrew, Alarms is the rightmost tab, titles are centered, and the gear on a not-ready screen is on the left. The bar does not animate.

Wait for the user to say the shell looks right before committing Tasks 2–5. Then one commit:

```bash
git commit -m "$(cat <<'EOF'
Show a home bar for alarms, timer, stopwatch, and sleep.

EOF
)"
```

Stage only the files this plan names. Do not stage `.superpowers/`.
