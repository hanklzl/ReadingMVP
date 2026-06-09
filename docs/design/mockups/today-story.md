# Mockup: Today Story

Route: `today`

Purpose: give the child one clear daily reading action and show gentle progress. This is the default first screen.

## Phone Wireframe

```text
+------------------------------------------------+
| 小小中文经典                         [parent] [gear] |
| Little Mandarin Classics                       |
+------------------------------------------------+
|                                                |
| Today / 今日                                   |
|                                                |
| +--------------------------------------------+ |
| | +----------------------+                   | |
| | | cover art            |  桃园三结义        | |
| | | warm Chinese style   |  The Oath of...   | |
| | +----------------------+  Level 1  5-8 min | |
| |                                            | |
| | Progress  [======----]  60%                | |
| |                                            | |
| | [book] Continue reading                    | |
| +--------------------------------------------+ |
|                                                |
| +----------------+  +----------------+         |
| | Words          |  | Quiz           |         |
| | 6 new words    |  | 3 questions    |         |
| +----------------+  +----------------+         |
|                                                |
| Up next / 接下来                                |
| +--------------------------------------------+ |
| | 草船借箭             Borrowing Arrows...    | |
| | [small progress bar]                         | |
| +--------------------------------------------+ |
|                                                |
| [Today]        [Library]        [Parent]       |
+------------------------------------------------+
```

## Layout

- Top app bar uses localized app name. The parent and settings icons are 48dp targets.
- Today story card is the visual anchor. Use `storyCoverHeroMin` to `storyCoverHeroMax` with a square cover.
- Chinese title appears first, English title second and may wrap to two lines.
- Continue/start CTA is the only primary button on the screen.
- Secondary summary tiles show vocabulary count and quiz count. They are flat cards, not nested inside the story card.
- Up Next uses a compact story row to preview the next story in source order.
- Bottom navigation shows top-level routes only.

## Components

- `StoryCardHero`
- `ProgressBar`
- `PrimaryButton`
- `SummaryTile`
- `StoryRowCompact`
- `BottomNavigation`

## Interactions

- Tap primary CTA -> Reading page for today's story.
- Tap cover/title -> same as CTA.
- Tap Words tile -> Vocabulary page for today's story.
- Tap Quiz tile -> Quiz page if reading is complete; otherwise show a localized gentle message that reading comes first.
- Tap Parent icon or bottom Parent -> parent report. Use a parent gate if implemented.
- Progress updates from `ProgressService`.

## Bilingual Considerations

- App title may show one localized name in the app bar, but the landing identity can include both languages if space allows.
- Story titles always show `title_zh` then `title_en`.
- English metadata strings come from resources and must wrap normally.
- Chinese UI in zh-Hans should not shrink; prefer two-line labels when needed.

## Safety And Privacy Notes

- Do not display a child name or avatar.
- Cover image alt text uses the bilingual story title.
- Story preview must avoid violent imagery and weapon emphasis.
