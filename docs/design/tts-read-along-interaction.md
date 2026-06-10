# TTS Read-Along Interaction Spec

Date: 2026-06-10  
Owner: product-manager  
Status: authoritative MVP interaction spec  
Applies to: Android MVP first, iOS follows the same behavior when the full Xcode path is ready.

## 1. Purpose

The reading page must help overseas bilingual children ages 5-8 read a short Chinese classic with audio support, pinyin, large text, and zero pressure. Audio is a scaffold for reading, not a test and not open voice chat.

This document restores the lost authoritative interaction spec from `docs/qa/pm-tts-interaction-report.txt` and aligns it with:

- `docs/tts-audio-plan.md`: pre-generated sentence audio first, system TTS fallback, word-boundary route for future word highlight.
- `docs/design/design-system.md` and `docs/design/design-tokens.md`: visual tokens, child-friendly tone, accessibility, localized platform strings.
- Current reader implementation in `MainActivity.kt`, `ReadingPresentation.kt`, `AudioService.kt`, `TtsService.kt`, `StoryAudio.kt`, and analytics schema.

## 2. MVP Decisions

| Area | Decision |
|---|---|
| Page entry | Opening a story reading page must not auto-play audio. Playback starts only after an explicit tap. |
| Primary mode | Listening read-along: play from the current sentence through the end of the story. |
| Secondary mode | Tap-to-listen: tapping one sentence's speaker plays that sentence only and does not advance. |
| Follow-after-me | Not in MVP. Do not ask the child to record, compare, or submit voice. Default product posture is no child voice collection. |
| Highlight | P0: current sentence highlight, with hanzi and pinyin ruby visually synchronized as one highlighted sentence unit. P1: timed word/character highlight using word-boundary or alignment timestamps. |
| Audio source | Use pre-generated sentence audio first. If missing, corrupt, or playback fails, fall back to system TTS for the same sentence. |
| Scrolling | During playback, auto-follow the current sentence. If the user manually scrolls, pause auto-follow and show a return-to-reading-place affordance. |
| Completion | Audio reaching the end of the story stops and offers the next reading-flow action. It must not auto-submit quiz, auto-mark comprehension, or navigate without user intent. |
| Analytics | Use the fixed anonymous event list; do not create new MVP event names for read-along. Do not log story text, child voice, raw AI prompts, child identity, or PII. |

## 3. Modes

### 3.1 Listening Read-Along, P0

Entry points:

- Top app bar audio button.
- Full audio row primary button labeled from platform resources, e.g. "Read all" / "朗读全文".
- If playback is paused, the same control becomes Resume.

Behavior:

- Starts at the current sentence cursor. If no sentence cursor exists for the visible paragraph, start at the first sentence of the visible paragraph.
- Continues sentence by sentence across paragraph boundaries until the story ends, unless the user pauses, stops, navigates away, manually changes page/paragraph, or the OS interrupts audio.
- Highlights the active sentence and keeps ruby pinyin aligned inside that sentence.
- Inserts a natural pause between sentences and a slightly longer pause at paragraph boundaries.
- At story end: stop playback, clear "playing" state, keep the last sentence cursor, and reveal the next flow action such as "Review new words" / "复习生字". Do not auto-open the next screen.

Default:

- Auto-continue is on for the full read-along control.
- Default speaking rate is slightly slow. Product target is about `0.90x` normal adult narration speed. For generated audio this should be handled at generation time or player speed when safe; for system TTS use the platform speech-rate control.

### 3.2 Tap-To-Listen, P0/P0+

Entry points:

- A 48dp minimum speaker icon on each sentence row/card.
- Tapping the sentence body may select/focus the sentence, but it should not start audio unless the visual design intentionally makes the whole sentence a clear audio control with an accessible label.

Behavior:

- Plays only the tapped sentence.
- Does not advance to the next sentence.
- Highlights only that sentence while it is playing or paused.
- If full read-along is currently playing, tapping a sentence speaker switches to tap-to-listen for that sentence and disables auto-continue for this playback action.
- After the sentence finishes, playback stops and the cursor remains on that sentence.

Rationale:

- This matches children's repeated listening behavior and avoids surprise jumps when they only want to hear a hard sentence.

