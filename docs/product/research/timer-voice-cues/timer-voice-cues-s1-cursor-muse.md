---
agent: cursor-muse
model: muse-wonder-1.0-high
stage: 1
task_id: timer-voice-cues
date: 2026-11-19
baseline_commit: 060ca6864b6dd16b3ccdf46ded9545520bde7464
independence: confirmed
inputs_consulted:
  - tmp/.multi-agent-convergence-protocol.md
  - docs/superpowers/specs/2026-11-12-timer-design.md
  - app/src/main/java/com/elroi/lemurloop/domain/timer/CountdownTimer.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerListController.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerSchedulePlan.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerSettings.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerAlarmIntents.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerTransitions.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerBootReconciler.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerRingPorts.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerRingSession.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerCreation.kt
  - app/src/main/java/com/elroi/lemurloop/domain/scheduler/TimerScheduler.kt
  - app/src/main/java/com/elroi/lemurloop/data/scheduler/AndroidTimerScheduler.kt
  - app/src/main/java/com/elroi/lemurloop/receiver/TimerFiredReceiver.kt
  - app/src/main/java/com/elroi/lemurloop/receiver/BootReceiver.kt
  - app/src/main/java/com/elroi/lemurloop/service/TimerRingService.kt
  - app/src/main/java/com/elroi/lemurloop/data/timer/AndroidTimerSideEffects.kt
  - app/src/main/java/com/elroi/lemurloop/data/local/entity/TimerEntity.kt
  - app/src/main/java/com/elroi/lemurloop/data/local/AppDatabase.kt
  - app/src/main/java/com/elroi/lemurloop/data/repository/TimerSettingsRepositoryImpl.kt
  - app/src/main/java/com/elroi/lemurloop/data/repository/TimerSettingsDataStore.kt
  - app/src/main/java/com/elroi/lemurloop/data/repository/TimerRepositoryImpl.kt
  - app/src/main/java/com/elroi/lemurloop/data/repository/TimerDataWiper.kt
  - app/src/main/java/com/elroi/lemurloop/domain/repository/TimerSettingsRepository.kt
  - app/src/main/java/com/elroi/lemurloop/di/AppModule.kt
  - app/src/main/java/com/elroi/lemurloop/ui/viewmodel/TimerViewModel.kt
  - app/src/main/java/com/elroi/lemurloop/ui/viewmodel/TimerSettingsViewModel.kt
  - app/src/main/java/com/elroi/lemurloop/ui/screen/timer/TimerSettingsScreen.kt
  - app/src/main/java/com/elroi/lemurloop/ui/screen/timer/TimerScreen.kt
  - app/src/main/java/com/elroi/lemurloop/ui/screen/timer/TimerFocusScreen.kt
  - app/src/main/java/com/elroi/lemurloop/domain/manager/TextToSpeechManager.kt
  - app/src/main/java/com/elroi/lemurloop/domain/manager/SettingsSearch.kt
  - app/src/main/java/com/elroi/lemurloop/ui/screen/settings/SettingsSearchLabels.kt
  - app/src/main/java/com/elroi/lemurloop/LemurLoopApp.kt
  - app/src/main/AndroidManifest.xml
  - app/src/test/java/com/elroi/lemurloop/domain/timer/TimerAlarmIntentsTest.kt
  - app/src/test/java/com/elroi/lemurloop/domain/timer/TimerListControllerTest.kt
  - app/src/test/java/com/elroi/lemurloop/domain/timer/TimerBootReconcilerTest.kt
  - app/src/test/java/com/elroi/lemurloop/data/repository/TimerSettingsRepositoryImplTest.kt
  - app/src/test/java/com/elroi/lemurloop/data/local/entity/TimerEntityTest.kt
  - app/src/test/java/com/elroi/lemurloop/ui/screen/settings/SettingsSubScreenRoutesTest.kt
---

# Timer voice cues — Stage 1 pass (cursor-muse)

## Recommendation

