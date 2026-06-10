# Reading Page Redesign — Story-First, Collapsed-Settings UI

Date: 2026-06-10
Owner: product-manager
Status: authoritative UI/interaction redesign for the reading page (`story/{id}/read`)
Applies to: Android Compose MVP first; iOS SwiftUI mirrors the same IA when the full Xcode path is ready.

Companion specs (do not contradict; this doc only changes *exposure/placement*, not behavior):

- `docs/design/tts-read-along-interaction.md` — authoritative read-along behavior, state machine, defaults, analytics, child safety. **Every feature here is preserved.**
- `docs/design/design-system.md`, `docs/design/design-tokens.md` — visual tokens.
- `docs/design/mockups/reading-page.md` — original phone wireframe (superseded by §6 here).
- Current code: `apps/reader/androidApp/.../MainActivity.kt` (`ReadingScreen`, `ReadingTopBar`, `ReadingControls`, `ReadingFullAudioRow`, `ReadingAudioCoachmark`), `apps/reader/iosApp/.../ContentView.swift` (`ReadingScreen`, `readingControls`, `ReadingTopBar`, `LMCPlaybackModeControl`, `ReadingAudioCoachmark`, `ReadingBottomBar`).

---

## 1. Problem (current state)

For a 5-8 year old, the reading page is inverted: chrome dominates, the story is pushed below the fold. Today, before a child sees any story text, the screen stacks (verified in current Android `ReadingScreen` lines ~1997-2085 and iOS `readingControls` lines ~2728-2839):

1. Top bar: close ×, story title, audio play/pause icon.
2. Progress bar + "2 / 5".
3. Pinyin switch + Text size S / M / L.
4. Read-along / Tap-to-listen mode toggle (always visible).
5. Auto-continue switch (always visible).
6. Playback speed `0.9x` dropdown (always visible).
7. Status line: "Sentence 1 / 3 · Stopped/Playing · Recorded".
8. Action cluster: Read all + Previous sentence + Next sentence + Repeat sentence + Stop (4-5 buttons, always rendered).
9. First-run coachmark card.
10. **Then** the story text.

