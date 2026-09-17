# Open threads

Work in progress across agent sessions: handoffs, pending decisions, and fixes waiting on verification. Any agent may add, update or remove entries. Date each entry, and remove it once it's resolved (git history keeps the record).

## `feature/per-alarm-persona` — revive or abandon? (2026-09-17)

The last remaining old remote branch. It has two unmerged commits not in `main`: `f9a0c92` (three-tier AI intelligence UI with a Local AI toggle) and `7d71682` (API key UX with a stacked verify button and status chip). It was never opened as a PR. The decision is pending on whether it overlaps with the current Intelligence Health and API Credentials settings. **Don't delete it** until that's decided.

## Cleanup: RULE.md exceptions, Hebrew folders, BUG-8 (2026-09-17)

Handed to a separate chat. Check git log and PRs for its outcome:

- RULE.md "Current exceptions" is out of date: `AppDataRepository` and `DiagnosticLogRepository` already exist. The same text is in `.cursor/skills/lemurloop-architecture/SKILL.md` and `docs/REFACTORING_PLAN.md`.
- `res/values-he` and `res/values-iw` look identical. Investigate whether both are needed; don't delete either without approval (commit `ab31377` chose `iw` deliberately).
- BUG-8: snooze may drop the Smart Wakeup extras and `BRIEFING_ENABLED` (noted in `AlarmServiceExtrasTest`, missing from `known_bugs.md`). Work in progress on branch `fix/bug-8-snooze-extras`.

## Device verification pending (2026-09-17)

BUG-2, BUG-4 and BUG-5 in [known_bugs.md](../../known_bugs.md) are fixed in code but haven't been checked on a real device.
