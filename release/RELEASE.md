# Little Mandarin Classics MVP Release Plan

Status: Android release automation is wired for local preflight and GitHub tag releases. This file does not claim that any build, test, archive, or store review step currently passes unless the commands below have actually been run for the target tag.

## Release Scope

Little Mandarin Classics / 小小中文经典 is an MVP for overseas 5-8 year old Chinese heritage and bilingual children. Android is the validation baseline for this iteration. iOS SwiftUI remains a skeleton until full Xcode is available and archive validation can be completed.

MVP scope:

- P0: 15 Three Kingdoms stories, onboarding placement, adaptive reading path, reading page, pinyin, karaoke-style child-voice narration, tap-word dictionary, vocabulary notebook, SRS, next-day review pack, interactive ordering/matching/cloze practice, quiz, completion/progress records, and ability map.
- P1: parent weekly action-plan report, PII-free parent share card, controlled story-scoped AI explanation, AI safety console view/delete for on-device AI logs, read-aloud and retell recording with local-only parent-managed playback/delete, sound effects, feedback, invite code, and basic anonymous analytics.
- Out of scope for this release: open AI chat, community/social features, formal payment/subscription, teacher/classroom workflows, large content backend, and complex gamification.
- Privacy baseline: parent-only accounts, no child real-name/profile PII, child voice never uploaded, local data where claimed, controlled AI only, and anonymous analytics only.

## Version Strategy

- App package / bundle id: `com.littlemandarin.classics`.
- Seed/internal MVP version: start at `0.1.0`.
- Android version values live in `apps/reader/version.properties`; Gradle reads this file for `versionName` and `versionCode`.
- Android `versionCode` follows the MusicFreeAndroid formula: `MAJOR * 10000 + MINOR * 100 + PATCH`, so `v0.1.0` is `100`.
- Android: keep `versionName` aligned to the tag without the leading `v`, and increment `versionCode` monotonically for every uploaded AAB.
- iOS: keep `CFBundleShortVersionString` aligned to product version, and increment `CFBundleVersion` monotonically for every TestFlight upload.
- Release candidates: use `0.1.0-rc.N` in release notes and internal tracking. Store-visible versions should stay numeric where required by platform tooling.
- Tags/commits are handled only at milestone time by the orchestrating agent; this documentation batch must not stage or commit files.

## Required Build And Test Commands

Run from `apps/reader` with the Gradle wrapper that lives at `apps/reader/gradlew`:

```bash
./gradlew :shared:allTests
./gradlew :androidApp:assembleDebug
```

Signed release artifacts use the same `ANDROID_RELEASE_*` signing environment as MusicFreeAndroid. For a local full preflight from the repository root:

```bash
source /Users/zili/code/android/MusicFreeAndroid/.env.release.local
bash scripts/release/preflight.sh v0.1.0
```

Or produce signed release artifacts directly from `apps/reader`:

```bash
./gradlew :androidApp:bundleRelease :androidApp:assembleRelease
```

iOS archive and TestFlight validation are 待完整 Xcode / pending full Xcode.

Do not publish, upload, or mark a store build ready from this document alone. Uploads require the platform checklist evidence in `release/checklist-android.md` or `release/checklist-ios.md`.

## GitHub Tag Release

The executable Android release chain is `.github/workflows/android-release.yml`.

1. Update `apps/reader/version.properties`.
2. Run `source /Users/zili/code/android/MusicFreeAndroid/.env.release.local && bash scripts/release/preflight.sh vX.Y.Z`.
3. Commit the version/docs/app changes.
4. Tag and push:
   ```bash
   git tag vX.Y.Z
   git push origin main
   git push origin vX.Y.Z
   ```
5. CI validates the tag/version match, runs `:shared:allTests` and `:androidApp:assembleDebug`, builds signed AAB/APK artifacts, packages R8 mapping, creates or updates the GitHub Release, prepends `CHANGELOG.md`, and publishes `gh-pages/release/version.json`.

## Release Artifacts

Expected Android artifacts after successful validation:

- Debug APK for smoke testing: `apps/reader/androidApp/build/outputs/apk/debug/androidApp-debug.apk`
- Release AAB for Google Play: `apps/reader/androidApp/build/outputs/bundle/release/*-release.aab`
- Release APK for direct smoke/install and GitHub Release: `apps/reader/androidApp/build/outputs/apk/release/*-release.apk`
- R8 mapping zip on GitHub Release: `mapping-vX.Y.Z.zip`
- Published manifest: `gh-pages/release/version.json`
- Local build record: `versionName`, `versionCode`, AAB path, build timestamp, git revision, signing source name, and release-note draft, without any secret values.

Expected iOS artifacts after full Xcode is ready:

- Shared framework / XCFramework output consumed by SwiftUI.
- Signed iOS archive and TestFlight build.
- Local archive/upload record: `CFBundleShortVersionString`, `CFBundleVersion`, archive timestamp, upload timestamp, git revision, signing source name, and release-note draft, without any secret values.

Required non-binary release artifacts before store submission:

- Signing instructions and local/CI secret flow from `release/signing/README.md`.
- Privacy policy, Google Play Data Safety, App Store privacy labels, and age rating from `release/compliance/`.
- English and Simplified Chinese store listings plus screenshot script from `release/store/`.

## Internal Testing Tracks

Google Play Internal Testing:

- Use the signed release AAB only after `:shared:allTests`, `:androidApp:assembleDebug`, and `:androidApp:bundleRelease` have passed.
- Use the debug APK for local smoke only; Play Internal Testing receives the signed release AAB.
- Limit testers to team accounts and seed families.
- Roll out in stages: team/internal smoke test, then a small seed-family group, then expanded seed group after no blocker issues.
- Include release notes that state this is an invite-code MVP focused on short Chinese classic reading sessions.
- Verify install, launch, 15-story library, onboarding/adaptive path, reading, pinyin/karaoke narration, tap-word dictionary, vocab notebook/SRS/review pack, ability map, interactive practice, quiz, progress, parent report, PII-free share card, AI safety console view/delete, read-aloud and retell recording/playback/delete, feedback, controlled AI explanation, invite code, sound effects, offline/local claims, and anonymous analytics.

TestFlight:

- 待完整 Xcode / pending full Xcode for build, archive, signing, upload, and device validation.
- Do not open external TestFlight until the SwiftUI skeleton is upgraded to functional parity with the Android MVP gates.
- Use internal testers first, then seed families only after App Privacy, age rating, review notes, physical-device microphone/audio tests, and local/offline claim checks are complete.

## Privacy-Sensitive Device Checks

These checks are required for any seed rollout that claims the related feature:

- Read-aloud and retell recordings: verify microphone permission allow/deny flows, record, local playback, delete, re-record, restart persistence, and delete persistence on a physical device.
- Recording privacy: confirm child voice stays on device and is not uploaded, transcribed remotely, sent to AI, sent to analytics, attached to feedback, included in share cards, or written to AI safety logs.
- AI safety console: verify story-scoped AI activity/off-topic refusal logs are viewable, deletable, stored on device, and free of child PII, raw audio, invite codes, account IDs, or precise device identifiers.
- Parent share card: verify generation is parent/user initiated and excludes child real name, child voice, child photo/avatar, account ID, invite code, precise location, device ID, unique tracking token, or raw per-question history.
- Offline/local claims: verify airplane-mode behavior for bundled stories, reading, pinyin, vocabulary, quiz, progress, notebook, SRS due state, ability map, practice, share-card generation, and local recording playback. Do not claim fully offline if AI explanation, feedback, analytics, invite validation, accounts, or other network flows are required.

## Store Review Notes

Release notes and review notes must preserve these facts:

- The app is for child-directed Chinese reading with parent-visible progress, not an open chat product.
- AI explanation is controlled and scoped to the current story. Off-topic questions should be redirected to the story.
- No child real name or child personal identity data is collected.
- Read-aloud and retell recordings are local-only, parent-managed, and child voice is never uploaded.
- The AI safety console supports parent/tester view and delete of on-device AI logs.
- Parent share cards are PII-free and parent/user initiated.
- Local/offline wording must match verified behavior; avoid "fully offline" unless all advertised features pass airplane-mode testing.
- Parent account data, if present, must be minimal and disclosed.
- Analytics must be anonymous and must not include child PII.
- Story content is adapted from public-domain classics, with source notes and safety review.
- Violence in classic war stories is softened into age-appropriate themes such as wisdom, courage, cooperation, respect, and kindness.

## Go / No-Go Gates

Go only if all gates are satisfied:

- Android validation baseline passes: `:shared:allTests`, `:androidApp:assembleDebug`, signed `:androidApp:bundleRelease`, signed `:androidApp:assembleRelease`, and `bash scripts/release/preflight.sh vX.Y.Z`.
- Content passes `content-safety-reviewer` and `story-qa-validator` gates.
- P0 workflows are complete end to end; P1 release commitments are either complete or explicitly removed from store claims.
- Privacy/compliance artifacts are complete and match actual app behavior.
- Signing uses local files or CI secrets only; no keystores, certificates, provisioning profiles, or passwords are committed.
- Store listing, screenshots, age rating, Data Safety, and privacy labels are complete and consistent.
- Controlled AI has no open-chat entry point and no child PII collection.
- Physical-device checks pass for microphone permission, read-aloud/retell recording, local playback, parent-managed delete, and deletion persistence.
- AI safety log view/delete works and logs contain no child PII, raw audio, or precise identifiers.
- Parent share card is verified PII-free.
- Offline/local claims are limited to tested behavior and do not overstate network-dependent AI, feedback, analytics, invite, account, or cloud flows.
- Internal testing has no blocker crashes, content-safety issues, privacy regressions, or data-loss defects.

No-go if any of the following are true:

- Build/test/archive validation has not been run for the platform being released.
- Compliance artifacts are missing or contradict implementation.
- Any flow collects child PII, exposes open chat, or sends non-anonymous child analytics.
- Child voice or raw recording leaves the device, appears in logs/analytics/feedback/share flows, or is sent to AI.
- AI safety logs cannot be viewed/deleted, contain PII/raw audio, or survive delete after restart.
- Share cards include child PII, child voice/photo, invite codes, precise identifiers, or tracking tokens.
- Offline/local claims are broader than tested behavior.
- Signing secrets are present in the repository or release artifacts.
- iOS is proposed for TestFlight or App Store review before full Xcode archive validation.

## Rollback Criteria

Trigger rollback or release halt if internal testing finds:

- Crash or launch failure affecting normal first-session use.
- Incorrect quiz/progress data that misleads parent reports.
- Unsafe story content, age-inappropriate AI output, or open-ended AI behavior.
- Any child PII capture, voice upload, logging, analytics leakage, PII-bearing share card, non-deletable AI safety log, or undisclosed SDK behavior.
- Broken invite-code gating, feedback submission, or account/session behavior that blocks seed testing.

Rollback actions:

- Google Play: pause rollout or deactivate the affected Internal Testing release; upload a fixed AAB with a higher `versionCode`.
- TestFlight: expire the affected build, remove it from tester groups, and upload a fixed build with a higher `CFBundleVersion` after archive validation.
- Server/config: disable affected AI, analytics, invite, or content features if feature flags or remote config exist.
- Content: remove or replace affected story assets, then rerun content safety and QA validation.
