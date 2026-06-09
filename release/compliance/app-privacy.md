# Apple App Privacy Details Draft

Status: store-submission draft for operator/legal review. This document is not legal advice.

Last updated: 2026-06-10

Scope: Little Mandarin Classics / 小小中文经典 MVP baseline for iOS.

Official context checked: [Apple App Privacy Details](https://developer.apple.com/app-store/app-privacy-details/) requires App Privacy answers to disclose data collected by the app and third-party partners, whether data is linked to the user or used for tracking, and to keep answers accurate and current. [Apple App Review Guidelines](https://developer.apple.com/app-store/review/guidelines/) restrict Kids Category apps from sending personally identifiable information or device information to third parties and discourage third-party analytics/advertising except limited compliant cases.

## Product assumptions for this draft

- App serves overseas Chinese/bilingual children ages 5-8 and parents.
- No open chat, user-generated social content, public profiles, third-party ads, behavioral advertising, or cross-app tracking.
- No child real name, child email, child phone, birthday, school, photo, contacts, precise location, IDFA, device ID, or other child identity.
- Anonymous product analytics may collect product interaction events not linked to a child identity.
- Optional parent feedback may collect feedback text, broad child age band, and optional parent contact email.
- Controlled AI explanations are limited to the current story. AI logging, retention, vendor processing, or model-training use must be reviewed before launch.

## Tracking

Draft App Store Connect answer: No, the app does not track users.

Requirements for this answer to remain true:

- Do not use IDFA.
- Do not use third-party advertising SDKs.
- Do not link app data with third-party data for advertising, advertising measurement, data broker use, or cross-app/user tracking.
- Do not transmit device identifiers or device fingerprinting signals to third parties.

## Data linked to the user

### Contact Info - Email Address

Declare only if optional parent feedback/support with contact email ships.

| Field | Draft answer |
|---|---|
| Collected? | Yes, only when a parent chooses to provide an email address for feedback or support. |
| Linked to user? | Yes, linked to the parent support/feedback case. Not linked to a child identity. |
| Purpose | Customer Support. |
| Tracking? | No. |

### User Content - Other User Content

Declare if parent feedback text is retained or reviewed outside the device.

| Field | Draft answer |
|---|---|
| Collected? | Yes, only when a parent submits optional feedback. |
| Linked to user? | Linked to the parent feedback case if parent contact is provided; otherwise not linked. |
| Purpose | Customer Support and App Functionality/Product Improvement. |
| Tracking? | No. |

### Other Data - Child age band in parent feedback

Declare if a parent feedback form asks for child age or age band.

| Field | Draft answer |
|---|---|
| Collected? | Yes, only as a broad age band provided by parent feedback. No birthday. |
| Linked to user? | Only to the parent feedback case if parent contact is provided. |
| Purpose | Customer Support and Product Improvement. |
| Tracking? | No. |

## Data not linked to the user

### Usage Data - Product Interaction

Declare if anonymous analytics are enabled.

| Field | Draft answer |
|---|---|
| Collected? | Yes, anonymous product interaction/progress events. |
| Examples | App open, story open, audio play, pinyin toggle, vocabulary open, quiz completion, story completion, parent report open, AI explain request count. |
| Linked to user? | No. Must not include child identity, parent contact, IDFA, device ID, precise location, or free-form child text. |
| Purpose | Analytics and App Functionality/Product Improvement. |
| Tracking? | No. |

## Data not collected in MVP baseline

- Location: no precise or approximate location.
- Identifiers: no User ID, IDFA, device ID, or other device identifiers.
- Contact Info from children: no child name, child email, child phone, address, or other child contact information.
- Contacts: none.
- Photos or Videos: none.
- Audio Data: none. TTS/audio playback does not record the child.
- Gameplay Content: not applicable.
- Browsing History: none.
- Search History: none.
- Purchases: none in MVP baseline. If subscriptions/IAP are added, update this file and place purchase flows behind a parent gate where required.
- Financial Info: none in MVP baseline.
- Health and Fitness: none.
- Sensitive Info: none.
- Diagnostics: none unless crash/diagnostic reporting is added. If added, disclose crash data/performance data.

## Kids Category suitability notes

Recommended App Store category posture: Kids Category candidate only if final implementation confirms:

- No third-party advertising.
- No IDFA, device identifiers, precise location, child PII, or device information sent to third parties.
- Any analytics is first-party or limited Kids-compliant analytics that does not collect or transmit identifiable information about children, their location, or their devices.
- External links, purchases, subscriptions, account management, and feedback submission are behind an appropriate parent gate.
- Controlled AI has no open chat, no unrestricted web access, no child profiling, and no prompt/response retention unless legally reviewed and disclosed.

## Pre-launch blockers/checks

- Fill in operator legal name, privacy policy URL, privacy contact, and data deletion/contact URL.
- Confirm iOS SDK list and any privacy manifests.
- Confirm whether analytics, feedback, crash reporting, subscriptions, or AI backend are included in the submitted build.
- Confirm any third-party services collect no child PII, no device information, and no tracking data.
- Re-run App Privacy answers whenever data collection, SDKs, AI handling, accounts, or feedback retention changes.
