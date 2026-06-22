# Android Release Checklist

Status: Play internal-testing rollout draft only. Do not mark validation complete unless the commands have actually been run from `apps/reader` and the device smoke checks below have passed.

## Release Metadata

- [ ] Confirm app id is `com.littlemandarin.classics`.
- [ ] Set `versionName` for the MVP release, starting at `0.1.0` unless a later milestone changes it.
- [ ] Increment `versionCode` for every AAB uploaded to Google Play.
- [ ] Confirm `apps/reader/version.properties` matches the release tag, e.g. `v0.1.0` -> `versionName=0.1.0`, `versionCode=100`.
- [ ] Confirm Android is the validation baseline for the MVP.
- [ ] Confirm UI languages include English and Simplified Chinese resources, with no hardcoded user-facing UI copy.
- [ ] Confirm this checklist contains no keystore values, passwords, upload keys, API keys, provisioning details, or publish credentials.

## MVP Scope Gate

- [ ] Fifteen Three Kingdoms MVP stories are present and visible in the story library.
- [ ] Onboarding placement and adaptive reading path are present for first-session seed users.
- [ ] Reading page shows Chinese text with pinyin and karaoke-style child-voice narration where claimed.
- [ ] Tap-word dictionary opens from reading text and connects only to controlled story-scoped AI explanation.
- [ ] Vocabulary notebook, SRS review, and next-day review pack are present.
- [ ] Ability map reflects completed reading, vocabulary, practice, and review activity.
- [ ] Interactive practice supports the shipped ordering, matching, and cloze activities.
- [ ] Quiz flow has exactly 3 questions per story.
- [ ] Completion/progress records are present.
- [ ] Parent weekly action-plan report is present.
- [ ] Parent share card is present and store claims describe it as PII-free.
- [ ] AI safety console can view and delete the on-device AI safety log.
- [ ] Read-aloud and retell recording flows are present, local-only, and parent-managed.
- [ ] Sound effects are present where claimed and can be muted or controlled if the app exposes that setting.
- [ ] Feedback entry is present or store claims are adjusted.
- [ ] Invite code and basic anonymous analytics are present or store claims are adjusted.
- [ ] No open AI chat, community, child messaging, or child profile PII flow exists.

## Content And Child Safety Gate

- [ ] All 15 story JSON files match `content/schema/story.schema.json`.
- [ ] Each MVP story is 300-600 Chinese characters, Level 1-3, and includes pinyin, 5-8 vocab items, exactly 3 single-choice questions, and 1 retell prompt.
- [ ] `content-safety-reviewer` has approved age suitability, softened classic-war details, and positive values.
- [ ] `story-qa-validator` has approved schema validity, pinyin alignment, word count, and answer consistency.
- [ ] Public-domain source notes and source URLs are present for story content.

## Privacy And Compliance Gate

- [ ] `release/compliance/privacy-policy.md` is complete and approved.
- [ ] `release/compliance/data-safety.md` is complete and matches Android behavior.
- [ ] `release/compliance/age-rating.md` is complete and matches child-directed positioning.
- [ ] COPPA / GDPR-K assumptions are reviewed.
- [ ] No child real name or child personal identity data is collected.
- [ ] Parent account data, if any, is minimal and disclosed.
- [ ] Analytics are anonymous and contain no child PII.
- [ ] AI explanation is scoped to the current story and cannot become open chat.
- [ ] AI safety log is stored on device, viewable by the parent/tester console, deletable, and contains no child PII, raw audio, invite codes, account IDs, or precise device identifiers.
- [ ] Read-aloud and retell recordings stay on device, are parent-managed, and are not uploaded, attached to feedback, sent to AI, sent to analytics, shared, or logged.
- [ ] Parent share card contains no child real name, child voice, child photo/avatar, account ID, invite code, precise location, device ID, unique tracking token, or raw per-question history.
- [ ] Play Data Safety disclosures match mic/audio recording, local storage, AI safety logs, analytics, feedback, controlled AI explanation, and any network SDK behavior.
- [ ] Third-party SDK behavior is inventoried and disclosed before upload.

## Signing Gate

- [ ] Follow `release/signing/README.md`; do not modify it from this release-manager checklist.
- [ ] Android keystore/upload key is generated or available outside the repository.
- [ ] ReadingMVP uses the same signing identity as MusicFreeAndroid through the shared `ANDROID_RELEASE_*` variable names.
- [ ] Play App Signing decision is recorded.
- [ ] Signing passwords and key files are stored only in local files or CI secrets.
- [ ] Verify no `*.keystore`, `*.jks`, `*.p12`, `*.mobileprovision`, passwords, or signing secrets are tracked.
- [ ] Release signing configuration reads secrets from environment or local-only files.
- [ ] GitHub Environment `release` contains `ANDROID_RELEASE_KEYSTORE_BASE64`, `ANDROID_RELEASE_STORE_PASSWORD`, `ANDROID_RELEASE_KEY_ALIAS`, and `ANDROID_RELEASE_KEY_PASSWORD`.

## Build Validation

Run from `apps/reader`:

```bash
./gradlew :shared:allTests
./gradlew :androidApp:assembleDebug
```

After signing is configured:

```bash
./gradlew :androidApp:bundleRelease :androidApp:assembleRelease
```

Full local preflight from the repository root:

```bash
source /Users/zili/code/android/MusicFreeAndroid/.env.release.local
bash scripts/release/preflight.sh v0.1.0
```

