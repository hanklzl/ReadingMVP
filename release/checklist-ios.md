# iOS Release Checklist

Status: 待完整 Xcode / pending full Xcode. This checklist documents future validation and must not be treated as completed archive evidence.

## Release Metadata

- [ ] Confirm bundle id is `com.littlemandarin.classics`.
- [ ] Set `CFBundleShortVersionString` to the MVP version, starting at `0.1.0` unless a later milestone changes it.
- [ ] Increment `CFBundleVersion` for every TestFlight upload.
- [ ] Confirm iOS SwiftUI is currently a skeleton until full Xcode is ready.
- [ ] Confirm UI strings use String Catalog / platform resources, with English and Simplified Chinese support.

## Build And Archive Validation

All items in this section are 待完整 Xcode / pending full Xcode:

- [ ] Open the iOS project/workspace in full Xcode.
- [ ] Confirm SwiftUI app consumes the KMP shared XCFramework.
- [ ] Build the iOS app for simulator.
- [ ] Build the iOS app for physical device.
- [ ] Run available unit/UI tests.
- [ ] Archive the app successfully.
- [ ] Validate signing, entitlements, and provisioning profiles.
- [ ] Upload a build to App Store Connect.
- [ ] Confirm the uploaded build processes successfully for TestFlight.

## MVP Functional Parity Gate

Do not start external TestFlight until these match the Android MVP baseline:

- [ ] Story library is functional.
- [ ] Reading page is functional.
- [ ] Pinyin toggle is functional.
- [ ] Vocabulary cards are functional.
- [ ] Audio /朗读 is functional.
- [ ] Quiz flow is functional with exactly 3 questions per story.
- [ ] Completion/progress records persist.
- [ ] Parent report is functional or store claims are adjusted.
- [ ] Feedback entry is functional or store claims are adjusted.
- [ ] Controlled AI explanation is functional or store claims are adjusted.
- [ ] Invite code and basic anonymous analytics are functional or store claims are adjusted.
- [ ] No open AI chat, community, child messaging, or child profile PII flow exists.

## Privacy And Compliance Gate

- [ ] `release/compliance/privacy-policy.md` is complete and approved.
- [ ] `release/compliance/app-privacy.md` is complete and matches iOS behavior.
- [ ] `release/compliance/age-rating.md` is complete and matches child-directed positioning.
- [ ] COPPA / GDPR-K assumptions are reviewed.
- [ ] No child real name or child personal identity data is collected.
- [ ] Parent account data, if any, is minimal and disclosed.
- [ ] Analytics are anonymous and contain no child PII.
- [ ] AI explanation is scoped to the current story and cannot become open chat.
- [ ] Third-party SDK behavior is inventoried and reflected in App Privacy labels.

## Signing And Certificates

All build-specific items are 待完整 Xcode / pending full Xcode:

- [ ] Follow `release/signing/README.md`; do not modify it from this release-manager checklist.
- [ ] Apple Developer Team is selected.
- [ ] Bundle identifier is registered.
- [ ] Distribution certificate is available outside the repository.
- [ ] App Store provisioning profile is available outside the repository.
- [ ] Signing assets are stored only in local keychain or CI secrets.
- [ ] Verify no certificates, provisioning profiles, passwords, or signing secrets are tracked.

## App Store Connect And TestFlight

- [ ] Create or update App Store Connect app record.
- [ ] Add English and Simplified Chinese metadata from `release/store/`.
- [ ] Add screenshots using `release/store/screenshots.md`.
- [ ] Add privacy policy URL.
- [ ] Complete App Privacy labels using `release/compliance/app-privacy.md`.
- [ ] Complete age rating consistently with child-directed use.
- [ ] Add review notes that state the app has controlled AI explanation only, no open chat, no child PII, and anonymous analytics.
- [ ] Upload internal TestFlight build only after full Xcode archive validation.
- [ ] Add team internal testers first.
- [ ] Add seed-family external testers only after internal TestFlight smoke testing passes.

## TestFlight Smoke Test

All app-run items are 待完整 Xcode / pending full Xcode until an installable build exists:

- [ ] Fresh install and first launch work.
- [ ] Story library opens and lists MVP stories.
- [ ] Reading page shows Chinese story text correctly.
- [ ] Pinyin toggle works.
- [ ] Vocabulary card content shows pinyin and English meaning.
- [ ] Audio playback starts, stops, and survives navigation.
- [ ] Quiz scoring and explanations are correct.
- [ ] Completion/progress state persists after app restart.
- [ ] Parent report reflects completed stories and quiz results.
- [ ] Feedback flow can submit or queue feedback without child PII.
- [ ] Controlled AI explanation handles story-related questions and rejects off-topic questions.
- [ ] Invite-code path works for seed users.
- [ ] Anonymous analytics events fire only for approved events and contain no child PII.

## Go / No-Go

- [ ] Go for internal TestFlight only after full Xcode build, archive, signing, upload, and smoke testing pass.
- [ ] Go for external TestFlight only after MVP functional parity with Android and compliance review.
- [ ] No-go if archive validation is pending, child PII is collected, analytics are not anonymous, AI behaves like open chat, signing secrets are committed, or App Privacy disclosures do not match implementation.

## Rollback

- [ ] Expire the affected TestFlight build.
- [ ] Remove the build from tester groups.
- [ ] Upload a fixed build with a higher `CFBundleVersion` after full Xcode archive validation.
- [ ] Disable affected AI, invite, feedback, analytics, or content paths through config if available.
- [ ] Remove unsafe or invalid story content and rerun content safety plus QA validation.
