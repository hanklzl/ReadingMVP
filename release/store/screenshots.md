# Store Screenshot Shot List

## Principles

- Use real app screens or production-faithful mockups.
- Use bilingual overlay captions: English first, Simplified Chinese second.
- Keep every screenshot child-safe: no bloody scenes, weapon close-ups, frightening copy, public rankings, or child identity.
- Do not show a free-form conversation box. AI-related screenshots must show scoped actions tied to the current story.
- Use non-identifying sample progress. Do not show a child name, photo, school, birthday, parent email, location, file path, or voice waveform.
- Keep parent-facing sharing examples PII-free and framed as family updates, not social competition.

## Frame 1: Today + Adaptive Recommendation

**Screen:** `today`

**Goal:** Show the daily entry point: a short Today card, placement-aware recommendation, Level 1-3 path, review amount, and one-tap start.

**Caption EN:** Today’s story, matched to your child’s path

**Caption ZH:** 今日故事，按孩子水平推荐

**Setup Notes:** Show “桃园三结义 / The Oath of the Peach Garden” or the next incomplete story as the recommended card. Include a small line such as “Recommended after placement: Level 1 · Review 5 words” and a “Start reading” action. Keep the session promise at 5-8 minutes. If the card shows story art, use final approved child-safe cover art.

**Privacy / AI-Safe Copy:** No child profile, name, streak competition, exact age, or school context. Recommendation should look adaptive and private, not comparative.

## Frame 2: Reading With Karaoke + Tap-Word

**Screen:** `story/{id}/read`

**Goal:** Show the core reading experience: Chinese story text, pinyin, per-character karaoke highlight, Qwen child-voice audio controls, and a tap-word dictionary sheet.

**Caption EN:** Pinyin, Qwen voice, and tap-word help

**Caption ZH:** 拼音、童声朗读，点词就懂

**Setup Notes:** Use a calm paragraph from “桃园三结义.” Show pinyin on, one highlighted character in the active sentence, play/pause controls, and a bottom sheet for a word such as “仁义 / ren yi / loyalty and kindness.” The sheet can show curated meaning, pinyin, English definition, example sentence, and a bounded “Explain this word” action.

**Privacy / AI-Safe Copy:** Audio reads only story content. Do not show child voice recording controls in this frame. Do not show a free-text input field.

## Frame 3: Vocab Notebook + SRS

**Screen:** `word-book` or vocabulary review view

**Goal:** Show that story vocabulary becomes a private notebook with spaced repetition review.

**Caption EN:** Story words come back at the right time

**Caption ZH:** 生字本按时复习

**Setup Notes:** Show 5-8 approved story words with pinyin and English meaning, due count, and gentle review actions such as “Know it” / “Practice again.” Include progress like “5 words due today” without implying a public score.

**Privacy / AI-Safe Copy:** Use only story vocabulary. Avoid shame words such as “failed,” “weak,” or “bad score.”

## Frame 4: Next-Day Review Pack

**Screen:** `review-pack` or Today pending review pack card

**Goal:** Show mistake remediation as a short next-day pack: missed-question support plus due words.

**Caption EN:** A gentle review pack for tomorrow

**Caption ZH:** 次日复习包，温和补一补

**Setup Notes:** Show a pack with sections such as “Review 3 words,” “Reread one sentence,” and “Try one question again.” Use supportive copy tied to the story, for example “Let’s look back at why Liu Bei wanted to help the village.”

**Privacy / AI-Safe Copy:** No failure framing, leaderboards, public scores, or comparison to other children.

## Frame 5: Practice Round

**Screen:** `play-more-practice`

**Goal:** Show low-pressure interactive practice generated from the story.

**Caption EN:** Practice with ordering, matching, and cloze

**Caption ZH:** 排序、匹配、填空，边玩边练

**Setup Notes:** Show one compact round with sentence ordering, word matching, or cloze. If possible, include a light sound-effect state such as a check mark and gentle feedback after a correct answer.

**Privacy / AI-Safe Copy:** Do not show public points, badges, or competitive ranks. Keep all practice text from approved story content.

## Frame 6: Ability Map

**Screen:** `ability-map`

**Goal:** Show the story-driven ability map across reading dimensions.

**Caption EN:** See what each story is helping practice

**Caption ZH:** 看见故事练到了哪些能力

**Setup Notes:** Show dimensions such as character recognition, word meaning, sentence reading, listening, comprehension, retelling, and culture. Highlight recently practiced dimensions and a private progress fraction.

**Privacy / AI-Safe Copy:** Avoid percentile, grade-level, public ranking, or diagnostic medical/clinical language.

## Frame 7: Parent Weekly Action Plan + Share Card

**Screen:** `parent-report` / weekly action plan

**Goal:** Show parent-visible progress that turns into a practical next step.

**Caption EN:** A weekly plan parents can act on

**Caption ZH:** 每周行动计划，家长看得懂

**Setup Notes:** Show weekly aggregate cards: stories read, reading days, words reviewed, quiz support, mastered words, needs-practice words, suggested next step, and retell prompt. Include a PII-free share card preview with non-identifying text such as “This week: 3 stories read · 12 words reviewed.”

**Privacy / AI-Safe Copy:** Do not show parent contact info, child name, location, school, photo, exact age, or share destinations. The share card must contain progress only.

## Frame 8: Parent Safety Console + Local Recordings

**Screen:** parent safety / privacy controls

**Goal:** Show parent control over AI safety logs and local read-aloud or retell recordings.

**Caption EN:** Parents can review and delete local records

**Caption ZH:** 家长可查看和删除本地记录

**Setup Notes:** Show the parent AI safety console with a small on-device log list such as “Explained word: 仁义” and actions to delete entries. Show local recording management as counts or rows without file names, for example “Read-aloud recordings: 2 · Retell recordings: 1,” with delete controls.

**Privacy / AI-Safe Copy:** Do not show raw child free text, voice waveform, file path, uploaded status, or any child identifier. Include local-only wording for recordings and AI log.

## Screens Needing Real Art / Cover Images

- Frame 1 and any story-card crop in Frame 2 still need final approved story cover art or illustrations. The current repo has cover prompts / placeholder rendering, so final store screenshots should not use placeholder initial-letter covers.
- Cover art must be child-safe: no blood, injury, weapon close-ups, frightening battlefield scenes, adult-styled violence, or embedded text that could conflict with localized overlay captions.
- If real cover art is not ready, use production-faithful UI screenshots that crop away the cover area or mark the screenshot as a draft for internal review only.