**Adopt with specific amendments.** Do not implement the design as written.
The product locks, the hybrid shape, and the refusal to use `setAlarmClock`
for cues are right for this codebase. But four mechanisms in the design are
not yet implementable as specified: (1) "halfway" is undefined on any rebuild
after pause, resume, or +1:00, and the stated planner inputs cannot express
it; (2) the last-10 countdown is specified as a wall-clock delay loop rather
than anchored to the timer's end, so a late start speaks digits after the
ring has begun; (3) the design silently assumes the existing on-device
Text-to-Speech (TTS) manager can speak these cues, and it cannot — it forces
United States English and a persona voice and can wait forever for
initialisation; (4) the settings-change, override-change, undo, and ring-start
paths that must cancel or rebuild cues are not wired to any code that exists
today, so toggles would appear to work while stale alarms keep speaking.
Amendments A1–A10 below name the design section and the replacement rule for
each. With those folded in, this is buildable in one slice without touching
the morning-alarm pipeline.

## Reasoning

### What I verified in the design's favour

- The end-ring path is as the design describes [verified]: `AndroidTimerScheduler`
  arms one alarm per timer, `TimerFiredReceiver` starts `TimerRingService`,
  and schedule mode is `setAlarmClock` when exact alarms are allowed, else
  `setAndAllowWhileIdle` — see
  [AndroidTimerScheduler.kt](../../../../app/src/main/java/com/elroi/lemurloop/data/scheduler/AndroidTimerScheduler.kt)
  and the pure mode function in
  [TimerAlarmIntents.kt](../../../../app/src/main/java/com/elroi/lemurloop/domain/timer/TimerAlarmIntents.kt).
  A second, non-`setAlarmClock` path therefore cannot steal the system
  "next alarm" slot from the end alarm, which keeps the Do Not Disturb (DND)
  "until next alarm" behaviour owned by the ring, as the locked timer spec
  requires [verified: the spec states this trade-off explicitly in
  [2026-11-12-timer-design.md](../../../superpowers/specs/2026-11-12-timer-design.md)].
- The database version claim is right and the older spec's is stale [verified]:
  `@Database` says version 27 in
  [AppDatabase.kt](../../../../app/src/main/java/com/elroi/lemurloop/data/local/AppDatabase.kt),
  while the 2026-11-12 spec still says 26. The next migration is 27→28.
- Timer settings really do live in a DataStore named `timer_settings`, read
  and written wholesale by `TimerSettingsRepositoryImpl` [verified], so the
  four new keys belong there and need no new store.
- The settings-search route for Timer settings already exists end to end
  [verified]: registry item `timer_settings`, `settingsSubScreen` branch, and
  title/description label branches are all present, and
  `SettingsSubScreenRoutesTest` pins them.
- The manifest already declares the media-playback foreground service
  permission [verified], and `TimerRingService` already demonstrates the
  "call `startForeground` before reading any settings" pattern the locked
  spec mandates [verified].

### Findings, each with the concrete failure

**F1 — "Halfway" is undefined after the first rebuild, and the planner inputs
cannot fix that.** The design's planner takes "wall-clock now, timer end
epoch, remaining duration at plan time, effective flags". Input state: a
10-minute timer paused at 4 minutes elapsed (6 remaining), then resumed.
If halfway means 50% of the original duration, the fire time is
`endsAt − originalDuration/2`. If it means half of the remaining duration at
plan time — the only definition the stated inputs actually support — the cue
fires with 3 minutes left and announces "halfway" for a second time at a
point that is not halfway through anything. Worse: hear the true halfway cue,
then press +1:00. `TimerTransitions.addOneMinute` adds 60 s to both the
original duration and the remaining time [verified], so any recomputed
halfway (either definition) can move back into the future and speak again.
"Past milestones are not replayed" is therefore unimplementable from the
current timer row: `CountdownTimer` stores id, label, original duration,
end, paused remaining, phase, creation time — no run anchor and no
already-spoken state [verified in
[CountdownTimer.kt](../../../../app/src/main/java/com/elroi/lemurloop/domain/timer/CountdownTimer.kt)].

**F2 — Boundary collisions the skip rules miss.** Input state: a fresh
20-second timer. Halfway falls at exactly 10 seconds remaining, which is
exactly the EnterLastTen epoch. The design omits halfway only when remaining
is *under* 20 seconds, so both alarms fire at the same instant and two
service intents race to speak — the exact overlap the "skip colliding cues"
lock forbids. Second input: a fresh 8-second timer with Start and LastTen
both effective. EnterLastTen "fires immediately" and Start also fires at
start; the plan contains two milestones at the same epoch and no priority
rule. The design's own test list ("8s yields enter-last-10 only") quietly
assumes Start is suppressed there, but no section says so.

