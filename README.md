# LemurLoop: Android Alarm Clock Xtreme Reimagined

Welcome to **LemurLoop**, your accountability partner and AI-powered wake-up assistant.

## Features at a Glance

- **Gentle Wake**: Volume crescendo over 60 seconds with customizable vibration patterns (BASIC, PULSE, HEARTBEAT, STACCATO).
- **Alarm-Pal Briefing**: Wakes you up with a spoken briefing — weather, calendar events, and motivation — powered by Google Gemini (cloud) with automatic fallback to an on-device MediaPipe LLM.
- **Persona Voices**: Briefings are read aloud via Google Cloud TTS with selectable persona voices.
- **Accountability Buddy**: Texts a friend via SMS if you oversleep. Buddy opt-in uses a 4-digit confirmation code; alert delay is customizable (default: 5 minutes).
- **Smart Dismissal Challenges**: Solve a math problem (3 difficulty levels) or smile for the camera (ML Kit face detection) to dismiss.
- **Evasive Snooze**: After a set number of snoozes, the snooze button starts moving around the screen — faster with each extra snooze — so it's harder to hit.
- **Smart Wakeup Verification**: Multi-step check after dismissal to confirm you're truly awake.
- **Smart Alarm Suggestions**: Suggests alarms based on your calendar.
- **Sleep Tracking** *(planned, not yet implemented)*: Monitor restlessness via accelerometer throughout the night.
- **Bilingual (EN/HE)**: Full Hebrew localization with RTL layout support.
- **Diagnostic Logging**: Built-in debug trail stored in the local database for troubleshooting.

## How to Run

1. **Open in Android Studio**: Select `Open` and choose this folder.
2. **Sync Gradle**: Allow Android Studio to download dependencies.
3. **Run**: Press the green Play button.
4. **Permissions**: Grant requested permissions (Notification, Alarm, SMS, Calendar, Camera) on the first run.

## New computer

Git does not include machine-local files. After clone:

1. Android Studio Gradle JDK must be **17 or 21**, not the bundled JBR if it is Java 25 (this repo is Gradle 8.5). Prefer a Temurin 21 install; do not pick an IDE entry named `jbr-21` whose path is still `…/jbr/Contents/Home`.
2. Optional debug keys in gitignored `local.properties`: `GEMINI_API_KEY`, `CLOUD_TTS_API_KEY` — see [docs/GOOGLE-GEMINI-AND-TTS-API-KEYS.md](docs/GOOGLE-GEMINI-AND-TTS-API-KEYS.md). Or paste keys in **Settings → Intelligence → API Credentials**.
3. Optional Firebase: `app/src/google-services.json` with `package_name` **`com.elroi.lemurloop`** (not only the old `com.elroi.alarmpal` client). Analytics / Remote Config / on-device model download; not required for Gemini chat.

## Running Unit Tests

- **JDK requirement**: JDK 17+ is recommended (max **21** for Gradle 8.5).
- **Gradle JDK configuration**:
  - Do **not** pin `org.gradle.java.home` to Android Studio’s bundled JBR (it may be Java 25).
  - Use Android Studio’s Gradle JDK setting (Temurin 17/21) or `JAVA_HOME`.
  - CI uses JDK 17 and strips any `org.gradle.java.home` line (see `.github/workflows/unit-tests.yml`).
- **Run JVM unit tests**:
  - From the command line: `./gradlew :app:testDebugUnitTest`
  - From Android Studio: use the Gradle tool window or run tests from the gutter in `app/src/test/java`.
- **CI**: GitHub Actions runs the same unit tests on push/PR to `main` (see `.github/workflows/unit-tests.yml`); the workflow uses JDK 17 and does not rely on a local `org.gradle.java.home` path.

## Architecture

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material You / Material 3)
- **Architecture pattern**: MVVM + Clean Architecture (domain / data / ui layers)
- **DI**: Hilt
- **DB**: Room (SQLite, schema v21 with 16+ versioned migrations)
- **Preferences**: DataStore
- **Background work**: WorkManager (briefing pre-generation), foreground services (alarm playback, sleep tracking)
- **Camera / ML**: CameraX + ML Kit Face Detection (smile dismissal)
- **On-device LLM**: MediaPipe Tasks GenAI (Gemini fallback)
- **Cloud AI**: Google Generative AI SDK (Gemini), Google Cloud Text-to-Speech
- **Analytics**: Firebase Analytics + Remote Config
- **Async**: Kotlin Coroutines + Flow

## Google Cloud API keys (Gemini + TTS)

LemurLoop uses **separate** API keys for Gemini and for Cloud Text-to-Speech. Setup, project enablement, and why Google Cloud does not allow combining both APIs on one restricted key are documented in [docs/GOOGLE-GEMINI-AND-TTS-API-KEYS.md](docs/GOOGLE-GEMINI-AND-TTS-API-KEYS.md).
