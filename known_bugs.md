# Known Bugs

This document tracks bugs identified in the `aNewDawnAlarmClock` project.

## Pending Verification (Fixed in `fix/bug-fixes-2026-03`)

The following bugs have been implemented but are still awaiting manual verification in the UI/Service layer:

### BUG-2: AlarmActivity Intent Extra Key Typo
*   **Issue**: The intent extra key for "Evasive Snoozes Before Moving" was misspelled in `AlarmActivity.kt`, causing the setting to be ignored.
*   **Fix**: Updated to use the consistent `AlarmService.EXTRA_EVASIVE_SNOOZES_BEFORE_MOVING` constant.
*   **Status**: Fixed in code. Awaiting manual verification of Evasive Snooze behavior.

### BUG-4: Wakeup Check Skipped after Natural TTS Completion
*   **Issue**: The "Smart Wakeup" safety check only triggered if the user manually stopped the briefing. If the briefing finished naturally, the check was skipped.
*   **Fix**: Added `scheduleWakeupCheck()` to the `onDestroy()` method of `AlarmService` to ensure it fires regardless of how the briefing ends.
*   **Status**: Fixed in code. Awaiting verification of the 3-minute post-briefing safety check.

### BUG-5: AccountabilityManager Toast Threading Crash
*   **Issue**: `Toast.makeText()` was called from a background IO coroutine when an SMS failed, which causes an Android runtime crash.
*   **Fix**: Wrapped the Toast call in a `Handler(Looper.getMainLooper()).post` block.
*   **Status**: Fixed in code. Awaiting verification of error handling during SMS failure.