**F3 — Last-10 is not anchored to the end, so digits can speak over the ring.**
Input state: EnterLastTen alarm delivered 2 seconds late (exact-alarm
delivery and foreground-service start are not instantaneous). The design says
the service speaks "one second per utterance for the remaining count (≤ 10)"
counted from service start. It therefore begins at "10" with 8 seconds left
and speaks "2, 1" *after* the end alarm has fired and `TimerRingService` is
already playing — overlapping the ring the cues must never disturb. The
design also does not say digits are driven by utterance completion; a fixed
1-second cadence `QUEUE_FLUSH`es any digit that takes longer than a second
to pronounce and idles after any that takes less.

**F4 — The existing TTS manager cannot deliver the locked behaviour.**
Three verified facts from
[TextToSpeechManager.kt](../../../../app/src/main/java/com/elroi/lemurloop/domain/manager/TextToSpeechManager.kt):
(a) `onInit` sets `Locale.US` and every speak path reapplies a persona voice
configuration (pitch, speech rate, English-preferring voice selection), so a
Hebrew app language with a Hebrew label would still be spoken as US English
with the morning persona's pitch — contradicting both "locale follows the app
language" and "never persona". This is a silent fallback of exactly the kind
a green smoke test cannot catch. (b) `speak()` suspends on
`isInitialized.first { it }` with no timeout, and `isInitialized` is only set
on the success path; if TTS init fails or the language pack is missing, the
coroutine never returns, so a `SPEAK_ONCE` service that awaits it never
reaches `stopSelf` and a foreground service hangs instead of the design's
"log; skip that utterance". (c) `speak()` returns when the utterance is
*queued*, not finished, so the design's "speak the one line, then `stopSelf`"
either kills the service before the line is heard or relies on undocumented
same-process survival. Reusing this manager as-is is the single most likely
way this feature ships broken while every JVM test passes.

**F5 — Nothing that exists today would rebuild cues on settings or override
changes.** `TimerSettingsViewModel` only writes DataStore [verified];
`TimerListController` has no settings dependency and its `rename()` path
bypasses even the end-alarm scheduler, calling `repository.upsert` directly
[verified]. Input state: master off, a 30-minute timer running; user opens
Timer settings and turns the master and halfway on. Under the design's
integration note ("hooks sit next to end-alarm arming in
`TimerListController`") nothing re-arms that timer, so no halfway cue is ever
scheduled — the toggle appears broken. Reverse state: halfway cue armed, user
turns the halfway toggle off; the already-armed alarm still fires and speaks,
because no hook cancels it. The same gap applies to the new per-timer
override control (no controller method exists for it), to `undo()` (which
restores a running timer and re-arms its end via a separate branch the design
never mentions [verified]), and to `duplicate()` (a genuinely new run that
should emit Start, on a code path separate from `start()` [verified]).

**F6 — The receiver/service design has no stale-delivery or multi-timer
ownership rules.** The design's receiver "always starts" the service and the
service speaks. Input state: halfway alarm delivered, service starting, user
pauses the timer before TTS initialisation completes. Cancellation removes
*alarms*; it cannot retract an intent already delivered, and the design gives
the service no instruction to re-load the timer, check it is still Running,
re-resolve the effective flags, or check the milestone window before
speaking — so "Halfway" is announced for a paused timer. Multi-timer state:
timer A is in its last-10 countdown when timer B's halfway fires. Android
delivers both intents to one instance of `TimerVoiceCueService` [unverified
platform behaviour, but the standard service model]; the design defines
per-timer countdowns but only a bare `stopSelf`, so B's completion can tear
down A's countdown, and "ring for A cancels only A's cues" has no mechanism.
Ring start itself is also unhooked: `TimerRingHandler.onFire` cancels only
the end alarm through its ports [verified], including in the
already-ringing `sessionOpen` branch, so nothing in the ring path stops an
in-flight countdown for the timer that just fired.

**F7 — The exact-alarm instruction, read literally, crashes or misleads.**
`TimerVoiceCueScheduler` is told to use `setExactAndAllowWhileIdle` "(or the
same exact-alarm fallback family the end timer already uses when
`setAlarmClock` is unavailable)". Those are not the same thing [verified]:
when exact alarms are denied, the end timer falls back to plain
`setAndAllowWhileIdle`, precisely because the exact variant is not permitted.
Calling the exact variant unconditionally on Android 12+ without checking
`canScheduleExactAlarms()` throws `SecurityException` (named in the
[AndroidManifest.xml](../../../../app/src/main/AndroidManifest.xml)
permission model, which declares both exact-alarm permissions [verified]);
thrown inside a controller edit, that exception aborts the timer operation
itself — a cue failure breaking the end-ring guarantee. The failure table's
"best-effort `setAndAllowWhileIdle`" is the right behaviour, but the
architecture paragraph contradicts it and never names the permission check,
the catch, or the `RTC_WAKEUP` clock type the end fallback uses [verified in
`AndroidTimerScheduler`]. Separately, "request codes … must not collide with
end-fire intents" misstates the existing identity scheme [verified]:
`TimerAlarmIntents` uses one shared request code `0` for every timer and
both actions, and identity comes from action + component + data URI; a test
titled "request-code uniqueness vs end-fire" would either fail against a
faithful implementation or push implementers toward per-id hash codes the
locked design explicitly forbids.

