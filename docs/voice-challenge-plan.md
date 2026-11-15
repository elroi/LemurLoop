# Voice Challenge — plan

**Slug:** claude-opus-4-8
**Status:** Revision 2 (vision frozen; Stage 1 planned). R2 folds in the nine accepted
findings from the cross-vendor review in
[voice-challenge-plan.review-codex.md](voice-challenge-plan.review-codex.md).
**Last updated:** 2026-11-15

A spoken, hands-free wake-up challenge. The app speaks a challenge aloud — greeting
you by name and asking the existing math problem — and you answer **by voice**, with
Automatic Speech Recognition (ASR, i.e. speech-to-text) checking your spoken answer.
This is the first step toward full voice-and-audio interaction at wake-up, reached in
stages. Each stage is useful on its own.

---

## The feature in plain language

Over time, waking up with LemurLoop becomes a *spoken conversation*, not a screen you
tap. The app talks, you talk back, and dismissing the alarm is something you do with
your voice. We get there gradually.

### Stage 1 — Speak your name + the math challenge *(build first)*

> The Smart Wakeup check appears (a few minutes after you dismissed the alarm). A voice
> says: *"Good morning, Alex. Let's make sure you're up — what's seven plus five?"* You
> say, *"Twelve."* It hears you: *"Great, you're awake."* The check clears. A wrong
> answer gets *"Not quite — what's seven plus five?"* and it waits. After **two** misses,
> or if it can't make out your voice, the **number pad appears** so you're never stuck.
> If you ignore it entirely, the check times out and **re-triggers the alarm** (today's
> behavior, unchanged).

Reuses what the app already has — the persona voice (Text-to-Speech, TTS) and the
`MathProblem` generator — and adds the one thing it doesn't: **listening** (ASR). The
name makes it personal from day one and reuses the existing `BRIEFING_USER_NAME`
setting.

### Stage 2 — Voice controls the moment *(later)*

Spoken intent beyond digits: *"snooze," "I'm up," "repeat that," "give me an easier
one."* The wake-up becomes responsive to what you say, not just the number.

### Stage 3 — Part of the whole interaction *(later)*

The voice challenge folds into the briefing and the Lemur's conversational style, so
greeting, briefing, challenge and dismissal are one continuous spoken exchange. Connects
to `BriefingGenerator` and Lemur chat.

### Through-line across every stage

- **Opt-in** — off by default; enabled per alarm ("Answer out loud").
- **Never locks you out** — speech failing always falls back to tapping/typing.
- **Room-aware** — share a bedroom? Leave it off; nothing makes you talk at 6am.
- **Bilingual UI** — English and Hebrew, like the rest of the app (see language scope).

---

## Decisions (R2)

- **Recognition backend — pilot mode, "whatever works."** Use Android's default
  `SpeechRecognizer`, requesting on-device recognition when available
  (`EXTRA_PREFER_OFFLINE` / `createOnDeviceSpeechRecognizer` on Application Programming
  Interface (API) 31+) and accepting server-backed recognition as the fallback.
  **Honest caveat:** server recognition sends wake-up audio to Google. Acceptable for a
  private pilot; **the backend choice and a privacy notice must be revisited before any
  public release.** Recorded as an open item, not settled for production.
- **Retry budget — 2 spoken attempts, then the number pad.**

## Language scope: English-first voice, bilingual UI, Hebrew voice later

The risky, uncertain part of Stage 1 is **parsing a spoken answer into a number**, which
is language-specific (English number words differ from Hebrew's gendered forms, and
recognition quality differs). Doing both languages at once doubles the parsing logic and
the test matrix before the approach is proven.

- **Voice interaction is English-only in Stage 1.** `SpeechRecognizer` is pinned to
  `en-US`; the spoken number-word parser is English-only.
- **UI strings stay fully bilingual.** Any new string goes in `values`, `values-iw` and
  `values-he` from the start — `HebrewResourceParityTest` requires `values-he` and
  `values-iw` to stay identical. English-only lives in the *behavior*, never in
  `strings.xml`.