That is ~9 control bands. Children's UX research is explicit that young users need minimal choices per screen, shallow navigation, and low cognitive load ([NN/g children's usability](https://www.nngroup.com/articles/childrens-websites-usability-issues/), [Gapsy UX for kids](https://gapsystudio.com/blog/ux-design-for-kids/)). The market leaders for reading agree: Apple Books hides controls behind a single corner button and reveals chrome only on tap ([TidBITS on iOS 16 Books](https://tidbits.com/2022/10/03/apples-books-ios-16/)); Du Chinese keeps the Chinese text as the prominent middle layer with settings tucked into a toggleable bottom toolbar ([Du Chinese review](https://flexiclasses.com/mandarin/du-chinese-review/)); Epic keeps a kid-navigable, distraction-light Read-to-Me surface ([Epic on Google Play](https://play.google.com/store/apps/details?id=com.getepic.Epic&hl=en_US)).

**Goal:** story is the hero on open; everything optional is collapsed; the resident control set is the child-friendly minimum; playback controls appear only while playing.

---

## 2. Redesign summary (key decisions)

| # | Decision | Rationale |
|---|---|---|
| D1 | **Story text is the hero on open.** Only a slim top bar + the scrollable story are visible above the fold. The first sentence (with ruby pinyin) sits high on the screen. | Story-first; matches Apple Books / Du Chinese center-text priority. |
| D2 | **One resident primary action: a big "▶ 朗读 / Read" button**, docked at the bottom. Plus a small gear in the top bar. Nothing else audio-related is resident when stopped. | Single obvious affordance; large target for 5-8 (≥56dp). |
| D3 | **Collapse all settings into a "Reading settings" bottom sheet** opened by the gear: pinyin toggle, text size S/M/L, listening mode (read-along / tap-to-listen), auto-continue, playback speed. None of these are resident anymore. | Progressive disclosure; removes 4 resident control bands. |
| D4 | **Transport controls live in a compact bottom **Player bar** that appears only while Playing/Paused** and collapses back to the big Read button when Stopped. Contents: pause/resume, previous sentence, next sentence, repeat, stop, source/status, sentence count. | Controls "appear on demand," never compete with text at rest. |
| D5 | **Progress is low-key:** a thin 2dp progress hairline under the top bar, no always-on "Sentence x/y" band. Page count "2/5" moves to a muted label in the top bar. Sentence count moves into the player bar (only while playing). | De-emphasize metrics; no competitive framing (per safety rules). |
| D6 | **Coachmark is a one-time, single dismissible card overlaid at the bottom** (near the Read button + a sentence speaker), with Skip/Done. Stored locally, never re-shown. Already keyed by `reading_coachmark_tts_row_dismissed`. | One-time guidance without blocking the story. |
| D7 | **Tap-to-listen is reachable from every sentence's speaker icon and from the settings sheet's mode switch.** Read-along is the default. The mode switch is no longer resident. | Both modes preserved; the per-sentence speaker is the natural tap-to-listen entry, matching Du Chinese tap-word/sentence behavior. |
| D8 | **"Back to reading place" pill** floats just above the player bar only when auto-follow is broken during playback. | Preserve existing behavior; keep it contextual, not resident. |
| D9 | **No feature is removed.** Read-along, tap-to-listen, speed, repeat, auto-continue, per-character karaoke, pinyin toggle, text size, audio-source label, AI "Ask" — all preserved, just relocated per §3. | Hard requirement. |

Net effect: resident chrome drops from ~9 bands to **2** (slim top bar + docked Read button). Everything else is one tap away.

---

## 3. Information architecture — where everything goes

Three surfaces. Each existing control maps to exactly one home.

### 3.1 Default screen (Stopped, story-first)

Resident, top to bottom:

- **Top bar (slim, 56-64dp):** `[×]` close · story title (`titleZh`, ellipsized) · muted page count "2/5" · `[⚙]` settings gear. **Removed from top bar:** the standalone audio icon (the docked Read button replaces it).
- **Progress hairline:** 2dp `colorPrimary` line directly under the top bar (the existing 8dp `LmcProgressBar` is thinned and de-chromed). Accessible label "Page 2 of 5" retained.
- **Story body:** scrollable sentence list with ruby pinyin (unchanged rendering: `SentenceTextBlock` / `ReadingSentenceView`), each sentence row keeps its **48dp speaker icon** (tap-to-listen entry) and tappable sentence body. The AI "Ask" card stays inline at the end of the paragraph (unchanged).
- **Docked bottom: big primary "▶ Read" button** (`reading_read_all` for read-along mode, `reading_sentence_play` label only inside the sheet for tap-to-listen). Full-width, `buttonPrimaryHeight` 56dp, `radiusLg` 16dp, `colorPrimary` / `onPrimary`, leading play icon. This replaces today's bottom `BottomActionRow` Previous/Next as the *audio* primary; paragraph navigation moves per D-note below.

Paragraph navigation note: today's bottom "Previous / Next (→ Vocabulary)" paragraph buttons remain necessary. Keep them, but demote: the **bottom dock holds the big Read button as the centerpiece**, with small **‹ / ›** paragraph-step icon buttons (48dp) flanking it (left = previous paragraph, right = next paragraph / "New words" on last page). This keeps one obvious primary (Read) while preserving paragraph paging in the same row. See wireframe §6.

### 3.2 Reading settings sheet (gear → modal bottom sheet)

Opened by the top-bar gear. Modal bottom sheet, `radiusXl` 24dp top corners, `colorSurface`, drag handle, dismiss on scrim tap / swipe-down / Done. Contents (each row ≥48dp, label from resources):

1. **Pinyin** — `Switch` (`reading_pinyin`). Persists per device. Toggling does **not** stop audio (existing rule §9.1).
2. **Text size** — S/M/L segmented chips (`reading_text_size` + `TextSizeChips`). Persists per device; keeps active sentence visible after reflow.
3. **Listening mode** — segmented: Read-along / Tap-to-listen (`reading_mode_read_along` / `reading_mode_tap_to_listen`). Reuses `ReadingSessionMode` / `LMCReadingPlaybackMode`.
4. **Auto-continue** — `Switch` (`reading_auto_continue`); disabled/dimmed when mode = Tap-to-listen (mirrors current iOS `.disabled` rule).
5. **Speaking speed** — segmented or stepper: Slow / 0.9x / Normal (`reading_playback_speed`, `reading_speed_*`). Reuses `ReadingPlaybackSpeed` / `LMCReadingPlaybackSpeed`.

Changing any setting while audio plays must not stop playback and must keep the current sentence visible (existing §9). The sheet is purely a relocation of today's `ReadingControls` + the mode/auto-continue/speed parts of `ReadingFullAudioRow`.

Optional grouping (nice-to-have, not required): a "Display" subgroup (pinyin, text size) and a "Listening" subgroup (mode, auto-continue, speed) with small `titleMedium` group labels.

### 3.3 Player bar (only while Playing / Paused)

A compact bottom bar that **replaces the docked Read button** while audio is active, then collapses back when Stopped. One row of icon buttons + a thin status line. Contents (all ≥48dp targets, icon + localized contentDescription):

- **‹ previous sentence** (`reading_previous_sentence`) — disabled at story start.
- **⏸ / ▶ pause-resume** — primary, slightly larger (56dp) and tinted `colorPrimary`.
- **↻ repeat sentence** (`reading_repeat_sentence`).
- **› next sentence** (`reading_next_sentence`) — disabled at story end.
- **⏹ stop** (`action_stop_audio`).
- **Status microline** (one line, `labelMedium`, `onSurfaceVariant`): "Sentence 2 / 4 · Recorded/System voice · Playing/Paused". This is the only place sentence count + source live now.

"Back to reading place" pill (`reading_back_to_reading_place`) floats just above this bar when `autoFollowEnabled == false` during playback.

All of these already exist in `ReadingFullAudioRow` / iOS transport `HStack`; this is a relocation + visibility gate (show only when `status != Stopped`), not new logic.

### 3.4 Mapping table — every current control's new home

| Control (today, resident) | New home | Resident at rest? |
|---|---|---|
| Close × | Top bar | Yes |
| Story title | Top bar | Yes |
| Audio play/pause icon (top bar) | **Removed** — folded into docked Read button / player bar | — |
| Progress bar + "2/5" | Hairline + muted top-bar count | Yes (low-key) |
| Pinyin switch | Settings sheet | No |
| Text size S/M/L | Settings sheet | No |
| Read-along / Tap-to-listen toggle | Settings sheet (mode) + per-sentence speaker | No |
| Auto-continue switch | Settings sheet | No |
| Playback speed 0.9x | Settings sheet | No |
| Sentence x/y status line | Player bar microline | Only while playing |
| Audio source "Recorded" | Player bar microline | Only while playing |
| Read all (primary) | Docked Read button | Yes (the one resident action) |
| Previous / Next sentence | Player bar | Only while playing |
| Repeat sentence | Player bar | Only while playing |
| Stop | Player bar | Only while playing |
| Back to reading place | Floating pill above player bar | Only when off-follow |
| Coachmark | One-time bottom card | Once, then never |
| Per-sentence speaker | Each sentence row | Yes (already in story body) |
| Paragraph Previous / Next (→ Vocab) | Bottom dock, demoted ‹/› icons flanking Read | Yes |
| AI "Ask" card | Inline, end of paragraph (unchanged) | Yes (inline) |

No row maps to "deleted." Every feature survives.

---

## 4. Key interaction flows

### 4.1 Open story (silent, story-first)
1. Page opens → **no audio** (unchanged hard rule §11). Top bar + hairline + story text + docked Read button visible. First sentence is near the top.
2. First-ever open also shows the one-time coachmark card at the bottom; child can read or tap Done.

### 4.2 Start read-along
1. Tap big **▶ Read** → starts from the current sentence (first visible sentence if none), auto-continue per setting.
2. Docked Read button **morphs into the Player bar**; the active sentence highlights (teal container + ruby) and auto-scrolls into view.
3. Per-character karaoke highlight runs (recorded timings or TTS range callbacks) — unchanged.

### 4.3 Tap one sentence (tap-to-listen)
1. Tap a sentence's **speaker icon** → plays only that sentence, does not advance (unchanged §3.2). Player bar appears for that sentence; on finish, returns to Stopped (Read button re-docks).
2. If read-along is playing, tapping a speaker switches to that one sentence (existing `replaySentenceOnly` / `playSentenceOnly`).

### 4.4 Pause / resume / stop
- Pause/resume/stop from the Player bar (or background/lock/audio-focus auto-pause, unchanged §10.3). Stop collapses the Player bar back to the Read button.

### 4.5 Change a setting mid-read
1. Tap gear → sheet slides up over the story; audio keeps playing under the scrim.
2. Toggle pinyin / size / speed / mode / auto-continue → applied live; active sentence stays visible; audio not interrupted (unchanged §9).
3. Dismiss → back to reading; Player bar still active.

### 4.6 Manual scroll during playback
- Drag the list → auto-follow pauses, audio continues, **"Back to reading place"** pill appears above the Player bar; tap it to recenter and re-enable follow (unchanged §8.2).

### 4.7 Paragraph paging / completion
- ‹ / › step paragraphs (stops audio per existing `resetPlaybackForParagraph`). Last page › becomes "New words" → Vocabulary. No auto-navigation, no `story_complete` from audio (unchanged §9.5).

---

## 5. Visual + token mapping

| Element | Token(s) | Notes |
|---|---|---|
| Top bar | `colorSurface`, `topAppBarHeight` 64 (may compress to 56), `elevationAppBar` 0 | Divider/hairline instead of shadow. |
| Progress hairline | `colorPrimary` (or `colorSuccess` at 100%), height 2dp (thinned from `progressHeight` 8) | Keep accessible "Page x of y". |
| Top-bar page count | `typeLabelMedium`, `colorOnSurfaceVariant` | Muted. |
| Gear icon button | `iconButtonSize` 48, `LmcIcon.Settings`, tint `colorPrimary` | Reuses existing canvas gear. |
| Docked Read button | `buttonPrimaryHeight` 56, `buttonPrimaryRadius` 16, `colorPrimary`/`colorOnPrimary`, leading play icon | Full bleed minus `screenPaddingPhone` 16. |
| Paragraph ‹/› icon buttons | `iconButtonSize` 48, `colorSecondary` | Flank the Read button. |
| Settings sheet | `colorSurface`, `radiusXl` 24 top, `elevationModal` 6, drag handle | Rows ≥`minTouchTarget` 48; `cardPadding` 16; `space5` 20 between groups. |
| Sheet switches/chips | Selected = `colorSecondaryContainer` + `colorSecondary` border + `colorOnSecondaryContainer` (per design-system Secondary chip) | Pinyin/mode/size/speed. |
| Player bar | `colorSurface`, `elevationModal` 6 (or 1dp + top divider), full-width, safe-area aware | `bottomActionHeight` 72 region. |
| Player primary (pause/resume) | 56dp, `colorPrimary` tint | Others 48dp `colorSecondary`. |
| Player status microline | `typeLabelMedium`, `colorOnSurfaceVariant` | Sentence count + source + status. |
| Active sentence | `colorSecondaryContainer` `#D9F1EE` surface, `colorOnSecondaryContainer` `#063432` text, `readingPanelRadius` 16, `readingPanelPadding` 16 | Unchanged from §7.1. |
| Karaoke active char | existing per-char treatment over the sentence highlight | Unchanged. |
| Reading body | `typeReadingHanzi{S,M,L}` / `typeReadingPinyin{S,M,L}`, `fontChineseReading` / `fontPinyin` | Unchanged. |
| "Back to reading place" pill | `colorSecondaryContainer`, `radiusFull`, ≥48dp | Unchanged behavior. |
| Coachmark card | `colorSecondaryContainer` / `colorOnSecondaryContainer`, `radiusLg` 16 | One-time. |
| Motion | `motionFast` 120 (toggles), `motionMedium` 180 (sheet/player bar enter) | Respect Reduce Motion: fade/instant, no animated scroll. |

Contrast, ≥48dp targets, font-scale ≥1.3x, color-not-alone, no-dark-fallback rules from design-system all carry over unchanged.

---

## 6. ASCII wireframes

### 6.1 Default screen — Stopped (story-first)

```text
+------------------------------------------------+
| [x]  桃园三结义                    2/5     [⚙] |   <- slim top bar
|------------------------------------------------|   <- 2dp progress hairline
|                                                |
|  hěn jiǔ yǐ qián, zhuō xiàn...           [🔊]  |   <- sentence + 48dp speaker
|  很久以前，涿县贴出榜文，盼望有人一起保护乡里。      |
|                                                |
|  liú bèi dú wán bǎng wén...              [🔊]  |
|  刘备读完榜文，心里想着百姓过安稳日子。             |
|                                                |
|  ... (story scrolls; first sentence on top)    |
|                                                |
|  [ ✨ Ask about this part ]                     |   <- inline AI card (unchanged)
|                                                |
+------------------------------------------------+
| [ ‹ ]      [   ▶  Read   ]            [ › ]     |   <- docked: big Read + paragraph step
+------------------------------------------------+
```

### 6.2 Playing — Player bar replaces Read button

```text
+------------------------------------------------+
| [x]  桃园三结义                    2/5     [⚙] |
|------------------------------------------------|
|                                                |
|  liú bèi dú wán bǎng wén...              [🔊]  |
| +============================================+ |   <- ACTIVE sentence (teal)
| | 刘备读完榜文，心里想着百姓过安稳日子。          | |   <- ruby + karaoke char
| +============================================+ |
|                                                |
|  ... story continues ...                       |
|                                                |
|            [ ◎ Back to reading place ]         |   <- only if scrolled away
+------------------------------------------------+
|  Sentence 2 / 4 · Recorded · Playing           |   <- status microline
| [ ‹ ]   [ ⏸ ]   [ ↻ ]   [ › ]        [ ⏹ ]     |   <- Player bar (only while active)
+------------------------------------------------+
```

### 6.3 Reading settings sheet (gear tapped)

```text
+------------------------------------------------+
|                (story dimmed behind)           |
| .............................................. |
| +--------------------------------------------+ |
| |                  ──                        | |   <- drag handle
| |  Reading settings                          | |
| |                                            | |
| |  Pinyin                          [ ON  ]   | |
| |  Text size            [ S ] [ M ] [ L ]    | |
| |  Listening mode  [ Read-along ][ Tap ]     | |
| |  Auto-continue                   [ ON  ]   | |
| |  Speaking speed   [ Slow ][ 0.9x ][ Norm ] | |
| |                                            | |
| |                            [   Done   ]    | |
| +--------------------------------------------+ |
+------------------------------------------------+
```

---

## 7. Android (Compose) implementation checklist

File: `apps/reader/androidApp/src/main/java/com/littlemandarin/classics/MainActivity.kt`. No `shared` changes required — all relocations are UI; reducers/state (`ReadingSessionReducer`, `SentencePlaybackUiState`, `ReadingSessionMode`, `ReadingPlaybackSpeed`) are reused as-is.

1. **`ReadingScreen` layout (lines ~1992-2213):** replace the resident `Column { ReadingTopBar; ReadingControls; ReadingFullAudioRow; coachmark; LazyColumn; BottomActionRow }` with:
   - `ReadingTopBar` (modified, see #2)
   - thin progress hairline (see #3)
   - story `LazyColumn` in the weighted `Box` (unchanged content, including per-sentence speaker + AI card)
   - bottom dock: **`ReadingBottomDock`** that shows either the big Read button row (Stopped) or **`ReadingPlayerBar`** (Playing/Paused), switched on `playbackState.status`.
   - Keep coachmark as a one-time overlay anchored above the dock (`showCoachmark`, `ReadingCoachmarkDismissedKey` reused).
   - Add `var showSettingsSheet by remember { mutableStateOf(false) }`.
2. **`ReadingTopBar` (lines ~2895-2969):** keep `[×]` + title; **replace** the trailing audio `HeaderIconButton` with a **gear** `HeaderIconButton(icon = LmcIcon.Settings, contentDescription = settings_title, onClick = { showSettingsSheet = true })`. Thin the embedded `LmcProgressBar` to a 2dp hairline (new `progressHairlineHeight`) and render the muted "2/5" count inline in the bar (move `reading_progress_count` up from the second row, drop the standalone progress row, or keep a 2dp row). Drop `playbackStatus`/`onAudioClick` params (now handled by dock).
3. **New `ReadingSettingsSheet` composable** using `ModalBottomSheet` + `rememberModalBottomSheetState` (Material3; not yet used in this file — net-new import `androidx.compose.material3.ModalBottomSheet`). Move into it the bodies of today's `ReadingControls` (pinyin `Switch` + `TextSizeChips`) **and** the mode `FilterChip` pair + auto-continue `Switch` + speed `DropdownMenu` from `ReadingFullAudioRow`. Wire to the existing callbacks already threaded in `ReadingScreen`: `onPinyinChange`, `onTextSizeChange`, `onPlaybackModeChange`, `onAutoContinueChange` (`setAutoContinue`), `onPlaybackSpeedChange`. Disable the auto-continue row when `playbackMode == TapToListen`.
4. **New `ReadingPlayerBar` composable** = the transport `FlowRow` extracted from `ReadingFullAudioRow` (lines ~3149-3231): pause/resume (primary), previous, repeat, next, stop, plus the status microline (`reading_sentence_progress` + status string + `audioSourceText`) extracted from lines ~3048-3085. Render only when `playbackState.status != Stopped`. Keep the "Back to reading place" `TextButton`/pill (existing `showReturnToReadingPlace`) just above it.
5. **`ReadingBottomDock` composable:** when Stopped → big `Button` (`reading_read_all`, 56dp, `RadiusLg`, leading `LmcIcon.Audio`) flanked by two 48dp `HeaderIconButton`s for paragraph prev (`LmcIcon.Previous`, `readingState.canGoPrevious`) and next (`LmcIcon.Next` / "New words" on last page). Reuse the existing `next`/`previous` reducer transitions and `resetPlaybackForParagraph` from the old `BottomActionRow` (lines ~2184-2211); reuse `startCurrentSentencePlayback` for Read. When Playing/Paused → render `ReadingPlayerBar`.
6. **Delete `ReadingControls` and `ReadingFullAudioRow`** once their contents move to #3/#4 (or keep as thin wrappers calling the new composables). The bottom `BottomActionRow` paragraph buttons fold into `ReadingBottomDock` (#5).
7. **Coachmark:** keep `ReadingAudioCoachmark` content but render it as a one-time overlay above the dock; behavior + `ReadingCoachmarkDismissedKey` unchanged.
8. **Strings (already present, reuse):** `reading_read_all`, `reading_pinyin`, `reading_text_size`, `reading_mode_read_along`, `reading_mode_tap_to_listen`, `reading_auto_continue`, `reading_playback_speed`, `reading_speed_*`, `reading_sentence_progress`, `reading_audio_*`, `reading_audio_source_*`, `reading_back_to_reading_place`, `reading_previous_sentence`, `reading_next_sentence`, `reading_repeat_sentence`, `action_stop_audio`, `settings_title`, `action_done`. **Add (both `values/` and `values-zh/`):** `reading_settings_title` ("Reading settings" / "阅读设置") for the sheet header — or reuse `settings_title` if acceptable. No new analytics events (per §14 of the interaction spec).
9. **No behavior changes:** autoplay-off, state machine, auto-pause on background/focus (`DisposableEffect` lines ~1980-1990), karaoke drivers, analytics (`paragraph_audio_play`, `pinyin_toggle`) all stay. Sheet toggles call the same reducers; pinyin/size changes still must not stop audio.
10. **A11y/build:** gear, Read, and every player/sheet control keep ≥48dp + localized contentDescription; verify at 1.3x font scale and 360dp width; `./gradlew :androidApp:assembleDebug` and `:shared:allTests` must pass.

### Android component inventory (new vs. reused)

- New: `ReadingSettingsSheet`, `ReadingPlayerBar`, `ReadingBottomDock`.
- Reused/relocated: `ReadingTopBar` (modified), `TextSizeChips`, `HeaderIconButton`, `ReadingAudioCoachmark`, `lmcFilterChipColors`, all `ReadingScreen` playback functions and reducers.
- Removed/folded: `ReadingControls`, `ReadingFullAudioRow`, resident `BottomActionRow` audio role.

---

## 8. iOS (SwiftUI) implementation checklist

File: `apps/reader/iosApp/LittleMandarinClassics/ContentView.swift`. Mirror the Android IA. Consumes the same `ReaderViewModel` flags (`isReadingAudioActive`, `isReadingAudioPaused`, `readingMode`, `autoContinueEnabled`, `playbackSpeed`, `activeCharIndex`, `readingAudioLocation`) — no shared changes.

1. **`ReadingScreen.body` (lines ~2594-2722):** keep `ReadingTopBar` + `ScrollViewReader/ScrollView` of paragraphs + drag-gesture follow logic. **Remove** the resident `readingControls` block from the top of the scroll content (lines ~2617, 2728-2840). Add `@State private var showSettingsSheet = false`. Replace the bottom `ReadingBottomBar` with a new **`ReadingBottomDock`** (Read button + paragraph ‹/›) that swaps to **`ReadingPlayerBar`** when `viewModel.isReadingAudioActive`.
2. **`ReadingTopBar` (lines ~3755-3788):** keep `[xmark]` + title + muted `countText`; **replace** the trailing play `IconButton` with a **gear** `IconButton(systemName: "gearshape.fill", labelKey: "settings_title", action: { showSettingsSheet = true })`. Thin `LMCProgressBar` to a 2dp hairline.
3. **New `ReadingSettingsSheet` View** presented via `.sheet(isPresented: $showSettingsSheet)` with `.presentationDetents([.medium])` and `.preferredColorScheme(.light)` (match existing `FeedbackSheet` pattern, lines ~3387-3391). Move into it: the pinyin `Toggle`, `LMCSegmentedReadingSize`, `LMCPlaybackModeControl`, the auto-continue `Toggle` (`.disabled(readingMode == .tapToListen)`), and the speed `Menu` — all lifted from current `readingControls`. Bind to existing `showPinyin`, `readingSize`, `readingModeBinding`, `autoContinueBinding`, `viewModel.setPlaybackSpeed`.
4. **New `ReadingPlayerBar` View** = the transport `HStack` currently gated by `if viewModel.isReadingAudioActive` (lines ~2780-2817): previous, pause/resume (primary), repeat, next, stop, with a status microline (`reading_sentence_progress` + status + source). Pin it to the bottom; show only while active. Keep the existing "Back to reading place" `Button` (lines ~2618-2632) floating just above it (move it out of the scroll content into the bottom overlay, or keep in-scroll — but visually it should sit near the bar).
5. **`ReadingBottomDock` View:** Stopped → big primary `Button` (`primaryAudioButtonLabelKey`, `LMCPrimaryButtonStyle`, `speaker.wave.2.fill`) flanked by paragraph `IconButton`s (`chevron.left` prev / `chevron.right` or "New words" next) reusing `ReadingBottomBar`'s `previous`/`next` closures + `viewModel.nextReadingTransition`. Active → `ReadingPlayerBar`. Reuse `startPlaybackAtVisiblePosition()` for Read.
6. **Delete** the old `readingControls` computed property; fold `ReadingBottomBar` into `ReadingBottomDock`.
7. **Coachmark:** keep `ReadingAudioCoachmark` (lines ~3823-3842) but render once as a bottom overlay above the dock, keyed by the existing `@AppStorage("reading_coachmark_tts_row_dismissed")`.
8. **Strings:** reuse existing `LocalizedStringKey`s (`reading_read_all`, `reading_sentence_play`, `reading_mode_*`, `reading_auto_continue`, `reading_playback_speed`, `reading_sentence_progress`, `reading_back_to_reading_place`, `settings_title`, `action_done`, etc.). Add `reading_settings_title` to the String Catalog (en + zh-Hans) if not reusing `settings_title`.
9. **No behavior changes:** autoplay-off (`.task`), auto-pause `onDisappear`/interruption, karaoke (`activeCharIndex`), follow/scroll logic, analytics — unchanged. Sheet bindings call the same view-model setters; pinyin/size changes must not stop audio.
10. **A11y:** gear/Read/player/sheet controls keep `labelKey` accessibility labels and ≥44pt targets; Dynamic Type to ≥1.3x; light-scheme only. iOS build verified when full Xcode path is ready (per AGENTS §3).

### iOS component inventory (new vs. reused)

- New: `ReadingSettingsSheet`, `ReadingPlayerBar`, `ReadingBottomDock`.
- Reused/relocated: `ReadingTopBar` (modified), `LMCSegmentedReadingSize`, `LMCPlaybackModeControl`, `IconButton`, `ReadingAudioCoachmark`, `LMCPrimaryButtonStyle` / `LMCSecondaryButtonStyle`.
- Removed/folded: `readingControls` property, `ReadingBottomBar` audio role.

---

## 9. Constraints honored

- **No feature removed** — read-along, tap-to-listen, speed, repeat, auto-continue, per-character karaoke, pinyin toggle, text size, source label, AI Ask all preserved (§3.4).
- **Behavior unchanged** — all decisions, state machine, defaults, auto-pause, and acceptance criteria in `tts-read-along-interaction.md` still hold; this doc only moves controls.
- **Child-friendly** — resident chrome cut to 2 bands; ≥48dp targets; one obvious primary; minimal choices per screen ([NN/g](https://www.nngroup.com/articles/childrens-websites-usability-issues/)).
- **Design tokens** — every surface mapped in §5; no new colors.
- **i18n** — all labels via platform resources; one optional new key (`reading_settings_title`).
- **Zero punishment / no PII** — progress stays low-key and non-competitive; no new analytics events; no child data added.

---

## 10. Sources

Children's reading / immersive reader UX references (minimal chrome, progressive disclosure, large targets):

- Apple Books iOS 16 — controls hidden in a reading menu, chrome on tap (immersive reading): https://tidbits.com/2022/10/03/apples-books-ios-16/
- Apple Books — read books / reading menu: https://support.apple.com/guide/iphone/read-books-iphc1af7c57/ios
- Du Chinese — text-prominent reader, toggleable bottom toolbar for pinyin/font/speed, tap-word lookup: https://flexiclasses.com/mandarin/du-chinese-review/
- Du Chinese — graded reader review (clean interface, three-part reading screen): https://ninchanese.com/blog/2022/10/18/du-chinese-review-of-a-great-graded-reader/
- Du Chinese on the App Store: https://apps.apple.com/us/app/du-chinese-read-learn-chinese/id1052961520
- Epic — kid-navigable Read-to-Me, synchronized audio + text: https://play.google.com/store/apps/details?id=com.getepic.Epic&hl=en_US
- Epic — Read-to-Me / audiobooks: https://apps.apple.com/us/app/epic-kids-books-reading/id719219382
- Nielsen Norman Group — Children's UX usability (minimal choices, shallow navigation): https://www.nngroup.com/articles/childrens-websites-usability-issues/
- Nielsen Norman Group — Design for kids by physical development (motor control, tablet-first, big targets): https://www.nngroup.com/articles/children-ux-physical-development/
- Gapsy — UX design for kids (large touch targets ~2cm for ages 4+, low cognitive load, customizable sensory): https://gapsystudio.com/blog/ux-design-for-kids/

Behavior/spec source of truth (unchanged): `docs/design/tts-read-along-interaction.md`.
