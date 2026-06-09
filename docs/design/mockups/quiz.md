# Mockup: Quiz

Route: `story/{id}/quiz`

Purpose: ask exactly three single-choice comprehension questions and give gentle feedback.

## Question Wireframe

```text
+------------------------------------------------+
| [back] Quiz / 小测验                    1 / 3  |
| [======----------------]                       |
+------------------------------------------------+
| 刘备看到榜文后最想做什么？                      |
|                                                |
| +--------------------------------------------+ |
| | ○ 保护乡里                                  | |
| +--------------------------------------------+ |
| +--------------------------------------------+ |
| | ○ 去摘桃子                                  | |
| +--------------------------------------------+ |
| +--------------------------------------------+ |
| | ○ 独自旅行                                  | |
| +--------------------------------------------+ |
|                                                |
|                                      [Submit]  |
+------------------------------------------------+
```

## Feedback Wireframe

```text
+------------------------------------------------+
| [back] Quiz / 小测验                    1 / 3  |
| [======----------------]                       |
+------------------------------------------------+
| 刘备看到榜文后最想做什么？                      |
|                                                |
| +--------------------------------------------+ |
| | [check] 保护乡里                            | |
| +--------------------------------------------+ |
| 正文说刘备想着让百姓过安稳日子。                |
|                                                |
|                                      [Next]    |
+------------------------------------------------+
```

## Completion Wireframe

```text
+------------------------------------------------+
| Story Complete / 完成啦                         |
+------------------------------------------------+
| [gentle success icon]                           |
| 3 / 3                                           |
|                                                |
| Retell / 复述                                   |
| 说说刘备、关羽、张飞为什么愿意结为兄弟。          |
|                                                |
| [Read again]                       [Done]      |
+------------------------------------------------+
```

## Layout

- One question per screen.
- Progress indicates question number out of exactly 3.
- Options are large, full-width 56dp minimum rows.
- Submit button is disabled until one option is selected.
- Feedback appears after submit. Do not automatically advance before feedback can be read.
- Completion shows score and `retell_prompt`.

## Components

- `QuizTopBar`
- `ProgressBar`
- `QuizOption`
- `PrimaryButton`
- `SecondaryButton`
- `FeedbackMessage`
- `CompletionSummary`

## Interactions

- Tap option -> selected state.
- Submit -> score answer and show correct/incorrect state plus explanation.
- Next -> next question.
- Done -> update progress and return Today.
- Read again -> Reading page at first paragraph.

## Bilingual Considerations

- Question prompt, options, explanation, and retell prompt are Chinese story content.
- UI labels and feedback headers come from resources.
- Do not translate story questions in UI unless the content model later provides bilingual question fields.

## Safety And Privacy Notes

- Feedback is neutral and supportive. Use localized copy like "Let's look back at the story" instead of blame.
- Store quiz score as progress only, not as a public child performance profile.
- No free-text answer submission from children in MVP.