- **Supported-language gate.** The voice challenge offers itself only when the app locale
  is English. A Hebrew user gets the existing typed math challenge (unchanged). That gate
  is the seam where a later "Stage 1.5 — Hebrew voice" increment plugs in.

---

## Where Stage 1 lands

The spoken math challenge is added to the **Smart Wakeup check**
(`WakeupCheckActivity`), which today is just a countdown and an "I'm awake" button.
Crucially, that check is scheduled via AlarmManager **~3 minutes after dismissal**
(`AlarmService.scheduleWakeupCheck`, `triggerAt = now + delay`), so **the alarm sound
and its foreground service are already stopped** when the check runs. That means a quiet
room and **no audio-focus contention** between the alarm player, TTS and ASR — the single
biggest reason this is the right insertion point. A correct answer **clears the check**;
it does not "stop the alarm" (there is none ringing). A timeout **re-triggers** the alarm,
exactly as today.

Putting voice at *dismissal* (`AlarmActivity`, alarm actively ringing) would reintroduce
the audio-focus problem, so it is explicitly deferred. The ASR layer is built so it can be
wired there later if we solve that.

---

## Stage 1 — step-by-step technical plan

Test-driven development (TDD) per `AGENTS.md`: a failing test before production code for
each unit with testable logic. The pure units (number-word parser, answer-match, error
classification) are unit-tested; the Android pieces (`SpeechRecognizer` wrapper, Composable
wiring) are verified on-device, since there is no `androidTest` suite.

1. **Spoken-number parser** *(first slice — pure Kotlin, no Android)* — new
   `domain/dismissal/SpokenNumberParser.kt`: English recognition output → `Int`
   ("twelve" → 12, "a hundred and five" → 105, bare digits "12" → 12, "for"/"to"
   homophones considered), `null` on unparseable input. **Unit-tested first.**

2. **Microphone permission — requested at opt-in, not at alarm time.**
   - Add `android.permission.RECORD_AUDIO` to `AndroidManifest.xml`.
   - Request it when the user enables "Answer out loud" in the **unlocked** creation/edit
     flow. At alarm time, if the permission is missing or revoked → **immediate typed
     fallback**, never a lockscreen permission dialog at 6am.

3. **ASR wrapper** — new `domain/dismissal/VoiceAnswerRecognizer.kt`, mirroring the sensor
   wrappers (`SmileDetector`, `ShakeDetector`): wraps `SpeechRecognizer`, pinned to
   `en-US`, prefers on-device per the R2 decision. Must run on the **main thread**, and be
   **cancelled/destroyed** across finish, timeout, rotation and process death. Exposes a
   result or a **typed error**:
   - *Immediate fallback:* permission denied, recognizer/language unavailable, service
     disconnected.
   - *Retryable (within the 2-attempt budget):* no-match, speech timeout.
   The number pad must also appear immediately if init hangs.

4. **Speak the prompt — completion-aware.** The existing
   `TextToSpeechManager.speak()` is `suspend` but only awaits *initialization*, then fires
   `tts.speak(..., QUEUE_FLUSH, ...)` and returns — it does **not** await `onDone`. Add a
   completion-aware path (await `UtteranceProgressListener.onDone` for a unique utterance
   id, plus a short acoustic-settling gap) so listening starts **after** the app stops
   talking. Build the spoken string from `BRIEFING_USER_NAME` (degrade gracefully if
   blank) + `MathProblem.question` read as words ("seven plus five", not "7 + 5").

5. **Challenge loop** — in `WakeupCheckActivity`: speak → listen → parse → compare to
   `MathProblem.answer`. Correct → clear the check (`finish()`). Wrong → re-prompt, up to
   **2 attempts**, then reveal the number-pad fallback (reuse the `AlarmActivity` math
   input pattern). Timeout → re-trigger the alarm (unchanged `onExpired`).

