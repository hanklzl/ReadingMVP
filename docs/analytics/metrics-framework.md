# Analytics Metrics Framework

Date: 2026-06-10  
Scope: MVP seed-user validation for Little Mandarin Classics. Metrics must be computed from anonymous analytics events and local/progress aggregates only.

## Principles

- Measure meaningful child reading habit, not raw app opens.
- Treat an anonymous installation/family as the MVP household proxy. Do not create or transmit child account identifiers.
- Keep analytics minimal: no child real name, school, grade, birthday, photo, exact location, contacts, advertising ID, raw AI prompt text, or free-text feedback in analytics events.
- Use the current event list exactly. If a product metric cannot be computed from these events or local progress aggregates, mark it as future event needed.
- Segment only by non-identifying product context such as app version, platform, locale, story, content level, and seed cohort when the cohort id is non-personal.

## Current Event List

- `app_open`
- `story_open`
- `paragraph_audio_play`
- `pinyin_toggle`
- `vocab_open`
- `quiz_start`
- `quiz_complete`
- `ai_explain_request`
- `story_complete`
- `parent_report_open`

## North Star

**Weekly Meaningful Reading Families**

Number and percentage of anonymous installations/families that complete at least 2 distinct stories in a rolling 7-day window, with a completed quiz for those stories when quiz data is available.

Formula:

`count_distinct(anonymous_install_id where distinct story_complete.story_id >= 2 in 7 days and matching quiz_complete exists for completed stories) / count_distinct(anonymous_install_id with app_open in same acquisition cohort)`

Why this is the North Star:

- It reflects the core promise: a child building a short, repeatable Chinese reading habit.
- It requires completed stories rather than raw opens.
- It is aligned with the MVP success signal: child completes at least 2 stories in the first week.
- It remains anonymous and household-level, avoiding child identities.

MVP target:

- Baseline validation: at least 2 completed stories in first week for a meaningful share of seed families.
- Expansion signal: combine with story completion rate at or above 40% and D7 retention around 30%.

## Metric Tree

| Layer | Metric | Definition | Source | MVP interpretation |
|---|---|---|---|---|
| North Star | Weekly Meaningful Reading Families | Anonymous families with at least 2 distinct `story_complete` events in 7 days, preferably with matching `quiz_complete` | `story_complete`, `quiz_complete`, `app_open` | Core habit signal |
| Activation | First story started | New anonymous installs with `story_open` within 24 hours of first `app_open` | `app_open`, `story_open` | Can families find and start reading? |
| Activation | First story completed | New anonymous installs with `story_complete` within 7 days of first `app_open` | `app_open`, `story_complete` | First value moment |
| Activation | First-week 2-story completion | New anonymous installs with at least 2 distinct `story_complete` events within 7 days | `app_open`, `story_complete` | Required MVP success signal |
| Engagement | Story completion rate | `story_complete` stories / `story_open` stories | `story_open`, `story_complete` | Baseline 40%, excellent 70% |
| Engagement | Reading funnel completion | `app_open` -> `story_open` -> `quiz_start` -> `quiz_complete` -> `story_complete` by cohort | Listed events | Identifies the largest drop-off |
| Engagement | Quiz completion rate | `quiz_complete` / `quiz_start` | `quiz_start`, `quiz_complete` | Checks whether questions block completion |
| Engagement | Quiz accuracy | Sum `correct_count` / sum `question_count` | `quiz_complete`, local progress aggregate | Checks comprehension and story difficulty |
| Engagement | Audio-assisted reading rate | Stories with `paragraph_audio_play` / stories opened or completed | `paragraph_audio_play`, `story_open`, `story_complete` | Validates TTS/audio as a bridge |
| Engagement | Pinyin support use | Stories with `pinyin_toggle` enabled / stories opened | `pinyin_toggle`, `story_open` | Indicates difficulty and support needs |
| Engagement | Vocab support use | Stories with `vocab_open` / stories opened or completed | `vocab_open`, `story_open`, `story_complete` | Indicates word-level support needs |
| Engagement | AI explanation use | Stories with `ai_explain_request` / stories opened or completed | `ai_explain_request`, `story_open`, `story_complete` | Validates controlled AI utility |
| Retention | D1 retention | New anonymous installs with `app_open` on day 1 after first `app_open` | `app_open` | Early habit signal |
| Retention | D7 retention | New anonymous installs with `app_open` or `story_complete` on day 7 after first `app_open` | `app_open`, `story_complete` | Around 30% is expansion signal |
| Retention | Reading days in first week | Distinct local days with `story_complete` within first 7 days | `story_complete`, local progress aggregate | Habit consistency |
| Parent value | Parent report open rate | Anonymous installs with `parent_report_open` / installs with at least one `story_complete` | `parent_report_open`, `story_complete` | Validates parent progress value |
| Parent value | Report-after-reading rate | Anonymous installs with `parent_report_open` within 7 days after first `story_complete` | `parent_report_open`, `story_complete` | Checks whether report supports paid decision |
| Parent value | Weekly report repeat use | Anonymous installs with 2+ `parent_report_open` events in a week | `parent_report_open` | Indicates ongoing parent interest |
| Conversion intent | Willingness to pay | Seed families explicitly expressing willingness to pay / 100 seed families | Manual feedback or future event needed | Required signal: 10/100 seed families |
| Conversion intent | Paid conversion | Paying families / eligible families | Future event needed | Not covered by current MVP event list |

