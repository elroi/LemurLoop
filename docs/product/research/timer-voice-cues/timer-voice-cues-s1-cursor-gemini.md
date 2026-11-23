---
agent: cursor-gemini
model: gemini-4-carbon-high
stage: 1
task_id: timer-voice-cues
date: 2026-11-19
baseline_commit: 060ca6864b6dd16b3ccdf46ded9545520bde7464
independence: confirmed
inputs_consulted:
  - tmp/.multi-agent-convergence-protocol.md
  - /Users/elroi/.claude/skills/multi-agent-convergence/SKILL.md
  - /Users/elroi/.claude/skills/multi-agent-convergence/scripts/verify.sh
  - .cursor/skills/lemurloop-architecture/SKILL.md
  - .cursor/skills/lemurloop-ui-and-settings/SKILL.md
  - AGENTS.md
  - RULE.md
  - known_bugs.md
  - docs/agents/OPEN-THREADS.md
  - docs/agents/MANUAL-DEVICE-QA.md
  - docs/REFACTORING_PLAN.md
  - docs/RTL-LOCALIZATION.md
  - docs/HEBREW-TRANSLATION-GLOSSARY.md
  - docs/superpowers/specs/2026-11-12-timer-design.md
  - app/build.gradle.kts
  - app/src/main/AndroidManifest.xml
  - app/src/main/java/com/elroi/lemurloop/LemurLoopApp.kt
  - app/src/main/java/com/elroi/lemurloop/MainActivity.kt
  - app/src/main/java/com/elroi/lemurloop/di/AppModule.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/CountdownTimer.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerTransitions.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerSchedulePlan.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerListController.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerBootReconciler.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerAlarmIntents.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerRingSession.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerRingPorts.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerSettings.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerCreation.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerLayoutRules.kt
  - app/src/main/java/com/elroi/lemurloop/domain/scheduler/TimerScheduler.kt
  - app/src/main/java/com/elroi/lemurloop/domain/scheduler/AlarmScheduler.kt
  - app/src/main/java/com/elroi/lemurloop/domain/repository/TimerRepository.kt
  - app/src/main/java/com/elroi/lemurloop/domain/repository/TimerSettingsRepository.kt
  - app/src/main/java/com/elroi/lemurloop/domain/repository/AppDataRepository.kt
  - app/src/main/java/com/elroi/lemurloop/domain/manager/TextToSpeechManager.kt
  - app/src/main/java/com/elroi/lemurloop/domain/manager/SettingsSearch.kt
  - app/src/main/java/com/elroi/lemurloop/domain/manager/SettingsManager.kt
  - app/src/main/java/com/elroi/lemurloop/domain/dismissal/VoiceChallengeInteractor.kt
  - app/src/main/java/com/elroi/lemurloop/data/local/AppDatabase.kt
  - app/src/main/java/com/elroi/lemurloop/data/local/dao/TimerDao.kt
  - app/src/main/java/com/elroi/lemurloop/data/local/entity/TimerEntity.kt
  - app/src/main/java/com/elroi/lemurloop/data/local/GoogleCloudTtsEngine.kt
  - app/src/main/java/com/elroi/lemurloop/data/repository/TimerRepositoryImpl.kt
  - app/src/main/java/com/elroi/lemurloop/data/repository/TimerSettingsDataStore.kt
  - app/src/main/java/com/elroi/lemurloop/data/repository/TimerSettingsRepositoryImpl.kt
  - app/src/main/java/com/elroi/lemurloop/data/repository/TimerDataWiper.kt
  - app/src/main/java/com/elroi/lemurloop/data/repository/AppDataRepositoryImpl.kt
  - app/src/main/java/com/elroi/lemurloop/data/scheduler/AndroidTimerScheduler.kt
  - app/src/main/java/com/elroi/lemurloop/data/scheduler/AndroidAlarmScheduler.kt
  - app/src/main/java/com/elroi/lemurloop/data/timer/AndroidTimerSideEffects.kt
  - app/src/main/java/com/elroi/lemurloop/data/timer/AndroidTimerFinishedNotifier.kt
  - app/src/main/java/com/elroi/lemurloop/receiver/TimerFiredReceiver.kt
  - app/src/main/java/com/elroi/lemurloop/receiver/BootReceiver.kt
  - app/src/main/java/com/elroi/lemurloop/receiver/WakeupCheckReceiver.kt
  - app/src/main/java/com/elroi/lemurloop/receiver/AlarmReceiver.kt
  - app/src/main/java/com/elroi/lemurloop/service/TimerRingService.kt
  - app/src/main/java/com/elroi/lemurloop/service/AlarmService.kt
  - app/src/main/java/com/elroi/lemurloop/service/SleepTrackingService.kt
  - app/src/main/java/com/elroi/lemurloop/ui/MainScreen.kt
  - app/src/main/java/com/elroi/lemurloop/ui/navigation/Screen.kt
  - app/src/main/java/com/elroi/lemurloop/ui/navigation/LemurLoopNavGraph.kt
  - app/src/main/java/com/elroi/lemurloop/ui/activity/TimerRingActivity.kt
  - app/src/main/java/com/elroi/lemurloop/ui/viewmodel/TimerViewModel.kt
  - app/src/main/java/com/elroi/lemurloop/ui/viewmodel/TimerSettingsViewModel.kt
  - app/src/main/java/com/elroi/lemurloop/ui/viewmodel/SettingsViewModel.kt
  - app/src/main/java/com/elroi/lemurloop/ui/screen/timer/TimerScreen.kt
  - app/src/main/java/com/elroi/lemurloop/ui/screen/timer/TimerFocusScreen.kt
  - app/src/main/java/com/elroi/lemurloop/ui/screen/timer/TimerSettingsScreen.kt
  - app/src/main/java/com/elroi/lemurloop/ui/screen/timer/TimerKeypadScreen.kt
  - app/src/main/java/com/elroi/lemurloop/ui/screen/timer/TimerControls.kt
  - app/src/main/java/com/elroi/lemurloop/ui/screen/settings/SettingsSearchLabels.kt
  - app/src/main/java/com/elroi/lemurloop/ui/screen/settings/SettingsScreen.kt
  - app/src/main/res/values/strings.xml
  - app/src/main/res/values-iw/strings.xml
  - app/src/main/res/values-he/strings.xml
  - app/src/test/java/com/elroi/lemurloop/HebrewResourceParityTest.kt
  - app/src/test/java/com/elroi/lemurloop/ui/screen/settings/SettingsSubScreenRoutesTest.kt
  - app/src/test/java/com/elroi/lemurloop/domain/manager/SettingsSearchTest.kt
  - app/src/test/java/com/elroi/lemurloop/domain/timer/TimerAlarmIntentsTest.kt
  - app/src/test/java/com/elroi/lemurloop/domain/timer/TimerListControllerTest.kt
  - app/src/test/java/com/elroi/lemurloop/domain/timer/TimerSchedulePlanTest.kt
  - app/src/test/java/com/elroi/lemurloop/domain/timer/TimerBootReconcilerTest.kt
  - app/src/test/java/com/elroi/lemurloop/domain/timer/TimerRingSessionTest.kt
  - app/src/test/java/com/elroi/lemurloop/data/repository/TimerDataWiperTest.kt
  - app/src/test/java/com/elroi/lemurloop/data/repository/TimerSettingsRepositoryImplTest.kt
  - app/src/test/java/com/elroi/lemurloop/data/local/entity/TimerEntityTest.kt
