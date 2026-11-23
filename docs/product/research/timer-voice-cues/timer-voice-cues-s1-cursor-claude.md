---
agent: cursor-claude
model: claude-sonnet-5-5-high
stage: 1
task_id: timer-voice-cues
date: 2026-11-19
baseline_commit: 060ca6864b6dd16b3ccdf46ded9545520bde7464
independence: confirmed        # no other agent's s1/s2 output was opened; I listed docs/product/research/ (folder names only) and never opened anything inside docs/product/research/timer-voice-cues/ except to write this file
inputs_consulted:
  - tmp/.multi-agent-convergence-protocol.md
  - docs/superpowers/specs/2026-11-12-timer-design.md
  - docs/agents/MANUAL-DEVICE-QA.md
  - app/src/main/AndroidManifest.xml
  - app/build.gradle.kts (grep for minSdk / targetSdk / compileSdk only)
  - app/src/main/java/com/elroi/lemurloop/data/scheduler/AndroidTimerScheduler.kt
  - app/src/main/java/com/elroi/lemurloop/domain/scheduler/TimerScheduler.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerListController.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerAlarmIntents.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerSchedulePlan.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerBootReconciler.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerTransitions.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/CountdownTimer.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerSettings.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerRingPorts.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerRingSession.kt
  - app/src/main/java/com/elroi/lemurloop/receiver/TimerFiredReceiver.kt
  - app/src/main/java/com/elroi/lemurloop/receiver/BootReceiver.kt
  - app/src/main/java/com/elroi/lemurloop/service/TimerRingService.kt
  - app/src/main/java/com/elroi/lemurloop/data/timer/AndroidTimerSideEffects.kt
  - app/src/main/java/com/elroi/lemurloop/data/repository/TimerDataWiper.kt
  - app/src/main/java/com/elroi/lemurloop/data/repository/TimerSettingsRepositoryImpl.kt
  - app/src/main/java/com/elroi/lemurloop/data/repository/TimerSettingsDataStore.kt
  - app/src/main/java/com/elroi/lemurloop/data/local/entity/TimerEntity.kt
  - app/src/main/java/com/elroi/lemurloop/data/local/AppDatabase.kt (lines 150-190)
  - app/src/main/java/com/elroi/lemurloop/di/AppModule.kt (lines 95-195)
  - app/src/main/java/com/elroi/lemurloop/ui/viewmodel/TimerViewModel.kt
  - app/src/main/java/com/elroi/lemurloop/ui/viewmodel/TimerSettingsViewModel.kt
  - app/src/main/java/com/elroi/lemurloop/domain/manager/TextToSpeechManager.kt (grep only)
  - app/src/main/java/com/elroi/lemurloop/domain/manager/SettingsSearch.kt (grep only)
  - app/src/main/java/com/elroi/lemurloop/LemurLoopApp.kt (lines 35-85)
  - app/src/test/java/com/elroi/lemurloop/domain/timer/TimerListControllerTest.kt (first 70 lines + grep)
---

# Timer voice cues: Stage 1 review (cursor-claude)

Marking convention: `[verified]` = I opened the code at the baseline commit. `[unverified: platform]` = Android platform behavior I know from documentation or experience but cannot prove from this repository; each of those needs a device check before it is trusted. Acronyms: Text-to-Speech (TTS), foreground service (FGS), Do Not Disturb (DND), Application Not Responding (ANR).

## Recommendation

**Adopt with specific amendments.** The product locks are deliverable and the central instinct is right: cue alarms must not use `setAlarmClock`, so they do not take the system "next alarm" slot or end a DND "until next alarm" rule. The pure resolver plus planner split is also right. But the design as written has defects that produce wrong or duplicate speech in ordinary use, and it puts the lifecycle hooks in the wrong place. The nine amendments below are the replacement rules. The first five are blockers: they cause wrong behavior on common inputs.

