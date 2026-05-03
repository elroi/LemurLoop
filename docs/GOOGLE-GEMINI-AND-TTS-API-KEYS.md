# Google Gemini and Cloud Text-to-Speech API keys

This document captures setup lessons for **LemurLoop**, which uses **two separate Google APIs** and stores **two separate keys** in app settings (see `SettingsManager`: `GEMINI_API_KEY` for Generative AI / Gemini, `CLOUD_TTS_API_KEY` for persona Cloud TTS).

---

## 1. Two products, two keys

| Capability | Google product | Typical console |
|------------|----------------|-----------------|
| Cloud AI (briefings, Gemini) | **Gemini API** (Generative Language) | [Google AI Studio](https://aistudio.google.com/) API keys and/or Google Cloud Console |
| Persona voice (Cloud TTS) | **Cloud Text-to-Speech API** | Google Cloud Console |

Do not assume one credential covers both. The app has **two fields** for a reason.

---

## 2. Gemini API key

1. Use **Google AI Studio** → **API keys** → **Create API key**, or Cloud Console → **APIs & Services** → **Credentials** → create an API key.
2. Restrict the key to **Gemini API** (or your chosen Generative Language scope) under **API restrictions** when you are ready to lock it down.
3. Paste this key into the app’s **Gemini / Cloud AI** configuration (wizard or settings as labeled).

---

## 3. Cloud Text-to-Speech (persona voice)

TTS is **not** enabled by toggling something on the key in AI Studio. You enable the **API on the Google Cloud project**, then use a key that is allowed to call that API.

1. In [Google Cloud Console](https://console.cloud.google.com/), select the **same project** you use for billing (e.g. “Default Gemini Project”).
2. **APIs & Services** → **Library** → search **Cloud Text-to-Speech API** → **Enable**.
3. Ensure **billing** is linked to that project if Cloud TTS calls fail with quota/billing errors (Cloud TTS is billed through Google Cloud).
4. **Credentials** → **Create credentials** → **API key** (recommended: a **dedicated** key for TTS only).
5. Edit the new key → **API restrictions** → **Restrict key** → select **only** **Cloud Text-to-Speech API**.
6. Paste this key into the app’s **Google Cloud Text-to-Speech** / persona voice field and use **Test API Key** if the UI offers it.

---

## 4. Why one restricted key cannot list both Gemini and TTS

If you try to add **Cloud Text-to-Speech API** while **Gemini API** is already selected for the same key, Google Cloud may show:

**“Cannot be combined with the currently selected API restrictions.”**

That is a **platform limitation**: you cannot combine those API restrictions on a single key in that picker.

**Practical approach:** keep **two keys**:

- **Key A** — restricted to **Gemini API** only (Generative AI).
- **Key B** — restricted to **Cloud Text-to-Speech API** only.

Using two keys matches how LemurLoop stores them separately and follows least privilege.

**Avoid (unless you accept risk):** setting **API restrictions** to **None** so one key can call any enabled API. That is weaker security and still may not match keys created only through certain flows.

---

## 5. “I see no restriction” — two different restriction types

Google Cloud separates:

| Type | Meaning |
|------|--------|
| **Application restrictions** | *Where* the key may be used (HTTP referrers, IP addresses, Android apps, etc.). **None** = not limited by source. |
| **API restrictions** | *Which* Google APIs the key may call. If only **Gemini API** is listed, **Cloud Text-to-Speech will fail** for that key until you use a different key with TTS allowed. |

So “no application restriction” does **not** mean the key can call TTS; check **API restrictions** and the **Selected APIs** list.

---

## 6. Security hygiene

- Treat keys like passwords; do not commit them to git or paste them into public issues/chats.
- Prefer **restricting each key** to the single API it needs.
- Optionally add **application restrictions** appropriate to how the app calls Google (e.g. Android app restriction for production keys once you know the package/signing certificate).

---

## 7. Optional: `local.properties` for debug installs (device)

The repo root **`local.properties`** file is already gitignored (Android convention). For **debug builds only**, Gradle reads two optional entries and bakes them into `BuildConfig`; on first launch, **LemurLoop** copies them into DataStore **only if** the corresponding saved key is still empty (it never overwrites keys you already set in Settings).

Add lines next to your existing `sdk.dir`:

```properties
GEMINI_API_KEY=your_gemini_key_here
CLOUD_TTS_API_KEY=your_cloud_tts_key_here
```

Then **Sync Project with Gradle Files** and **Run** a **debug** build to your device. After install, Cloud AI and Cloud TTS toggles are turned on automatically when a key was seeded from `local.properties`.

**If keys don’t refresh after editing `local.properties`:** Gradle sync (or **Clean Project**, then **Rebuild**). Seeding runs only when the saved setting is still empty—clear app storage or remove keys in Settings if you need to apply new values from `local.properties`.

**Release builds** leave these `BuildConfig` dev fields empty; they are not read from `local.properties`.

---

## 8. Reference (code)

- Key storage and flows: `app/src/main/java/com/elroi/lemurloop/domain/manager/SettingsManager.kt` (`GEMINI_API_KEY`, `CLOUD_TTS_API_KEY`, `saveGeminiApiKey`, `saveCloudTtsApiKey`).
- Debug seeding from `local.properties`: `app/build.gradle.kts` (`DEV_GEMINI_API_KEY`, `DEV_CLOUD_TTS_API_KEY`), `LemurLoopApp.seedDevApiKeysFromBuildConfig()`.
