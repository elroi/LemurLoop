# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

LemurLoop is a single-module Android alarm clock app (`:app`, package `com.elroi.lemurloop`) in Kotlin + Jetpack Compose, with Hilt, Room, DataStore and WorkManager. Full project rules live in [RULE.md](RULE.md) and [.cursor/rules/](.cursor/rules/); the essentials are summarized below.

## Commands

```bash
./gradlew :app:testDebugUnitTest                                   # all JVM unit tests (what CI runs)
./gradlew :app:testDebugUnitTest --tests "com.elroi.lemurloop.domain.chat.LemurChatParserTest"   # one class
./gradlew :app:testDebugUnitTest --tests "*LemurChatParserTest.parseModelOutput*"                # one method
./gradlew :app:assembleDebug                                       # debug APK
./gradlew :app:installDebug                                        # install on connected device/emulator
./gradlew :app:lintDebug                                           # Android lint
```

- **JDK:** Gradle 8.5 accepts Java 17–21 only. Never set `org.gradle.java.home` to Android Studio's bundled JBR (it may be Java 25, even when labeled `jbr-21` — trust `java -version`). CI (`.github/workflows/unit-tests.yml`) uses Temurin 17 and strips `org.gradle.java.home` from `gradle.properties`.
- Tests are JVM-only under `app/src/test`; there is no `androidTest` suite. Naming: `functionUnderTest_condition_expectedResult`. Prefer fakes over mocks.

## Local setup (gitignored, not in a fresh clone)

