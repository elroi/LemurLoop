---
agent: cursor
model: composer (orchestrator; not a Stage 1 contestant)
stage: 2
task_id: timer-voice-cues
date: 2026-11-19
baseline_commit: 060ca6864b6dd16b3ccdf46ded9545520bde7464
independence: confirmed
inputs_consulted:
  - docs/product/research/timer-voice-cues/timer-voice-cues-s1-cursor-claude.md
  - docs/product/research/timer-voice-cues/timer-voice-cues-s1-cursor-gemini.md
  - docs/product/research/timer-voice-cues/timer-voice-cues-s1-cursor-glm.md
  - docs/product/research/timer-voice-cues/timer-voice-cues-s1-cursor-muse.md
  - tmp/task-timer-voice-cues.md
  - docs/superpowers/specs/2026-11-19-timer-voice-cues-design.md
  - docs/superpowers/specs/2026-11-12-timer-design.md
  - app/src/main/java/com/elroi/lemurloop/domain/manager/TextToSpeechManager.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerListController.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerTransitions.kt
  - app/src/main/java/com/elroi/lemurloop/data/scheduler/AndroidTimerScheduler.kt
  - app/src/main/java/com/elroi/lemurloop/domain/timer/TimerAlarmIntents.kt
  - app/src/main/java/com/elroi/lemurloop/data/local/AppDatabase.kt
  - app/src/main/AndroidManifest.xml
---

# Stage 2 meta-analysis — timer-voice-cues

Scorecard weighting was locked before reading passes: unique risk ×3, correctness ×2, completeness ×2, actionability ×1, artifact quality ×1.

I authored the design under review in the brainstorm chat, but I did **not** write a Stage 1 pass. No "Where the other passes beat mine" section for a contestant pass.

## 1. Provenance

| File | Model | Independence | Notes |
|---|---|---|---|
| s1-cursor-claude | claude-sonnet-5-5-high | treat as **confirmed** | `verify.sh` flagged COMPROMISED because of an inline YAML comment after `independence: confirmed`. Derivation check: no peer citations. Distinct hash. |
| s1-cursor-gemini | gemini-4-carbon-high | confirmed | Distinct hash |
| s1-cursor-glm | glm-5.2-high | confirmed | Distinct hash |
| s1-cursor-muse | muse-wonder-1.0-high | confirmed | Distinct hash |

**4 files, 4 independent voices.** No byte-identical twins. No peer-pass citations. Claude listed the research folder name only and did not open peer bodies — do not exclude from convergence tallies.

## 2. Convergence (settled unless a gate overturns)

All four recommend **adopt with specific amendments**. None recommend reject or adopt-as-written.

Verified shared ground:

| Claim | Verdict |
|---|---|
| Cue path must not use `setAlarmClock` (preserve next-alarm / DND for end ring) | **CONFIRMED** — `AndroidTimerScheduler` + `timerAlarmScheduleMode` |
| Live Room `@Database` is version **27**; next migration 27→28 | **CONFIRMED** |
| End URI `lemurloop://timer/` vs cue `lemurloop://timer-cue/` does not collide under current prefix parse | **CONFIRMED** |
| Existing end-alarm fallback when exact denied is `setAndAllowWhileIdle` (inexact), not an exact twin of `setExactAndAllowWhileIdle` | **CONFIRMED** — design wording was loose |
| `TextToSpeechManager` hard-codes `Locale.US` and applies persona voice config | **CONFIRMED** — cannot be the cue engine as-is |
| `play` is a first-class controller path (`TimerListController.play` → `TimerTransitions.play`) | **CONFIRMED** — “Start on every play that begins a run” as written would re-announce on resume |
| `+1:00` raises `originalDurationMillis` and moves end | **CONFIRMED** — naive replan of halfway from new original can schedule a second Halfway |
| Cue cancel/rebuild cannot live only inside `TimerListController` | **CONFIRMED** — ring cancel, wiper, boot reconciler also arm/cancel end alarms |

Shared product judgment (not every claim re-proved in this meta pass, but unanimous and consistent with code):

