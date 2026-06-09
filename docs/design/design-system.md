# Little Mandarin Classics Design System

This design system is the source of truth for the MVP mobile UI of "小小中文经典 / Little Mandarin Classics". It targets overseas bilingual children ages 5-8 and their parents. The visual direction is warm, bright, Chinese storybook inspired, and safe for children.

Implementation constraints:

- UI strings must come from platform resources: Android `res/values*/strings.xml`, iOS String Catalog.
- Shared KMP logic exposes locale-neutral data and resource keys only.
- Story body remains Chinese. Story titles are bilingual. Vocabulary meanings are English.
- No child real names, photos, identifiers, or free-form child profile fields.
- AI affordances, if added later, must be controlled story explanations only, not open chat.

## Visual Principles

- Warm storybook, not heavy historical drama.
- Chinese watercolor and soft ink cues through cover art, mild dividers, and rounded shapes.
- Balanced palette: vermilion, teal, leaf green, warm gold, rice paper, and ink.
- Large readable text, calm density, and obvious next actions.
- Feedback is encouraging and specific. Avoid shame language, scary imagery, and battle emphasis.
- Motion is subtle and optional. Do not use flashing, shaking error states, or surprise effects.

## Color

All text/background pairs used for body text and controls must meet WCAG AA. The MVP default is a light theme. If branded dark tokens are not implemented, use the LMC light color scheme even when the OS is in dark mode; do not fall back to platform/default Material dark colors because they break the storybook palette and contrast assumptions.

| Role | Token | Hex | Use |
| --- | --- | --- | --- |
| Primary | `color.primary` | `#B84535` | Main CTA, selected reading control, active nav |
| On primary | `color.onPrimary` | `#FFFFFF` | Text/icons on primary |
| Primary container | `color.primaryContainer` | `#FFE0D6` | Gentle badges, active chip background |
| On primary container | `color.onPrimaryContainer` | `#3D0E08` | Text/icons on primary container |
| Secondary | `color.secondary` | `#126B68` | Pinyin toggle, library filters, links |
| On secondary | `color.onSecondary` | `#FFFFFF` | Text/icons on secondary |
| Secondary container | `color.secondaryContainer` | `#D9F1EE` | Calm selected surfaces |
| Tertiary | `color.tertiary` | `#8A6100` | Streaks, level, warm highlights |
| Tertiary container | `color.tertiaryContainer` | `#FFF4D8` | Progress and achievement backgrounds |
| Background | `color.background` | `#FFF8EC` | App background, rice paper tone |
| Surface | `color.surface` | `#FFFFFF` | Cards, sheets, app bars |
| Surface variant | `color.surfaceVariant` | `#F3F7F1` | Reading panels, quiet list rows |
| Outline | `color.outline` | `#9AA7A0` | Dividers, inactive option border |
| Text primary | `color.textPrimary` | `#202523` | Main text |
| Text secondary | `color.textSecondary` | `#4F5E58` | Metadata, English subtitle |
| Success | `color.success` | `#3B7A3B` | Correct answer, completed progress |
| Error | `color.error` | `#B3261E` | Incorrect answer, validation |
| Info | `color.info` | `#2B6CA3` | Audio, help, neutral notice |

Contrast notes:

- `#B84535` on white is 5.33:1.
- `#126B68` on white is 6.31:1.
- `#202523` on rice background is 14.73:1.
- `#4F5E58` on rice background is 6.47:1.

Do not place small white text on pale gold, pale teal, or pale red containers. Use the matching "on container" ink color.

## Typography

Use platform-native fallbacks first, with Noto families as cross-platform targets when bundled or available.

| Content | Preferred fonts | Notes |
| --- | --- | --- |
| Chinese UI | `Noto Sans SC`, `PingFang SC`, system sans | Clear simplified Chinese UI labels |
| Chinese story body | `Noto Serif SC`, `Source Han Serif SC`, `Songti SC`, fallback sans | More book-like; fall back to sans if serif is unavailable |
| Pinyin | `Inter`, `SF Pro Rounded`, `Roboto`, system sans | Rounded, clear tone marks |
| English UI | `Inter`, `SF Pro`, `Roboto`, system sans | Good wrapping and numerals |
| Numerals | Tabular system numerals where available | Progress, question count, streak |

Type scale:

| Token | Size / line height | Weight | Use |
| --- | --- | --- | --- |
| `displaySmall` | 32sp / 40sp | 700 | First-screen app title or major result |
| `headlineLarge` | 28sp / 36sp | 700 | Page title |
| `headlineMedium` | 24sp / 32sp | 700 | Story title Chinese |
| `titleLarge` | 22sp / 30sp | 700 | Section title, quiz prompt |
| `titleMedium` | 18sp / 26sp | 700 | Card title, vocab word |
| `bodyLarge` | 18sp / 28sp | 400 | Parent-facing body, settings rows |
| `bodyMedium` | 16sp / 24sp | 400 | Metadata, English subtitles |
| `labelLarge` | 16sp / 22sp | 700 | Buttons, chips |
| `labelMedium` | 14sp / 20sp | 600 | Small metadata, level labels |
| `readingHanziS` | 22sp / 36sp | 500 | Reading page small |
| `readingHanziM` | 26sp / 42sp | 500 | Reading page default |
| `readingHanziL` | 30sp / 48sp | 500 | Reading page large |
| `readingPinyinS` | 13sp / 22sp | 400 | Pinyin small |
| `readingPinyinM` | 15sp / 24sp | 400 | Pinyin default |
| `readingPinyinL` | 17sp / 28sp | 400 | Pinyin large |

Reading text rules:

- Default reading size is `readingHanziM`.
- Pinyin appears above or immediately before the matching Chinese phrase/paragraph, never in a separate hidden area.
- Keep Chinese line length comfortable: about 12-16 Chinese characters on a 360dp-wide phone.
- Use generous paragraph spacing: 20dp after each paragraph group.
- Dynamic Type / font scale must be supported up to at least 1.3x without clipping primary controls.

## Spacing

Use a 4dp base grid with child-friendly touch spacing.

| Token | Value | Use |
| --- | --- | --- |
| `space.0` | 0dp | Reset |
| `space.1` | 4dp | Tight icon/text gap |
| `space.2` | 8dp | Small internal gap |
| `space.3` | 12dp | Chip padding, compact groups |
| `space.4` | 16dp | Screen side padding on phones |
| `space.5` | 20dp | Paragraph spacing |
| `space.6` | 24dp | Section spacing |
| `space.8` | 32dp | Major vertical groups |
| `space.10` | 40dp | Large top/bottom breathing room |
| `space.12` | 48dp | Minimum touch target |

Responsive spacing:

- Phone side padding: 16dp.
- Large phone / compact tablet side padding: 24dp.
- Tablet content max width: 720dp for reading, 960dp for library/report grids.
- Never shrink tappable targets below 48dp by 48dp.

## Shape

| Token | Value | Use |
| --- | --- | --- |
| `radius.xs` | 6dp | Progress bars, small chips |
| `radius.sm` | 8dp | Repeated cards and compact controls |
| `radius.md` | 12dp | Story cards, quiz options |
| `radius.lg` | 16dp | Primary buttons, reading panels |
| `radius.xl` | 24dp | Modal sheets, parent gate |
| `radius.full` | 999dp | Circular icon buttons, pills |

Cards should not be nested inside other cards. A page section may use background bands or plain layout; use cards only for individual repeated items, modals, and framed controls.

## Icon Style

- Android: Material Symbols Rounded or Material Icons rounded variant.
- iOS: SF Symbols with rounded visual weight where possible.
- Stroke/weight should feel medium, friendly, and simple.
- Use filled or tinted icons only for selected states.
- Avoid weapon, battle, skull, fire, or scary symbols for story content.
- Preferred icons:
  - Read: book open.
  - Audio: volume or play circle.
  - Vocabulary: sparkles or text fields, not "test" imagery.
  - Quiz: help circle or checklist.
  - Parent report: bar chart or shield person.
  - Settings: gear.
  - Privacy: shield check.

## Components

### Story Card

Purpose: preview a story and resume or start reading.

- Min height: 152dp in list, 220dp for Today hero.
- Cover art: 96dp square in list, 140-180dp wide in Today card; 8dp radius.
- Text:
  - `title_zh` as primary title.
  - `title_en` below, max 2 lines.
  - Level chip uses platform resource string plus `level`.
- Progress bar:
  - Height 8dp, radius full.
  - Completed color `success`; track `tertiaryContainer`.
- CTA:
  - Primary if today's story or not started.
  - Secondary text button for completed stories.
