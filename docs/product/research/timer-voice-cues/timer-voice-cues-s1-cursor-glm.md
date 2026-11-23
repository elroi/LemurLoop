---
agent: cursor-glm
model: glm-5.2-high
stage: 1
task_id: timer-voice-cues
date: 2026-11-19
baseline_commit: 060ca6864b6dd16b3ccdf46ded9545520bde7464
independence: confirmed
inputs_consulted:
  - docs/superpowers/specs/2026-11-12-timer-design.md
  - app/src/main/java/com/elroi/lemurloop/data/scheduler/AndroidTimerScheduler.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerAlarmIntents.kt
  - app/src/main/java/com/elroi/lemurloop/receiver/TimerFiredReceiver.kt
  - app/src/main/java/com/elroi/lemurloop/receiver/BootReceiver.kt
  - app/src/main/java/com/elroi/lemurloop/service/TimerRingService.kt
  - app/src/main/java/com/elroi/lemurloop/service/AlarmIntentExtras.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerListController.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerSchedulePlan.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerBootReconciler.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerSettings.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerRingSession.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/CountdownTimer.kt
  - app/src/main/java/com/elroi/lemurloop/data/local/entity/TimerEntity.kt
  - app/src/main/java/com/elroi/lemurloop/data/local/AppDatabase.kt
  - app/src/main/java/com/elroi/lemurloop/data/repository/TimerSettingsDataStore.kt
  - app/src/main/java/com/elroi/lemurloop/data/repository/TimerSettingsRepositoryImpl.kt
  - app/src/main/java/com/elroi/lemurloop/data/timer/AndroidTimerSideEffects.kt
  - app/src/main/java/com/elroi/lemurloop/data/timer/AndroidTimerFinishedNotifier.kt
  - app/src/main/java/com/elroi/lemurloop/di/AppModule.kt
  - app/src/main/java/com/elroi/lemurloop/ui/viewmodel/TimerViewModel.kt
  - app/src/main/java/com/elroi/lemurloop/ui/screen/timer/TimerSettingsScreen.kt
  - app/src/main/java/com/elroi/lemurloop/ui/screen/timer/TimerScreen.kt
  - app/src/main/java/com/elroi/lemurloop/ui/screen/timer/TimerFocusScreen.kt
  - app/src/main/java/com/elroi/lemurloop/ui/activity/TimerRingActivity.kt
  - app/src/main/java/com/elroi/lemurloop/domain/manager/SettingsSearch.kt
  - app/src/main/java/com/elroi/lemurloop/ui/screen/settings/SettingsSearchLabels.kt
  - app/src/test/java/com/elroi/lemurloop/ui/screen/settings/SettingsSubScreenRoutesTest.kt
  - app/src/main/AndroidManifest.xml
  - AGENTS.md
---

# Timer voice cues — Stage 1 review (cursor-glm)

## Recommendation

**Adopt with specific amendments.** The design is structurally sound, matches the
live timer package, and respects every product lock. It correctly catches that the
2026-11-12 timer spec's "version 26" line is stale (the live `@Database` annotation
is version 27). Three amendments are required before execution: (1) the Start
emission rule as written would speak "Timer started" on every pause/resume; (2)
`mediaPlayback` is the wrong foreground service type for a Text-to-Speech (TTS)-only
service; (3) the design omits manifest registration and the `START_NOT_STICKY`
choice for the last-10 service. Several smaller actionability gaps are noted below.

## Verified facts

- `AppDatabase` `@Database` annotation is `version = 27`; `MIGRATION_26_27` exists
  and is registered in `AppModule.addMigrations(...)`. The design's note that the
  2026-11-12 spec's "version 26" line is stale is correct. New columns on `timers`
  require `MIGRATION_27_28` + bump to 28 + registration. [verified]
- `timer_settings` DataStore exists (`TimerSettingsDataStore.kt`, name
  `"timer_settings"`). `TimerSettingsRepositoryImpl` stores booleans via
  `booleanPreferencesKey`. Adding `voiceCuesEnabled` / `voiceCueStart` /
  `voiceCueHalfway` / `voiceCueLastTen` follows the existing pattern. [verified]
- `TimerSettings` data class has defaulted fields (e.g. `keepScreenOn = true`).
  Four new defaulted fields fit. [verified]
- `AndroidTimerScheduler` uses `setAlarmClock` when `canScheduleExactAlarms()`
  (or SDK < 31), else `setAndAllowWhileIdle`. The design's hard rule that cue
  alarms must NOT use `setAlarmClock` is correct: `setAlarmClock` makes the alarm
  the system "next alarm" and can end a Do Not Disturb (DND) "until next alarm"
  rule. `setExactAndAllowWhileIdle` / `setAndAllowWhileIdle` avoid that. [verified]