**F8 — Migration shape is a trap the design walks past.** The design says
"three nullable `Boolean` columns", which is correct, but the most recent
migration in this file, `MIGRATION_26_27`, adds a boolean as
`INTEGER NOT NULL DEFAULT 0` [verified]. An implementer copying that nearest
pattern makes Inherit physically impossible for every existing timer row:
legacy timers would read as explicit Off, and enabling the master later
would produce silence that looks like a resolver bug. The design never warns
against this, and its test list has no migrated-row case.

**F9 — Audibility under DND and media mute is unspecified, not satisfied.**
The design says cues "do not own `STREAM_ALARM` prioritisation", which is
correct as far as the ring's volume capture goes [verified: the ring captures
and restores `STREAM_ALARM` once per session]. But no section names the TTS
audio attributes or stream. Default TTS output follows the media stream
[unverified against this codebase, since the manager sets no stream], so with
media muted or a DND mode that only exempts alarms, every cue is silently
absent while alarms still ring — "cues work screen-off / background" would
pass a daytime smoke test and fail the exact night-time scenario a timer
with voice cues is for. The design must state the expected behaviour, in
either direction, and pin it with a device check.

## Amendments (design section → replacement rule)

- **A1 — §Pure domain / §Skip and collide rules (planner).** Replace the
  planner input list with: the full `CountdownTimer`, wall-clock now,
  effective flags, an `allowStart` flag, and a persisted
  `voiceCueHalfwayConsumed` marker. Halfway epoch is defined as
  `endsAtEpochMillis − originalDurationMillis / 2` (50% of the original
  duration, which stays invariant across pause/resume because both the end
  and the remaining time shift together). The cue service sets the marker
  when the halfway utterance is queued; it is cleared on `start`,
  `duplicate`, and whenever a timer returns to Paused at its full original
  duration (refresh, ring dismiss), and never by pause, play, or +1:00.
  Add this as a fourth Room column in the same migration; planner tests must
  cover pause-at-6-remaining, +1:00-after-halfway, and refresh-then-play.
- **A2 — §Skip and collide rules (collisions).** Replace "omitted when
  remaining duration at plan time is under 20 seconds" with an epoch rule:
  omit Halfway whenever `halfwayEpoch >= enterLastTenEpoch` (this covers the
  20-second equality and every already-past halfway). Add a same-epoch
  priority, LastTen > Halfway > Start: at a fresh start with remaining
  ≤ 10 seconds, Start is suppressed and only the shortened countdown is
  planned. Boundary tests at 20 s, 11 s, 10 s, and 8 s are required.
- **A3 — §Voice cue service (LAST_TEN).** Replace "speak one second per
  utterance for the remaining count" with a wall-clock-anchored rule: on
  entry, load the timer and compute the first digit as
  `ceil((endsAt − now) / 1000)` clamped to 1…10; each later digit is due at
  its `endsAt − k·1000` boundary and is dropped if that boundary has passed;
  no digit is spoken at or after `endsAt`; digits are chained on utterance
  completion (with a timeout), not on a fixed delay. This makes a 2-second
  late entry start at "8" and finish before the ring instead of over it.