### 3.3 Follow-After-Me, Future Only

Not in MVP:

- No microphone prompt.
- No voice recording.
- No pronunciation score.
- No leaderboard, streak penalty, or red/yellow correctness state for speaking.

Future gate:

- Requires separate privacy review, parent consent design, data retention policy, and child-safety review.
- Default must remain no child voice collection unless a parent explicitly opts in through a privacy-reviewed flow.

## 4. Controls

### 4.1 Required Controls

| Control | Placement | States | Behavior |
|---|---|---|---|
| Play / Pause / Resume | Top app bar and full audio row | stopped, playing, paused | Stopped starts read-along from current sentence. Playing pauses. Paused resumes current sentence; MVP may restart the current sentence if precise resume is unavailable. |
| Stop | Full audio row, visible only while playing/paused | enabled while active | Stops audio, clears active highlight, keeps sentence cursor for next play. |
| Next sentence | Full audio row | enabled when there is a next sentence | Moves to the next sentence. If playing, continue from that next sentence. If paused/stopped, move cursor and remain not playing unless explicitly tapped as an audio action. |
| Repeat current sentence | Full audio row and optional sentence overflow | enabled when a current sentence exists | Restarts the current sentence only. Does not advance. |
| Auto-continue switch | Full audio row or compact settings sheet | on/off | On: read-along continues to story end. Off: play controls read only the current sentence and stop. Default on. |
| Speaking speed | Full audio row or settings sheet | slow/default/normal, or equivalent | Default slightly slow. MVP can expose a compact menu if space allows; otherwise persist default and keep future UI slot. |
| Per-sentence speaker | Each sentence row/card | idle/playing/paused for active sentence | Plays only that sentence. Must have localized content description: "Play this sentence" / "朗读这一句". |

Use platform resources for all labels. Do not hardcode UI text in Compose or SwiftUI.

### 4.2 Compact Phone Layout

Phone reading page should keep global bottom navigation hidden. The audio row sits below pinyin/text-size controls and above the scrollable sentence list.

Suggested order:

1. Top app bar: close, story title, compact play/pause icon.
2. Reading progress: paragraph/page count and progress bar.
3. Support controls: pinyin toggle, text-size segmented control.
4. Audio row: sentence count, source/status, play/pause, next sentence, repeat, stop, optional auto-continue/speed menu.
5. Scrollable sentence list with ruby text and per-sentence speaker.
6. Bottom action row: previous paragraph/page, next paragraph/page or review words.

### 4.3 Tablet / Large Width

- Reading content remains constrained to `readingMaxWidth` (`720dp`).
- Audio controls can wrap using `FlowRow`; no control may shrink below `48dp` touch target.
- Do not stretch story text across the full tablet width.

## 5. State Machine

### 5.1 Playback State

The implementation should model the transport separately from the visible paragraph.

| State | Meaning | Highlight | Primary action |
|---|---|---|---|
| Idle / Stopped | No audio playing. Cursor may point to the last selected/current sentence. | No active highlight, or optional subtle selected sentence only if needed for context. | Play |
| Playing | Audio is active for one sentence. | Current sentence highlighted. Ruby pinyin inside it uses the highlighted sentence styling. | Pause |
| Paused | Audio interrupted by user or OS, cursor preserved. | Current sentence remains highlighted with paused status. | Resume |
| Buffering / Loading | Optional short state while opening generated audio. | Current sentence highlighted. | Disabled primary or spinner in button only if delay is visible. |
| Error fallback | Generated audio failed and system TTS is starting. | Current sentence remains highlighted. | Continue with TTS or show non-blocking unavailable message if TTS also fails. |

### 5.2 Events

| Event | From | To | Notes |
|---|---|---|---|
| `play_read_along` | Idle, Paused | Playing | Starts from current sentence. Auto-continue follows switch. |
| `pause` | Playing | Paused | User pause, app background, audio focus loss, lock screen, call, or headphones disconnected. |
| `resume` | Paused | Playing | May restart active sentence if exact resume is not supported. |
| `stop` | Playing, Paused, Buffering | Idle | Stops both generated audio player and system TTS. |
| `play_sentence(sentence)` | Any | Playing | Stops current audio and plays selected sentence only. |
| `repeat_sentence` | Playing, Paused, Idle | Playing | Replays current sentence only. |
| `next_sentence` | Any | Playing or same transport state | Crosses paragraph boundary if needed. At story end, stops and exposes next CTA. |
| `manual_paragraph_next_previous` | Any | Idle | Stops audio and changes visible paragraph/page. |
| `story_end` | Playing | Idle | No auto-navigation. |

