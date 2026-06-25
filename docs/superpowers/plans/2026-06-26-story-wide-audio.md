# Story-Wide Audio Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace physical sentence audio slicing with one story-level wav plus sentence time ranges.

**Architecture:** The content pipeline writes `audio/story.wav` and an `audio.json` whose sentence entries point at that shared resource and carry `startMs`, `endMs`, `durationMs`, and per-character karaoke timings relative to the sentence range. Shared KMP parsing exposes those range fields without platform APIs. Android plays a sentence by seeking into the shared story wav and stopping at the sentence end time.

**Tech Stack:** Python `wave` + pytest for content pipeline, Kotlin Multiplatform with kotlinx.serialization and kotlin.test, Android MediaPlayer actual implementation.

---

### Task 1: Pipeline Story-Wide Generation

**Files:**
- Modify: `pipeline/tests/test_transformer.py`
- Modify: `pipeline/transformer/audio.py`

- [ ] **Step 1: Write the failing test**

Add a pytest/unittest case proving `generation_mode="story"` synthesizes once, writes `audio/story.wav`, and each sentence entry has the same `audioPath` with `startMs`/`endMs`.

- [ ] **Step 2: Run test to verify it fails**

Run: `.venv/bin/python -m pytest pipeline/tests/test_transformer.py::CharTimingTest::test_generate_audio_manifest_story_mode_uses_one_story_wav_with_sentence_ranges -q`

Expected: FAIL because `story` is not an accepted generation mode.

- [ ] **Step 3: Write minimal implementation**

Extend `resolve_audio_generation_mode` to accept `story`; synthesize the joined story text once; align the joined text once; map sentence character ranges to absolute `startMs`/`endMs`; copy the story wav to `audio/story.wav`; write sentence entries that share `audio/story.wav` and keep `chars[]` relative to sentence start.

- [ ] **Step 4: Run test to verify it passes**

Run: `.venv/bin/python -m pytest pipeline/tests/test_transformer.py::CharTimingTest::test_generate_audio_manifest_story_mode_uses_one_story_wav_with_sentence_ranges -q`

Expected: PASS.

### Task 2: Validator Support for Shared Audio Ranges

**Files:**
- Modify: `pipeline/tests/test_validator.py`
- Modify: `pipeline/validator/validate.py`

- [ ] **Step 1: Write the failing test**

Add validator coverage for entries using the same `audio/story.wav` with `startMs`/`endMs`; the validator must compare `durationMs` to `endMs - startMs`, validate the shared wav exists, and reject invalid ranges.

- [ ] **Step 2: Run test to verify it fails**

Run: `.venv/bin/python -m pytest pipeline/tests/test_validator.py::StoryValidatorTest::test_audio_manifest_accepts_story_wav_sentence_ranges -q`

Expected: FAIL because validator currently expects each `audioPath` to match `audio/pX_sY.wav` and compares `durationMs` to the whole wav duration.

- [ ] **Step 3: Write minimal implementation**

Allow story-wide manifests when `ttsProfile.generationMode == "story"`; validate relative `audioPath`, shared wav readability, `startMs >= 0`, `endMs > startMs`, `endMs <= wav duration`, and `durationMs == endMs - startMs` within tolerance.

- [ ] **Step 4: Run test to verify it passes**

Run: `.venv/bin/python -m pytest pipeline/tests/test_validator.py::StoryValidatorTest::test_audio_manifest_accepts_story_wav_sentence_ranges -q`

Expected: PASS.

### Task 3: KMP Manifest and Service Contract

**Files:**
- Modify: `apps/reader/shared/src/commonTest/kotlin/com/littlemandarin/classics/shared/story/StoryAudioJsonTest.kt`
- Modify: `apps/reader/shared/src/commonMain/kotlin/com/littlemandarin/classics/shared/story/StoryAudio.kt`
- Modify: `apps/reader/shared/src/commonMain/kotlin/com/littlemandarin/classics/shared/service/AudioService.kt`
- Modify: `apps/reader/shared/src/iosMain/kotlin/com/littlemandarin/classics/shared/service/IosAudioService.kt`

- [ ] **Step 1: Write the failing shared test**

Add a kotlin.test case proving `StoryAudioJson.decodeManifest` parses `startMs`/`endMs` and maps `audio/story.wav` to `stories/<id>/audio/story.wav`.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:allTests`

Expected: FAIL because `StoryAudioSegment` has no start/end range fields.

- [ ] **Step 3: Write minimal implementation**

Add `startMillis` and `endMillis` to `StoryAudioSegment`; parse/encode `startMs` and `endMs`; add range-aware playback to `AudioService` while keeping old sentence-file fallback compatibility.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :shared:allTests`

Expected: PASS.

### Task 4: Android Range Playback Integration

**Files:**
- Modify: `apps/reader/shared/src/androidMain/kotlin/com/littlemandarin/classics/shared/service/AndroidAudioService.kt`
- Modify: `apps/reader/androidApp/src/main/java/com/littlemandarin/classics/MainActivity.kt`

- [ ] **Step 1: Add or update focused tests where existing harness allows**

Use shared tests for range contract and compile checks for Android actual behavior; do not add brittle MediaPlayer unit tests without a Robolectric harness.

- [ ] **Step 2: Implement range playback**

Load the shared resource path from the manifest segment; seek to `startMillis`; poll/stop at `endMillis`; report `currentPositionMillis()` relative to the sentence start so the existing karaoke reducer remains unchanged.

- [ ] **Step 3: Wire reading page to manifest segment**

Use `audioManifest.segmentFor(paragraphIndex, sentenceIndex)` to decide generated audio availability and pass that segment into range-aware playback.

- [ ] **Step 4: Verify Android compile/build**

Run: `./gradlew :androidApp:assembleDebug`

Expected: BUILD SUCCESSFUL.

### Task 5: Full Verification and Merge Back

**Files:**
- Verify changed files only, then merge branch back to `/Users/zili/code/android/ReadingMVP`.

- [ ] **Step 1: Run pipeline tests**

Run: `.venv/bin/python -m pytest pipeline/tests/test_transformer.py pipeline/tests/test_validator.py`

Expected: all tests pass.

- [ ] **Step 2: Run shared tests**

Run: `./gradlew :shared:allTests`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run Android debug build**

Run: `./gradlew :androidApp:assembleDebug`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Merge back**

After verification, merge or fast-forward `feat/story-wide-audio` into the main checkout, preserving user changes and reporting exact verification evidence.