6. **Per-alarm opt-in option** — "Answer out loud" boolean. The full propagation chain
   (mismatched extra keys fail silently):
   - `domain/model/Alarm` — new field; `data/local/entity/AlarmEntity` — new column +
     **a new Room migration** (bump `AppDatabase` version, add `MIGRATION_x_y`, register in
     `AppModule`) **with a default for existing rows**; entity↔domain **mappers**.
   - Repository **save/update** copies; **clone/duplicate** path; **reboot/reschedule**
     reconstruction (`BootReceiver`).
   - `service/AlarmIntentExtras` — new `EXTRA_VOICE_CHALLENGE_ENABLED` constant.
   - `data/scheduler/AndroidAlarmScheduler` — put the extra.
   - `receiver/AlarmReceiver` — forward into the `AlarmService` intent.
   - `service/AlarmService` — **read the extra and hold it in a field**, then
     **re-`putExtra` it inside `scheduleWakeupCheck`** (that method builds a curated subset
     of extras — the new one must be added there).
   - `receiver/WakeupCheckReceiver` — **forward** it into the activity intent.
   - `ui/activity/WakeupCheckActivity` — read it.
   - Creation surfaces — **frozen list:** `AlarmCreationWizard` and `AlarmDetailScreen`
     expose the toggle; chat-proposal wiring is **out of Stage 1**. Each producer sets the
     field explicitly and gets a persistence test.
   - `SettingsSearch` registry — a `SettingSearchItem` entry if it surfaces in Settings.

7. **Strings** — all new user-facing strings in `values`, `values-iw`, `values-he`
   (parity test). Spoken prompt strings are English-only by design but still live in
   resources.

8. **Tests.**
   - `SpokenNumberParserTest` — words, digits, "and" forms, homophones, junk → null.
     (TDD anchor, slice 1.)
   - Error-classification unit — each `SpeechRecognizer` error code → immediate-fallback
     vs retryable.
   - Answer-match / retry-count logic as a pure unit where extractable.
   - Room **migration + default-for-existing-rows** test.
   - `AlarmServiceExtrasTest` — pin the new extra key constant.
   - **End-to-end option propagation** through schedule → receive → dismiss →
     `scheduleWakeupCheck` → `WakeupCheckReceiver` → activity (the chain in step 6).
   - Manual device checklist: permission grant / deny / permanent-deny **while locked**;
     correct answer; wrong answer within budget; unrecognized speech → number-pad
     fallback; on-device vs server path; process recreation / rotation; timeout
     re-trigger; Hebrew locale shows the typed challenge (gate works).

---

## Review notes (maintainer / product-UX / reliability / QA)

- **Maintainer:** follows the established wrapper pattern and the documented per-alarm
  option chain (now with the AlarmService re-put and WakeupCheckReceiver forward hops that
  R1 missed); the risky logic (parser, error classification) is isolated and pure.
- **Product/UX:** opt-in, a 2-try budget, and a guaranteed tap fallback keep it from ever
  trapping a sleepy user; the name reuses an existing setting rather than adding onboarding.
- **Reliability:** no audio-focus contention (check runs with the alarm already stopped);
  completion-aware speak prevents the app hearing itself; typed error taxonomy drives
  correct retry-vs-fallback behavior.
- **QA/testability:** no `androidTest` suite, so UI wiring is manual; the plan pushes logic
  into pure units and names an explicit device checklist plus an end-to-end propagation
  test.

---

## Risks & open questions

- **Recognition backend for production** — pilot accepts "whatever works" incl. server
  recognition; **must** be revisited with a privacy notice before public release.
- **ASR accuracy / ambient noise at wake-up** — mitigated by the 2-try budget + fallback.
- **Recognizer / on-device model availability** — varies by device/ROM/API level; treated
  as an immediate-fallback error.
- **Speak-then-listen timing** — needs device tuning even with `onDone` + settling gap.
- **Empty user name** — greeting degrades to "Good morning. What's…".
- **Play Store review** — `RECORD_AUDIO` needs a clear in-app rationale and privacy wording.

---

## Out of scope (Stage 1)

Hebrew voice; non-math spoken intents (snooze/repeat); briefing integration; wiring the
voice path into `AlarmActivity` dismissal; chat-proposal toggle; buddy/SMS and
sleep-tracking surfaces.
