# Mockup: Reading Page

Route: `story/{id}/read`

Purpose: provide a focused Chinese reading experience with optional pinyin, audio, and adjustable type size.

## Phone Wireframe

```text
+------------------------------------------------+
| [x] 桃园三结义                         [audio] |
| [========----------------] 2 / 5               |
+------------------------------------------------+
| Pinyin [on]        Text size [A-] [A] [A+]     |
|                                                |
| +--------------------------------------------+ |
| | hěn jiǔ yǐ qián, zhuō xiàn...              | |
| | 很久以前，涿县贴出榜文，盼望有人一起保护乡里。 | |
| |                                            | |
| | liú bèi dú wán bǎng wén...                 | |
| | 刘备读完榜文，心里想着百姓过安稳日子。        | |
| +--------------------------------------------+ |
|                                                |
| [prev]                              [Next]     |
+------------------------------------------------+
```

## Large Text State

```text
+------------------------------------------------+
| [x] 桃园三结义                         [audio] |
| [========----------------] 2 / 5               |
+------------------------------------------------+
| Pinyin [on]        Text size [A-] [A] [A+]     |
|                                                |
| 很久以前，涿县贴出榜文，盼望有人一起保护乡里。   |
|                                                |
| 刘备读完榜文，心里想着百姓过安稳日子。          |
|                                                |
| [prev]                              [Next]     |
+------------------------------------------------+
```

## Layout

- Hide global bottom navigation during reading.
- Top app bar includes close/back, Chinese story title, and page-level audio button.
- Progress indicator uses current paragraph/page count, not a competitive score.
- Controls row contains pinyin toggle and font size segmented control.
- Reading content is vertically scrollable. Bottom actions remain reachable and respect safe areas.
- Default content width is full phone width with 16dp side padding; tablets constrain to `readingMaxWidth`.
- MVP may render paragraph-level pinyin above Chinese text. If ruby text is later implemented, keep the same type tokens.

## Components

- `ReadingTopBar`
- `ProgressBar`
- `PinyinToggle`
- `FontSizeSegmentedControl`
- `PinyinTextBlock`
- `IconButton`
- `BottomActionRow`

## Interactions

- Pinyin toggle persists per device.
- Font size choices map to S/M/L reading tokens and persist per device.
- Audio button invokes platform TTS for the current paragraph or page.
- Next advances paragraph/page. Last page CTA goes to Vocabulary.
- Close/back returns to Today or Library and saves progress.

## Bilingual Considerations

- Reading body is Chinese content data.
- Pinyin uses tone marks and must not be uppercased.
- Control labels are localized platform strings.
- In English UI, "Text size" may wrap; controls must not shrink below 48dp.

## Safety And Privacy Notes

- No free-form AI chat entry on this page in MVP.
- Audio/TTS should read only current story content.
- Progress saved locally or through privacy-reviewed storage only.