---

# Stage 1 Review — Timer Voice Cues (`cursor-gemini`)

## Recommendation

**Adopt with specific amendments.** The core product locks and hybrid split — keeping `AlarmManager.setAlarmClock` exclusively on `AndroidTimerScheduler` for the zero-time ring while routing voice cues through a separate Uniform Resource Identifier (URI) scheme, non-alarm-clock wakeups, and a short-lived `mediaPlayback` Foreground Service (FGS) — preserve end-ring reliability, system "next alarm" display, and Do Not Disturb (DND) "until next alarm" semantics `[verified]`. However, implementing the design as written against `060ca6864b6dd16b3ccdf46ded9545520bde7464` produces six concrete defects: silent or persona-mutated Hebrew Text-to-Speech (TTS), infinite `Halfway` replays after pause/play or clock reconcile, dropped or phase-lagged `10…1` digits from cold screen-off TTS initialization and Central Processing Unit (CPU) autosleep, multi-timer `stopSelf()` and `ForegroundServiceStartNotAllowedException` / `RemoteServiceException` crashes, missed/spurious rebuilds across decoupled callers (`TimerSettingsViewModel`, `TimerRingService.captureAlarmVolume`, `duplicate`, `undo`), and schema/localization mismatches. Amend `What the user hears`, `Skip and collide rules`, `Architecture (hybrid)`, `Integration points`, `Settings and UI`, `Data`, `Multi-timer behavior`, `Failures`, and `Tests (JVM)` before writing the implementation plan.

---

## Verification of Design Claims Against Baseline (`060ca6864b6dd16b3ccdf46ded9545520bde7464`)

Every repository claim in the design was audited directly against source files:

| Design claim | Repository ground truth | Verdict |
|---|---|---|
| End timer uses `AndroidTimerScheduler` -> `TimerFiredReceiver` -> `TimerRingService` with `setAlarmClock` when exact alarms are allowed, falling back to `setAndAllowWhileIdle` | [AndroidTimerScheduler.kt](../../../../app/src/main/java/com/elroi/lemurloop/data/scheduler/AndroidTimerScheduler.kt) lines 23–45 and [TimerAlarmIntents.kt](../../../../app/src/main/java/com/elroi/lemurloop/domain/timer/TimerAlarmIntents.kt) lines 71–77 schedule `setAlarmClock` when `sdkInt < 31 \|\| canScheduleExactAlarms()`, else `setAndAllowWhileIdle`, targeting `TimerFiredReceiver` (`lemurloop://timer/<id>`), which starts `TimerRingService` | `[verified]` |
| `lemurloop://timer-cue/<timerId>/<kind>` will not collide with `lemurloop://timer/<id>` end-fire intents | `TimerAlarmIntents.DATA_PREFIX` is `"lemurloop://timer/"` (`TimerAlarmIntents.kt:19, 41–44`). `"lemurloop://timer-cue/..."` fails `startsWith("lemurloop://timer/")` and targets a distinct receiver component, so `Intent.filterEquals` cannot collide with end-ring `PendingIntent`s | `[verified]` |
| `AppDatabase` was version **27** (`2026-11-12-timer-design.md`'s "version 26" is stale); Room table is `timers` | [AppDatabase.kt](../../../../app/src/main/java/com/elroi/lemurloop/data/local/AppDatabase.kt) lines 180–185 has `@Database(..., version = 27, exportSchema = false)` with `MIGRATION_26_27` registered in [AppModule.kt](../../../../app/src/main/java/com/elroi/lemurloop/di/AppModule.kt) line 54; `TimerEntity` uses `tableName = "timers"`. Both [AGENTS.md](../../../../AGENTS.md) line 49 and [2026-11-12-timer-design.md](../../../superpowers/specs/2026-11-12-timer-design.md) line 71 still state 26 | `[verified]` |
| Timer settings live in `timer_settings` DataStore via `TimerSettings` / `TimerSettingsRepository` | [TimerSettingsDataStore.kt](../../../../app/src/main/java/com/elroi/lemurloop/data/repository/TimerSettingsDataStore.kt) line 9 defines `name = "timer_settings"`; [TimerSettings.kt](../../../../app/src/main/java/com/elroi/lemurloop/domain/timer/TimerSettings.kt) and [TimerSettingsRepositoryImpl.kt](../../../../app/src/main/java/com/elroi/lemurloop/data/repository/TimerSettingsRepositoryImpl.kt) store ring fields using `snake_case` preference keys (not `camelCase` as shown in the design's DataStore key column) | `[verified]` |
| Notification ID must be distinct from "timer ring and sleep" | Incomplete list: four IDs exist in live code — `SleepTrackingService` = `2` ([SleepTrackingService.kt](../../../../app/src/main/java/com/elroi/lemurloop/service/SleepTrackingService.kt) line 53), `AlarmService` = `111` ([AlarmService.kt](../../../../app/src/main/java/com/elroi/lemurloop/service/AlarmService.kt) line 874), `TimerFinishedNotification.ID` = `220` ([TimerRingPorts.kt](../../../../app/src/main/java/com/elroi/lemurloop/domain/timer/TimerRingPorts.kt) line 20), and `TimerRingNotification.ID` = `221` ([TimerRingSession.kt](../../../../app/src/main/java/com/elroi/lemurloop/domain/timer/TimerRingSession.kt) line 82) | `[verified]` |
| "Boot / `TIME_SET` / process reconcile re-arm future cues only" | **False for process start:** `TimerBootReconciler` is invoked **only** in [BootReceiver.kt](../../../../app/src/main/java/com/elroi/lemurloop/receiver/BootReceiver.kt) line 54 (`ACTION_BOOT_COMPLETED` and `ACTION_TIME_SET`). Neither [LemurLoopApp.kt](../../../../app/src/main/java/com/elroi/lemurloop/LemurLoopApp.kt), [MainActivity.kt](../../../../app/src/main/java/com/elroi/lemurloop/MainActivity.kt), nor [TimerViewModel.kt](../../../../app/src/main/java/com/elroi/lemurloop/ui/viewmodel/TimerViewModel.kt) runs timer reconciliation on process start | `[verified]` |
| "Rebuild / cancel hooks sit next to end-alarm arming in `TimerListController` / timer side-effects" | **Insufficient:** `TimerListController` is instantiated privately inside `TimerViewModel` (`TimerViewModel.kt:37–42`). `TimerSettingsViewModel`, `TimerRingService`, `TimerBootReconciler`, and `TimerDataWiper` never touch `TimerListController` | `[verified]` |
| On-device Android TTS with app-language English + Hebrew (`values`, `values-he`, `values-iw`) and no persona | **Existing `TtsEngine` violates both:** [TextToSpeechManager.kt](../../../../app/src/main/java/com/elroi/lemurloop/domain/manager/TextToSpeechManager.kt) hardcodes `Locale.US` / `Locale.ENGLISH` (lines 76, 218, 244), unconditionally applies morning `PersonaVoiceConfig` (lines 128, 151, 199–236), and is shut down process-wide by `AlarmService` (`AlarmService.kt:581, 855`) | `[verified]` |

### Compatibility with Locked Timer Product (`2026-11-12-timer-design.md`)

- **End-ring scheduling & DND / "next alarm":** Because `TimerVoiceCueScheduler` never calls `setAlarmClock` and uses `lemurloop://timer-cue/<timerId>/<kind>` on a separate receiver, cue wakeups cannot appear in `AlarmManager.getNextAlarmClock()`, cannot end a DND "until next alarm" rule, and cannot overwrite or cancel `lemurloop://timer/<id>` `PendingIntent`s `[verified]`.
- **Single ring session & `STREAM_ALARM` capture:** Provided `TimerVoiceCueService` never mutates `STREAM_ALARM` volume or writes `capturedAlarmVolume` in `TimerSettings`, `TimerRingService`'s volume capture/restore and ramp (`TimerRingService.kt:255–278`) remain unaffected `[verified]`. However, `TimerRingService` writing `capturedAlarmVolume` to `timer_settings` **will** break voice cues unless settings-change listeners ignore non-cue fields (see Finding 5).

---

## Concrete Findings and Required Amendments

### Finding 1 (Critical): Reusing `TextToSpeechManager` (`TtsEngine`) or un-wrapped `Service` resources breaks Hebrew speech, leaks morning personas, and races with `AlarmService`

- **Concrete failure (inputs/state -> wrong outcome):**
  1. *State:* App language is set to Hebrew (`"he"`, mapped to `"iw"` in [SettingsManager.kt](../../../../app/src/main/java/com/elroi/lemurloop/domain/manager/SettingsManager.kt) line 276) and morning persona is `"COACH"` (`pitch = 0.8f, speechRate = 1.4f`, `SettingsManager.kt:88–90, 194`). User enables Timer Voice Cues and starts a timer.
  2. *Wrong outcome A (if `TtsEngine` / `TextToSpeechManager` is injected):* `TextToSpeechManager` initializes with `Locale.US` (`TextToSpeechManager.kt:76`), filters strictly for `Locale.ENGLISH` voices (`line 244`), and falls back to `Locale.US` (`line 218`). Passing Hebrew UTF-8 text from `values-iw/strings.xml` to an English voice produces silence or garbled output `[verified]`. On English timers, `speak()` / `speakAndAwait()` unconditionally calls `applyPersonaVoiceConfig()` (`lines 128, 151`), speaking timer cues in the Drill Sergeant / Hypeman / Zen morning persona — violating the lock `"Voice: On-device Android TTS only (never Cloud TTS / persona)"` `[verified]`. Furthermore, because `TextToSpeechManager` is `@Singleton` (`AppModule.kt:211–214`), `AlarmService.onDestroy()` calling `ttsManager.shutdown()` (`AlarmService.kt:855`) kills in-flight timer cues, and `TimerVoiceCueService` stopping TTS aborts active morning alarm briefings or spoken math challenges (`VoiceChallengeInteractor`) `[verified]`.
  3. *Wrong outcome B (even with a dedicated `TextToSpeech` in `TimerVoiceCueService`):* When a user switches App Language in Settings (`en` -> `he` or `he` -> `system`, [SettingsScreen.kt](../../../../app/src/main/java/com/elroi/lemurloop/ui/screen/settings/SettingsScreen.kt) lines 1339–1360), `MainActivity` wraps its own `Activity` context (`MainActivity.kt:76–171`), and `AndroidManifest.xml` has no `AppLocalesMetadataHolderService` `[verified]`. A newly started `android.app.Service` receives an un-wrapped `ContextImpl` with the OS system locale, while `MainActivity.kt:118, 130` mutates `Locale.setDefault()` to `"iw"` and **never** resets it when switching back to `"system"` `[verified]`. Calling `service.getString(...)` or `Locale.getDefault()` speaks the wrong language or pairs Hebrew strings with an English TTS locale.
- **Design sections to amend:** `What the user hears`, `Voice cue service`, `Persona review`
- **Replacement rule:** `TimerVoiceCueService` must own a dedicated, service-scoped `android.speech.tts.TextToSpeech` wrapper (never `TtsEngine` / `TextToSpeechManager`) configured with neutral `pitch = 1.0f` and `speechRate = 1.0f`, never reading `SettingsManager.alarmDefaultsFlow`. Before resolving strings or speaking, read `SettingsManager.appLanguageFlow.first()`: resolve `"he"`/`"iw"` to `Locale("iw", "IL")` (with fallback to `Locale.forLanguageTag("he-IL")` if `setLanguage` returns `LANG_MISSING_DATA` or `LANG_NOT_SUPPORTED`), `"en"` to `Locale.US`, and `"system"` to `Resources.getSystem().configuration.locales[0]` (never `Locale.getDefault()`). Load all spoken strings through `createConfigurationContext(Configuration(resources.configuration).apply { setLocale(resolvedLocale) }).resources`.

---

### Finding 2 (Critical): Planner omits `originalDurationMillis`, collides at `20s`, and contradicts itself on short-timer `Start`

- **Concrete failure (inputs/state -> wrong outcome):**
  1. *Halfway replay:* `Skip and collide rules` specifies planner inputs as `(now, endsAtEpochMillis, remainingDurationAtPlanTime, effectiveFlags, allowStart)` — omitting `originalDurationMillis`. User starts a `10:00` (`600_000 ms`) timer, hears `"Halfway"` at `5:00` remaining, then pauses and resumes at `4:00` remaining (or changes a setting, or `BootReceiver` handles `ACTION_TIME_SET`). With `240_000 ms >= 20_000 ms` remaining and no `originalDurationMillis`, the planner computes `50%` of `4:00` and schedules `"Halfway"` again at `2:00` remaining (and again at `1:00` if paused again), violating both `"one midway at 50%"` and `"Past milestones are not replayed"`.
  2. *Exact 20-second collision + label overlap:* `"Halfway is omitted when remaining duration at plan time is under 20 seconds"` (`< 20_000 ms`) permits `Halfway` on a `20_000 ms` timer. At `duration = 20_000 ms`, `Halfway` (`now + 10_000 ms`) and `EnterLastTen` (`endsAt - 10_000 ms = now + 10_000 ms`) fire at the **exact same millisecond**. On a `21s–23s` timer with a 40-character label (`TimerLimits.MAX_LABEL_LENGTH = 40`, [CountdownTimer.kt](../../../../app/src/main/java/com/elroi/lemurloop/domain/timer/CountdownTimer.kt) line 21), `"{label}, halfway"` (~2–3s speech) is cut off mid-word by `EnterLastTen`'s `QUEUE_FLUSH`.
  3. *Internal contradiction on short `Start`:* `Tests (JVM)` states `"8s yields enter-last-10 only with shortened countdown"`, yet `Skip and collide rules` has **no rule** omitting `Start` on short timers. Following `Skip and collide rules` on an `8s–12s` timer emits both `Start` and `EnterLastTen`, causing `"Timer started"` / `"{label} started"` + cold TTS bind to collide directly with `"10, 9, 8…"`.
- **Design sections to amend:** `Skip and collide rules`, `Pure domain` (`TimerVoiceCuePlan`), `Tests (JVM)`
- **Replacement rule:**
  1. Include `originalDurationMillis: Long` in `TimerVoiceCuePlan` inputs. Compute `halfwayAt = endsAtEpochMillis - (originalDurationMillis / 2L)` (which also handles `+1:00` accurately because `TimerTransitions.addOneMinute` increments `originalDurationMillis` in [TimerTransitions.kt](../../../../app/src/main/java/com/elroi/lemurloop/domain/timer/TimerTransitions.kt) lines 66–71 `[verified]`).
  2. Emit `Halfway` only when `effective.halfway`, `halfwayAt > nowEpochMillis`, `originalDurationMillis >= 24_000L` (at least `2s` clearance before `endsAt - 10_000L`; strictly never `<= 20_000L`), and `halfwayAt <= endsAtEpochMillis - 12_000L`.
  3. Emit `Start` (at `nowEpochMillis`) only when `allowStart && effective.start` and either (`effective.lastTen && remainingMillis >= 14_000L`) or (`!effective.lastTen && remainingMillis >= 3_000L`). Thus an `8s` timer with all cues enabled emits `EnterLastTen` only (matching the JVM test), while an `8s` timer with `LastTen` disabled still speaks `Start`.

---

### Finding 3 (Critical): Cold screen-off TTS initialization, missing `PARTIAL_WAKE_LOCK`, and unguarded Doze latency break `Last-10` and leak late cues into the ring

- **Concrete failure (inputs/state -> wrong outcome):**
  1. *Cold TTS bind lag on `EnterLastTen`:* Because `SPEAK_ONCE` calls `stopSelf()`, `TimerVoiceCueService` is not running when `EnterLastTen` fires at `endsAt - 10_000L` on a 5-minute screen-off timer. Broadcast delivery + `startForegroundService` + cold binder initialization of `com.google.android.tts` (`onInit`) routinely takes `400–1,200 ms`. By the time `onInit` succeeds, `8.8s–9.4s` remain: either `"10"` is skipped on every normal timer (if wall-clock gated) or all ten digits shift ~1s late so `"1"` collides with `TimerRingService` at `00:00:00` (if sequentially looped).
  2. *Screen-off CPU autosleep mid-countdown:* `AlarmManager`'s `RTC_WAKEUP` broadcast wake lock is released as soon as `TimerVoiceCueReceiver.onReceive` returns. Unlike `TimerRingService` (which launches `TimerRingActivity` with `FLAG_TURN_SCREEN_ON | FLAG_KEEP_SCREEN_ON` and plays continuous looping audio, [TimerRingActivity.kt](../../../../app/src/main/java/com/elroi/lemurloop/ui/activity/TimerRingActivity.kt) lines 104–108 `[verified]`), `TimerVoiceCueService` runs screen-off and suspends in coroutine `delay()` calls between short ~300ms digits. A foreground service without a `PowerManager.PARTIAL_WAKE_LOCK` does **not** prevent kernel CPU suspend between utterances, stalling `LAST_TEN` until `setAlarmClock` fires at zero. (`android.permission.WAKE_LOCK` is already declared in [AndroidManifest.xml](../../../../app/src/main/AndroidManifest.xml) line 6 `[verified]`.)
  3. *Doze `AllowWhileIdle` 9-minute quota & late cue delivery:* Android's `DeviceIdleController` throttles `setExactAndAllowWhileIdle` to **one wakeup per ~9 minutes per app** in Doze. Routing `Start` through `AlarmManager` wastes that wakeup token at `t = 0`. Worse, if `Halfway` or `setAndAllowWhileIdle` fallback is delayed by Doze until `setAlarmClock` wakes the device at `endsAt`, `TimerVoiceCueReceiver` can deliver `"Halfway"` with `2s` left or `50 ms` past zero before `TimerRingService` writes `phase = Ringing`.
- **Design sections to amend:** `Android scheduling`, `Voice cue service`, `Failures`
- **Replacement rule:**
  1. Dispatch immediate user-triggered cues (`Start`, and immediate `LAST_TEN` when `remaining <= 11_500L` on interactive `start`/`play`/`duplicate`) directly to `TimerVoiceCueService` without `AlarmManager`, preserving the `AllowWhileIdle` quota for background wakeups. (`BootReceiver` / `TIME_SET` must still use `AlarmManager` only.)
  2. Schedule `EnterLastTen` with a `1_500 ms` warm-up lead (`endsAtEpochMillis - 11_500L` when `remaining > 11_500L`) so `TextToSpeech` warms up before `endsAtEpochMillis - 10_000L`. Drive `LAST_TEN` from wall-clock `endsAtEpochMillis`: for `n` in `10 downTo 1`, delay until `target = endsAtEpochMillis - n * 1_000L`, skip `n` if `now > target + 350L`, and exit before speaking if `now >= endsAtEpochMillis - 150L`.
  3. Acquire a timed `PowerManager.PARTIAL_WAKE_LOCK` in `TimerVoiceCueService` (`acquire(8_000L)` for `SPEAK_ONCE`, `acquire(15_000L)` for `LAST_TEN`), releasing it when idle and in `onDestroy()`.
  4. Load `CountdownTimer` from `TimerRepository` inside `TimerVoiceCueService` right before speaking (which also picks up `rename` edits automatically) and drop `SPEAK_ONCE` if `timer.phase != Running`, `remaining <= 0`, `now - scheduledEpoch > 5_000L`, or (`kind == Halfway && remaining <= 12_000L`). Document in `Failures` that Deep Doze's 9-minute `AllowWhileIdle` cap can skip mid-timer cues on stationary screen-off runs without battery-optimization exemption, and late cues drop cleanly rather than speaking stale progress.

---

### Finding 4 (Critical): Single-instance `TimerVoiceCueService` `stopSelf()`, `stopService()` race, and Android 12+ inexact-fallback FGS crash

- **Concrete failure (inputs/state -> wrong outcome):**
  1. *Multi-timer `stopSelf()` kills concurrent `LAST_TEN`:* Android instantiates at most one `TimerVoiceCueService`. Timer B enters `LAST_TEN` (`startId = 1`). Two seconds later, Timer A triggers `SPEAK_ONCE` (`Halfway`, `startId = 2`). One second later, `SPEAK_ONCE` finishes `"Halfway"` and calls `stopSelf()` (even `stopSelf(2)` stops the service because `2` is the latest `startId`), **killing Timer B's remaining 7 seconds of countdown**. Likewise, if Timer A rings and cancels cues via `context.stopService(...)` (copying `AndroidTimerRingStop` in [AndroidTimerSideEffects.kt](../../../../app/src/main/java/com/elroi/lemurloop/data/timer/AndroidTimerSideEffects.kt) lines 14–18 `[verified]`), it kills Timer B's cues, violating `"Ring for timer A cancels only A’s cues; B’s cues keep running"`.
  2. *`RemoteServiceException` crash on fast cancel:* `TimerVoiceCueReceiver` calls `context.startForegroundService(...)`; 20 ms later (before `onStartCommand` runs on the main thread), user taps `Pause` or `TimerRingService` calls `context.stopService(...)`. Stopping the service before `startForeground()` executes crashes the process with `ForegroundServiceDidNotStartInTimeException`.
  3. *`ForegroundServiceStartNotAllowedException` crash when exact alarms are denied:* `app/build.gradle.kts` sets `targetSdk = 35` (`line 59` `[verified]`). On Android 12 / 12L (Application Programming Interface (API) 31–32, where `USE_EXACT_ALARM` does not exist) when `canScheduleExactAlarms() == false`, the fallback `AlarmManager.setAndAllowWhileIdle` is an **inexact** alarm. Android 12+ grants the background FGS-start exemption only to *exact* alarms; calling `context.startForegroundService(...)` from an inexact `setAndAllowWhileIdle` broadcast throws `ForegroundServiceStartNotAllowedException` and crashes the app.
- **Design sections to amend:** `Android scheduling`, `Voice cue service`, `Multi-timer behavior`, `Failures`
- **Replacement rule:**
  1. `TimerVoiceCueService.onStartCommand` must call `promoteToForeground()` synchronously as its first statement on every invocation (including cancel intents), mirroring [TimerRingService.kt](../../../../app/src/main/java/com/elroi/lemurloop/service/TimerRingService.kt) line 88 `[verified]`.
  2. Maintain `activeJobs: MutableMap<Long, Job>` and `latestStartId: Int` in `TimerVoiceCueService`. Per-timer cancellation (`pause`, `delete`, `ringStart`, per-timer override edit) dispatches `ACTION_CANCEL_TIMER` (`EXTRA_TIMER_ID`) — never `context.stopService(...)` — cancelling only `activeJobs[timerId]` (calling `tts.stop()` only if `activeSpeakingTimerId == timerId`). `ACTION_STOP_ALL` (on `wipe`) cancels all entries. Call `stopForeground(STOP_FOREGROUND_REMOVE)` and `stopSelf(latestStartId)` **only** when `activeJobs` is empty (guarding `startService(ACTION_CANCEL_TIMER)` behind an atomic `TimerVoiceCueSession.isRunning()` flag just like `TimerRingSession` in `TimerRingPorts.kt:28–36` so cancelling when idle does not needlessly spin up the FGS!).
  3. In `TimerVoiceCueReceiver`, wrap `context.startForegroundService(...)` in `try / catch (Exception)` (specifically catching `ForegroundServiceStartNotAllowedException` on API 31+), logging and skipping the cue rather than crashing when exact alarms are denied.

---

### Finding 5 (High): Decoupled callers (`TimerSettingsViewModel`, `TimerRingService.captureAlarmVolume`, `duplicate`, `undo`) and missing process-start reconcile

- **Concrete failure (inputs/state -> wrong outcome):**
  1. *Settings screen decoupled from `TimerListController`:* `TimerListController` is a private field inside `TimerViewModel` ([TimerViewModel.kt](../../../../app/src/main/java/com/elroi/lemurloop/ui/viewmodel/TimerViewModel.kt) lines 37–42 `[verified]`). Navigating Alarms -> Settings -> Timer (`LemurLoopNavGraph.kt:193`) opens `TimerSettingsScreen` (`TimerSettingsViewModel`) without `TimerViewModel` in memory `[verified]`. Toggling Voice Cues ON or OFF there never rebuilds or cancels cues on running timers if hooks live only in `TimerListController`.
  2. *`capturedAlarmVolume` & volume slider spam:* `TimerRingService.captureAlarmVolume` (`TimerRingService.kt:258–261`) writes `capturedAlarmVolume` into `TimerSettings` (`timer_settings` DataStore) whenever Timer A starts ringing, and `TimerSettingsScreen.kt:101–117` writes `volumePercent` / `rampMinutes` continuously while dragging sliders `[verified]`. If "settings change -> cancel cue alarms and any last-10 service, then rebuild" reacts to every `TimerSettings` write, **Timer A ringing mutates `TimerSettings` and kills Timer B's `LAST_TEN` service**, and dragging the volume slider restarts `LAST_TEN` dozens of times per second.
  3. *`duplicate` and `undo` silently drop overrides/cues:* `TimerTransitions.duplicate` (`TimerTransitions.kt:98–105`) delegates to `start(id, label, originalDurationMillis, now)` instead of `source.copy(...)` `[verified]`, silently wiping the three per-timer override columns to `null` (Inherit). Both `TimerListController.duplicate` (`TimerListController.kt:65–73`) and `TimerListController.undo` (`lines 88–102`) schedule via `scheduler.schedule` directly rather than `edit()` `[verified]`.
  4. *Process reconcile absent:* Because `TimerBootReconciler` only runs in `BootReceiver` (`BootReceiver.kt:54` `[verified]`), process start never reconciles cues unless wired.
- **Design sections to amend:** `Integration points`, `Skip and collide rules`
- **Replacement rule:**
  1. Extract a Hilt-injected domain `TimerVoiceCueCoordinator` shared by `TimerListController` / `TimerViewModel`, `TimerSettingsViewModel`, `TimerRingService` (`onFire` cancels that `timerId`; `onDismiss` cancels all ringing IDs), `TimerBootReconciler` (re-arms future cues only for `plan.schedule` rows where `phase == Running && endsAt > now`; cancels cues for `plan.cancel`, `plan.ringSoon`, and `plan.finishQuietly`), and `TimerDataWiper`.
  2. Trigger settings-driven cue rebuilds **only** when `(voiceCuesEnabled, voiceCueStart, voiceCueHalfway, voiceCueLastTen)` changes (`distinctUntilChanged`), and when rebuilding a running timer already inside `LAST_TEN` whose `effective(LastTen)` stayed `true`, leave the running `LAST_TEN` job undisturbed.
  3. Update `TimerTransitions.duplicate` to copy `voiceCueStartOverride`, `voiceCueHalfwayOverride`, and `voiceCueLastTenOverride` from `source`; invoke the coordinator in `duplicate` (`allowStart = true`) and `undo` (`allowStart = false`, only when `snapshot.phase == Running && snapshot.endsAtEpochMillis > now`).
  4. Invoke `TimerBootReconciler.reconcile(System.currentTimeMillis())` once per process start (or explicitly remove "process reconcile" from the spec so prose does not claim unwired behavior).

---

### Finding 6 (Medium): Manifest wiring, per-kind `PendingIntent` cancel, notification channel/IDs, AudioFocus, Room SQLite affinity, Settings Search, and Hebrew copy

- **Concrete failures & replacement rules by section:**
  1. **Manifest & `PendingIntent` cancellation (`Android scheduling`):**
     - *Failure:* Missing `<receiver>` / `<service>` entries in `AndroidManifest.xml` silently drop explicit broadcasts/starts (`AGENTS.md:102`); cancelling only one generic URI fails `Intent.filterEquals` against `lemurloop://timer-cue/<timerId>/<kind>`.
     - *Rule:* Register `TimerVoiceCueReceiver` (`exported="false"`) and `TimerVoiceCueService` (`exported="false"`, `android:foregroundServiceType="mediaPlayback"`) in `AndroidManifest.xml`. Use a single broadcast action `ACTION_TIMER_VOICE_CUE` for all three `TimerVoiceCueKind`s so `cancelAll(timerId)` iterates `TimerVoiceCueKind.entries` (`Start`, `Halfway`, `EnterLastTen`), resolving each with `PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE` and cancelling both `AlarmManager` and the `PendingIntent`.
  2. **Notification ID & channel (`Voice cue service`):**
     - *Failure:* Colliding with `AlarmService` (`111`) or `TimerFinishedNotification` (`220`), or using a default-importance channel, plays a notification chime/vibration on every voice cue.
     - *Rule:* Assign `TimerVoiceCueNotification.ID = 222` (unit-tested `!= 2, 111, 220, 221`) on a `NotificationManager.IMPORTANCE_LOW` channel (`setSound(null, null)`, `enableVibration(false)`, `setSilent(true)`, `FOREGROUND_SERVICE_DEFERRED`).
  3. **AudioAttributes & AudioFocus (`Voice cue service` / `Failures`):**
     - *Failure:* Unspecified `TextToSpeech` audio attributes talk over music/podcasts without ducking (or pause podcasts permanently).
     - *Rule:* Set explicit `AudioAttributes` (`CONTENT_TYPE_SPEECH`, usage per Open Question 1) and request `AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` while speaking, abandoning focus when `activeJobs` empties or in `onDestroy()`.
  4. **Room SQLite column affinity & DataStore keys (`Data`):**
     - *Failure:* Writing `BOOLEAN` instead of `INTEGER` in Structured Query Language (SQL) `ALTER TABLE timers ADD COLUMN ...` crashes Room's runtime `TableInfo` check on upgrade (`exportSchema = false`, no `androidTest` suite in repo `[verified]`).
     - *Rule:* In `MIGRATION_27_28` (`AppDatabase` version `28`, registered in `AppModule.addMigrations`), add `voiceCueStartOverride INTEGER DEFAULT NULL`, `voiceCueHalfwayOverride INTEGER DEFAULT NULL`, `voiceCueLastTenOverride INTEGER DEFAULT NULL`. Use `snake_case` DataStore keys (`voice_cues_enabled`, `voice_cue_start`, `voice_cue_halfway`, `voice_cue_last_ten`) matching [TimerSettingsRepositoryImpl.kt](../../../../app/src/main/java/com/elroi/lemurloop/data/repository/TimerSettingsRepositoryImpl.kt) lines 71–79 `[verified]`.
  5. **Settings Search & Hebrew copy (`Settings and UI` / `What the user hears`):**
     - *Failure:* [SettingsSubScreenRoutesTest.kt](../../../../app/src/test/java/com/elroi/lemurloop/ui/screen/settings/SettingsSubScreenRoutesTest.kt) lines 24–28 asserts `SettingsSearchLabels.titleRes(id)` but **not** `descriptionRes(id)` `[verified]`, so omitting `descriptionRes` silently renders `"Search settings..."`. Passing `second.toString()` for `10…1` speaks masculine Hebrew numbers (`שניים, אחד`) instead of feminine (`שתיים, אחת` for `שניות`), and `"{label} started"` with a bare masculine verb (`התחיל`) clashes with feminine labels (`כביסה`, `ביצה`).
     - *Rule:* Wire both `titleRes` and `descriptionRes` in [SettingsSearchLabels.kt](../../../../app/src/main/java/com/elroi/lemurloop/ui/screen/settings/SettingsSearchLabels.kt) (plus a unit test asserting `descriptionRes != R.string.settings_search_hint`), keep `values-iw` and `values-he` byte-identical per [HebrewResourceParityTest.kt](../../../../app/src/test/java/com/elroi/lemurloop/HebrewResourceParityTest.kt), store spoken integers `10…1` in localized string resources (feminine Hebrew cardinals), and phrase labeled Hebrew `Start` gender-neutrally (e.g., `"הטיימר %1$s התחיל"` or `"%1$s — התחלה"` per [HEBREW-TRANSLATION-GLOSSARY.md](../../../HEBREW-TRANSLATION-GLOSSARY.md)).

---

## Reasoning

The hybrid architecture is the right structural choice for LemurLoop:

1. **Zero-time ring isolation holds:** Keeping `setAlarmClock` strictly on `AndroidTimerScheduler` preserves exact wakeup and DND/status-bar semantics for the actual ring while avoiding polluting the user's lock-screen next-alarm indicator with intermediate speech milestones `[verified]`.
2. **Short FGS for `Last-10` beats ten exact alarms:** Ten 1-second `AlarmManager` broadcasts would fail Doze/App-Standby coalescing and receiver startup jitter; a single `LAST_TEN` FGS session with a `1.5s` TTS warm-up lead, wall-clock tick anchoring, and a `15s` `PARTIAL_WAKE_LOCK` delivers crisp per-second speech without holding a full-duration FGS for multi-hour timers.
3. **Pure Java Virtual Machine (JVM) testability matches the timer package:** `domain/timer` already isolates scheduling/ring rules in pure Kotlin (`TimerSchedulePlan`, `TimerRingHandler`, `TimerAlarmIntents`) with fast fake-based JVM tests `[verified]`. Adding `TimerVoiceCueResolver` and `TimerVoiceCuePlan` (with `originalDurationMillis`) fits the exact architecture in [RULE.md](../../../../RULE.md).

---

## Risks and Unknowns (Including Risks to This Recommendation)

1. **Android Doze 9-minute `setExactAndAllowWhileIdle` ceiling (inherent risk of avoiding `setAlarmClock`):** On stationary screen-off devices already in Deep Doze — or when running back-to-back short timers while unplugged and stationary — Android's OS-wide ~9-minute cooldown between `AllowWhileIdle` alarms can still delay `Halfway` or `EnterLastTen` even after removing `Start` from `AlarmManager`. Our staleness guard prevents late/bogus speech over the ring, meaning the failure mode degrades cleanly to a skipped mid-timer cue rather than corrupted audio, unless the user grants battery-optimization exemption.
2. **Missing offline Hebrew TTS voice data on some Original Equipment Manufacturer (OEM) devices:** Even when `TimerVoiceCueService` sets `Locale("iw", "IL")` / `"he-IL"`, some phones lack downloaded Hebrew TTS voice packs (`LANG_MISSING_DATA`). The design's fail-soft rule (log and skip without affecting the timer ring) handles this safely, though users enabling Voice Cues in Hebrew on a device without Hebrew TTS data will receive no UI warning in V1.
3. **Pre-warm FGS visibility:** Starting `TimerVoiceCueService` `1.5s` before `10s` means the `IMPORTANCE_LOW` notification exists for up to `11.5s` instead of `10s` (mitigated on Android 12+ by `NotificationCompat.FOREGROUND_SERVICE_DEFERRED`).

---

## What Would Need to Be True for Me to Be Wrong

1. **Why "reject the hybrid architecture in favor of a full-run FGS while any timer has cues enabled" could beat this recommendation:** If real-device QA shows OEM battery managers (Samsung/Xiaomi/Pixel Light Doze) routinely throttle `EnterLastTen` on 3–8 minute screen-off kitchen/workout timers despite `Start` bypassing `AlarmManager`, then the non-`setAlarmClock` hybrid cannot reliably deliver screen-off cues without holding a `mediaPlayback` / `specialUse` FGS for the duration of cue-enabled timers. For V1, however, keeping battery/shade impact zero outside cue moments is the right default until device QA proves Doze throttling bites normal timer lengths.
2. **Why "adopt as written" would only hold if:** LemurLoop were English-only, never paused/resumed timers past 50%, never ran two timers concurrently, never mutated `capturedAlarmVolume` in `TimerSettings`, and had zero cold-TTS bind latency — every one of which is falsified by the live codebase `[verified]`.

---

## Open Questions for the Human

1. **Audio stream for voice cues (`USAGE_MEDIA` vs `USAGE_ALARM`):** Should `TimerVoiceCueService` speak on `USAGE_MEDIA` (`STREAM_MUSIC`, silent when media volume is muted, independent of Timer Settings volume) or `USAGE_ALARM` (`STREAM_ALARM`, audible when phone media is muted — matching user expectation when toggling Voice Cues directly underneath the Timer Volume slider — while still **not** capturing/overwriting `STREAM_ALARM` volume)?
2. **Lock per-timer override User Interface (UI) surface & `Duplicate` behavior:** The design leaves `"On the focus / detail surface (or row overflow)"` open. Should V1 lock per-timer `Inherit / On / Off` controls onto **`TimerFocusScreen` only** (recommended, avoiding cramming three 3-state controls into `TimerRowMenu`'s overflow dropdown), and should **Duplicate** copy the source timer's three voice-cue overrides (recommended) rather than resetting them to `Inherit`?
3. **Global Master `Off` vs explicit per-timer `On`:** The formula `override(cue) ?: (master && globalToggle(cue))` keeps a timer with `override = On` speaking even if the user later flips the Global Master switch `Off` in Timer Settings. Confirm that Global Master `Off` means *"default is Off for `Inherit` timers"* (keeping per-timer `On` active), rather than acting as a global mute across overridden timers too.