- `TimerAlarmIntents` uses `REQUEST_CODE = 0` for ALL timer intents (fire, show,
  cancel). Differentiation is by action + data URI, not request code. The design's
  "request codes and URIs must not collide" framing is slightly off: the existing
  code already shares request code 0 across all timer intents and relies on URI
  distinction. The cue URI prefix `lemurloop://timer-cue/<timerId>/<kind>` is
  distinct from `lemurloop://timer/<id>`, so PendingIntent matching will not
  collide. The request-code concern is moot for the existing pattern. [verified]
- `TimerListController` is where end-alarm arming happens (`start`, `edit` via
  pause/play/addOneMinute/refresh, `undo`, `duplicate`). The design's "rebuild /
  cancel hooks sit next to end-alarm arming" is accurate. The controller is in
  `domain/timer` and depends only on the `TimerScheduler` interface; cue
  scheduling means injecting a `TimerVoiceCueScheduler` domain interface here
  (or adding an Android side-effect orchestrator — none exists today;
  `AndroidTimerSideEffects.kt` only has ring stop/controls/grant releaser).
  [verified]
- `TimerRingHandler.onFire` calls `cancelAlarm(timerId)` when a timer begins
  ringing. There is no existing hook to cancel cue alarms on ring start — must be
  added. The design correctly identifies this. [verified]
- `TimerBootReconciler` re-arms future ends and cancels paused/finished; it never
  replays a "start" milestone. The design's "Boot / TIME_SET / process reconcile
  re-arm future cues only" is consistent, but the reconciler needs extending to
  also re-arm cue milestones with the Start-omitted flag. [verified]
- `TimerRingService` uses `foregroundServiceType="mediaPlayback"` and
  `TimerRingNotification.ID = 221`. The design's "notification id distinct from
  timer ring and sleep" is actionable; pick something other than 221 and the
  sleep notification id. [verified]
- `TimerRingSession` is a singleton "ring running" flag. The cue service has its
  own lifecycle. [verified]
- Settings search: `SettingsSearchRegistry` in `domain/manager/SettingsSearch.kt`
  has one `timer_settings` SubScreen item. `SettingsSubScreenRoutesTest` asserts
  every SubScreen route is handled and every item has its own title string (not
  the generic `settings_screen_title`). Adding a `timer_voice_cues` item with
  target `SubScreen("timer_settings")` requires new title+desc strings in
  `SettingsSearchLabels` for the new id; the test then passes. The design's
  "title + description for the block and routes that open Timer settings" is
  loose — the existing pattern is one item per sub-screen, not per block. [verified]
- `TimerKeypadScreen` (create), `TimerFocusScreen` (focus/detail), `TimerScreen`
  row overflow menu (`TimerRowMenu`: Rename/Duplicate) all exist. The design's
  "Not on the create keypad. On the focus / detail surface (or row overflow)" is
  reachable on both surfaces it names. [verified]
- Manifest: `TimerFiredReceiver` is registered; `TimerRingService` is registered
  with `foregroundServiceType="mediaPlayback"`. Adding `TimerVoiceCueReceiver`
  and `TimerVoiceCueService` requires new manifest entries. The design does not
  call this out. [verified]
- `TimerDataWiper` is wired in `AppModule`. The design's "wipe-all cancels cue
  alarms and stops any cue service" requires extending it. [verified]
- The repo has no `androidTest` suite (per AGENTS.md). The design's "No
  instrumented suite required for V1 beyond existing project norms" is consistent
  with that. [verified]

## Required amendments

### A1. Start emission rule would speak on every resume (spec bug)

**Section:** "Skip and collide rules" — `Start only on a user start or play that
newly begins a run.`

**Failure:** Timer is paused, then the user taps Play. `TimerTransitions.play`
transitions Paused → Running, which "newly begins a run." Per the literal rule,
the rebuild emits Start, so the device speaks "Timer started" on every resume.
For a quiet-by-default convenience layer this is wrong.

**Replacement rule:** Start fires only on the initial `TimerListController.start`
path (and `duplicate`, which creates a new running timer). `play`, `+1:00`,
`refresh`, `undo`, boot/clock reconcile, and process reconcile never emit Start.
The "rebuild allowed to emit Start" flag is true only on the create-and-start
path; every other rebuild passes `emitStart = false`.

### A2. `mediaPlayback` is the wrong foreground service type for a TTS-only service

**Section:** "Voice cue service" — `short-lived foreground service, mediaPlayback
type`.

**Failure:** `TimerRingService` legitimately uses `mediaPlayback` because it
plays `MediaPlayer` alarm audio (USAGE_ALARM). A cue service that only runs
Android `TextToSpeech` is not playing media in the `mediaPlayback` sense. Google
Play policy and Android 14+ foreground-service-type enforcement may flag or
reject a TTS-only `mediaPlayback` service.

**Replacement rule:** Split the two actions. `SPEAK_ONCE` (Start / Halfway) is a
single short utterance; run it inside the receiver's `goAsync()` window (up to
~10 s) with a wake lock, no foreground service. `LAST_TEN` needs up to ~10 s of
speech across up to 10 utterances and does need a foreground service; use
`FOREGROUND_SERVICE_TYPE_SPECIAL_USE` (Android 14+) with a written justification
in the manifest, or document why `mediaPlayback` is acceptable for TTS in this
app. Pick one and name it in the spec.