### 5.3 Cursor Rules

- Cursor identity: `(story_id, paragraph_index, sentence_index)`.
- Sentence indices are zero-based internally.
- Current sentence defaults to the first sentence in the current visible paragraph.
- Saved reading position remains paragraph-level for existing progress. Sentence cursor may be local UI state in MVP; persist sentence-level resume later only after product review.
- If content or sentence segmentation changes and the saved cursor is invalid, clamp to the nearest valid paragraph and sentence.

## 6. Sentence Segmentation And Rhythm

Use the existing shared sentence segmenter behavior as the MVP contract:

- Sentence-ending marks: `。`, `！`, `？`, `；`, `…`.
- Consecutive ending marks and trailing right quote/bracket marks attach to the sentence.
- Whitespace-only segments are ignored.
- Audio resources follow `stories/<storyId>/audio/p{paragraph}_s{sentence}.wav` and manifest entries from `audio.json`.

Rhythm:

- Do not begin playback on page open.
- Between sentences: add a small listening breath, target `250-450ms`, unless the generated file already contains a natural tail.
- Between paragraphs: add a slightly longer pause, target `450-700ms`.
- At the last sentence: stop cleanly, do not loop by default.
- If the child taps Next sentence during playback, skip the current audio and start the next sentence immediately.
- If the child taps Stop, do not auto-resume after any pending audio callback.

## 7. Highlight And Ruby

### 7.1 P0 Sentence Highlight

The active sentence is highlighted as one readable unit:

- Use `colorSecondaryContainer` (`#D9F1EE`) for the highlighted sentence surface.
- Use `colorOnSecondaryContainer` (`#063432`) for active text where the implementation supports per-state text color.
- Keep the sentence surface radius at `readingPanelRadius` / `radiusLg` (`16dp`) and padding at `readingPanelPadding` (`16dp`).
- The sentence highlight must include both hanzi and pinyin. If ruby cells are visible, the pinyin and hanzi for the active sentence sit inside the same highlighted surface.
- If pinyin is hidden, the same sentence highlight applies to hanzi only.
- Non-active sentence cards use `colorSurfaceVariant` and normal reading text colors.

Current implementation note:

- Android already renders sentence-level surfaces and ruby cells. Engineering should pass highlight state through ruby text colors as needed so pinyin and hanzi clearly read as the active sentence, not just the container.

### 7.2 P1 Word / Character Highlight

Not required for MVP completion:

- Timed word/character highlight requires word-boundary or alignment timestamps.
- Azure Neural TTS word-boundary events or a later alignment pipeline should produce timing aligned to `PinyinCell` or equivalent.
- P1 highlight must preserve the sentence-level highlight as the background and add a smaller active word/character treatment. Do not replace sentence highlight with only a moving cursor.
- P1 analytics must not log text or spoken audio; only anonymous timing/feature metadata if a future event is approved.

## 8. Auto-Scroll And Manual Scroll

### 8.1 Auto-Follow

When playback is active and the user has not manually taken control:

- Scroll the current sentence into view before or as its audio begins.
- Prefer centering the sentence vertically when possible; on small screens, ensure the full sentence surface and speaker button are visible.
- Use subtle motion (`motionMedium` or platform standard). Respect Reduce Motion by jumping or fading without animated scrolling.
- Do not move screen-reader focus automatically on every sentence. Announce status only through accessible control state or polite live region if needed.

### 8.2 Manual Scroll Override

If the user drags/flings the reading list while playback is active:

- Pause auto-follow immediately.
- Continue audio unless the user explicitly pauses; manual scroll means "let me look around", not "stop reading".
- Show a visible, localized affordance: "Back to reading place" / "回到朗读处".
- The affordance must be reachable by keyboard/TalkBack and at least 48dp high.
- Tapping it scrolls to the active sentence and restores auto-follow.
- Auto-follow also restores when the user taps any playback control that changes the current sentence, such as Next sentence or Repeat.