- **A4 — §What the user hears / §Failures (TTS path).** Add an explicit rule:
  cues must not call `TextToSpeechManager.speak` / `speakAndAwait` as they
  exist today. Provide a cue-specific speak function (new wrapper or a cue
  mode on the manager) that sets the TTS language from the app language
  (Hebrew locales included), applies no persona voice, pitch, or rate, uses a
  unique utterance id per timer/kind/digit, waits for that utterance's
  completion or error with a bounded timeout, and on init failure or missing
  language logs, skips, and lets the service stop. Name the audio attributes
  / stream in this section (see Open questions) and state the expected
  DND / media-mute behaviour in the failure table.
- **A5 — §Integration points.** Replace "rebuild / cancel hooks sit next to
  end-alarm arming" with a single cue-reconcile entry point (given a timer id,
  or all timers) invoked from: `TimerListController.start`, `duplicate`,
  `play`, `pause`, `addOneMinute`, `delete`, and `undo` (undo and play are
  reconcile-style, `allowStart = false`; only `start` and `duplicate` pass
  `allowStart = true`); `refresh` (cancel-only, for stale-service safety);
  `TimerSettingsViewModel` on any voice-cue field change (reconcile all
  running timers); a new controller method for per-timer overrides
  (persist, then reconcile that timer); `TimerRingHandler.onFire` in both
  branches and the ring-dismiss path via a new handler port, so ring start
  cancels that timer's cues and last-10 state; `TimerDataWiper.wipe`
  (cancel all cue alarms and stop cue state, beside the existing ring
  cleanup [verified location]); and `TimerBootReconciler`, which must cancel
  cues for Paused, Ringing, ring-soon, and finish-quietly timers and re-arm
  only future milestones for Running ones. Extend the controller tests'
  fake-scheduler pattern to assert cue calls per method, including `undo`.
- **A6 — §Android scheduling / §Voice cue service / §Multi-timer behavior.**
  Replace "receiver always starts the service; `stopSelf`" with: the receiver
  forwards timer id, cue kind, and planned epoch; on every start the service
  first promotes to foreground (before any DataStore read, matching
  `TimerRingService` [verified]), then re-loads the timer and settings,
  re-resolves effective flags, and drops the cue silently unless the timer
  exists, is Running, and the milestone is still in its valid window. The
  service keeps per-timer countdown state, ends a countdown only for its own
  timer (ring start, pause, delete, wipe, settings-off), uses
  start-id-aware shutdown only when no timer has active work, and a flushed
  foreign utterance never destroys another timer's remaining digits — that
  timer resumes at its next wall-clock boundary (A3 makes this natural).
- **A7 — §Android scheduling (exact alarms and intent identity).** Replace
  the scheduling sentence with: check `canScheduleExactAlarms()` on
  Android 12+; when allowed use `setExactAndAllowWhileIdle(RTC_WAKEUP, …)`;
  on denial or `SecurityException` fall back to
  `setAndAllowWhileIdle(RTC_WAKEUP, …)`; never `setAlarmClock`; cue
  scheduling can never throw into a timer operation. Replace the
  request-code sentence with a pure `TimerCueIntents` spec mirroring
  `TimerAlarmIntents`: distinct action, distinct component, data URIs
  `lemurloop://timer-cue/<timerId>/<kind>`, the same shared request code as
  the end intents, and a test asserting action/component/URI distinctness
  plus cancel-spec equality — not request-code uniqueness.
- **A8 — §Data (Room).** Name the concrete change: version 27→28,
  `MIGRATION_27_28` registered in `AppModule`, adding
  `voiceCueStartOverride`, `voiceCueHalfwayOverride`,
  `voiceCueLastTenOverride`, and (per A1) `voiceCueHalfwayConsumed` — the
  three overrides as bare `INTEGER` with **no** `NOT NULL` and **no**
  `DEFAULT`, explicitly unlike `MIGRATION_26_27`. Tests: entity round-trip
  preserving `null` overrides, and a migrated legacy row reading as Inherit.
- **A9 — §Settings and UI (search).** Replace "settings search registry:
  title + description for the block" with: update the existing
  `timer_settings` search item's description string (all three locales) to
  mention voice cues. Do not add a second registry item unless it also gets
  its own `SettingsSearchLabels` title/description branches — a registry row
  without them falls back to the generic Settings label and fails the
  existing every-item-has-its-own-title test [verified].
