# Voice Challenge — plan

**Slug:** claude-opus-4-8
**Status:** Draft (vision frozen; Stage 1 planned)
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

> The alarm rings. A voice says: *"Good morning, Elroi. Let's wake up — what's seven
> plus five?"* You say, *"Twelve."* It hears you: *"Correct. Have a good day."* The
> alarm stops. A wrong answer gets *"Not quite — what's seven plus five?"* and it
> waits. After a couple of misses, or if it can't make out your voice, the **number
> pad appears** so you're never stuck.

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

## Language scope: English-first voice, bilingual UI, Hebrew voice later

The risky, uncertain part of Stage 1 is **parsing a spoken answer into a number**, which
is language-specific (English number words differ from Hebrew's gendered forms, and
recognition quality differs). Doing both languages at once doubles the parsing logic and
the test matrix before the approach is proven.

Decision:

- **Voice interaction is English-only in Stage 1.** `SpeechRecognizer` is pinned to
  `en-US`; the spoken number-word parser is English-only.
- **UI strings stay fully bilingual.** Any new string (e.g. the "Answer out loud"
  setting label) goes in `values`, `values-iw` and `values-he` from the start —
  `HebrewResourceParityTest` requires `values-he` and `values-iw` to stay identical.
  English-only lives in the *behavior*, never in `strings.xml`.
- **Supported-language gate.** The voice challenge offers itself only when the app
  locale is English. A Hebrew user gets the existing typed math challenge (unchanged) —
  never worse off, just not voice-enabled yet. That gate is the exact seam where a later
  "Stage 1.5 — Hebrew voice" increment plugs in (a second parser + recognizer locale).

---

## Where Stage 1 lands

The spoken math challenge replaces the current button-only **Smart Wakeup check**
(`WakeupCheckActivity`), which today is just a countdown and an "I'm awake" button —
the cleanest insertion point, and the challenge is *stronger* there than a single tap.
The existing typed math challenge in `AlarmActivity` (dismissal) is left as-is for now;
the ASR layer is built so it can also be wired there in a later increment.

---

## Stage 1 — step-by-step technical plan

Test-driven development (TDD) per `AGENTS.md`: a failing test before production code for
each unit that has testable logic (the number-word parser and the answer-match are the
prime candidates; the `SpeechRecognizer` wrapper and Composable wiring are verified
manually on device, since there is no `androidTest` suite).

1. **Microphone permission.**
   - Add `android.permission.RECORD_AUDIO` to `AndroidManifest.xml`.
   - Runtime request flow (the challenge requests it on first use; denial → fall back to
     the typed challenge, never a dead end).

2. **ASR wrapper** — new `domain/dismissal/VoiceAnswerRecognizer.kt`, mirroring the
   sensor wrappers (`SmileDetector`, `ShakeDetector`): wraps Android `SpeechRecognizer`,
   pinned to `en-US`, exposes a suspend/callback API that returns recognized text or a
   typed failure (no match / no permission / no speech / recognizer unavailable).

3. **Spoken-number parser** — new `domain/dismissal/SpokenNumberParser.kt`: maps English
   recognition output to an `Int` ("twelve" → 12, "a hundred and five" → 105, bare
   digits "12" → 12). Pure Kotlin, no Android → **unit-tested first**. Returns `null` on
   unparseable input so the caller can retry.

4. **Speak the prompt** — reuse `TextToSpeechManager.speak(text, personaId)`. Build the
   spoken string from `BRIEFING_USER_NAME` + the `MathProblem.question`, read as words
   ("seven plus five", not "7 + 5"). Serialize speak-then-listen so the recognizer does
   not capture the app's own voice.

5. **Challenge loop + fallback** — in `WakeupCheckActivity`: speak → listen → parse →
   compare to `MathProblem.answer`. Correct → `finish()`. Wrong → re-prompt. After N
   misses or any recognizer failure → reveal the number-pad fallback (reuse the
   `AlarmActivity` math input pattern). Timeout still re-triggers the alarm as today.

6. **Per-alarm opt-in option** — "Answer out loud" boolean. This touches the full
   per-alarm option chain (per `AGENTS.md`; mismatched extra keys fail silently):
   - `domain/model/Alarm` — new field.
   - `data/local/entity/AlarmEntity` — new column + **a new Room migration**
     (bump `AppDatabase` version, add `MIGRATION_x_y`, register in `AppModule`).
   - `service/AlarmIntentExtras` — new `EXTRA_VOICE_CHALLENGE_ENABLED` constant.
   - `data/scheduler/AndroidAlarmScheduler` — put the extra.
   - `receiver/AlarmReceiver` — **forward** the extra (easy to miss).
   - `WakeupCheckActivity` — read the extra.
   - Creation surfaces: `AlarmCreationWizard`, `AlarmDetailScreen` (and chat proposal if
     applicable) — expose the toggle.
   - `SettingsSearch` registry — a `SettingSearchItem` entry if it appears in Settings.

7. **Strings** — all new user-facing strings in `values`, `values-iw`, `values-he`
   (parity test). Spoken prompt strings are English-only by design but still live in
   resources.

8. **Tests.**
   - `SpokenNumberParserTest` — words, digits, "and" forms, junk → null. (TDD anchor.)
   - Answer-match logic (correct/incorrect/retry count) as a pure unit where extractable.
   - `AlarmServiceExtrasTest` — extend to pin the new extra key constant.
   - Manual device checklist: permission grant + denial, correct answer, wrong answer
     retry, unrecognized speech → number-pad fallback, timeout re-trigger, Hebrew locale
     shows the typed challenge (gate works).

---

## Review notes (maintainer / product-UX / reliability / QA)

- **Maintainer:** follows the established wrapper pattern and the documented per-alarm
  option chain; the risky logic (parser) is isolated and pure, so it is unit-testable and
  reviewable without a device.
- **Product/UX:** opt-in and a guaranteed tap fallback keep it from ever trapping a
  sleepy user; the name reuses an existing setting rather than adding onboarding.
- **Reliability:** ASR accuracy is the weak point — mitigated by retries, a visible
  fallback, and speak/listen serialization so the app doesn't hear itself.
- **QA/testability:** no `androidTest` suite, so UI wiring is manual-only; the plan pushes
  as much logic as possible into pure units and names an explicit device checklist.

---

## Risks & open questions

- **ASR accuracy / ambient noise at wake-up** — the core risk; fallback is the safety net.
- **Recognizer availability** — some devices/ROMs lack an on-device recognizer; treat as
  a failure → fall back.
- **Play Store review** — `RECORD_AUDIO` is a sensitive permission; needs a clear
  in-app rationale and privacy-notice wording.
- **"Speak-then-listen" timing** — must not capture the TTS output; needs device tuning.
- **Empty user name** — `BRIEFING_USER_NAME` may be blank; greeting degrades gracefully
  ("Good morning. What's…").

---

## Out of scope (Stage 1)

Hebrew voice; non-math spoken intents (snooze/repeat); briefing integration; wiring the
voice path into `AlarmActivity` dismissal; the buddy/SMS and sleep-tracking surfaces.