### A3. Manifest registration and `START_NOT_STICKY` are unspecified

**Section:** "Architecture (hybrid)" / "Voice cue service."

**Failure:** The design names `TimerVoiceCueReceiver` and `TimerVoiceCueService`
but does not list their manifest entries or the service's restart policy. If
`LAST_TEN` uses `START_STICKY` (the `TimerRingService` default), the system will
restart the service after a kill and re-enter `onStartCommand` mid-countdown,
causing double-speak — the exact failure the design says to avoid.

**Replacement rule:** Add a manifest `<receiver>` for `TimerVoiceCueReceiver`
and a `<service>` for `TimerVoiceCueService` with the chosen
`foregroundServiceType`. The service returns `START_NOT_STICKY` for `LAST_TEN`
(and for `SPEAK_ONCE` if a service is used), so a system kill does not restart
mid-countdown. State this in the spec.

## Smaller actionability gaps

- **G1. Request-code framing.** The design says "Request codes and URIs must not
  collide." The existing timer intents all share request code 0 and distinguish
  by URI; the cue intents can do the same as long as the URI prefix differs.
  Reword to: "URIs must not collide with `lemurloop://timer/<id>`; request codes
  may reuse 0 or use a distinct range — match the existing pattern."
- **G2. Halfway boundary at exactly 20 s.** The rule "Halfway omitted when
  remaining < 20 s" leaves the 20 s case emitting both Halfway (at 10 s
  remaining) and EnterLastTen (at 10 s remaining) at the same epoch. The
  "do not speak overlapping lines" rule then drops one non-deterministically.
  Pin the threshold with `<=` or `<` and add a planner test for the 20 s case.
- **G3. `+1:00` halfway recomputation.** `+1:00` extends the duration while
  running, so the halfway epoch shifts. The design says "rebuild from current
  remaining time" — make explicit that halfway is recomputed against the new
  remaining duration, and that an already-fired Halfway is not replayed.
- **G4. Cross-timer `QUEUE_FLUSH`.** The design accepts `QUEUE_FLUSH` (newer
  utterance wins) for V1. If timer A is mid-last-10 countdown and timer B fires
  Start, B's utterance flushes A's current digit and A loses remaining digits.
  The design acknowledges missed digits only for service kills, not for
  cross-timer flush. Either accept it explicitly for cross-timer too, or use a
  per-timer TTS engine instance / utterance id so flush is scoped.
- **G5. Settings search item id and strings.** Name the new registry id (e.g.
  `timer_voice_cues`), the `SettingsSearchLabels` title/desc string keys, and
  confirm the target is `SubScreen("timer_settings")`. The
  `SettingsSubScreenRoutesTest` will then pass without changes.
- **G6. Hebrew parity test.** `HebrewResourceParityTest` requires `values-he`
  and `values-iw` to stay identical. The design says strings go in `values`,
  `values-he`, `values-iw` but does not mention keeping the two Hebrew variants
  byte-identical. Add one line.
- **G7. `TimerSettingsRepositoryImplTest` exists.** New DataStore keys will need
  read/write round-trip tests added there; the design's test list does not name
  this file.
- **G8. `TimerAlarmIntents` parser.** The cue receiver needs its own URI parser
  (`lemurloop://timer-cue/<id>/<kind>`). The design implies this but does not
  name where it lives; suggest a `TimerVoiceCueIntents` object mirroring
  `TimerAlarmIntents`, with a JVM test pinning the URI shape (the existing
  `TimerAlarmIntentsTest` is the template).

## What I would need to be wrong

- That `play` after `pause` is intended to re-announce Start. If the product
  intent really is "resume speaks 'Timer started'", then A1 is wrong. I judge
  this unlikely for a quiet-by-default convenience layer, but it is a product
  call, not a code fact.
- That `mediaPlayback` is acceptable for TTS in this app's Play profile. If the
  app already ships a TTS-only `mediaPlayback` service elsewhere and it passed
  review, A2 weakens. I did not find one in the manifest; `TimerRingService` and
  `AlarmService` both play real media. A2 stands unless such a precedent exists.
- That the existing timer intents actually rely on request-code uniqueness
  somewhere I did not read. I read `TimerAlarmIntents` and `AndroidTimerScheduler`
  end to end; request code is a const 0. G1 stands.

## Open questions for the human

1. Should `play` after `pause` announce Start? (A1) Default assumption: no.
2. Is `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` acceptable for the last-10 service,
   or does the app prefer to justify `mediaPlayback` for TTS? (A2) This affects
   manifest and targetSdk posture.
3. Should `SPEAK_ONCE` avoid a foreground service entirely (run inside
   `goAsync`), or always use a foreground service for parity with `LAST_TEN`?
   (A2) Affects receiver timeout risk on slow devices.

## Out of scope for this pass

No code, branches, or plans were created. Analysis only.