If playback is paused:

- Manual scroll does not change the sentence cursor.
- Tapping a sentence speaker changes cursor to that sentence and plays only it.

## 9. Linked Reading Features

### 9.1 Pinyin Toggle

- Pinyin toggle persists per device using reader settings.
- Toggling pinyin must not stop audio.
- If toggled during playback, the active sentence remains highlighted and the current sentence stays in view.
- Track only `pinyin_toggle` with anonymous properties. Do not log paragraph text or pinyin text.

### 9.2 Text Size

- Text size choices map to existing reading tokens:
  - Small: `typeReadingHanziSmall` / `typeReadingPinyinSmall`.
  - Medium: `typeReadingHanziMedium` / `typeReadingPinyinMedium` default.
  - Large: `typeReadingHanziLarge` / `typeReadingPinyinLarge`.
- Changing text size during playback must keep the active sentence visible after layout reflow.
- Support font scale up to at least `1.3x` without clipping controls.

### 9.3 Tap Word / Character Help

MVP behavior:

- Tapping an existing vocabulary word can open the vocabulary explanation surface or word book entry.
- If audio is playing, opening a vocab surface should pause playback and keep the current sentence cursor.
- Closing the surface returns to the reading page with a clear Resume control.
- Track with existing `vocab_open` using `vocab_id`, `story_id`, and `open_source`; do not log the raw word if a stable id exists.

If per-character help is added later:

- It must remain controlled to the current story.
- Do not turn it into open chat.
- Do not log raw selected text in analytics.

### 9.4 Word Book

- Read-along should support discovery of vocabulary without making word review mandatory before finishing the story.
- A completed audio read-through may surface "Review new words" but should not punish skipping it.
- Word book analytics must remain anonymous and use existing word book/review events when present in code; this spec does not add new read-along event names.

### 9.5 Reading Progress And Completion

- Paragraph/page progress remains the visible reading progress indicator.
- Audio progress can show sentence count within the current paragraph, e.g. "Sentence 2 / 4".
- Listening to audio is not the same as answering comprehension questions.
- A story is considered product-complete only through the existing reading -> vocab -> quiz/completion flow. Audio reaching story end can enable the next step but must not auto-fire `story_complete`.
- Closing the reading page saves paragraph progress but not child identity.

## 10. Audio Source And Fallback

### 10.1 Source Priority

For every sentence:

1. Check `audio.json` / `StoryAudioManifest` for a usable pre-generated segment.
2. If present, play generated audio.
3. If the file is missing, corrupt, unsupported, or playback throws, fall back to system TTS for that same sentence.
4. If system TTS is unavailable, show a calm non-blocking message and keep reading UI usable.

The fallback must not skip the sentence, silently jump ahead, or mark the story complete.

### 10.2 Source Label

The UI may show a small status label:

- Pre-generated audio: user-facing "Recorded" / "录制音频" or later "Story voice" / "故事朗读".
- System fallback: "System voice" / "系统朗读".

The label should be secondary text, not a warning. Children should not feel something went wrong unless audio is actually unavailable.

### 10.3 App And OS Interruptions

MVP does not provide background read-aloud. Pause playback when:

- App goes to background.
- Device locks.
- Incoming call or another app takes audio focus.
- Headphones or Bluetooth audio disconnects.
- Android receives audio becoming noisy.
- The story route is closed or navigated away from.

After interruption:

- State becomes Paused if a current sentence exists.
- Resume requires an explicit user tap.
- Do not auto-resume after calls, unlock, or reconnect.

## 11. Settings Defaults

| Setting | MVP default | Persistence | Notes |
|---|---|---|---|
| Auto-play on page open | Off | Not configurable in MVP | Must remain off. |
| Auto-continue | On for read-along | Per device if exposed | Off means one sentence at a time. |
| Speaking speed | Slightly slow, target `0.90x` | Per device if exposed | Use child-friendly label; avoid "slow learner" wording. |
| Pinyin | On by default today, using existing reader setting | Per device | Aligns with current `DefaultShowPinyinByDefault = true`. |
| Text size | Medium | Per device | Aligns with existing `ReadingTextSize.Medium`. |
| Audio source | Generated first, TTS fallback | Not user-facing setting in MVP | Provider choice is pipeline/config, not child UI. |
| Child voice recording | Off / unavailable | Not configurable in MVP | Future only with privacy review and parent consent. |

