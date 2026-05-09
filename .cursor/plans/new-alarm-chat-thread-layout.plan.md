---
name: New alarm chat thread layout
overview: "Fix Lemur chat first paint (thread-first layout + optional copy/bubble polish) in two PRs: (1) structural Compose/layout—chips in thread, composer-only bottom bar, insets/scroll; (2) conversation polish—local assistant bubble, tiered onboarding copy, RTL doc—without changing the Gemini starter/draft pipeline."
todos:
  - id: pr1-layout-chips-composer
    content: "[PR1] LemurChatScreen: chips into LazyColumn after onboarding; bottomBar = ChatInputBar only; LazyColumn contentPadding(bottom = named dp constant) for composer clearance; leadingItemCount helper + LaunchedEffect scroll index"
    status: pending
  - id: pr1-scroll-ime-smoke
    content: "[PR1] IME/scroll QA small phone; manual smoke: chip send path, isSending, wizard/detail, preview"
    status: pending
  - id: pr2-assistant-bubble
    content: "[PR2] Local assistant starter bubble (EN/iw/he); match ChatBubble assistant styling; gate with showOnboarding"
    status: completed
  - id: pr2-copy-tier
    content: "[PR2] Short onboarding primary line + Learn more (expand/sheet) for long copy; sync values-he + values-iw"
    status: completed
  - id: pr2-rtl-doc-smoke
    content: "[PR2] RTL/TalkBack pass for new copy + in-thread chips; extend docs/RTL-LOCALIZATION.md; footnote proactive UX plan (bottomBar chips superseded); full regression smoke"
    status: completed
isProject: false
---

# New alarm chat — thread-first layout (post–proactive UX)

## Screenshot diagnosis (validated)

- **Top:** App bar title + privacy line (correct anchor).
- **Thread area:** Large instructional block (“Chat with Lemur” + long body + wizard/detail text buttons).
- **Middle:** ~50–60% empty canvas — `LazyColumn` does not fill space between list content and fixed `bottomBar`.
- **Bottom:** Starter chips + composer read as **footer chrome**, not as the start of a message thread.

Root cause: **onboarding + messages + preview** scroll together while **chips are pinned** in `bottomBar` ([`LemurChatScreen.kt`](app/src/main/java/com/elroi/lemurloop/ui/screen/alarm/LemurChatScreen.kt)), producing a disconnected hierarchy.

## Product decision (locked for this plan)

| Topic | Decision |
|-------|----------|
| Mental model | **Chat-first** — primary job is conversational alarm creation; wizard/detail remain exit ramps. |
| Fixed chrome | **Top app bar** (title + privacy) + **composer-only** bottom bar with IME padding. |
| Starter pipeline | **No change** — chip tap still sends full prompt via existing `sendStarterMessage` / shared send path into Gemini + draft merge. |

## Target UX (first paint)

1. **Scroll thread (single column):** **PR1:** onboarding → **horizontal starter chips** → user/assistant bubbles → **alarm preview**. **PR2:** prepend **local assistant bubble** (no API) and replace long onboarding with **headline + one line + “Learn more”** as needed.
2. **Bottom bar:** `ChatInputBar` only — same horizontal padding pattern as today (**PR1**).
3. **Copy tiering:** Primary line answers “what do I do?” in **≤5 seconds**; long `lemur_chat_onboarding_body` content behind **Learn more** (**PR2**).

## Engineering notes

### `showOnboarding` (source of truth)

- **Today:** Derived in [`LemurChatScreen`](app/src/main/java/com/elroi/lemurloop/ui/screen/alarm/LemurChatScreen.kt) as `val showOnboarding = state.messages.isEmpty()` from `LemurChatUiState` — **not** a separate ViewModel field. PR1/PR2 keep using this for gating local UI (onboarding, chips, bubble); no change required unless product wants persistence across empty sessions.

### Bottom clearance (commit: **fixed `contentPadding`, no spacer item**)

- Use **`LazyColumn` `contentPadding = PaddingValues(..., bottom = …)`** with a **file-private constant** (e.g. `LemurChatComposerBottomInset`), **not** a trailing spacer `item`, for PR1 — avoids an extra list row, TalkBack focus oddity, and scroll-to-end targeting a spacer.
- Value is an **approximate upper bound** for [`ChatInputBar`](app/src/main/java/com/elroi/lemurloop/ui/screen/alarm/LemurChatScreen.kt) (OutlinedTextField up to `maxLines = 4` + send row + padding). **~100–140 dp** range is typical; tune once in PR1 QA. **No `onGloballyPositioned` in PR1** unless QA shows systematic clip; document in KDoc that the constant must stay in sync if `ChatInputBar` changes.