| # | Design section | Defect in one line | Severity |
|---|---|---|---|
| A1 | Skip and collide rules | `+1:00` after Halfway was spoken makes Halfway speak a second time | High |
| A2 | Skip and collide rules / Planner input | Halfway and Start collision thresholds use the wrong variable and ignore speech length | High |
| A3 | Voice cue service (`LAST_TEN`) | No wake lock, no TTS warm-up, relative delays: the countdown stalls or lands after the ring | High |
| A4 | Voice cue service / Multi-timer | `stopSelf()` and re-entry make one timer's cue kill or double another's countdown | High |
| A5 | Architecture (receiver) | Nothing validates a cue at fire time, so late or stale alarms speak wrong content | High |
| A6 | Integration points | Hooks sit in `TimerListController`, but arm/cancel is also called from three other classes (reconciler, wiper, ring handler), and Undo and settings changes have no hook | Medium-high |
| A7 | Skip and collide rules (reconcile) | "Fires immediately" on reconcile breaks the locked "never `startForegroundService`" rule | Medium |
| A8 | Failures (exact alarms denied) | The inexact fallback makes the receiver's FGS start throw on API 31+ and speaks stale cues | Medium |
| A9 | Voice / What the user hears | No audio attributes, focus, or locale mechanism; Hebrew silently produces nothing | Medium |

## Reasoning

### What I verified about the design's repository claims

