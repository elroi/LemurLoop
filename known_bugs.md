# Known Bugs

This document tracks bugs identified in the `aNewDawnAlarmClock` project.

## Pending Verification (Fixed in `fix/bug-fixes-2026-03`)

The following bugs have been implemented but are still awaiting manual verification in the UI/Service layer:

### BUG-2: AlarmActivity Intent Extra Key Typo
*   **Issue**: The intent extra key for "Evasive Snoozes Before Moving" was misspelled in `AlarmActivity.kt`, causing the setting to be ignored.
*   **Fix**: Updated to use the consistent `AlarmService.EXTRA_EVASIVE_SNOOZES_BEFORE_MOVING` constant.
*   **Status**: Fixed in code (`AlarmActivity.kt` reads `AlarmIntentExtras.EXTRA_IS_EVASIVE_SNOOZE` / `EXTRA_EVASIVE_SNOOZES_BEFORE_MOVING`). Awaiting manual verification of Evasive Snooze behavior.
*   **Manual test checklist**:
    1. Create an alarm with **Evasive Snooze** enabled and **Snoozes before moving** set to `1`.
    2. Trigger the alarm, dismiss the challenge to get to the ringing screen, then tap **Snooze** once (snooze count is now 1, at the threshold).
    3. Let the alarm re-fire after the snooze duration.
    4. On this second ring, tap **Snooze** again.
    5. **Expected**: the Snooze button visibly drifts around the screen on this second tap (evasive behavior engaged), instead of staying put. If it does not move, the setting is still being ignored.

### BUG-4: Wakeup Check Skipped after Natural TTS Completion
*   **Issue**: The "Smart Wakeup" safety check only triggered if the user manually stopped the briefing. If the briefing finished naturally, the check was skipped.
*   **Fix**: Added `scheduleWakeupCheck()` to the `onDestroy()` method of `AlarmService` to ensure it fires regardless of how the briefing ends.
*   **Status**: Fixed in code (`AlarmService.onDestroy()` calls `scheduleWakeupCheck()` when smart wakeup + briefing were active and TTS wasn't cancelled). Awaiting verification of the 3-minute post-briefing safety check.
*   **Manual test checklist**:
    1. Enable **Smart Wakeup** and **Briefing** on an alarm, with a short wakeup-check delay (e.g. 1 minute) for a faster test.
    2. Trigger the alarm and let the briefing play to completion naturally — do **not** tap Stop/Dismiss on the TTS.
    3. Wait for the configured wakeup-check delay after the briefing ends.
    4. **Expected**: the Wake-up Check screen (`WakeupCheckActivity`) appears after the delay, confirming `scheduleWakeupCheck()` fired even though the briefing ended on its own.
    5. Repeat, but this time tap **Stop** on the TTS partway through, and confirm the check still fires (the `handleStopTts()` path).

### BUG-5: AccountabilityManager Toast Threading Crash
*   **Issue**: `Toast.makeText()` was called from a background IO coroutine when an SMS failed, which causes an Android runtime crash.
*   **Fix**: Wrapped the Toast call in a `Handler(Looper.getMainLooper()).post` block.
*   **Status**: Fixed in code (`AccountabilityManager.kt:207-208` posts the `Toast` via `Handler(Looper.getMainLooper())`). Awaiting verification of error handling during SMS failure.
*   **Manual test checklist**:
    1. Set up an alarm with a **Buddy** phone number and a short accountability delay.
    2. Force the SMS send to fail — e.g. revoke the SMS permission, or use an invalid/unreachable phone number, so `sendMissedAlarmMessage()` hits its failure path.
    3. Trigger the alarm and let the accountability timer elapse without dismissing.
    4. **Expected**: a Toast reporting the SMS failure appears (via `R.string.toast_sms_failed`) and the app does **not** crash. Watch Logcat/crash logs (`cacheDir/crash_*.txt`) to confirm no `CalledFromWrongThreadException`.

### BUG-8: AlarmService.handleSnooze() Dropped Extras on Snooze
*   **Issue**: `handleSnooze()` built the re-fire `Intent` for `AlarmReceiver` by hand with a manual `putExtra` chain that silently omitted `EXTRA_BRIEFING_TIMEOUT` (and, before an earlier partial fix, also omitted the Smart Wakeup extras and `EXTRA_BRIEFING_ENABLED`). Any alarm with a non-default "briefing timeout" (how long to wait for reading when TTS is off) reset to the 30s default after every snooze.
*   **Fix**: Extracted the forwarded-extras mapping into a pure `AlarmSnoozeExtras.build(...)` function (`app/src/main/java/com/elroi/lemurloop/service/AlarmSnoozeExtras.kt`), covering every extra `AlarmReceiver` forwards on initial fire, including `EXTRA_BRIEFING_TIMEOUT`. `handleSnooze()` now applies this map to the `Intent` instead of listing `putExtra` calls by hand, so a future extra added to one side can't silently drop off the other without the test noticing. Regression test: `AlarmSnoozeExtrasTest` (`app/src/test/java/com/elroi/lemurloop/service/AlarmSnoozeExtrasTest.kt`) asserts the forwarded key set matches `AlarmReceiver`'s canonical set and that a non-default briefing timeout survives.
*   **Status**: Fixed in code, JVM unit test covers it (no manual verification needed — this is a pure-function regression test, not a UI behavior).