### IME / chips (explicit acceptance)

- **Accepted for v1:** After the user opens the keyboard, **starter chips may scroll off or sit under the IME**; no requirement to pin chips above the keyboard. Happy path (tap chip first) is unchanged; follow-up typing does not need chips visible.

### Scroll-to-latest index (avoid fragile math)

- Replace ad-hoc `onboardingItems` with a **single file-private function** (e.g. `leadingItemCountBeforeMessages(showOnboarding: Boolean, includeLocalAssistantBubble: Boolean): Int`) or equivalent **one derived `val`** whose branches match the **exact** `item { }` / `items()` order in the `LazyColumn`.
- **PR1:** Implement counts for **onboarding + chips** (two leading items when `showOnboarding`; document in comment). **PR2:** When adding the assistant bubble as the **first** gated item, **extend the same helper** and reuse it in `LaunchedEffect` — avoids an off-by-one when the bubble lands.

### Merge / conflict note (`LemurChatPrompts.kt`)

- **PR1 should be UI-only** (`LemurChatScreen.kt` and, if needed, strings unchanged). If the working branch already modifies [`LemurChatPrompts.kt`](app/src/main/java/com/elroi/lemurloop/domain/chat/LemurChatPrompts.kt), **land or revert prompt changes separately** from PR1, or **rebase PR1 onto** a clean base so the layout PR stays reviewable and conflict-free.

### Other

- **LazyColumn item order:** **PR1:** `showOnboarding` gates onboarding + chips. **PR2:** same flag also gates assistant bubble (inserted as first item). Chips disabled when `isSending`.

## Explicit non-goals

- New nav routes, new JSON fields, or Gemini calls on screen open.
- Replacing `string-array` starter prompts with hardcoded Kotlin.

## Acceptance checklist

- No large **intentional blank band** between intro/chips and composer on a typical phone height when the thread is empty.
- User can infer next action (type or tap a chip) without reading a paragraph above the fold.
- Chip tap → full prompt → streaming + draft behavior **unchanged** from current proactive UX ship.
- **IME:** Chips scrolling away or under the keyboard after the composer is focused is **acceptable**; no blocking UX requirement to keep chips pinned above IME for v1.

## Two-PR split (recommended)

| | **PR1 — Structure** | **PR2 — Content & polish** |
|---|---------------------|----------------------------|
| **Branch** | `feature/lemur-chat-thread-layout` (example) | `feature/lemur-chat-thread-content` branched from **main after PR1 merge** (or stacked on PR1 if your workflow prefers) |
| **Goal** | Remove dead band: chips live in the **same** `LazyColumn` as onboarding → messages → preview; **composer-only** `bottomBar`; bottom **inset** so content is not clipped; **scroll index** fixes in `LaunchedEffect`. | **Feel** like a chat: **local assistant bubble**; **tiered onboarding** (short line + Learn more); **RTL doc** + a11y pass on new strings/layout. |
| **Strings** | Prefer **none** or trivial (e.g. one line if required by layout—ideally zero). Keep existing `lemur_chat_onboarding_*` as-is for PR1. | All new/changed **EN + values-iw + values-he**; glossary alignment where applicable. |
| **Risk profile** | Compose/layout regression, IME overlap, scroll-to-latest off-by-one. | Copy length, localization parity, expandable/sheet behavior. |
| **Acceptance (PR1)** | No large empty band between onboarding/chips and composer on first open; chips still one-tap full prompt; `isSending` still gates chips; wizard/detail + preview unchanged. | Primary onboarding line readable at a glance; Learn more exposes prior detail; bubble does not trigger API; full RTL smoke row updated. |
| **Merge order** | Land **first**. | Land **second** (depends on PR1 for item order if bubble is first list child—implement bubble in PR2 **above** onboarding in the list PR1 already created). |

**PR1 implementation note:** In PR1, list order is **onboarding → chips → messages → preview** (no bubble yet). PR2 inserts the **assistant bubble** as the first onboarding-gated item when you add it.

**PR titles (suggested)**

1. `ui: Lemur chat — chips in thread, composer-only bottom bar`
2. `ui: Lemur chat — assistant opener + tiered onboarding copy`

## Branch

Use a **non-`main`** branch per PR (see table above).