## 12. First-Time Guidance

Show a one-time, lightweight guide on the reading page after the first story opens:

- Do not block reading with a full tutorial.
- Use at most two coach marks:
  - Play: "Tap play to hear the story from here." / "点播放，从这里开始听读。"
  - Sentence speaker: "Tap a sentence speaker to hear just that sentence." / "点一句旁边的小喇叭，只听这一句。"
- The guide must have Skip/Done and must not auto-start audio.
- Store guide completion locally/anonymously.
- Do not add a new analytics event for guide views in MVP unless the analytics framework is separately updated.

## 13. Accessibility And Child Safety

### 13.1 Accessibility

- Meet WCAG AA contrast for all text/control states.
- Every audio control must be reachable without drag gestures.
- Minimum touch target is `48dp x 48dp`.
- Buttons use verbs and localized content descriptions: Play, Pause, Resume, Stop, Repeat sentence, Next sentence, Back to reading place.
- Do not rely on color alone for playback state; use icon + label/state text.
- Respect Android font scale and iOS Dynamic Type up to at least `1.3x`.
- Respect Reduce Motion.
- Screen reader semantics:
  - Sentence speaker announces sentence position, e.g. "Play sentence 2 of 4".
  - Current sentence may expose selected/current state.
  - Ruby visual layout should expose the hanzi sentence as readable content, not force the screen reader through every pinyin cell.

WCAG audio-control alignment:

- Because the page does not auto-play audio, it avoids surprise audio on entry.
- Once audio starts, visible Pause and Stop controls are always available.
- If future behavior ever adds auto-playing audio longer than 3 seconds, it must provide pause/stop or independent audio control immediately and pass accessibility review first.

### 13.2 Child Safety

- No punishment, score, streak loss, red failure state, or shame copy for pausing, replaying, slowing down, or using pinyin.
- No scary/battle audio effects; narration should stay warm and calm.
- The product must not ask a child to speak into the microphone in MVP.
- The AI explanation entry remains controlled to the current story and uses the existing off-topic refusal: "这个问题和今天的故事关系不大，我们先回到故事里吧。"
- No child real name, nickname, birthday, exact age, grade, school, photo, voice recording, exact location, contacts, advertising ID, raw story text selection, or raw AI prompt in analytics.

## 14. Analytics

### 14.1 Fixed Event Alignment

MVP read-along must use existing analytics event names only:

- `paragraph_audio_play`
- `pinyin_toggle`
- `vocab_open`
- `quiz_start`
- `quiz_complete`
- `story_complete`
- `story_open`
- `parent_report_open`
- `app_open`
- `ai_explain_request`

Do not add `sentence_audio_play`, `audio_pause`, `audio_stop`, `audio_error`, `read_along_start`, or guide events in MVP unless `docs/analytics/metrics-framework.md` and shared analytics schema are intentionally updated through a separate analytics task.

### 14.2 Audio Event Mapping

Use `paragraph_audio_play` for audio-assisted reading:

Required properties:

- `story_id`
- `paragraph_index`
- `audio_source`

Optional properties already supported by code/schema:

- `sentence_index`
- `playback_speed_bucket`

Product rules:

- Log when a sentence playback attempt starts, not on every progress tick.
- For full read-along, logging each sentence start is acceptable if volume is low and privacy constraints are met; otherwise throttle to first sentence per paragraph for dashboards. The event name stays `paragraph_audio_play`.
- If generated audio fails and system TTS fallback starts, one additional `paragraph_audio_play` with fallback `audio_source` is acceptable to measure fallback rate.
- Canonical `audio_source` values should follow the analytics framework: `recorded` for pre-generated/story audio and `tts` for system TTS. Current Android code uses `generated`; engineering should normalize that value to `recorded` or update the metrics framework deliberately before release.
- `sentence_index` is numeric only; do not include sentence text.
- `playback_speed_bucket` may be `slow`, `default_slow`, or `normal`; do not log exact device voice name if it could become identifying.

