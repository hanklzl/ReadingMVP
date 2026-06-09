# Android Release Checklist

Status: checklist only. Do not mark validation complete unless the commands have actually been run from `apps/reader`.

## Release Metadata

- [ ] Confirm app id is `com.littlemandarin.classics`.
- [ ] Set `versionName` for the MVP release, starting at `0.1.0` unless a later milestone changes it.
- [ ] Increment `versionCode` for every AAB uploaded to Google Play.
- [ ] Confirm Android is the validation baseline for the MVP.
- [ ] Confirm UI languages include English and Simplified Chinese resources, with no hardcoded user-facing UI copy.

## MVP Scope Gate

- [ ] Story library is present.
- [ ] Reading page is present.
- [ ] Pinyin toggle is present.
- [ ] Vocabulary cards are present.
- [ ] Audio /朗读 is present.
- [ ] Quiz flow has exactly 3 questions per story.
- [ ] Completion/progress records are present.
- [ ] Parent report is present or store claims are adjusted.
- [ ] Feedback entry is present or store claims are adjusted.
- [ ] Controlled AI explanation is present or store claims are adjusted.
- [ ] Invite code and basic anonymous analytics are present or store claims are adjusted.
- [ ] No open AI chat, community, child messaging, or child profile PII flow exists.

## Content And Child Safety Gate

- [ ] Story JSON matches `content/schema/story.schema.json`.
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
- [ ] Third-party SDK behavior is inventoried and disclosed before upload.

## Signing Gate

- [ ] Follow `release/signing/README.md`; do not modify it from this release-manager checklist.
- [ ] Android keystore/upload key is generated or available outside the repository.
- [ ] Play App Signing decision is recorded.
- [ ] Signing passwords and key files are stored only in local files or CI secrets.
- [ ] Verify no `*.keystore`, `*.jks`, `*.p12`, `*.mobileprovision`, passwords, or signing secrets are tracked.
- [ ] Release signing configuration reads secrets from environment or local-only files.

## Build Validation

Run from `apps/reader`:

```bash
./gradlew :shared:allTests
./gradlew :androidApp:assembleDebug
```

After signing is configured:

```bash
./gradlew :androidApp:bundleRelease
```

- [ ] `:shared:allTests` passes.
- [ ] `:androidApp:assembleDebug` passes.
- [ ] `:androidApp:bundleRelease` passes with release signing.
- [ ] Debug APK is available for smoke testing.
- [ ] Release AAB is available for Google Play Internal Testing.

## Smoke Test

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

## Google Play Internal Testing

- [ ] Create or update Google Play Internal Testing track.
- [ ] Upload signed release AAB.
- [ ] Add seed-family and team tester groups.
- [ ] Add English and Simplified Chinese release notes.
- [ ] Complete main store listing from `release/store/en.md` and `release/store/zh.md`.
- [ ] Add screenshots using `release/store/screenshots.md`.
- [ ] Add privacy policy URL.
- [ ] Complete Data Safety using `release/compliance/data-safety.md`.
- [ ] Complete age rating / target audience / Families-related declarations consistently with child-directed use.
- [ ] Add review notes that state the app has controlled AI explanation only, no open chat, no child PII, and anonymous analytics.

## Go / No-Go

- [ ] Go only after build validation, content gates, compliance gates, signing gates, and smoke testing all pass.
- [ ] No-go if child PII is collected, analytics are not anonymous, AI behaves like open chat, signing secrets are committed, or store disclosures do not match implementation.

## Rollback

- [ ] Pause or deactivate the Internal Testing release if a blocker appears.
- [ ] Upload a fixed AAB with a higher `versionCode`.
- [ ] Disable affected AI, invite, feedback, analytics, or content paths through config if available.
- [ ] Remove unsafe or invalid story content and rerun content safety plus QA validation.
