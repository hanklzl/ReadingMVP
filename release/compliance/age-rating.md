# Age Rating and Kids/Family Policy Draft

Status: store-submission draft for operator/legal review. This document is not legal advice.

Last updated: 2026-06-10

Scope: Little Mandarin Classics / 小小中文经典 MVP baseline for Google Play and Apple App Store.

Official context checked: [Google Play Families policy](https://support.google.com/googleplay/android-developer/answer/9893335), [Apple App Review Guidelines](https://developer.apple.com/app-store/review/guidelines/) for Kids Category, and [Apple age-rating definitions](https://developer.apple.com/help/app-store-connect/reference/app-information/age-ratings-values-and-definitions). Apple defines 4+ as apps with no objectionable material; 9+ may include infrequent or mild cartoon/fantasy violence, profanity, crude humor, mature/suggestive content, horror, or fear themes.

## Product facts

- Audience: overseas Chinese/bilingual children ages 5-8 and parents.
- Experience: daily 5-8 minute Chinese classic story reading with pinyin, audio/TTS, vocabulary, three comprehension questions, progress, parent report, and controlled AI explanation.
- First content: child-safe Three Kingdoms adaptations.
- Safety redlines: no graphic violence, horror, adult content, open chat, social sharing, third-party ads, child PII, location, contacts, photos, ad ID, or device IDs.
- Conflict in classic stories must be adapted into wisdom, courage, cooperation, kindness, and cultural learning.

## Apple age rating recommendation

Recommended target: 4+, provided the final story corpus contains no objectionable material and all Three Kingdoms conflict is non-graphic, non-frightening, and framed as age-appropriate moral/cultural storytelling.

Fallback: 9+ if final App Store questionnaire or content review determines that any story includes infrequent or mild fantasy/cartoon violence, weapons, fear themes, or other content not suitable for children under 9.

To preserve 4+:

- Avoid visible injury, blood, death details, threats, executions, torture, frightening imagery, or realistic battle scenes.
- Avoid profanity, crude humor, romance/adult themes, alcohol/tobacco/drug references, gambling, horror, and fear themes.
- Avoid unrestricted web access, user-generated content, direct messaging, or open AI chat.
- Keep AI answers short, current-story-only, child-safe, and able to refuse off-topic prompts.
- Keep all external links, purchases, subscriptions, account setup, and feedback submission behind a parent gate if added.

## Apple Kids Category recommendation

Recommended posture: candidate for Kids Category only after final implementation confirms:

- No third-party ads.
- No third-party analytics unless it fits Apple's limited Kids Category allowance and does not collect/transmit identifiable child information, location, or device information.
- No PII or device information sent to third parties.
- Parent gates protect external links, purchases, subscriptions, account management, and feedback.
- Privacy policy and App Privacy Details match the final build.

Suggested age band: 6-8 for the App Store Kids Category if the store form requires a narrower band. The product may still describe the learning audience as 5-8 in parent-facing copy only if final content is appropriate for age 5.

## Google Play target audience and Families recommendation

Recommended Google Play posture:

- Declare children as a target audience.
- Select the age group(s) that accurately match the submitted build. If Play Console requires covering age 5 explicitly, select the group that includes age 5 only if every story and interaction is suitable for younger children in that group. Otherwise narrow launch positioning to 6-8 for store submission and revisit age-5 marketing after review.
- Opt into Families/Teacher Approved only if final build satisfies all Families requirements.

Required answers/practices:

- Content accessible to children is child-appropriate.
- Play Console Target Audience and Content, Data Safety, and IARC answers are accurate.
- No `AD_ID` permission.
- No AAID, Android ID, SIM Serial, Build Serial, BSSID, MAC, SSID, IMEI, IMSI, or other device identifier transmission.
- No location permission and no precise location collection/transmission.
- No third-party ads.
- Any SDK/API is approved or suitable for child-directed services and does not collect child PII, device identifiers, precise location, or ad data.
- No social sharing, open messaging, unrestricted web access, or free-form child exchange of personal information.

## IARC/content questionnaire guidance

Use the final reviewed content, not product intent, when answering.

Draft answers for MVP baseline if content safety review confirms all stories meet redlines:

- Violence: None, if stories only discuss strategy/cooperation without depictions of injury, combat, weapons use, or frightening battle detail.
- Fear/Horror: None.
- Sexuality/Nudity: None.
- Profanity/Crude Humor: None.
- Alcohol/Tobacco/Drug References: None.
- Gambling/Simulated Gambling/Loot Boxes: None.
- User-generated content/social interaction: None.
- Unrestricted web access: None.
- Advertising: None.
- Location sharing: None.

Use "infrequent/mild cartoon or fantasy violence" rather than "none" if any shipped story depicts battle, weapons, fighting, or injury, even in softened language. This may raise the resulting age rating and should trigger content revision if the product wants to stay suitable for ages 5-8.

## Content review gate

Before submission, the final story corpus must pass content-safety and story-QA review for:

- 300-600 Chinese characters per story, level 1-3, age-appropriate wording.
- No graphic violence, gore, horror, adult content, or frightening details.
- Positive values: wisdom, courage, cooperation, kindness, respect, persistence.
- Three comprehension questions that do not reward violent framing.
- AI explanation prompts/answers that cannot introduce mature or frightening details.

## Pre-launch blockers/checks

- Final content corpus and AI answer style must be reviewed against the 4+ target.
- Final Play Console target age groups must be chosen by the operator; current product audience is 5-8 but store groupings may require a narrower launch posture.
- Confirm final SDKs, permissions, external links, feedback, purchases/subscriptions, account flows, and AI backend behavior before answering store questionnaires.
- Final legal/operator review is required before public submission.