- **A10 — new §Manifest and notification (missing from the design).** Register
  `TimerVoiceCueReceiver` and `TimerVoiceCueService`
  (`exported=false`, `foregroundServiceType="mediaPlayback"`, reusing the
  already-declared permission [verified]), and give the cue notification a
  named, tested id distinct from 111 (alarm), 220 (timer finished), 221
  (timer ring), and 2 (sleep) [verified ids]. Also state that spoken digits
  are formatted integers pronounced by the TTS locale, not string resources,
  so the "copy from string resources" rule applies to Start/Halfway only.

## Risks and unknowns

- My A1 marker is a fourth persisted field and a small state machine
  (set on delivery; cleared on start/duplicate/full-duration pause). It adds
  migration and lifecycle surface the design deliberately avoided; if the
  human prefers, the cheaper alternative is defining halfway as half of the
  *remaining* segment at each rebuild — but that renames the product promise,
  since it can speak "halfway" more than once per run and never at the true
  midpoint after a pause. I judge that worse than the marker, but it is a
  product call, not an engineering fact.
- Whether an exact-alarm broadcast may always start a media-playback
  foreground service from the background on every supported Android version
  (minimum SDK 26, target SDK 35 [verified]) is platform behaviour I have
  marked [unverified]. `TimerFiredReceiver` already relies on the same shape
  for the ring [verified], which is encouraging, not proof — cue delivery is
  more frequent and less critical, so a platform refusal must degrade to
  silence, never to a receiver crash. This needs one screen-off device check,
  master on, in the minimum smoke.
- A3's anchored countdown still depends on TTS utterance latency on real
  hardware [unverified]. Dropping late digits bounds the damage, but a very
  slow engine could compress the last digits into an unintelligible run;
  the completion-chained design surfaces that on a device instead of hiding
  it in a delay loop.
- Re-resolving settings at fire time (A6) means a cue can be dropped by a
  settings read that races a legitimate delivery; I accept this because the
  failure direction (silence) matches the quiet-by-default lock, whereas the
  opposite direction (speaking after the user turned it off) does not.
- I did not verify Hebrew TTS voice availability on any device; everything
  about Hebrew cue quality is [unverified] until the device smoke runs with
  the app language set to Hebrew.

## What you would need to be wrong

The strongest argument against this pass is that F1, F2, and F6 are
specification pedantry: an implementer reading the design charitably would
define halfway from the original duration, drop stale cues at the service,
and never hit the 20-second boundary in a demo. If that charitable reading
were guaranteed, "adopt as written" would be defensible and my amendments
would be gold-plating. I do not believe the guarantee exists, for three
repo-grounded reasons: the design's own planner input list excludes the
original duration, so the charitable definition is not the written one; this
codebase's stated history (per its own contributor notes) is that
silently-mismatched cross-file contracts are its recurring bug class; and
the two nearest implementation patterns an implementer will copy — the
`NOT NULL DEFAULT 0` boolean migration (F8) and the persona/US-English TTS
manager (F4) — both produce plausible-looking, silently wrong behaviour.
If a reviewer can show that the implementation plan, not the design, is the
intended home for these definitions, then the correct verdict drops to
"adopt, provided the plan carries A1–A10" — which is the same work, one
document later. My position is that a design labelled "Locked" should carry
them itself.

## Open questions for the human

1. **Halfway semantics.** Is the midway cue promised at 50% of the timer's
   original duration (my A1, needs the persisted consumed marker), or is
   "half of whatever remains when (re)planned" acceptable product behaviour?
   The two answers differ by a Room column and a state machine.
2. **Short-timer priority.** For a timer of 10 seconds or less with Start
   and LastTen both on, I have assumed the countdown wins and Start is
   suppressed (A2). Confirm, or specify that Start speaks and the countdown
   starts late/truncated.
3. **Audibility contract.** Should cues be heard when media is muted or DND
   is on (alarm-adjacent audio attributes, with a stated DND interaction),
   or is media-stream behaviour acceptable — cues silent under mute/DND,
   documented as such (A4/A9 wording depends on this answer)?
4. **Fresh-run reset.** After a ring is dismissed or a paused timer is
   refreshed back to full duration, is the next play a fresh run for cue
   purposes (halfway marker cleared, per A1)? I assumed yes; if instead cues
   are once-per-timer-lifetime, the marker clearing rules simplify.
