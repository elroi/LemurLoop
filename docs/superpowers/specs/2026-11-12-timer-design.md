# Xtreme-style timer

**Date:** 2026-11-12
**Status:** Locked. This is the revised design after the four-seat review. Online radio is a later plan.

A person can keep up to 20 countdown timers. The Timer tab lists them newest first. Each one pauses, continues, and rings on its own. The ring still fires with the screen off. Stopwatch and Sleep stay not ready. Chat, the wizard, buddy texts, math, and smile-to-dismiss stay on alarms only.

## What a timer is

A timer has a label, an original duration, a wall-clock end, a phase, and a creation time. The creation time is set once and kept across pause, play, dismiss, and undo. Duplicate is a new timer and gets its own creation time. The list sorts by that time, newest first, so saving a row again does not reorder the list.

The duration is from 1 second through 99:59:59. Digits that overflow a field carry: 90 seconds is 1 minute 30 seconds. Three presets default to 1:00, 5:00, and 10:00. Start at 0 is refused. Start creates a timer and starts it immediately.

Phases are Running, Paused, and Ringing.

- Pause freezes the time left. Pause is refused when the time left is already 0.
- Play continues from the time left.
- +1:00, only while running, adds 60 seconds to the original duration and to the time left, and never goes past 99:59:59.
- Refresh, only while paused, sets the time left back to the original duration, including minutes already added, and stays paused.
- Dismiss, only while ringing, stops the sound and leaves the timer paused at its original duration.
- Rename trims to 40 characters. A blank label is allowed.

There is no +1:00 while paused. `remainingAt` is never negative. A running timer whose end is already past reports 0 until reconcile runs.

Add and Duplicate at 20 timers are refused with "Timer limit reached". Nothing is deleted to make room. Undo does not raise the cap. If the list is already full, Undo is refused with the same message.

## Time

Each running timer stores the wall-clock epoch millisecond it ends. The screen derives the time left from that clock. A manual clock change lengthens or shortens the wait. A time-zone change alone changes nothing. There is no elapsed-time clock and no time-zone handler.

## When it is already late

Reconcile runs on boot, on `android.intent.action.TIME_SET`, on process start, and when a restored row has no live alarm. It is one pure function, `TimerSchedulePlan`. Receivers and ViewModels call that function.

- A running timer with a future end is armed at that end.
- A running timer less than 60 seconds late is armed at the current time, so the alarm broadcast can start the ring.
- A running timer 60 seconds late or more is paused at its original duration, and one notification says it finished.
- A paused timer is cancelled.
- A timer already ringing is armed again the same way only when the ring service is not running.

Boot and the clock receiver re-arm through `AlarmManager`. They never call `startForegroundService`.

Undo is not this path. Undo puts the same timer back, including its old end time. If that end time has already passed, the timer is armed at once and rings, however late it is.

## Scheduling

`AlarmManager.setAlarmClock` wakes the device. When exact alarms are not allowed, the fallback is `setAndAllowWhileIdle`, the same choice the morning alarm already makes. A timer can appear as the system next alarm until it fires, and that can end a Do Not Disturb rule that lasts until the next alarm.

The show intent on that alarm opens the Timer tab. It is not the fire broadcast. Each timer intent has its own data URI, `lemurloop://timer/<id>`, so cancel and schedule rebuild the same intent. The request code is not a hash of the id.

Timers are not alarm rows. The timer does not start the morning alarm service.

## Ring

One ring session. A second timer that finishes while one is already ringing joins that session. It does not recapture volume, restart the ramp, or open a second screen. One Dismiss stops the sound and dismisses every timer that is ringing. Dismiss is on the notification, the list row, and the full-screen timer. System Back on the ring screen dismisses the ring.

Playback is `MediaPlayer`, looping, `USAGE_ALARM`. The ramp is a coroutine delay loop on player gain, on by default, over 1 minute, and it can be off or 1 to 10 minutes. The service calls `startForeground` before it reads DataStore, including when the sound is silent. The notification id is not 111 and not the sleep notification id. The channel does not play a sound or vibrate of its own.

Sound is the device alarm ringtone, one on-device track, or silent. There is no online radio. The phone track is chosen with `ACTION_OPEN_DOCUMENT` (`audio/*`) and a persistable read permission. Replacing or clearing the track releases the previous grant. If the track will not open at ring time, that ring uses the default alarm ringtone.

Volume is stored as 0–100. Prioritize this volume is on by default and sets `STREAM_ALARM` once per ring session. The previous level is stored before the change and restored on dismiss, on destroy, and at the next start if a captured level is still stored. Allow volume-key changes during the ring is on by default. A concurrent morning alarm can still be affected. The services are not coupled to detect it.

Vibration is a rhythm, not a louder buzz. Off, light `RAPID_PULSE`, medium `BASIC`, strong `STACCATO`. Medium is the default. `HEARTBEAT` is not offered. `PULSE` is not stored. On phones that cannot set amplitude, the patterns still differ by timing. Strong will not feel heavier than medium.

Keep the screen on is on by default. It holds the screen only while the Timer tab or the full-screen timer is visible and at least one timer is running. One owner sets it. It does not keep the CPU awake after the person leaves the app.

Deleting the last ringing timer stops the service. There is no Undo for a ring that was dismissed this way.

## Data

Timers live in Room table `timers` on `AppDatabase` version 26 (`MIGRATION_25_26`). Timer settings live in a DataStore named `timer_settings`, not on `SettingsManager`.

Wipe all data cancels timer alarms, stops the ring, deletes timer rows, clears `timer_settings`, and releases the persisted track grant.

## Screens

The Timer tab shows the title, a settings gear, an add button, and, when empty, one sentence that there are no timers. The keypad and the countdown digits stay left to right in Hebrew. Swipe toward the end of the row deletes a timer. In English that is toward the right. In Hebrew that is toward the left. A 5-second Undo lives in memory on the Timer screen. Leaving the screen drops it.

Tapping a row opens the full-screen timer. System Back returns to the list. Routes `timer_new`, `timer_focus/{timerId}`, and `timer_settings` are not home-tab routes, so the bar stays hidden.

Settings gets a Timer row that opens Timer settings. A search hit for that screen must not fall through to the generic Settings label.

## Out of scope

Online radio and any station catalog. Stopwatch and Sleep behavior. Wiring natural-language `SetTimer`. Buddy SMS, briefing, math, or smile on a timer. Storing timers as alarms. An elapsed-time clock.
