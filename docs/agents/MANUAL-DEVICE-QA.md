# Manual device QA

Gotchas from walking checklists on a real phone. Keep this short; product rules stay in specs.

## After reboot

- Wireless `adb` often drops. Wait for unlock **and** `adb devices` before treating “no device” as a product failure.
- Timer (and alarm) restore runs on `BOOT_COMPLETED`, which is after first unlock — not at the wall-clock end time while the lock screen is still up.
- Overdue lateness is measured at reconcile after unlock. A “+10s” unlock that actually lands after 60s correctly takes the quiet finished path.

## Evidence before calling a failure

- Check `adb shell cat /proc/uptime`, notification `when`, and timer `createdAt` / duration before trusting recollection of a reboot or a shade notification.
- A “Timer finished” notification can be hours old and still sitting in the shade.
- **Blank tab ≠ empty data.** If the screenshot or a11y dump shows only the bottom home bar (no title, Settings gear, or FAB), that is missing chrome / wrong content slot — not “no rows.” An empty alarm list still shows the LemurLoop title, Settings, and Add.

## Timers: overdue lock (intentional)

- Less than 60 seconds late → ring (re-arm at `now`).
- 60 seconds late or more → pause at original length + finished notification.
- That window is a LemurLoop product lock from the Stage 3 gate answer. It is **not** a measured Alarm Clock Xtreme constant (Stage 2: neither 60s nor 10m was evidence).

## UI automation on a personal phone

- Confirm LemurLoop is the resumed activity before each tap. Other apps (e.g. Duolingo) stealing focus make misses look like product bugs.

## Timer voice cues

Optional spoken progress on countdown timers only. **Half-asleep checkbox script:** [TIMER-VOICE-CUES-HALF-ASLEEP-QA.md](TIMER-VOICE-CUES-HALF-ASLEEP-QA.md).

If screen-off last-10 fails on API 34/35 after honest proof, stop and ask before cutting scope.