### 14.3 Privacy Constraints

Never log:

- Story sentence text, pinyin text, selected raw text, raw AI prompt, raw AI response.
- Child voice, microphone state, or pronunciation score.
- Child name, account id, school, birthday, exact age, precise location, contacts, advertising ID.
- Free-form child feedback.

Dashboards should continue to compute audio-assisted reading from `paragraph_audio_play` and pinyin use from `pinyin_toggle`.

## 15. Visual Requirements For UI Designer

Use the existing Little Mandarin Classics design system:

- Palette: warm rice background, vermilion primary, teal secondary, leaf/success accents.
- Active sentence: teal secondary container; avoid one-note blue/purple audio UI.
- Cards: 16dp reading panel radius is acceptable for the reading surface; do not nest cards.
- Icons: use platform Material/SF rounded audio icons where available. Prefer play, pause, stop, repeat, skip-next, volume icons.
- Text:
  - Chinese story body uses reading hanzi tokens.
  - Pinyin uses reading pinyin tokens and tone marks.
  - Labels must fit English and Simplified Chinese.
- Audio controls:
  - Must wrap cleanly on 360dp phone width.
  - Must preserve 48dp minimum hit targets.
  - Should not cover story text or bottom actions.
- "Back to reading place" affordance:
  - Use a small sticky pill/button near the lower part of the reading viewport or above the audio row.
  - Use secondary container or surface with clear border; it must be visible but not alarming.

## 16. Engineering Requirements

### 16.1 Shared/KMP

- Keep shared pure Kotlin. No `android.*`, UIKit, or JVM-only APIs.
- Continue using `SentenceSegmenter`, `StoryAudioManifest`, `AudioService`, and `TtsService` abstractions.
- Add state fields as needed for:
  - `autoContinue`
  - `playbackSpeed`
  - active `SentencePlaybackTarget`
  - manual scroll follow state on platform UI
- Platform-specific audio focus, media player, and TTS behavior must live behind expect/actual or platform UI code.

### 16.2 Android Compose

Current baseline already includes:

- Sentence segmentation.
- Per-sentence generated audio first, system TTS fallback.
- Current sentence highlight.
- Auto-scroll to active sentence.
- Pause/resume/stop/next sentence controls.
- `paragraph_audio_play` with optional `sentence_index`.

Required gaps to close for this spec:

- Add per-sentence speaker buttons.
- Add repeat current sentence.
- Add explicit auto-continue switch or menu.
- Add speaking speed default/setting and apply it to TTS/generated playback where supported.
- Detect user manual scroll and pause auto-follow.
- Add "Back to reading place" affordance.
- Normalize analytics `audio_source` for generated audio to `recorded` or update the analytics framework intentionally.
- Pause on background/lock/audio focus/headphone disconnect and require explicit resume.
- Ensure ruby text receives active sentence styling, not only a highlighted container.

### 16.3 iOS SwiftUI

- Mirror Android behavior after the full Xcode path is ready.
- Use `AVAudioPlayer` for generated sentence assets and `AVSpeechSynthesizer` for fallback.
- Pause on app background, lock/audio interruption, route change, and headphones disconnect.
- Use String Catalog for labels and accessibility descriptions.

## 17. Implementation Phasing

### Phase 0: Current Baseline

Already represented in current code/report:

- Sentence segmentation.
- Pre-generated sentence audio manifest.
- Generated audio first, TTS fallback.
- Sentence highlight.
- Auto-scroll during playback.
- Basic play/pause/stop/next controls.
- Anonymous audio analytics.

### Phase 1: MVP Completion

Required before calling read-along interaction complete:

- No autoplay on entry verified.
- Listening read-along from current sentence to story end.
- Tap-to-listen per sentence, one sentence only.
- Repeat current sentence.
- Auto-continue default on, with visible control or compact settings.
- Slightly slow default speaking rate.
- Manual scroll disables auto-follow and shows "Back to reading place".
- Pinyin/text-size changes preserve current sentence and do not stop audio.
- Background/lock/call/headphone disconnect pause.
- Analytics event names stay fixed and anonymous.
- UI labels localized in English and Simplified Chinese.

