# iOS Release Checklist

Status: 待完整 Xcode / pending full Xcode. This TestFlight seed rollout checklist is documentation-only until full Xcode build, archive, upload, and device validation evidence exists.

## Release Metadata

- [ ] Confirm bundle id is `com.littlemandarin.classics`.
- [ ] Set `CFBundleShortVersionString` to the MVP version, starting at `0.1.0` unless a later milestone changes it.
- [ ] Increment `CFBundleVersion` for every TestFlight upload.
- [ ] Reconcile checked-in Xcode version fields before upload. Current iOS config must be reviewed because `CFBundleShortVersionString` / `MARKETING_VERSION` currently use `1.0` and `CFBundleVersion` / `CURRENT_PROJECT_VERSION` currently use `1`, while this draft release plan recommends seed MVP tracking from `0.1.0`.
- [ ] Confirm iOS SwiftUI is currently a skeleton until full Xcode is ready.
- [ ] Confirm UI strings use String Catalog / platform resources, with English and Simplified Chinese support.
- [ ] Confirm this checklist contains no certificate values, provisioning profile contents, passwords, API keys, or App Store Connect credentials.

## Build And Archive Validation

All items in this section are 待完整 Xcode / pending full Xcode:

- [ ] Open the iOS project/workspace in full Xcode.
- [ ] Confirm SwiftUI app consumes the KMP shared XCFramework.
- [ ] Produce or attach the release-mode shared XCFramework expected by the iOS app.
- [ ] Build the iOS app for simulator.
- [ ] Build the iOS app for physical device.
- [ ] Run available unit/UI tests.
- [ ] Archive the app successfully with App Store distribution settings.
- [ ] Validate signing, entitlements, provisioning profiles, microphone usage string, and privacy manifests.
- [ ] Upload a build to App Store Connect.
- [ ] Confirm the uploaded build processes successfully for TestFlight.
- [ ] Record `CFBundleShortVersionString`, `CFBundleVersion`, archive/upload timestamp, git revision, signing source name, and tester release-note draft without recording any secret values.

## MVP Functional Parity Gate

Do not start external TestFlight until these match the Android MVP baseline:

- [ ] Story library is functional.
- [ ] Reading page is functional.
- [ ] All 15 Three Kingdoms MVP stories are visible.
- [ ] Onboarding placement and adaptive reading path are functional.
- [ ] Pinyin toggle and karaoke-style child-voice narration are functional where claimed.
- [ ] Tap-word dictionary and controlled story-scoped AI explanation are functional.
- [ ] Vocabulary notebook, SRS review, next-day review pack, and ability map are functional.
- [ ] Audio /朗读 and sound effects are functional.
- [ ] Interactive ordering, matching, and cloze practice are functional.
- [ ] Quiz flow is functional with exactly 3 questions per story.
- [ ] Completion/progress records persist.
- [ ] Parent weekly action-plan report and PII-free share card are functional or store claims are adjusted.
- [ ] AI safety console can view and delete the on-device AI safety log or store/TestFlight claims are adjusted.
- [ ] Read-aloud and retell recording flows are local-only, parent-managed, and functional on physical device or store/TestFlight claims are adjusted.
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
- [ ] AI safety log is stored on device, viewable by the parent/tester console, deletable, and contains no child PII, raw audio, invite codes, account IDs, or precise device identifiers.
- [ ] Read-aloud and retell recordings stay on device, are parent-managed, and are not uploaded, attached to feedback, sent to AI, sent to analytics, shared, or logged.
- [ ] Parent share card contains no child real name, child voice, child photo/avatar, account ID, invite code, precise location, device ID, unique tracking token, or raw per-question history.
- [ ] App Privacy labels match microphone/audio recording, local storage, AI safety logs, analytics, feedback, controlled AI explanation, and any network SDK behavior.
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
- [ ] Add review notes that state the app has controlled story-scoped AI explanation only, no open chat, no child PII, anonymous analytics, local-only read-aloud/retell recording, PII-free parent share card, and parent/tester AI safety log view/delete.
- [ ] Upload internal TestFlight build only after full Xcode archive validation.
- [ ] Add team internal testers first.
- [ ] Add seed-family external testers only after internal TestFlight smoke testing passes.
- [ ] Do not invite seed families while iOS remains only a skeleton or before physical-device microphone/audio validation.

## TestFlight Smoke Test

All app-run items are 待完整 Xcode / pending full Xcode until an installable build exists:

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
- [ ] Physical device only: read-aloud microphone permission allow/deny flows work; recording, local playback, delete, and re-record all work.
- [ ] Physical device only: retell microphone permission allow/deny flows work; recording, local playback, delete, and re-record all work.
- [ ] Physical device only: recordings remain playable on device after restart and are removed after parent-managed delete.
- [ ] AI safety console shows story-scoped AI activity/off-topic refusals, deletes the on-device log, and the deletion persists after restart.
- [ ] Feedback flow can submit or queue feedback without child PII.
- [ ] Controlled AI explanation handles story-related questions and rejects off-topic questions.
- [ ] Invite-code path works for seed users.
- [ ] Anonymous analytics events fire only for approved events and contain no child PII.

## Offline / Local Claims Gate

All items are 待完整 Xcode / pending full Xcode until an installable build exists:

- [ ] Airplane mode: bundled story library, story reading, pinyin, vocabulary definitions, quiz, progress, notebook, SRS due state, ability map, practice, share-card generation, and local recording playback behave as claimed.
- [ ] Airplane mode: controlled AI explanation, feedback, analytics, invite validation, account, or cloud-dependent flows fail gracefully and are not described as offline if they need network.
- [ ] App Store metadata, TestFlight notes, screenshots, and review notes avoid "fully offline" unless every advertised feature passes offline testing.
- [ ] Local-only wording is limited to verified local behavior, especially read-aloud/retell recordings, AI safety logs, progress, and share-card generation.

## Go / No-Go

- [ ] Go for internal TestFlight only after full Xcode build, archive, signing, upload, and smoke testing pass.
- [ ] Go for external TestFlight only after MVP functional parity with Android, physical-device microphone/audio validation, offline/local-claim checks, and compliance review.
- [ ] Go only if on-device mic recording/playback, parent-managed delete, AI safety log view/delete, PII-free share card, and controlled AI refusal behavior are verified on the uploaded build.
- [ ] No-go if archive validation is pending, child PII is collected, analytics are not anonymous, AI behaves like open chat, signing secrets are committed, or App Privacy disclosures do not match implementation.
- [ ] No-go if iOS remains only a skeleton, physical-device mic tests are pending, recordings leave the device, cannot be deleted, appear in logs/analytics/feedback/share flows, or are sent to AI.
- [ ] No-go if AI safety logs cannot be viewed/deleted, contain PII/raw audio, or survive delete after restart.
- [ ] No-go if share cards contain PII, child voice/photo, invite codes, precise identifiers, or tracking tokens.
- [ ] No-go if offline/local claims are broader than tested behavior or any TestFlight seed-rollout claim differs from implementation.

## Rollback

- [ ] Expire the affected TestFlight build.
- [ ] Remove the build from tester groups.
- [ ] Upload a fixed build with a higher `CFBundleVersion` after full Xcode archive validation.
- [ ] Disable affected AI, invite, feedback, analytics, or content paths through config if available.
- [ ] Remove unsafe or invalid story content and rerun content safety plus QA validation.
