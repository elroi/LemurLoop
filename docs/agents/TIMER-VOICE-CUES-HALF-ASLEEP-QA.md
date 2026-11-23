# Timer voice cues — half-asleep checklist

Do this on a **real phone** (or emulator with speakers). Volume up. Don’t multitask.

**Install once** (laptop, worktree):

```bash
cd /Users/elroi/Developer/LemurLoop-timer-voice-cues
./gradlew :app:installDebug
```

Open **LemurLoop** on the phone. Stay in English for steps 0–6. Hebrew is step 7 only.

Mark each box. If a box fails, **stop** and note which number.

---

## 0. Find the switches (UI glance)

- [ ] Settings (gear) → **Timer** → see a **Voice cues** block
- [ ] Master is **off** (quiet by default)
- [ ] Under master you still see Start / Mid / Last-10 / Announce on resume, but they look **disabled**
- [ ] Open a timer’s **focus** screen → see **Voice cue overrides** with Inherit / On / Off

If you can’t find those, the wrong build is installed. Fix install before anything else.

---

## 1. Master off = silence (even if override says On)

1. Settings → Timer → Voice cues → leave **master OFF**
2. Open a timer focus → set Start / Mid / Last-10 overrides all to **On**
3. Start a **30 second** timer
4. Listen for ~30s (screen can stay on)

- [ ] Hear **nothing** spoken (no “started”, no “seconds left”, no 10…1)
- [ ] End **ring** still happens as usual

---

## 2. Happy path, screen off (~30s)

1. Settings → Timer → Voice cues → turn **master ON**
2. Leave children on (Start, Mid, Last-10, Announce on resume)
3. Focus overrides → all **Inherit**
4. Start a **30 second** timer
5. Immediately press power → **screen off**
6. Listen without unlocking

- [ ] Soon after start: hear something like **timer started** / start cue
- [ ] Around the middle: hear **time left** (e.g. “15 seconds left”) — **not** the word “halfway”
- [ ] Near the end: hear **10, 9, 8…1**
- [ ] Then the normal **end ring** (same as before this feature)

**If last-10 never speaks with screen off:** write API level (Settings → About phone) and stop. Don’t “fix” it yourself — tell the agent.

---

## 3. Pause stops speech; announce-on-resume

1. Master still ON, Announce on resume **ON**
2. Start a **60 second** timer
3. When it starts talking (or mid cue), tap **Pause**

- [ ] Speech **stops**

4. Tap **Play**

- [ ] Hear **Start** again

5. Pause again. Settings → Timer → Voice cues → turn **Announce on resume OFF**
6. Play again

- [ ] Resume is **silent** (no Start)
- [ ] Mid / last-10 can still happen later if still enabled — that’s OK

Turn Announce on resume **back ON** when done.

---

## 4. Short timer: Last-10 wins (no Start)

1. Master ON, Start ON, Last-10 ON
2. Start an **8 second** timer (or as close as the keypad allows under 10s)
3. Listen

- [ ] **No** Start phrase
- [ ] Hear digit countdown (into the last seconds)
- [ ] End ring still fires

---

## 5. Two timers (don’t cut the other)

1. Master ON, defaults
2. Start timer **A** for **45 seconds**
3. Immediately start timer **B** for **30 seconds**
4. Let B get into last-10 if you can hear it, then **dismiss/stop A’s ring** (or delete A when it ends) while B is still running

- [ ] B’s speech is **not** cut off just because A finished/dismissed
- [ ] B still reaches end ring normally

---

## 6. Per-timer Off override

1. Master ON (globals say cues on)
2. One timer → focus → set Start / Mid / Last-10 all to **Off**
3. Start that timer for **20 seconds**

- [ ] **No** spoken cues for that timer
- [ ] End ring still OK

Set overrides back to **Inherit** when done.

---

## 7. Hebrew language (optional, 2 min)

Only if you care about locale today:

1. App language → **Hebrew**
2. Master ON, start a **20s** timer
3. Listen

- [ ] App does **not** crash
- [ ] You hear **some** speech (Hebrew if the phone has HE TTS; otherwise English is OK)

Switch language back to English if you want.

---

## Done

- [ ] Steps 0–2 passed (must-have)
- [ ] Steps 3–6 passed (or note which failed)
- [ ] Step 7 done or skipped on purpose

Tell the agent: **“UI OK”** / **“smoke passed”** / **“failed step N: …”**  
No commit until you say so.
