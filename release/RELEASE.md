# Little Mandarin Classics MVP Release Plan

Status: planning document only. This file does not claim that any build, test, archive, or store review step currently passes.

## Release Scope

Little Mandarin Classics / 小小中文经典 is an MVP for overseas 5-8 year old Chinese heritage and bilingual children. Android is the validation baseline for this iteration. iOS SwiftUI remains a skeleton until full Xcode is available and archive validation can be completed.

MVP scope:

- P0: story library, reading page, pinyin toggle, vocabulary, audio, quiz, completion/progress records, and initial story content.
- P1: parent report, feedback, controlled AI explanation, invite code, and basic anonymous analytics.
- Out of scope for this release: open AI chat, community/social features, formal payment/subscription, teacher/classroom workflows, large content backend, and complex gamification.

## Version Strategy

- App package / bundle id: `com.littlemandarin.classics`.
- Seed/internal MVP version: start at `0.1.0`.
- Android: keep `versionName` aligned to product version, and increment `versionCode` monotonically for every uploaded AAB.
- iOS: keep `CFBundleShortVersionString` aligned to product version, and increment `CFBundleVersion` monotonically for every TestFlight upload.
- Release candidates: use `0.1.0-rc.N` in release notes and internal tracking. Store-visible versions should stay numeric where required by platform tooling.
- Tags/commits are handled only at milestone time by the orchestrating agent; this documentation batch must not stage or commit files.

## Required Build And Test Commands

Run from `apps/reader` with the Gradle wrapper that lives at `apps/reader/gradlew`:

```bash
./gradlew :shared:allTests
./gradlew :androidApp:assembleDebug
```

After Android signing is configured, produce the release bundle with:

```bash
./gradlew :androidApp:bundleRelease
```

iOS archive and TestFlight validation are 待完整 Xcode / pending full Xcode.

## Release Artifacts

Expected Android artifacts after successful validation:

- Debug APK for smoke testing: `apps/reader/androidApp/build/outputs/apk/debug/androidApp-debug.apk`
- Release AAB for Google Play: `apps/reader/androidApp/build/outputs/bundle/release/androidApp-release.aab`

Expected iOS artifacts after full Xcode is ready:

- Shared framework / XCFramework output consumed by SwiftUI.
- Signed iOS archive and TestFlight build.

Required non-binary release artifacts before store submission:

- Signing instructions and local/CI secret flow from `release/signing/README.md`.
- Privacy policy, Google Play Data Safety, App Store privacy labels, and age rating from `release/compliance/`.
- English and Simplified Chinese store listings plus screenshot script from `release/store/`.

## Internal Testing Tracks

Google Play Internal Testing:

- Use the signed release AAB only after `:shared:allTests`, `:androidApp:assembleDebug`, and `:androidApp:bundleRelease` have passed.
- Limit testers to seed families and team accounts.
- Include release notes that state this is an invite-code MVP focused on short Chinese classic reading sessions.
- Verify install, launch, story library, reading, pinyin, vocab, audio, quiz, progress, parent report, feedback, controlled AI explanation, invite code, and anonymous analytics.

TestFlight:

- 待完整 Xcode / pending full Xcode for build, archive, signing, upload, and device validation.
- Do not open external TestFlight until the SwiftUI skeleton is upgraded to functional parity with the Android MVP gates.
- Use internal testers first, then seed families only after App Privacy, age rating, and review notes are complete.

## Store Review Notes

Release notes and review notes must preserve these facts:

- The app is for child-directed Chinese reading with parent-visible progress, not an open chat product.
- AI explanation is controlled and scoped to the current story. Off-topic questions should be redirected to the story.
- No child real name or child personal identity data is collected.
- Parent account data, if present, must be minimal and disclosed.
- Analytics must be anonymous and must not include child PII.
- Story content is adapted from public-domain classics, with source notes and safety review.
- Violence in classic war stories is softened into age-appropriate themes such as wisdom, courage, cooperation, respect, and kindness.

## Go / No-Go Gates

Go only if all gates are satisfied:

- Android validation baseline passes: `:shared:allTests`, `:androidApp:assembleDebug`, and signed `:androidApp:bundleRelease`.
- Content passes `content-safety-reviewer` and `story-qa-validator` gates.
- P0 workflows are complete end to end; P1 release commitments are either complete or explicitly removed from store claims.
- Privacy/compliance artifacts are complete and match actual app behavior.
- Signing uses local files or CI secrets only; no keystores, certificates, provisioning profiles, or passwords are committed.
- Store listing, screenshots, age rating, Data Safety, and privacy labels are complete and consistent.
- Controlled AI has no open-chat entry point and no child PII collection.
- Internal testing has no blocker crashes, content-safety issues, privacy regressions, or data-loss defects.

No-go if any of the following are true:

- Build/test/archive validation has not been run for the platform being released.
- Compliance artifacts are missing or contradict implementation.
- Any flow collects child PII, exposes open chat, or sends non-anonymous child analytics.
- Signing secrets are present in the repository or release artifacts.
- iOS is proposed for TestFlight or App Store review before full Xcode archive validation.

## Rollback Criteria

Trigger rollback or release halt if internal testing finds:

- Crash or launch failure affecting normal first-session use.
- Incorrect quiz/progress data that misleads parent reports.
- Unsafe story content, age-inappropriate AI output, or open-ended AI behavior.
- Any child PII capture, logging, analytics leakage, or undisclosed SDK behavior.
- Broken invite-code gating, feedback submission, or account/session behavior that blocks seed testing.

Rollback actions:

- Google Play: pause rollout or deactivate the affected Internal Testing release; upload a fixed AAB with a higher `versionCode`.
- TestFlight: expire the affected build, remove it from tester groups, and upload a fixed build with a higher `CFBundleVersion` after archive validation.
- Server/config: disable affected AI, analytics, invite, or content features if feature flags or remote config exist.
- Content: remove or replace affected story assets, then rerun content safety and QA validation.