## Lifecycle Metric Definitions

### Activation

- First story started: percentage of new anonymous installs with at least one `story_open` within 24 hours of first `app_open`.
- First story completed: percentage of new anonymous installs with at least one `story_complete` within 7 days of first `app_open`.
- First-week 2-story completion: percentage of new anonymous installs with at least 2 distinct `story_complete.story_id` values within 7 days of first `app_open`.

### Engagement

- Story completion rate: distinct opened story attempts with `story_complete` / distinct `story_open` attempts.
- Quiz completion rate: `quiz_complete` count / `quiz_start` count.
- Quiz accuracy: sum of `quiz_complete.correct_count` / sum of `quiz_complete.question_count`.
- Support use per completed story: stories with audio, pinyin, vocab, or AI support events / stories with `story_complete`.

### Retention

- D1 retention: new anonymous installs with any `app_open` on the first day after first `app_open`.
- D7 retention: new anonymous installs with any `app_open` or `story_complete` on the seventh day after first `app_open`.
- First-week reading days: distinct local days with `story_complete` during the first 7 days.

### Parent Value

- Parent report open rate: anonymous installs with `parent_report_open` / anonymous installs with at least one `story_complete`.
- Report-after-reading rate: anonymous installs with `parent_report_open` within 7 days after first `story_complete` / anonymous installs with at least one `story_complete`.
- Weekly report repeat use: anonymous installs with at least 2 `parent_report_open` events in a 7-day window.

### Conversion Intent

- Willingness to pay: seed families explicitly saying they would pay / 100 seed families. This is future event needed unless tracked manually through privacy-reviewed parent feedback.
- Paid conversion: paying families / eligible families. This is future event needed because no payment event exists in the MVP event list.

## Product Validation Metrics

| Question | Metric | Success signal | Decision threshold |
|---|---|---|---|
| Are children building a habit? | First-week 2-story completion | Child completes at least 2 stories in first week | If low, improve first-run story selection and reduce reading friction before adding content volume |
| Is one story finishable? | Story completion rate | Baseline 40%, excellent 70% | Below 40% means the MVP reading loop is too hard or too long; 70% supports content expansion |
| Do families come back? | D7 retention | Around 30% is an expansion signal | Below 30% means fix habit loop, reminders, story sequence, or parent involvement before scaling acquisition |
| Do parents see value? | Parent report open rate | Must be high enough to show parents care about progress | If low, improve report entry points and report content before pricing tests |
| Is there commercial pull? | Willingness to pay | 10/100 seed families express willingness to pay | Future event needed or manually collected feedback; below target means revisit positioning/pricing |

## Dashboard Specs

### 1. MVP Validation Dashboard

Audience: product, data, founder review.  
Cadence: weekly during seed test, with daily checks after each release.  
Filters: acquisition cohort week, platform, app version, locale, story level.

Tiles:

- Weekly Meaningful Reading Families: count and percentage.
- First-week 2-story completion: percentage of new seed families.
- Story completion rate: overall and by story.
- D7 retention: cohort percentage.
- Parent report open rate: percentage after at least one completed story.
- Willingness to pay: count out of 100 seed families; future event needed unless captured manually.

Charts:

- Cohort table: first app open date vs D1, D3, D7 retention.
- Story completion trend by release/app version.
- Parent report opens per completed-story family.

### 2. Reading Funnel Dashboard

Audience: product, Android, content QA.  
Cadence: daily during MVP QA and first two seed weeks.

Funnel:

`app_open` -> `story_open` -> `quiz_start` -> `quiz_complete` -> `story_complete`

Breakdowns:

- By `story_id`, `story_order`, and `content_level`.
- By platform and app version.
- By whether the story had audio, pinyin, vocab, or AI support usage.

Primary questions:

- Are children opening stories but not starting quizzes?
- Are quizzes started but not completed?
- Are story completions concentrated in only the easiest stories?

### 3. Support Feature Dashboard

Audience: product, content, AI safety.  
Cadence: weekly.

Metrics:

- Audio-assisted reading rate from `paragraph_audio_play`.
- Pinyin enable rate from `pinyin_toggle`.
- Vocab open rate and distinct words reviewed from `vocab_open` plus local aggregate.
- AI explanation request rate from `ai_explain_request`.
- Quiz accuracy by story from `quiz_complete`.

Interpretation:

- High support usage with high completion means support features are working.
- High support usage with low completion means the story may be too hard or support flow may be too disruptive.
- High AI explanation usage should be reviewed against safety outcomes and request types, never raw child text.

### 4. Parent Value Dashboard

Audience: product, growth, privacy review.  
Cadence: weekly.

Metrics:

- Parent report open rate among families with completed stories.
- Report-after-reading rate within 7 days of first completion.
- Weekly report repeat use.
- Local/report aggregates shown in the parent report: stories read, reading days, quiz correct count, words reviewed.

Notes:

- The parent report must remain local/anonymous unless a parent account is added later.
- No child name, birthday, grade, school, photo, or exact location should appear in analytics or report telemetry.

### 5. Seed Conversion Dashboard

Audience: founder, product, growth.  
Cadence: weekly seed-family review.

Metrics:

- Seed family count: manual cohort roster or invite system; future event needed for fully automated coverage.
- Willingness to pay: 10/100 target; collected through parent feedback/interview or future feedback event.
- Price confidence: parent-stated acceptable price; future event needed.
- Paid conversion: future event needed.

## Event-To-Metric Mapping

| Event | Core metrics | Notes |
|---|---|---|
| `app_open` | Acquisition cohort, D1 retention, D7 retention, reading funnel start | Counts raw access but is not a success metric by itself |
| `story_open` | First story started, story completion denominator, reading funnel | Must include story context |
| `paragraph_audio_play` | Audio-assisted reading rate, support usage by story | Helps decide audio/TTS priority |
| `pinyin_toggle` | Pinyin support use, difficulty signal | Capture enabled/disabled state, not child identity |
| `vocab_open` | Vocab support use, words reviewed aggregate | Distinct `vocab_id` can be counted without free text |
| `quiz_start` | Quiz funnel denominator | Indicates transition from reading to comprehension |
| `quiz_complete` | Quiz completion rate, quiz accuracy, meaningful completion confirmation | Use counts, not raw child answers |
| `ai_explain_request` | AI support use, controlled AI demand, safety outcome counts | Do not log raw prompts or free text |
| `story_complete` | North Star, first-week 2-story completion, story completion rate, reading days | Main child reading value event |
| `parent_report_open` | Parent report open rate, report-after-reading rate, parent repeat use | Main parent value event |

## Required Anonymous Properties

Common properties on every event:

| Property | Type | Required | Purpose | Privacy constraint |
|---|---|---:|---|---|
| `event_id` | UUID | Yes | De-duplicate events | Random, not derived from user data |
| `event_name` | enum | Yes | Event routing | Must match the current event list exactly |
| `event_timestamp_utc` | ISO-8601 string | Yes | Cohorts and ordering | UTC only; do not store exact location |
| `anonymous_install_id` | random UUID | Yes | Anonymous cohort, retention, completion | Resettable; not a child account id; not derived from device identifiers |
| `session_id` | random UUID | Yes | Session-level funnel analysis | Reset each app session |
| `platform` | enum | Yes | Android/iOS comparison | `android`, `ios` |
| `app_version` | string | Yes | Release comparison | App version only |
| `schema_version` | integer | Yes | Event compatibility | No personal data |
| `ui_locale` | string | Yes | i18n quality | Locale only, not exact location |

Event-specific required properties:

| Event | Required properties | Optional anonymous properties |
|---|---|---|
| `app_open` | `open_type` (`cold_start`, `foreground`) | `is_first_open`, `days_since_first_open` |
| `story_open` | `story_id`, `story_order`, `content_level`, `open_source` | `previous_story_status` |
| `paragraph_audio_play` | `story_id`, `paragraph_index`, `audio_source` (`tts`, `recorded`) | `playback_speed_bucket` |
| `pinyin_toggle` | `story_id`, `enabled`, `surface` | `paragraph_index` |
| `vocab_open` | `story_id`, `vocab_id`, `open_source` | `content_level` |
| `quiz_start` | `story_id`, `question_count` | `attempt_number` |
| `quiz_complete` | `story_id`, `question_count`, `correct_count` | `attempt_number`, `duration_seconds_bucket` |
| `ai_explain_request` | `story_id`, `request_type`, `safety_outcome` | `target_type`; do not include raw prompt text |
| `story_complete` | `story_id`, `story_order`, `content_level` | `active_reading_seconds_bucket`, `quiz_completed` |
| `parent_report_open` | `entry_point`, `report_period` | `days_since_first_open` |