- Do not put text inside cover image. Cover image should have accessible label from story title.

### Primary Button

- Height: 56dp.
- Min width: 128dp.
- Horizontal padding: 20dp.
- Radius: 16dp.
- Background: `primary`; content: `onPrimary`.
- Disabled: `surfaceVariant` background, `textSecondary` content.
- Include icon when action benefits from recognition, e.g. play/read/next.
- Label comes from resources, not `shared`.

### Secondary Button / Chip

- Height: 48dp.
- Radius: full for chips, 16dp for buttons.
- Border: 1dp `outline`.
- Selected: `secondaryContainer` background with `secondary` border and `onSecondaryContainer` text.
- Use for pinyin on/off, font size, level filters, language choice.

### Pinyin Text Block

- Container: optional `surfaceVariant`, 16dp radius, 16dp padding.
- Each paragraph group:
  - Pinyin line uses `readingPinyin*`, `textSecondary`.
  - Chinese line uses `readingHanzi*`, `textPrimary`.
  - Keep phrase alignment readable; MVP may use paragraph-level pinyin if word-level ruby is not ready.
- Pinyin toggle state persists locally and never changes story data.
- Audio play button stays visible at paragraph or page level with 48dp hit target.

### Quiz Option

- Min height: 56dp.
- Radius: 12dp.
- Border: 2dp when selected/answered, 1dp otherwise.
- Text: `bodyLarge`, wraps to multiple lines.
- States:
  - Default: white surface, outline border.
  - Selected: secondary container and teal border.
  - Correct: success container or success border, check icon, answer text.
  - Incorrect: error border, close icon, plus brief explanation below.
- Feedback must use icon and text, not color alone.

### Progress

- Reading progress: top linear indicator, 8dp height.
- Story progress: card-level bar with completed percentage.
- Daily progress: small "1 story today" style copy from resources plus progress ring or bar.
- Parent report progress: aggregated counts only. No child real name or identifying label.

### Vocabulary Card

- Word: large Chinese (`headlineMedium` or `titleLarge`).
- Pinyin: `bodyMedium`, teal.
- Meaning: English `bodyLarge`.
- Example sentence: Chinese, `bodyLarge`.
- Audio button: 48dp icon button.
- Mark-known action: optional secondary button; avoid gamified pressure.

### App Navigation

MVP primary routes:

- Today.
- Library.
- Parent.
- Settings.

Reading flow routes:

- Story detail or Today -> Reading -> Vocabulary -> Quiz -> Completion/report update.

Phone navigation:

- Bottom navigation for top-level routes.
- Reading flow uses top app bar back/close and bottom action button, not the global bottom nav.

Tablet navigation:

- Navigation rail is acceptable for top-level routes.
- Reading max width stays constrained; do not stretch paragraphs full width.

## Accessibility

- Meet WCAG AA for all text and control states.
- Hit targets: at least 48dp by 48dp.
- Support Android font scale and iOS Dynamic Type up to at least 1.3x for MVP.
- Do not rely on color alone; include icons, labels, and state text.
- Use semantic headings for screen readers.
- Images have accessible labels from `title_zh` and `title_en`; decorative patterns are hidden.
- Buttons include verbs: Start, Continue, Play, Next, Submit.
- Quiz feedback is announced politely after answer submission.
- Audio controls must be operable without drag gestures.
- Avoid rapid animation. Respect reduce-motion settings.

## Child Safety And Privacy

- No child name input in MVP screens.
- Parent report uses local or anonymous progress labels such as "Reader" only if a label is needed.
- Settings must not ask for date of birth, location, photo, school, or contact details.
- AI-related entry points must be absent in MVP unless scoped to current story and guarded by approved prompts.
- Story art and iconography must avoid blood, wounds, weapon close-ups, fear, or glorified fighting.
- War-related stories should visually emphasize wisdom, friendship, courage, cooperation, and kindness.

## Bilingual Layout Rules

- Chinese title first, English title second.
- English subtitles can wrap to 2 lines; do not force small text to keep one line.
- Use resource strings for UI labels in both English and zh-Hans.
- Avoid text embedded in images.
- Keep buttons short in English and Chinese. If English labels are longer, allow the button to grow vertically rather than shrinking below 48dp.
- Story JSON content is not a UI string resource; it is content data and may be displayed directly.
