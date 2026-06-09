# Google Play Data Safety Draft

Status: store-submission draft for operator/legal review. This document is not legal advice.

Last updated: 2026-06-10

Scope: Little Mandarin Classics / 小小中文经典 MVP baseline for Android.

Official context checked: [Google Play Data Safety](https://support.google.com/googleplay/android-developer/answer/10787469) says developers are responsible for complete and accurate declarations, apps that collect no user data still need the form and privacy policy, and third-party SDK collection/sharing must be reviewed. [Google Play Families policy](https://support.google.com/googleplay/android-developer/answer/9893335) requires accurate target-audience answers, child-appropriate content, disclosure of children's personal/sensitive data collection, no AAID/device-ID transmission for solely child-targeted apps, and no precise location collection for solely child-targeted apps.

## Product assumptions for this draft

- Target audience: overseas Chinese/bilingual children ages 5-8 and parents.
- No child real name, child email, child phone number, birthday, school, grade, photo, contacts, precise location, ad ID, Android ID, or other device identifiers.
- No third-party ads, behavioral ads, cross-app tracking, social features, direct messaging, or unrestricted web access.
- Anonymous product analytics may collect app interaction/progress events that are not linked to a child identity.
- Optional parent feedback may collect parent-provided feedback text and optional parent contact email.
- Controlled AI explanation must be current-story only. Persistent AI logging, model-training use, or vendor retention is a pre-launch review gate.
- If future parent accounts, cloud sync, subscriptions, crash reporting, analytics SDKs, or AI backend logging are added, this file must be updated before Play submission.

## Data Safety form summary

| Question | Draft answer |
|---|---|
| Does the app collect or share any of the required user data types? | Yes, if anonymous analytics and/or optional parent feedback are enabled. No child PII is collected in the MVP baseline. |
| Is all user data collected by the app encrypted in transit? | Yes for any network submission, including analytics, feedback, and AI requests if enabled. Confirm TLS in implementation before submission. |
| Do users have a way to request data deletion? | Yes for optional parent feedback/contact via the operator contact channel. Local progress can be removed by deleting/resetting the app. Anonymous aggregate analytics may not be individually identifiable. |
| Does the app share user data with third parties? | No, for the MVP baseline. Service providers that process data only on behalf of the operator must be reviewed and disclosed if Play treats the activity as sharing. |
| Is data processed ephemerally? | AI requests should be real-time/ephemeral unless reviewed otherwise. Analytics and feedback are not ephemeral if retained. |

## Data types to declare

### App activity - App interactions

| Field | Draft answer |
|---|---|
| Collected? | Yes, if analytics are enabled. |
| Shared? | No. |
| Examples | App open, story open, audio play, pinyin toggle, vocabulary open, quiz start/complete, story complete, parent report open, AI explain request count. |
| Purpose | Analytics and app functionality/product improvement. |
| Required or optional? | Collected automatically when analytics are enabled. Provide an opt-out if the product decides to support one. |
| Linked to user? | No. Must not include child identity, parent contact, advertising ID, Android ID, device IDs, precise location, or free-form child text. |
| Used for ads or tracking? | No. |

### App activity - Other user-generated content

| Field | Draft answer |
|---|---|
| Collected? | Yes, only if optional parent feedback is enabled. |
| Shared? | No. |
| Examples | Parent issue description or suggestion text. |
| Purpose | Customer support, app functionality, and product improvement. |
| Required or optional? | Optional and parent-initiated. |
| Linked to user? | If the parent provides contact email, link only to the parent feedback case. Not linked to a child identity. |
| Used for ads or tracking? | No. |

### Personal info - Email address

| Field | Draft answer |
|---|---|
| Collected? | Yes, only if a parent chooses to provide a contact email in feedback or support. |
| Shared? | No. |
| Purpose | Customer support and responding to feedback. |
| Required or optional? | Optional. |
| Linked to user? | Yes, linked to the parent feedback/support request, not to a child identity. |
| Used for ads or tracking? | No. |
| Deletion | Delete on parent request through the operator contact channel. |

### Personal info - Other info

| Field | Draft answer |
|---|---|
| Collected? | Yes, only if parent feedback asks for a child age band. |
| Shared? | No. |
| Examples | Coarse child age band, such as 5-6 or 7-8, provided by parent. No birthday or exact birth date. |
| Purpose | Customer support and product improvement. |
| Required or optional? | Optional or minimize to broad age band. |
| Linked to user? | Only to the feedback case if parent contact is provided. Not linked to a persistent child profile. |
| Used for ads or tracking? | No. |

## Data types to answer "No" for MVP baseline

- Location: no approximate or precise location.
- Personal info: no child name, child email, child phone number, address, race/ethnicity, political/religious beliefs, sexual orientation, or birthday.
- Financial info: no payment data in MVP baseline. If subscriptions/IAP are added, update disclosures.
- Health and fitness: none.
- Messages: no emails, SMS, chat, or in-app messages between users.
- Photos and videos: none.
- Audio files or voice recordings: none. System TTS plays story audio; it does not record the child.
- Files and docs: none.
- Calendar: none.
- Contacts: none.
- App info and performance: none unless crash/diagnostic reporting SDK is added. If added, disclose crash logs/diagnostics.
- Device or other IDs: no AAID, Android ID, IMEI, IMSI, MAC, BSSID, SSID, Build Serial, SIM Serial, or device identifiers.

## Families policy checklist

- Target Audience and Content answers must accurately reflect ages 5-8 and parent-facing areas.
- Content accessible to children must be child-appropriate and must keep Three Kingdoms conflict adapted into wisdom, courage, cooperation, and kindness without graphic violence or fear.
- Do not request `AD_ID`.
- Do not transmit AAID or other device identifiers.
- Do not request location permissions or collect/transmit precise location.
- Do not include third-party ads.
- Any analytics SDK must be approved/suitable for child-directed services and must not collect device identifiers, location, or child PII.
- Parent report and settings must not ask for child name, birthday, school, photo, contact info, or location.
- External links, purchases, subscriptions, or account management must sit behind a parent gate if added.

## Pre-launch blockers/checks

- Fill in operator legal name, privacy contact, public privacy-policy URL, and data deletion request channel.
- Confirm final Android permissions and SDK list.
- Confirm analytics implementation uses no AAID, Android ID, device fingerprinting, precise location, or child PII.
- Confirm whether feedback is shipped; if yes, confirm fields, retention, deletion workflow, and whether any third-party form/mail/database processor is used.
- Confirm AI backend launch status, request fields, logging/retention, vendor terms, and whether prompts/responses are retained.