Local/progress aggregates used by dashboards:

- `stories_read_count`: count of completed stories.
- `reading_days_count`: distinct local days with completed stories.
- `quiz_correct_count` and `quiz_question_count`: aggregate quiz results.
- `words_reviewed_count`: distinct vocabulary items opened/reviewed.
- `story_progress_percent`: local progress by story for the parent report.

These aggregates must be anonymous and should not include child names, account ids, free text, exact location, or school details.

## Privacy Constraints

- Analytics cannot collect child real name, nickname, account identifier, birthday, exact age, grade, school, photo, voice recording, email, phone, address, precise GPS, IP-derived exact location, contacts, advertising ID, or raw typed AI text.
- Parent contact information may only be collected through an explicit parent feedback flow, not analytics events; it must be optional and separate from child progress metrics.
- AI analytics may store controlled metadata such as `request_type` and `safety_outcome`; do not store raw prompts, raw AI responses, or off-topic child questions.
- Geography should be handled outside MVP event payloads unless a future compliance-approved coarse market field is added.
- Do not create leaderboards, social comparison metrics, or public sharing analytics for children.
- Data retention should be short for raw events and longer only for aggregated anonymous metrics.
- Parent report telemetry should confirm usage of the report, not transmit detailed child-identifying profiles.

## Decision Playbook

| Signal | Likely diagnosis | Decision | Impact | Cost |
|---|---|---|---|---|
| Story completion rate below 40% | Stories are too hard, too long, or reading flow has friction | Shorten first stories, improve first-story selection, simplify quiz transition, review content level | High | Medium |
| Story completion rate at or above 70% and D7 near 30% | Core loop is working for seed families | Expand content pipeline and recruit more seed families | High | High |
| First-week 2-story completion is low but first story completion is acceptable | Habit loop is weak after first value moment | Improve next-story handoff, parent report prompt, and story sequence clarity | High | Medium |
| D7 retention below 30% | Families do not have a repeat-use trigger | Improve weekly routine, parent-facing progress prompt, and series continuity; notification metrics would be future event needed | High | Medium |
| Parent report open rate is low | Parents do not see or value progress reporting | Add clearer entry points from Today/settings and make report summary more useful | High for conversion | Low to medium |
| Parent report open rate is high but willingness to pay is below 10/100 | Parents are curious but value proposition or price is weak | Test positioning, pricing, and parent feedback copy before paid launch | High | Low |
| Audio use is high and completion is higher with audio | Audio is a key scaffold for the target age | Prioritize TTS quality, playback reliability, and possibly recorded audio | Medium to high | Medium |
| Pinyin enable rate is high and quiz accuracy is low | Reading level may exceed ability | Lower initial level, add easier first stories, review vocabulary burden | High | Medium |
| Vocab open rate is high but completion is low | Vocabulary support may interrupt reading or content has too many hard words | Reduce vocabulary load, improve inline definitions, check story difficulty | Medium | Medium |
| AI explanation requests are high | Controlled AI may be valuable if safe and focused | Invest in answer quality and safety review; keep raw prompts out of analytics | Medium to high | Medium |
| Quiz completion drops after `quiz_start` | Quiz is frustrating or unclear | Simplify question language, improve feedback, validate answer consistency | High | Low to medium |
| Willingness to pay reaches 10/100 seed families | Commercial pull exists | Prepare paid-plan experiment; subscription/payment events are future event needed | High | Medium to high |

## Future Event Needed

The current MVP event list does not cover every product-learning question. Do not infer these metrics from unrelated events.

| Metric | Why current events are insufficient | Future coverage |
|---|---|---|
| Willingness to pay | No current event records parent feedback or stated purchase intent | Feedback submission event or manual seed-family interview tracking |
| Paid conversion | No subscription, checkout, or purchase event exists | Payment/subscription events after monetization is implemented |
| Invite acceptance or seed-family denominator | No invite event exists | Invite accepted/redeemed event or manual cohort roster |
| Notification/reminder performance | No notification sent/opened event exists | Notification sent/opened/tapped events if reminders are added |
| Parent feedback sentiment | No feedback submission event exists | Feedback event or separate feedback database with privacy review |
| Parent gate completion | Only `parent_report_open` exists, not gate start/pass/fail | Future parent gate events only if privacy review approves |