- Hybrid shape (few cue alarms + short FGS for last-10) is the right architecture family.
- Planner must take `originalDurationMillis` (or equivalent) and an `allowStart` / eligibility story; “remaining under 20s” alone is insufficient.
- Last-10 must be **anchored to end epoch** (skip late digits), not a blind relative delay loop; needs a wake strategy while speaking screen-off.
- Multi-timer + one service: naive `stopSelf()` / `stopService()` violates “ring A cancels only A.”
- Fire-time validation (still Running, end unchanged, not absurdly late) is required.
- Dedicated on-device TTS for cues (not shared morning `TtsEngine`), locale-aware for Hebrew.

Treat the above as the default amendment set for the winning plan.

## 3. Divergence (decision-relevant)

### D1 — How to prevent Halfway replay (high)

- **Claude:** `halfwayEligible` computed at `+1:00` from old end/original; pause/play naturally safe if halfway = end − original/2.
- **Gemini:** also flags pause/play / TIME_SET if planner uses “half of remaining”; wants original duration + stricter clearance before last-10.
- **Muse:** persisted “halfway already spoken” marker (Room column).

**Survives verification:** Claude’s `+1:00` concrete failure is **CONFIRMED** against `TimerTransitions.addOneMinute`. Gemini’s “half of remaining” failure applies only if the planner is wrongly specified that way — the design said 50% midway but listed inputs without `originalDurationMillis`, so both are fixing the same underspecification. Muse’s persisted marker works but is heavier than needed if halfway is always `end - original/2` plus eligibility.

**Meta pick (default):** Claude A1 + Gemini clearance rules; no new Room column unless a gate demands once-per-lifetime semantics.

### D2 — Start on short timers / Start vs LastTen collision (high)

All note the design’s test line (“8s → enter-last-10 only”) contradicts missing Start-suppression rules. Claude/Gemini/Muse propose suppressing Start when remaining is too short for speech budget + countdown.

**Open gate for human** (see below). Default if unanswered: LastTen wins; Start suppressed when remaining &lt; ~14s with LastTen on.

### D3 — Foreground service type (medium)

- **GLM:** `mediaPlayback` wrong for TTS-only; prefer `SPECIAL_USE` or split SPEAK_ONCE out of FGS.
- **Claude:** reusing `mediaPlayback` is consistent with ring; optional `shortService` note, not a blocker.
- **Gemini/Muse:** assume `mediaPlayback` with other lifecycle fixes.

**Meta pick (default):** Keep `mediaPlayback` for V1 consistency with `TimerRingService` **unless** Play Console / device policy forces otherwise; document the justification. Do not block the plan on SPECIAL_USE.

### D4 — Start delivery path (medium)

Claude/Gemini: Start is a foreground user action — speak via direct FGS start from the app; reserve AlarmManager quota for Halfway / EnterLastTen.

**Meta pick (default):** adopt — reduces Doze quota burn and Start latency.

### D5 — Integration seam shape (medium)

Claude prefers decorating the scheduler seam or a single coordinator; Gemini emphasizes settings/ring/duplicate/undo listeners; Muse stresses settings-change and ring-start wiring gaps.

**Meta pick:** one `TimerVoiceCueCoordinator` (or equivalent) called from every arm/cancel/settings/override site — fold Claude A6 + Gemini Finding 5 + Muse wiring list. Do not scatter hooks only in `TimerListController`.

## 4. What each pass uniquely caught

| Pass | Unique / strongest contribution |
|---|---|
| **claude** | Best concrete `+1:00` Halfway double-fire; speech-budget thresholds; last-10 relative-delay failure; multi-timer `stopSelf`; fire-time validation; reconciler must not `startForegroundService` directly; clearest amendment table |
| **gemini** | Deepest TTS/`TtsEngine`/persona/`AlarmService.shutdown` blast radius; notification id inventory (2/111/220/221); settings-write false rebuild; Doze allow-while-idle quota; warm-up lead before last-10; Hebrew `createConfigurationContext` rule |
| **glm** | Sharpest one-line Start-on-resume defect; FGS type challenge; `START_NOT_STICKY` / manifest gaps; notes request-code “collision” framing is mostly moot |
| **muse** | Halfway undefined without original duration after pause; TTS manager init hang risk; audibility/mute/DND open question; fresh-run after dismiss/refresh |

## 5. Artifact defects