- `AppDatabase` is version 27 `[verified]` (`@Database(... version = 27 ...)`, with `MIGRATION_26_27` present). The 2026-11-12 timer spec's "version 26" is stale `[verified]`. The design is right on both counts.
- The end alarm uses `setAlarmClock` when allowed and `setAndAllowWhileIdle` otherwise `[verified]` (`AndroidTimerScheduler.schedule`, `timerAlarmScheduleMode`). Note the fallback is the **inexact** family, not "the exact-alarm fallback family" as the design phrases it.
- End-fire intents use data `lemurloop://timer/<id>` and a constant request code `0` `[verified]` (`TimerAlarmIntents`). `timerId()` parses by prefix `lemurloop://timer/`, so `lemurloop://timer-cue/<id>/<kind>` can never be parsed as an end-fire (the character after `timer` is `-`, not `/`) `[verified]`. The design's "intent URI / request-code uniqueness" test is nearly vacuous: PendingIntent identity is the intent filter (action, data, component), and the cue receiver is a different component anyway.
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` and `WAKE_LOCK` are already declared `[verified]`. `USE_EXACT_ALARM` is declared with no `maxSdkVersion` `[verified]`. `minSdk = 26`, `targetSdk = 35` `[verified]`.
- Timer notification ids already in use: finished = 220, ring = 221 `[verified]` (`TimerFinishedNotification.ID`, `TimerRingNotification.ID`). The sleep and morning-alarm ids I did not check `[unverified]`; the new id must avoid all of them.
- `TimerRingHandler.cancelAlarm` calls `timerScheduler.cancel(id)` at ring start `[verified]`. `TimerDataWiper.wipe` calls `timerScheduler.cancel` per timer `[verified]`. `TimerBootReconciler` calls `schedule` and `cancel` `[verified]`. So the arm/cancel call sites are: `TimerListController` (start, duplicate, edit, undo, delete), `TimerBootReconciler`, `TimerDataWiper`, and `TimerRingService` via the handler. The design's "hooks sit next to end-alarm arming in `TimerListController`" covers only the first.
- Reconcile is invoked from exactly one place: `BootReceiver`, for `BOOT_COMPLETED` and `TIME_SET` `[verified]` (grep for `.reconcile(`). The design (and the 2026-11-12 spec) say reconcile also runs "on process start" and "when a restored row has no live alarm"; that code does not exist at this commit `[verified]`.
- `TextToSpeechManager` hard-codes `Locale.US` (lines 76, 218-222) `[verified]`. It is the Gemini briefing / persona TTS path, so it is not the right class to reuse for timers.
- The app language is applied by wrapping the `Application` base context in `LemurLoopApp.attachBaseContext` using a DataStore value `[verified]`. A `Service`'s own context is not that wrapped context. So `this.getString(...)` inside a new service may resolve against the system locale on devices where per-app locale is not handled by the platform `[unverified: platform]`. `TimerRingService` already calls `getString` the same way.

### A1. Halfway replays after `+1:00` (Skip and collide rules)

The design says past milestones are not replayed and rebuilds on `+1:00` "from current remaining time". The planner is stateless and recomputes Halfway from the new duration. Concrete failure:

- Start a 10:00 timer. Halfway is spoken at 5:00 elapsed.
- At 5:10 elapsed, tap `+1:00`. Original becomes 11:00, end moves +60 s, new halfway = end minus 5:30 = 5:30 elapsed.
- 5:30 is after 5:10, so it is a future milestone. "Halfway" is spoken a second time, 20 seconds after the first.

`TimerTransitions.addOneMinute` raises `originalDurationMillis` and moves `endsAtEpochMillis` `[verified]`, so this is reachable from the existing `+1:00` control today.

**Replacement rule:** the planner takes a `halfwayEligible` flag, like the design's `allowStart` flag. `TimerListController.addOneMinute` has the pre-edit timer, so it computes `oldHalfwayEpoch = oldEnd - oldOriginal/2` and passes `halfwayEligible = oldHalfwayEpoch > now`. Pause then play needs no flag: with end = now + remaining and halfway = end - original/2, the milestone is in the future exactly when remaining > original/2, so it cannot double-fire. No new persisted state is needed.

### A2. Halfway and Start thresholds are wrong (Skip and collide rules, planner input list)

Two problems.

1. **Wrong variable.** The design says Halfway is omitted when "remaining duration at plan time is under 20 seconds". The planner input list also has no original duration, so Halfway at 50% of the run cannot be computed at all. The condition that matters is the gap between the Halfway epoch (`end - original/2`) and the first last-10 digit epoch (`end - 10 s`).
2. **Ignores speech length.** With the 20 s rule, a 21 s timer keeps Halfway at `end - 10.5 s`, which is 0.5 s before "10". "Pasta, halfway" takes about 1.5 s to say, so "10" arrives mid-utterance. With `QUEUE_FLUSH` (the design's V1 choice) one of them is cut off. A label can be 40 characters `[verified]` (`TimerLimits.MAX_LABEL_LENGTH`).

Also, the rules text and the test list disagree. The test says "8 s yields enter-last-10 only", which implies Start is omitted for short timers, but no rule says so. For an 8 s timer, Start and the first digit are both due at t = 0.

**Replacement rules** (one tunable constant `CUE_SPEECH_BUDGET_MILLIS = 4_000`):

- Planner input is `(nowEpoch, endEpoch, originalDuration, effective flags, allowStart, halfwayEligible)`. Remaining is derived as `end - now`.
- Halfway is planned only if `halfwayEpoch > now` and `halfwayEpoch <= end - 10_000 - BUDGET`. That means original >= 28 s.
- Start is planned only if `allowStart` and `(end - 10_000) - now >= BUDGET`, i.e. at least 14 s remain at start. Otherwise Start is dropped and the countdown takes over.
- Last-10 digit k (k = 10..1) is due at `end - k * 1000`. A digit is planned only if it is not already past by more than 400 ms. The first spoken digit is the largest such k. This pins down "remaining seconds" (ceil vs floor), which the design leaves open.
- Boundary tests at original = 9, 10, 13, 14, 27, 28, 29 s, plus a resumed run that begins inside the window.

### A3. Last-10 countdown timing (Voice cue service, `LAST_TEN`)

The design says "speak one second per utterance for the remaining count, then `stopSelf`" and sells the FGS as more reliable than ten alarms. It does not say how the second is measured, and three things break it:

1. **TTS cold start.** `TextToSpeech` initializes asynchronously. The alarm at `end - 10 s` starts a possibly cold process, then `startForeground`, then TTS init `[unverified: platform; cold engine init commonly takes hundreds of milliseconds to a second or more]`. "10" and "9" are late or missed on the most common case (screen off, app not in memory).
2. **No wake lock.** An FGS does not keep the CPU awake `[unverified: platform]`. A coroutine `delay(1000)` between utterances counts uptime. If the CPU suspends between short utterances, the sequence stretches and "1" is spoken after the ring has started at T. `WAKE_LOCK` is already in the manifest `[verified]`, so this is cheap to fix.
3. **Relative timing.** Digits must be anchored to the wall-clock end epoch, not to each other, or per-utterance TTS latency accumulates.

**Replacement rule:**

- Schedule the EnterLastTen alarm at `end - 10 s - WARMUP` (WARMUP about 3 s), action `PREPARE_LAST_TEN`. On receipt the service calls `startForeground` first (before any other work, as `TimerRingService` does `[verified]`; an FGS that does not call it in time is a crash), acquires a `PARTIAL_WAKE_LOCK` with a timeout of `WARMUP + 12 s`, and initializes TTS.
- A loop speaks digit k when `System.currentTimeMillis() >= end - k*1000`, computing each wait from the wall clock. It skips a digit that is more than about 400 ms late rather than speaking it late, and stops when `now >= end`.
- Optional hardening, not required for V1: `synthesizeToFile` the digits once per locale and play them from cache. That is still on-device TTS, so it respects the lock, and it removes engine latency from the countdown.

### A4. One service, many timers (Voice cue service, Multi-timer behavior)

The design has a single `TimerVoiceCueService`, `SPEAK_ONCE` then `stopSelf`, `LAST_TEN` then `stopSelf`, and "last-10 is per timer id". Concrete failures:

- Timer A is at T-6 s in its countdown. Timer B's Halfway alarm fires. B's `SPEAK_ONCE` finishes and calls `stopSelf()`. The service is destroyed; A's digits 5..1 never play. Plain `stopSelf()` stops the service regardless of other work.
- Timer A is mid-countdown. The user changes the system clock at T-7 s. `TimerBootReconciler` runs on `TIME_SET` `[verified]`, rebuilds, sees remaining 7 s, and "EnterLastTen fires immediately". A second `LAST_TEN` reaches the same timer id. Result: duplicated or restarted digits ("7, 7, 6, 6"). The design's no-restart rule only covers system kills, not rebuild re-entry.
- Pause on timer A: `Context.stopService` stops the whole service and silences B. The existing `AndroidTimerRingStop` is `stopService` by class name `[verified]`, which is fine for one ring session, but cue cancellation must be per timer id.

**Replacement rule:** the service holds `Map<timerId, Job>` for countdowns and a single serialized speaker. `LAST_TEN` for an id that already has an active job with the same `end` is ignored (idempotent). A `CANCEL_TIMER` action with the timer id cancels only that job. The service calls `stopSelf(startId)` only when the map is empty and the speaker is idle. Tests: a fake speaker plus a fake clock driving the service logic, asserting "B's Halfway does not cut A's countdown" and "duplicate `LAST_TEN` is a no-op".

### A5. No validation at fire time (receiver and service)

The intent carries only `timerId` and `kind` (`lemurloop://timer-cue/<id>/<kind>`). Nothing re-checks reality when it fires. Failures:

- Late delivery (Doze quota or throttling, `[unverified: platform]`; or the inexact fallback; or a busy device): "Halfway" is spoken at 95% elapsed; EnterLastTen arrives with 3 s left and the service would say "10".
- A race where the alarm has already been dispatched when the user pauses or deletes, so the cancel does not reach it.
- Settings changed after planning but before firing (resolution happens at plan time only).

**Replacement rule:** extras carry `endsAtEpochMillis` and `plannedAtEpochMillis`. At fire time the service drops the cue if any of these holds: the row is no longer Running; the row's `endsAtEpochMillis` differs from the extra; the cue is more than 10 s late (Start/Halfway) or the digit is more than 400 ms late (A3); or the effective flag, re-resolved from current settings and the row, is now off. Plan-time resolution stays (so a disabled feature does not wake the device); fire-time resolution is the second line of defense.

### A6. Integration points are in the wrong place (Integration points)

Arm/cancel call sites are listed under "What I verified" (four classes, of which the controller is one). Concrete failures from the current design wording:

- Ring start: `TimerRingHandler.onFire` calls `cancelAlarm` (the timer's `scheduler.cancel`) `[verified]`. If cue cancellation is a separate call "in `TimerListController`", ring start never stops that timer's last-10 service. Wipe has the same gap (`TimerDataWiper` has no cue port `[verified]`).
- Undo: the design's rebuild list (pause, play, +1:00, refresh, delete, dismiss, wipe, settings change, ring start) omits Undo. `TimerListController.undo` re-arms the end alarm `[verified]`; a restored running timer would have no cue alarms.
- Settings change: `TimerSettingsViewModel` has only the settings repository and the grant releaser `[verified]`. It has no timers and no scheduler, so "settings change cancels and rebuilds cue alarms" has no owner. The same applies to a per-timer override edit, which also needs a new controller method.
- `TimerScheduler.schedule(timerId, end)` carries no original duration, no `allowStart`, and no `halfwayEligible`, so it cannot feed the A1/A2 planner.

**Replacement rule:** put cues behind the existing `TimerScheduler` seam so every current arm/cancel call site is covered without new hooks. Concretely: change the port to `schedule(timer: CountdownTimer, nowEpoch: Long, run: TimerRunKind)` where `TimerRunKind` is `NewRun` (start, play, duplicate), `Edit` (`+1:00`, carrying `halfwayEligible`), or `Restore` (undo, reconcile). Keep `cancel(timerId)`. `AndroidTimerScheduler` (or a decorator) cancels the end alarm and all three cue alarms and the service job in `cancel`. Add a `TimerVoiceCueCoordinator.rebuildAll()` used by `TimerSettingsViewModel` and by the override setter. Updating the three fakes (`MemoryScheduler` in `TimerListControllerTest` and the ones in the boot and wiper tests) is part of the change; they implement the old two-method interface `[verified]` for the controller test, the other two `[unverified]`.

### A7. Reconcile and the locked "never `startForegroundService`" rule (Skip and collide rules)

The locked timer spec says boot and clock receivers re-arm through `AlarmManager` and never call `startForegroundService` `[verified]`, and `TimerBootReconciler` enforces it with a `check` `[verified]`. The cue design's "if remaining is 1..10 s, EnterLastTen fires immediately" has two readings: a direct service start (violates the lock) or an alarm at `now`. Also, "process reconcile" does not exist at this commit (see above).

**Replacement rule:** state it: on reconcile, an in-window EnterLastTen is scheduled as an alarm at `now`, never as a direct service start; Start is never emitted (already in the design). Drop "process reconcile" from the design text, or add a task for it. Add a reconciler test that asserts no direct service start.

### A8. The inexact fallback (Failures, "Exact alarms denied")

The design says "best-effort `setAndAllowWhileIdle`; cues may be late". Two issues:

- On API 31+ an FGS start from a broadcast that came from an inexact alarm is not covered by the exact-alarm exemption, so `startForegroundService` can throw `ForegroundServiceStartNotAllowedException` `[unverified: platform]`. The design's `TimerVoiceCueReceiver` has no try/catch, so the receiver crashes.
- Late cues speak stale content (A5).

How reachable is it? `USE_EXACT_ALARM` is declared without `maxSdkVersion` `[verified]`, so on API 33+ the exact path is the normal case `[unverified: platform; I believe install-time grant]`. The fallback is reachable mainly on API 31-32 after the user revokes exact alarms. It is a small population, but the failure is a crash, not just a missed cue.

**Replacement rule:** when exact alarms are not allowed, skip cue alarms entirely (quiet failure, logged) instead of the inexact fallback. The end ring is unaffected. Wrap every `startForegroundService` in the cue receiver in try/catch with a log line.

### A9. Audio path and locale (Voice, What the user hears)

The design locks on-device TTS and says cues "do not own `STREAM_ALARM` prioritization", but never specifies the audio path. Concrete failures:

- Default `TextToSpeech` routing is the media stream `[unverified: platform]`. Media volume at 0, a Bluetooth speaker, or DND set to alarms-only means the user hears nothing while the settings screen says cues are on. That may be acceptable ("quiet"), but it should be a documented decision with a chosen `AudioAttributes` usage, not an accident.
- No audio focus is requested, so cues talk over music or a podcast at the same volume, and during a call the cue may be spoken into the call audio `[unverified: platform]`. Use `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`, release it when idle, and skip the cue when `AudioManager.mode` is in-call or in-communication.
- **Locale.** The design says the locale follows the app language. No existing class does this: `TextToSpeechManager` is hard-coded to `Locale.US` `[verified]` and sits on the persona/Cloud path, so reusing it would risk breaking the "never Cloud TTS" lock `[unverified: I did not open its routing code beyond the grep]`. Build a dedicated `TimerVoiceCueSpeaker`. Resolve strings and the TTS `Locale` from `applicationContext` or the stored language, not from the service's own context (see "What I verified").
- **Hebrew voice availability.** If the device has no Hebrew voice, the design's rule is "log; skip that utterance". A Hebrew user with the master on then hears nothing, forever, with no signal. **Replacement rule:** when the master turns on, probe `isLanguageAvailable` for the app locale and show an inline note in the Voice cues block if it is missing. Fallback to English speech should be an explicit product decision, not an accident.
- Notification: an FGS shows a notification for each cue, and the channel must be low importance and silent so it does not itself make noise.

### Other findings (smaller)

- **Master vs override semantics.** `effective(cue) = override ?: (master && globalToggle(cue))` means a per-timer On speaks while the master is off. The UI text says the three toggles are "enabled only when master is on". Decide whether master is a kill switch or just the default; the resolver matrix test is only meaningful once this is decided. My recommendation: keep the formula as written (it is the only reason per-timer On exists) and say so in the Settings copy; show the per-timer control regardless of master state.
- **"Start on play".** The design says Start fires on "a user start or play that newly begins a run". Every pause-then-play would say "Timer started". If the intent is only start, duplicate, and play-after-Refresh, say so; otherwise a resume should be silent or say something else. Duplicate builds the copy via `TimerTransitions.start` `[verified]`, so it would reset overrides to Inherit unless the design says Duplicate copies them.
- **Settings read-modify-write.** `TimerSettingsViewModel.update` does `repository.update(change(repository.get()))` `[verified]`, and `TimerRingService.captureAlarmVolume` does the same with `runBlocking` `[verified]`. A voice toggle written between those two steps is lost. This is an existing race that adding four fields makes slightly more exposed; a per-key `dataStore.edit` for the cue keys avoids it.
- **Start via alarm.** Start is a user action in the foreground app. Routing it through alarm, receiver, and FGS adds latency and consumes allow-while-idle quota for no benefit. Start the service directly from the app (an FGS start from the foreground is allowed `[unverified: platform]`) and keep alarms for Halfway and EnterLastTen.
- **Doze quota.** Allow-while-idle alarms are rate-limited per app `[unverified: platform]`. 20 timers with two cue alarms each is a realistic worst case. A5 makes late cues drop instead of misspeak; device QA must include `adb shell dumpsys deviceidle force-idle`.
- **FGS type.** `mediaPlayback` is already declared for the ring service `[verified]`, so reusing it is consistent. On API 34+ a `shortService` type is designed for short critical work and avoids claiming media playback for spoken cues `[unverified: platform]`; worth a single paragraph in the plan, not a blocker.
- **DND claim.** Using `setExactAndAllowWhileIdle` instead of `setAlarmClock` keeps cues out of the "next alarm" slot, because `setAlarmClock` is the call that publishes an `AlarmClockInfo` (in the timer package only `AndroidTimerScheduler.schedule` calls it `[verified]`; I did not check the morning-alarm scheduler). That the DND "until next alarm" rule then ignores cue alarms is platform behavior `[unverified: platform]`. The end-ring path is untouched by the design as long as A6 keeps the end alarm call exactly as it is today.

## Risks and unknowns

- **Platform claims I could not verify from the repo** (all marked `[unverified: platform]`): FGS start exemption for exact-alarm broadcasts, FGS start from an inexact alarm, TTS cold-start latency, CPU sleep between utterances, Doze quota behavior, `shortService`, DND and stream routing. If the first one is wrong, the whole alarm-to-service design fails, not just the fallback. It is the first thing to prove on a device: master on, screen off, app swiped away, API 34 and API 35.
- **My amendments add complexity.** A6 changes a port that three test fakes implement and touches `TimerBootReconciler`, `TimerDataWiper`, `TimerRingService`. That is real churn, justified by removing separate hook sites in four classes, but it raises the PR size. It could be split: PR 1 adds the port change with no behavior (a pure refactor with tests), PR 2 adds cues.
- **The 4 s speech budget is a guess.** It is a constant with boundary tests, not a measured value. Hebrew speech length differs from English.
- **I did not read** `TimerRepositoryImpl`, `TimerDao`, the Settings and Focus screens, the Hebrew parity test, `AlarmService`'s TTS path, or the other two test fakes. The UI parts of the design (settings block, per-timer control) are not reviewed beyond the data model.

## What you would need to be wrong

The strongest case against amending: the cues are an optional, quiet-by-default convenience, and V1 may reasonably ship with "missed digits are acceptable". Under that reading A3, A4 and A5 are polish, and a simpler design is better: no last-10 service at all, just Start and Halfway as one-shot spoken cues, with the countdown deferred to V2. That would remove the largest source of defects (A3, A4, most of A5) and the wake-lock question. It would also break the locked cue set (start, halfway, last-10), so it is a product call, not mine. I would still recommend the amendments over cutting last-10 because a half-working countdown that stalls or doubles is worse than none, and the user opted in to it explicitly.

A second counter-argument: A6 (decorating the existing scheduler seam) could be wrong if cue planning needs state the end-alarm path should never see. If the team prefers cue code isolated from the locked timer package, the alternative is a separate `TimerVoiceCueScheduler` port called from a single `TimerCueCoordinator` that the controller, reconciler, wiper and ring handler all receive. That is more constructor churn but a cleaner boundary. Both fix the hook-sprawl failure; the failure is what matters, not the shape.

## Open questions for the human

1. **Master semantics.** Should a per-timer On speak while the global master is off, or is the master a kill switch? It changes the resolver and the test matrix.
2. **Resume.** Should Play after Pause say "Timer started"? If not, what should it say, if anything?
3. **Hebrew without a Hebrew voice.** Warn only, fall back to English speech, or stay silent? It changes the Settings UI and the failure table.
4. **Duplicate.** Does Duplicate copy the per-timer overrides or reset them to Inherit?
5. **Scope fallback.** If the device proof of the exact-alarm FGS start fails on API 34 or 35, is the team willing to ship Start and Halfway only (no countdown) for V1, or must the last-10 countdown run in the foreground-only case?
