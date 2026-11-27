# Open threads

Work in progress across agent sessions: handoffs, pending decisions, and fixes waiting on verification. Any agent may add, update or remove entries. Date each entry, and remove it once it's resolved (git history keeps the record).

## `feature/per-alarm-persona` — revive or abandon? (2026-09-17)

The last remaining old remote branch. It has two unmerged commits not in `main`: `f9a0c92` (three-tier AI intelligence UI with a Local AI toggle) and `7d71682` (API key UX with a stacked verify button and status chip). It was never opened as a PR. The decision is pending on whether it overlaps with the current Intelligence Health and API Credentials settings. **Don't delete it** until that's decided.

## Timer last-10 countdown rhythm — device check pending (2026-11-27)

`claude/countdown-rhythm-fix-9lyls2` stops the spoken 10…1 countdown from rushing and then stopping: the cue now fires 1.5 s early to warm up TTS, and each digit is pinned to the wall clock instead of waiting for the previous one to finish. Unit tests cover the plan and the cadence math, but the even rhythm still has to be checked by ear on a phone, with the screen on and off, in English and Hebrew.

## Device verification pending (2026-09-17)

BUG-2, BUG-4 and BUG-5 in [known_bugs.md](../../known_bugs.md) are fixed in code but haven't been checked on a real device.
