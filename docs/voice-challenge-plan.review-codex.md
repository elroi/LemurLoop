# codex

Cross-vendor review of [voice-challenge-plan.md](voice-challenge-plan.md) by the OpenAI
Codex command-line interface (CLI), acting as an independent different-vendor reviewer
(a genuinely non-Claude lens, per the "independent review means a different vendor" rule).
Codex saw the plan text only, not the source; the **Validation against source** notes
below were added by `claude-opus-4-8` after checking each finding against the code.

**Date:** 2026-11-15 · **Reviews:** voice-challenge-plan.md (Stage 1)

The example greeting name was genericized before sending the plan to the external service.

---

## Findings (Codex), with source-validation verdicts

1. **Lifecycle insertion point / semantics.** The plan says a correct answer "stops the
   alarm," but `WakeupCheckActivity` is a post-dismissal re-check.
   **Verdict: VALID (plan wording bug).** Confirmed in source: the check is scheduled via
   AlarmManager ~3 minutes *after* dismissal (`AlarmService.scheduleWakeupCheck`,
   `triggerAt = now + delay`). At the check there is no alarm ringing — a correct answer
   should *clear the check*; a timeout *re-triggers* the alarm (existing `onExpired`).
   Fix the plan's wording.

2. **Alarm audio vs recognition not coordinated (audio focus).**
   **Verdict: DOES NOT APPLY at this insertion point.** Because the check fires ~3 min
   after dismissal, the alarm sound/foreground service is already stopped — no audio-focus
   war with Text-to-Speech (TTS)/Automatic Speech Recognition (ASR). This *confirms*
   `WakeupCheckActivity` as the right Stage-1 home. The concern is real only if voice is
   ever moved to *dismissal* (`AlarmActivity`) — which is exactly why that is deferred.

3. **`speak()` returning ≠ speech finished.**
   **Verdict: CONFIRMED against source.** `TextToSpeechManager.speak()` is `suspend` but
   only awaits *initialization*, then calls `tts.speak(..., QUEUE_FLUSH, ...)` fire-and-
   forget. Listening must wait for `UtteranceProgressListener.onDone` (plus a short
   acoustic-settling gap). Stage 1 needs a completion-aware speak before it can listen.

4. **Offline recognition / network / privacy not decided.** Plain `createSpeechRecognizer`
   may send audio to Google's servers; true on-device recognition exists only from
   Application Programming Interface (API) level 31 and may lack a downloaded `en-US` model.
   **Verdict: VALID — open decision.** Needs an explicit on-device-vs-server choice with a
   privacy stance, a capability/language check, and deterministic fallback. See open
   decisions below.

5. **`RECORD_AUDIO` requested at the worst moment.** Prompting on first use, from a
   full-screen alarm over the lockscreen, is time-critical and may be locked.
   **Verdict: VALID.** Move the permission request to opt-in time ("Answer out loud" in the
   unlocked creation/edit flow). At alarm time, missing permission → immediate typed
   fallback. The plan's "requests it on first use" should change.

6. **Propagation chain likely misses a hop.**
   **Verdict: VALID — confirmed sharper than the plan.** The option must flow
   AlarmReceiver → `AlarmService` (read the extra, hold it in a field) →
   `scheduleWakeupCheck` (re-`putExtra` into the curated subset it builds) →
   `WakeupCheckReceiver` (forward) → `WakeupCheckActivity` (read). The plan's generic
   "receiver forward / activity read" bullet omits the AlarmService read-and-re-put and the
   WakeupCheckReceiver forward. Also add entity↔domain mappers, repository save/update
   copies, clone/duplicate, reboot/reschedule reconstruction, and a default for existing
   Room rows.

7. **Creation coverage underspecified** ("chat proposal if applicable" is not frozen; every
   alarm constructor/edit/clone/default factory must set the field).
   **Verdict: VALID.** Freeze the exact surfaces and give each a persistence test.

8. **Recognizer lifecycle/threading missing.** `SpeechRecognizer` must run on the main
   thread and be cancelled/destroyed across finish, timeout, rotation and process death.
   **Verdict: VALID.**

9. **"Any failure → fallback" is too coarse.** Permission-denied / unsupported-language →
   immediate fallback; no-match / timeout → retryable; network / busy / audio /
   service-disconnected → distinct handling. Number pad must appear immediately if TTS or
   recognizer init hangs. **Verdict: VALID.**

10. **Test plan misses the highest-risk cases.** Room migration/defaults, end-to-end option
    propagation through schedule→receive→dismiss→re-check, process recreation, permission
    grant/deny/permanent-deny while locked, offline/no-model behavior, lifecycle
    cancellation, typed fallback per failure class, API-level/OEM spread.
    **Verdict: VALID.** Pure parser tests alone do not retire the primary risks.

---

## Net assessment (claude-opus-4-8)

One Codex finding (#2) does not apply and in fact reinforces the plan's insertion choice.
The other nine are accepted. The load-bearing ones for a revision: #3 (completion-aware
speak), #5 (permission moved to opt-in), #6 (full propagation chain), #4 (recognition
backend + privacy), and #10 (integration tests). None change the Stage-1 vision; they
change *how* Stage 1 is built.

## Open decisions for the user (not Claude's to pick)

- **On-device vs server speech recognition.** On-device (API 31+) keeps wake-up audio off
  Google's servers — better privacy, narrower device support. Server recognition is broader
  but sends audio out. This is a privacy-model choice, so it is yours to make.
- **Retry budget before the number pad appears** (e.g. 2 spoken attempts, then fallback).