- Claude frontmatter: trailing comment on `independence` broke `verify.sh` parsing — fix in future seats (value alone on the line).
- Design under review (not a pass): DataStore key names shown as camelCase; live timer settings use snake_case preference keys (**CONFIRMED** by Gemini) — plan must follow repository pattern.
- Relative links in muse/gemini point at `../../../../app/...` from research dir — resolve if opened from that folder; acceptable for Stage 1.

## 6. Scorecard (shape, not a single total)

Scale per axis: 1–5.

| Axis (weight) | claude | gemini | glm | muse |
|---|---|---|---|---|
| Unique risk (×3) | 5 | 5 | 3 | 4 |
| Correctness (×2) | 5 | 5 | 4 | 4 |
| Completeness (×2) | 5 | 5 | 3 | 4 |
| Actionability (×1) | 5 | 4 | 4 | 4 |
| Artifact quality (×1) | 4 | 5 | 4 | 4 |

Claude and Gemini are co-leads on unique risk and correctness. GLM is thinner but punches above weight on Start-on-resume and FGS-type. Muse strengthens TTS and halfway-definition clarity.

## 7. Recommendation

**Merge with attribution** (not a single-winner exclusive plan):

1. **Base narrative / amendment spine:** `cursor-claude` (A1–A9 table).
2. **Fold in from `cursor-gemini`:** dedicated cue TTS + locale; notification id list; warm-up lead + end-anchored last-10; Doze/quota notes; settings-field filter so non-cue DataStore writes do not rebuild; coordinator coverage of settings/ring/duplicate/undo.
3. **Fold in from `cursor-glm`:** Start emits only on create-and-start / duplicate (not pause→play); manifest + `START_NOT_STICKY`; drop overstated request-code collision tests.
4. **Fold in from `cursor-muse`:** halfway always from `originalDurationMillis`; audibility contract section; fresh-run after dismiss/refresh clears eligibility.

**Winner to write Stage 4 plan after human gates:** treat **cursor-claude** as the named winner for plan authorship, with the merge map above mandatory. (Orchestrator may write the plan if the user asks this session to continue — still apply the merge map, do not re-author from the unamended design.)

**Do not implement** until Stage 3 gates below are answered.

## 8. Stage 3 gates (human) — one question each

Defaults in parentheses are the meta recommendation if you want a one-shot “take the defaults” reply.

1. **Master vs per-timer On:** Is global master a hard kill switch even when a timer override is On? (**Yes — master kills all.**)
2. **Resume:** After pause→play, announce Start again? (**No — silent resume.**)
3. **Short timer:** If Start and LastTen both on and remaining ≤ ~10–14s, who wins? (**LastTen; suppress Start.**)
4. **Halfway meaning:** 50% of **original** duration with no replay after spoken / after `+1:00` eligibility, or “half of remaining” on every replan? (**50% of original + eligibility; Claude A1.**)
5. **Hebrew TTS missing:** warn in settings, fall back to English speech, or stay silent? (**Silent + log; optional settings warning later.**)
6. **Duplicate:** copy per-timer voice overrides or reset to Inherit? (**Copy overrides.**)
7. **Mute / DND audibility:** alarm-adjacent (try to be heard) or media-like (may be silent)? (**Media-like for V1; document.**)
8. **FGS type:** keep `mediaPlayback` with written justification, or move to `SPECIAL_USE` / split? (**Keep `mediaPlayback` for V1.**)
9. **Scope fallback:** if last-10 screen-off FGS proves flaky on API 34/35, ship Start+Halfway only for V1? (**Prefer keep last-10; degrade to Start+Halfway only if device proof fails — product call.**)

Answer these (or say “take meta defaults”) before the amended spec / implementation plan is written.

## Stage 3 — human decisions (recorded 2026-11-19)

| # | Decision |
|---|---|
| 1 | Master = hard kill switch (A) |
| 2 | Announce on resume = configurable, default **on** |
| 3 | Short timer: LastTen wins |
| 4 | Mid cue = half of **remaining**; speak **time left**, not “halfway” |
| 5 | Missing Hebrew TTS → English fallback |
| 6 | Duplicate copies overrides |
| 7 | Alarm-adjacent audibility |
| 8 | FGS type = `mediaPlayback` |
| 9 | Keep last-10; degrade only after failed device proof |

Amended design: [docs/superpowers/specs/2026-11-19-timer-voice-cues-design.md](../../../superpowers/specs/2026-11-19-timer-voice-cues-design.md).