- [ ] `:shared:allTests` passes.
- [ ] `:androidApp:assembleDebug` passes.
- [ ] `:androidApp:bundleRelease` passes with release signing.
- [ ] `:androidApp:assembleRelease` passes with release signing.
- [ ] `bash scripts/release/preflight.sh vX.Y.Z` passes with the MusicFreeAndroid signing environment sourced.
- [ ] Debug APK is available for smoke testing.
- [ ] Release AAB is available for Google Play Internal Testing.
- [ ] Release APK, mapping zip, and `release/version.json` are available from the tag release workflow.
- [ ] Record `versionName`, `versionCode`, AAB path, build timestamp, git revision, signing source name, and tester release-note draft in local release notes without recording any secret values.
- [ ] Do a physical-device smoke pass before Play upload; emulator-only evidence is not enough for microphone, audio, storage, or share-card claims.

## Smoke Test

- [ ] Fresh install and first launch work.
- [ ] Story library opens and lists all 15 MVP stories.
- [ ] Onboarding placement and adaptive path choose the expected first reading path.
- [ ] Reading page shows Chinese story text correctly.
- [ ] Pinyin toggle works and karaoke highlight follows narration where claimed.
- [ ] Tap-word dictionary shows pinyin and English meaning, then routes only to controlled story-scoped AI explanation.
- [ ] Child-voice narration starts, stops, and survives navigation.
- [ ] Sound effects play only in intended interactions and do not mask narration or recording playback.
- [ ] Vocabulary notebook saves words; SRS and next-day review pack surface due items after restart/date change as designed.
- [ ] Ability map reflects reading, vocabulary, practice, and review progress.
- [ ] Ordering, matching, and cloze practice activities complete and persist results.
- [ ] Quiz scoring and explanations are correct.
- [ ] Completion/progress state persists after app restart.
- [ ] Parent weekly action-plan report reflects completed stories, quiz results, vocabulary/review signals, and recommended next actions.
- [ ] Parent share card is generated by explicit parent action and contains no child PII or unique tracking token.
- [ ] Physical device: read-aloud microphone permission allow/deny flows work; recording, local playback, delete, and re-record all work.
- [ ] Physical device: retell microphone permission allow/deny flows work; recording, local playback, delete, and re-record all work.
- [ ] Physical device: recordings remain playable on device after restart and are removed after parent-managed delete.
- [ ] AI safety console shows story-scoped AI activity/off-topic refusals, deletes the on-device log, and the deletion persists after restart.
- [ ] Feedback flow can submit or queue feedback without child PII.
- [ ] Controlled AI explanation handles story-related questions and rejects off-topic questions.
- [ ] Invite-code path works for seed users.
- [ ] Anonymous analytics events fire only for approved events and contain no child PII.

## Offline / Local Claims Gate

- [ ] Airplane mode: bundled story library, story reading, pinyin, vocabulary definitions, quiz, progress, notebook, SRS due state, ability map, practice, share-card generation, and local recording playback behave as claimed.
- [ ] Airplane mode: controlled AI explanation, feedback, analytics, invite validation, account, or cloud-dependent flows fail gracefully and are not described as offline if they need network.
- [ ] Store listing, release notes, screenshots, and tester notes avoid "fully offline" unless every advertised feature passes offline testing.
- [ ] Local-only wording is limited to verified local behavior, especially read-aloud/retell recordings, AI safety logs, progress, and share-card generation.

## Google Play Internal Testing

- [ ] Create or update Google Play Internal Testing track.
- [ ] Upload signed release AAB only after the build validation and physical-device smoke pass above.
- [ ] Add seed-family and team tester groups.
- [ ] Start with team/internal testers, then small seed-family group, then expanded seed group after no blocker issues.
- [ ] Add English and Simplified Chinese release notes.
- [ ] Complete main store listing from `release/store/en.md` and `release/store/zh.md`.
- [ ] Add screenshots using `release/store/screenshots.md`.
- [ ] Add privacy policy URL.
- [ ] Complete Data Safety using `release/compliance/data-safety.md`.
- [ ] Complete age rating / target audience / Families-related declarations consistently with child-directed use.
- [ ] Add review notes that state the app has controlled story-scoped AI explanation only, no open chat, no child PII, anonymous analytics, local-only read-aloud/retell recording, PII-free parent share card, and parent/tester AI safety log view/delete.
- [ ] Confirm Play Console declarations match `release/compliance/*`, store copy, screenshots, and actual app behavior before upload.

## Go / No-Go

- [ ] Go only after build validation, content gates, compliance gates, signing gates, physical-device smoke testing, offline/local-claim checks, and Play disclosures all pass.
- [ ] Go only if on-device mic recording/playback, parent-managed delete, AI safety log view/delete, PII-free share card, and controlled AI refusal behavior are verified on the uploaded build.
- [ ] No-go if child PII is collected, analytics are not anonymous, AI behaves like open chat, signing secrets are committed, or store disclosures do not match implementation.
- [ ] No-go if recordings leave the device, cannot be deleted, appear in logs/analytics/feedback/share flows, or are sent to AI.
- [ ] No-go if AI safety logs cannot be viewed/deleted, contain PII/raw audio, or survive delete after restart.
- [ ] No-go if share cards contain PII, child voice/photo, invite codes, precise identifiers, or tracking tokens.
- [ ] No-go if offline/local claims are broader than tested behavior or any seed-rollout claim differs from implementation.

## Rollback

- [ ] Pause or deactivate the Internal Testing release if a blocker appears.
- [ ] Upload a fixed AAB with a higher `versionCode`.
- [ ] Disable affected AI, invite, feedback, analytics, or content paths through config if available.
- [ ] Remove unsafe or invalid story content and rerun content safety plus QA validation.