- `local.properties`: `GEMINI_API_KEY`, `CLOUD_TTS_API_KEY` — injected into `BuildConfig.DEV_*` for **debug builds only** (release stays empty). Keys can also be pasted in Settings → Intelligence → API Credentials.
- `app/src/google-services.json`: optional. The Google Services plugin is applied only when this file exists, so builds and CI work without it. Its `package_name` must be `com.elroi.lemurloop` (the legacy `com.elroi.alarmpal` client alone won't initialize). Gemini chat and Cloud Text-to-Speech (TTS) do not need Firebase.
- `.signing.properties` + `lemurloop.jks`: release signing.
- Gemini and Cloud TTS use **separate keys, often in separate Google Cloud projects**. A TTS 403 `BILLING_DISABLED` names the project number that actually received the call — compare it to the billed project. The "AI Voice ok" chip in Intelligence Health mirrors the AI Brain status and does not prove TTS works. See [docs/GOOGLE-GEMINI-AND-TTS-API-KEYS.md](docs/GOOGLE-GEMINI-AND-TTS-API-KEYS.md).

## Architecture

Layers under `app/src/main/java/com/elroi/lemurloop/`:

- `domain` defines models, repository interfaces (`domain/repository`, `domain/scheduler`) and business logic; `data` implements those interfaces (Room DAOs/entities, `AndroidAlarmScheduler`, `GoogleCloudTtsEngine`).
- `ui` ViewModels should depend on `domain` only. `service` / `receiver` depend on `domain`, never on `ui`. `di/AppModule.kt` is wiring only.
- Known exception: many `domain/manager/*` classes are Android-aware (`SettingsManager` owns DataStore directly; `GeminiManager`, `TextToSpeechManager`, `CalendarManager` use platform APIs). [docs/REFACTORING_PLAN.md](docs/REFACTORING_PLAN.md) tracks moving persistence into `data`; parts of that plan (the wipe and diagnostic-log repositories) are already done, so check the code before trusting the exceptions list in RULE.md.

### Alarm firing pipeline (spans several files)

1. `AndroidAlarmScheduler` sets the exact system alarm, packing every alarm option into Intent extras, and enqueues `BriefingWorker` (WorkManager) to pre-generate the spoken briefing ahead of time.
2. `receiver/AlarmReceiver` copies those extras into an intent for `service/AlarmService` (foreground service: sound, vibration crescendo, briefing TTS) and reschedules repeating alarms. `BootReceiver` restores alarms after reboot.
3. `AlarmService` launches `ui/activity/AlarmActivity` (math / smile-to-dismiss challenges, snooze). **Evasive snooze** is purely on-screen: once `snoozeCount > evasiveSnoozesBeforeMoving`, the snooze button drifts around the screen, 50 dp/s faster per extra snooze — it does not require physical movement. After dismissal it schedules `WakeupCheckReceiver` → `WakeupCheckActivity` (the Smart Wakeup check); `onDestroy` also schedules it so the check fires even when the briefing ends naturally.

**Adding a per-alarm option** touches all of: `domain/model/Alarm`, `data/local/entity/AlarmEntity` + a new Room migration, the extra constant in `service/AlarmIntentExtras`, `AndroidAlarmScheduler` (put), `AlarmReceiver` (forward — easy to miss), and `AlarmService`/`AlarmActivity` (read). Mismatched extra keys fail silently (see BUG-2 in [known_bugs.md](known_bugs.md)); `AlarmServiceExtrasTest` pins the key constants but does not check that every hop forwards them.

### Database

Room `AppDatabase` (currently version 24, `exportSchema = false`). Migrations are `MIGRATION_x_y` vals in `data/local/AppDatabase.kt`, registered in `di/AppModule.kt` `addMigrations(...)`. Bump the version, add the migration, and register it — all three.

### Briefing (spoken wake-up summary)

`domain/generator/BriefingGenerator` gathers weather + calendar and asks `GeminiManager` for the script, falling back to on-device `LocalLLMManager` (MediaPipe; model fetched by `ModelDownloader`) and to `BriefingScriptBuilder` templates. Playback goes through `TextToSpeechManager`, using `GoogleCloudTtsEngine` persona voices when enabled and a valid key exists, otherwise the Android on-device TTS.

### Lemur chat (conversational alarm creation)

`LemurChatViewModel` streams Gemini replies using prompts from `domain/chat/LemurChatPrompts`; `LemurChatParser` extracts visible text plus an `AlarmProposal` block from model output (with a repair turn on malformed output). The alarm draft lives in the singleton `domain/creation/AlarmCreationSessionStore`, shared across three creation entry points — Chat (`LemurChatScreen`), Wizard (`AlarmCreationWizard`) and Detailed (`AlarmDetailScreen`) — all reached from `NewAlarmBottomSheet`. Chat transcripts stay in the ViewModel. Design history: [.cursor/plans/](.cursor/plans/).

### Buddy accountability

`domain/manager/AccountabilityManager` sends SMS (opt-in via 4-digit code handled by `SmsOptInReceiver`). Lifecycle texts (alarm saved, schedule changed, dismissed, overslept) come from `domain/buddy` (`AlarmBuddyLifecycleNotifier`, `BuddySmsTemplates`, `BuddyAlarmMaterialChange`). SMS failures run off the main thread — post any UI feedback to the main looper.

### Feature status

- **Sleep tracking is not implemented yet.** `SleepTrackingService` and a placeholder start/stop `SleepTrackingScreen` exist, but the README's feature list overstates it.
- BUG-2, BUG-4, BUG-5 in [known_bugs.md](known_bugs.md) are fixed in code but still await manual device verification.

## UI conventions

- One public `XxxScreen` Composable per screen; state in a ViewModel. New screens: a single `StateFlow<UiState>` + events. `SettingsViewModel` and the alarm list still use multiple `StateFlow`s (acceptable until refactored).
- Routes are centralized in `ui/navigation/Screen.kt` + `LemurLoopNavGraph.kt`.
- `SettingsScreen`, `AlarmDetailScreen` and `AlarmCreationWizard` are very large files — read the relevant section rather than the whole file.
- **Settings search** uses a static registry in `domain/manager/SettingsSearch.kt`: a new searchable setting needs a matching `SettingSearchItem` entry there.
- Version info on About comes from `BuildConfig` (`VERSION_SUFFIX`, `BUILD_DATE`) / PackageManager — don't hardcode it.

## Localization (English / Hebrew, RTL)

Strings live in `res/values`, `res/values-iw` and `res/values-he` — add new strings to all three. The app switches locale per-application at runtime. Follow [docs/HEBREW-TRANSLATION-GLOSSARY.md](docs/HEBREW-TRANSLATION-GLOSSARY.md) for alarm-clock terminology and [docs/RTL-LOCALIZATION.md](docs/RTL-LOCALIZATION.md) for layout rules.

## Workflow (from .cursor/rules)

- Work on a `feature/*` or `fix/*` branch, never directly on `main`; changes land via PR.
- TDD by default: add or adjust a failing test before production code, and add a regression test for every bug fix. Say so explicitly when skipping (e.g. pure UI tweaks).
- When writing a plan, review it from the maintainer, product/UX, reliability and QA/testability perspectives and fold that feedback into the plan before presenting it.
- Dependency versions go in the version catalog (`gradle/libs.versions.toml`), not inline.
