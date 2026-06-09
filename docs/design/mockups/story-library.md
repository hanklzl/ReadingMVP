# Mockup: Story Library

Route: `library`

Purpose: let children and parents browse the ordered story set, resume progress, and understand difficulty.

## Phone Wireframe

```text
+------------------------------------------------+
| [back?] Story Library / 故事库            [gear] |
+------------------------------------------------+
| [All] [Level 1] [Level 2] [Level 3]            |
|                                                |
| +--------------------------------------------+ |
| | [cover] 桃园三结义                          | |
| |         The Oath of the Peach Garden       | |
| |         Level 1  [======----] 60%          | |
| |         [Continue]                         | |
| +--------------------------------------------+ |
|                                                |
| +--------------------------------------------+ |
| | [cover] 草船借箭                            | |
| |         Borrowing Arrows with Boats        | |
| |         Level 2  Not started               | |
| |         [Start]                            | |
| +--------------------------------------------+ |
|                                                |
| +--------------------------------------------+ |
| | [cover] 空城计                              | |
| |         The Empty Fort Strategy            | |
| |         Level 2  Completed [check]         | |
| +--------------------------------------------+ |
|                                                |
| [Today]        [Library]        [Parent]       |
+------------------------------------------------+
```

## Expanded Layout

```text
+------------------------------------------------------------------+
| Story Library / 故事库                                      [gear] |
+------------------------------------------------------------------+
| [All] [Level 1] [Level 2] [Level 3]                              |
|                                                                  |
| +----------------------------+ +----------------------------+     |
| | [cover] title/progress/CTA | | [cover] title/progress/CTA |     |
| +----------------------------+ +----------------------------+     |
| +----------------------------+ +----------------------------+     |
| | [cover] title/progress/CTA | | [cover] title/progress/CTA |     |
| +----------------------------+ +----------------------------+     |
+------------------------------------------------------------------+
```

## Layout

- Phone: vertical list with one story card per row.
- Tablet/foldable: 2-column grid within `gridMaxWidth`.
- Filter chips stay below the app bar and scroll horizontally if needed.
- Preserve story order from the content list; filters should not randomize order.
- Each card uses cover, Chinese title, English title, level, progress, and one contextual action.

## Components

- `FilterChipRow`
- `StoryCardList`
- `ProgressBar`
- `PrimaryButton` or `SecondaryButton`
- `BottomNavigation` or tablet navigation rail

## Interactions

- Tap filter chip -> update list in place.
- Tap story card -> story detail or reading page depending on implementation scope.
- Tap Start/Continue -> Reading page.
- Completed cards can use a secondary "Read again" action.
- Empty filtered state uses a localized, friendly text and no illustration requirement for MVP.

## Bilingual Considerations

- `title_en` gets up to two lines before truncation.
- The level label is a resource string with numeric level from story data.
- Avoid fixed-width English labels inside chips; chips can grow.

## Safety And Privacy Notes

- Library progress is local or anonymous.
- Do not expose reading streak as public/social status.
- Cover art follows the no blood, no weapon close-up rule.
