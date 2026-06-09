# Mockup: Vocabulary

Route: `story/{id}/vocabulary`

Purpose: introduce 5-8 story words with pinyin, English meaning, example sentence, and audio.

## Phone Wireframe

```text
+------------------------------------------------+
| [back] New Words / 生字                 1 / 6  |
+------------------------------------------------+
| 桃园三结义                                      |
|                                                |
| +--------------------------------------------+ |
| | 榜文                              [audio]   | |
| | bǎng wén                                   | |
| | public notice                              | |
| |                                            | |
| | 涿县贴出榜文。                              | |
| +--------------------------------------------+ |
|                                                |
| [I know it]                         [Next]     |
|                                                |
| +--------------------------------------------+ |
| | word dots:  ● ○ ○ ○ ○ ○                    | |
| +--------------------------------------------+ |
+------------------------------------------------+
```

## Review List State

```text
+------------------------------------------------+
| [back] New Words / 生字                        |
+------------------------------------------------+
| +--------------------------------------------+ |
| | 榜文  bǎng wén        public notice [audio] | |
| +--------------------------------------------+ |
| | 结义  jié yì          become sworn friends  | |
| +--------------------------------------------+ |
| | 志同道合              share the same goal   | |
| +--------------------------------------------+ |
|                                      [Quiz]    |
+------------------------------------------------+
```

## Layout

- Start with one large card per word for early-reader focus.
- After completion or on revisit, a compact review list is acceptable.
- Word card uses large Chinese word, pinyin, English meaning, Chinese example, and audio.
- Dot progress is decorative plus accessible text such as localized "1 of 6".
- Bottom Next CTA becomes Quiz after the last word.

## Components

- `VocabularyTopBar`
- `VocabularyCard`
- `AudioIconButton`
- `SecondaryButton`
- `PrimaryButton`
- `StepDots`

## Interactions

- Audio reads the word and example sentence using platform TTS.
- "I know it" marks local familiarity if the data model supports it; otherwise hide for MVP.
- Next advances to next word.
- Back returns to Reading page or Today depending on entry point.

## Bilingual Considerations

- Word and example are Chinese content data.
- Meaning is English content data.
- UI labels are localized resources.
- Long English meanings wrap to two lines; do not reduce below `bodyMedium`.

## Safety And Privacy Notes

- No public leaderboard or social sharing.
- Avoid failure states such as "wrong word" or "you forgot".
- All examples must come from approved story content.