### Phase 2: Quality And P1

- Real Azure zh-CN child/story voice generation for seed stories.
- Audio quality review for tone accuracy, pacing, and child-friendly warmth.
- Word-boundary or alignment timestamps.
- Timed word/character highlight integrated with ruby cells.
- Optional richer speed menu after accessibility testing.

### Future: Follow-After-Me

- Separate privacy/product spec.
- Parent consent and retention model.
- No default child voice collection.

## 18. Acceptance Criteria

### Product / UX

- Opening the reading page is silent until the user taps Play or a sentence speaker.
- Play from sentence 2 of paragraph 3 continues through the rest of the story when auto-continue is on.
- Tapping a sentence speaker plays only that sentence and stops.
- Repeat replays the current sentence and does not advance.
- Stop clears active playback and does not trigger delayed auto-advance.
- At story end, audio stops and a next-step CTA is visible; no auto-navigation.
- Manual scroll during playback keeps audio going, stops auto-follow, and shows "Back to reading place".
- Tapping "Back to reading place" scrolls back to the active sentence and restores auto-follow.

### Visual / Accessibility

- Current sentence highlight includes hanzi and visible pinyin ruby.
- All audio controls have 48dp hit targets and localized accessibility labels.
- Text and controls do not clip at 360dp width or 1.3x font scale.
- Playback state is communicated by icon and text, not color alone.
- Reduced motion disables animated scroll.

### Engineering

- Generated audio is attempted before system TTS for each sentence.
- Generated audio failure falls back to TTS for the same sentence.
- Route close, app background, audio focus loss, call, lock, and headphone disconnect pause/stop as specified.
- Pinyin toggle and text size changes keep active sentence visible.
- `./gradlew :androidApp:assembleDebug` passes for implementation changes.
- Shared common tests pass if shared state/reducer changes are made.

### Analytics / Privacy

- No new MVP event names are introduced for read-along.
- `paragraph_audio_play` contains only anonymous ids/indices/source/speed bucket.
- No raw story text, pinyin text, child voice, AI prompt, or PII appears in analytics payloads.
- Fallback attempts can be measured without adding a new event.

## 19. Source Links

Interaction and market references from `docs/qa/pm-tts-interaction-report.txt`:

- Du Chinese: https://duchinese.net/
- Du Chinese App Store: https://apps.apple.com/us/app/du-chinese-read-learn-chinese/id1052961520
- Mandarin Companion on Du Chinese: https://mandarincompanion.com/6-best-apps-for-reading-chinese/
- Epic word highlighting: https://support.getepic.com/hc/en-us/articles/360027662011-Do-Read-to-Me-books-offer-word-highlighting
- Epic Read-To-Me/audio: https://support.getepic.com/hc/en-us/articles/204962039-Does-Epic-have-Audiobooks-and-Read-to-Me-books
- Reading Eggs Library: https://readingeggs.com/schools/reading-eggs-library/
- Reading Rockets audio-assisted reading: https://www.readingrockets.org/classroom/classroom-strategies/audio-assisted-reading
- Studycat: https://play.google.com/store/apps/details?id=com.pumkin.funchinese&hl=en_US
- W3C Audio Control: https://www.w3.org/WAI/WCAG21/Understanding/audio-control.html
- FTC COPPA FAQ: https://www.ftc.gov/business-guidance/resources/complying-coppa-frequently-asked-questions

TTS/provider references from `docs/tts-audio-plan.md`:

- Coval TTS provider comparison: https://www.coval.ai/blog/best-text-to-speech-providers-in-2026-how-to-choose-(and-why-vendor-benchmarks-lie)/
- Open-source TTS overview: https://neosophie.com/en/blog/20260317-tts
- Qwen3-TTS report: https://arxiv.org/html/2601.15621v1
- Fish Audio blind test: https://fish.audio/blog/blind-tts-provider-comparison-2026/
- ElevenLabs Mandarin TTS: https://elevenlabs.io/text-to-speech/mandarin-chinese
- Alibaba Model Studio Qwen-TTS/CosyVoice: https://www.alibabacloud.com/help/en/model-studio/realtime-tts-user-guide
