# Store Screenshot Shot List

## Principles

- Use real app screens or production-faithful mockups.
- Use bilingual overlay captions: English first, Simplified Chinese second.
- Keep every screenshot child-safe: no bloody scenes, weapon close-ups, fear-based copy, public rankings, or child identity.
- Do not show a free-form AI chat box. AI screenshots should show scoped actions tied to the current story.
- Use non-identifying sample progress. Do not show a child name, photo, school, birthday, email, or location.

## Frame 1: Story Library

**Screen:** `library`

**Goal:** Show the ordered Three Kingdoms story series, bilingual titles, level, and progress.

**Caption EN:** Classic Chinese stories, one short read at a time

**Caption ZH:** 中文经典故事，每天轻松读一篇

**Setup Notes:** Show “桃园三结义 / The Oath of the Peach Garden” in progress, “草船借箭 / Borrowing Arrows with Boats” not started, and “空城计 / The Empty Fort Strategy” completed. Use Level 1-3 filter chips and a visible Continue / Start action.

**Privacy / AI-Safe Copy:** Progress is local or anonymous. Do not show a child profile, leaderboard, or social sharing.

## Frame 2: Reading With Pinyin And Audio

**Screen:** `story/{id}/read`

**Goal:** Show the core reading experience: Chinese text, pinyin toggle, progress, audio/TTS, and readable layout.

**Caption EN:** Pinyin and audio help children start reading

**Caption ZH:** 拼音和朗读，帮助孩子开始读中文

**Setup Notes:** Use the “桃园三结义” reading page. Show pinyin on for the first paragraph, the audio icon in the top bar, and page progress such as `2 / 5`. Keep the text large enough for phone screenshots.

**Privacy / AI-Safe Copy:** Audio reads only the current story content. No AI entry point is shown on this reading frame.

## Frame 3: Pinyin Off / Larger Chinese Text

**Screen:** `story/{id}/read`

**Goal:** Communicate that pinyin can be turned off as children grow more confident.

**Caption EN:** Turn pinyin off when your child is ready

**Caption ZH:** 孩子准备好了，就关掉拼音读一读

**Setup Notes:** Use the same story with pinyin off and a slightly larger Chinese text setting. Show the pinyin toggle clearly in the off state and keep previous / next controls visible.

**Privacy / AI-Safe Copy:** Avoid competitive scoring language. Progress should look like reading progress, not a public performance score.

## Frame 4: Vocabulary Cards

**Screen:** `story/{id}/vocabulary`

**Goal:** Show bilingual support for new words in context.

**Caption EN:** Story words with pinyin and English meaning

**Caption ZH:** 生字配拼音和英文释义

**Setup Notes:** Show a word card such as “榜文 / bǎng wén / public notice” with the example sentence “涿县贴出榜文。” and an audio button. Include `1 / 6` or dot progress.

**Privacy / AI-Safe Copy:** Use approved story vocabulary only. Avoid negative learning states such as “forgot” or “wrong word.”

## Frame 5: Controlled Story Explanation

**Screen:** Story explanation bottom sheet or modal from a story page

**Goal:** Show AI as a bounded explanation tool, not open chat.

**Caption EN:** Ask about this story, not open chat

**Caption ZH:** 只解释当前故事，不开放聊天

**Setup Notes:** Show suggested actions like “Explain this sentence,” “What does 仁义 mean?” and “Help me understand this question.” The sample answer should be short, age-appropriate, and tied to the current story.

**Privacy / AI-Safe Copy:** Do not show a blank free-text chat composer. Include a subtle boundary message such as “Story questions only / 只回答和本故事有关的问题.”

## Frame 6: Three Gentle Questions

**Screen:** `story/{id}/quiz`

**Goal:** Show the exact 3-question comprehension check and supportive feedback.

**Caption EN:** Three quick questions after each story

**Caption ZH:** 每篇故事 3 道理解题

**Setup Notes:** Use the question “刘备看到榜文后最想做什么？” with the correct option “保护乡里.” Show the feedback state with a gentle explanation from the story text.

**Privacy / AI-Safe Copy:** Feedback should be neutral, such as “Let’s look back at the story.” Do not show shame-based messages or public score comparisons.

## Frame 7: Story Complete And Retell

**Screen:** `story/{id}/quiz` completion

**Goal:** Show completion, score, and family retell prompt.

**Caption EN:** Finish, then retell the story together

**Caption ZH:** 读完以后，一起复述故事

**Setup Notes:** Show “Story Complete / 完成啦,” a simple `3 / 3` result, and a retell prompt such as “说说刘备、关羽、张飞为什么愿意结为兄弟。” Include Read again and Done buttons.

**Privacy / AI-Safe Copy:** Store score only as private progress. Do not show child name, ranking, sharing, or badges that imply public competition.

## Frame 8: Parent Report

**Screen:** `parent-report`

**Goal:** Show parent-visible progress and privacy reassurance.

**Caption EN:** Parents can see reading progress

**Caption ZH:** 家长可以看到阅读进度

**Setup Notes:** Show weekly aggregate metrics: Stories read, Reading days, Quiz correct, Words reviewed. Include story progress rows for the Three Kingdoms stories and the visible privacy note “No child name or personal details are required.”

**Privacy / AI-Safe Copy:** Use aggregate, non-identifying data only. Do not show parent email, child identity, location, school, birthday, or photo.
