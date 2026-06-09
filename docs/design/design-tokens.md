# Design Tokens

These tokens are intended to map directly to Compose Material3 and SwiftUI. Names are semantic first, with primitive values documented for implementation.

Units:

- Color: hex sRGB.
- Android spacing/radius: `Dp`.
- Android type: `Sp`.
- SwiftUI spacing/radius/type: `CGFloat` / points.
- Token names use lower camel case for generated code.

## Color Tokens

| Token | Light value | Material3 mapping | SwiftUI suggestion |
| --- | --- | --- | --- |
| `colorPrimary` | `#B84535` | `primary` | `Color.lmcPrimary` |
| `colorOnPrimary` | `#FFFFFF` | `onPrimary` | `Color.lmcOnPrimary` |
| `colorPrimaryContainer` | `#FFE0D6` | `primaryContainer` | `Color.lmcPrimaryContainer` |
| `colorOnPrimaryContainer` | `#3D0E08` | `onPrimaryContainer` | `Color.lmcOnPrimaryContainer` |
| `colorSecondary` | `#126B68` | `secondary` | `Color.lmcSecondary` |
| `colorOnSecondary` | `#FFFFFF` | `onSecondary` | `Color.lmcOnSecondary` |
| `colorSecondaryContainer` | `#D9F1EE` | `secondaryContainer` | `Color.lmcSecondaryContainer` |
| `colorOnSecondaryContainer` | `#063432` | `onSecondaryContainer` | `Color.lmcOnSecondaryContainer` |
| `colorTertiary` | `#8A6100` | `tertiary` | `Color.lmcTertiary` |
| `colorOnTertiary` | `#FFFFFF` | `onTertiary` | `Color.lmcOnTertiary` |
| `colorTertiaryContainer` | `#FFF4D8` | `tertiaryContainer` | `Color.lmcTertiaryContainer` |
| `colorOnTertiaryContainer` | `#7A4E00` | `onTertiaryContainer` | `Color.lmcOnTertiaryContainer` |
| `colorBackground` | `#FFF8EC` | `background` | `Color.lmcBackground` |
| `colorOnBackground` | `#202523` | `onBackground` | `Color.lmcOnBackground` |
| `colorSurface` | `#FFFFFF` | `surface` | `Color.lmcSurface` |
| `colorOnSurface` | `#202523` | `onSurface` | `Color.lmcOnSurface` |
| `colorSurfaceVariant` | `#F3F7F1` | `surfaceVariant` | `Color.lmcSurfaceVariant` |
| `colorOnSurfaceVariant` | `#4F5E58` | `onSurfaceVariant` | `Color.lmcOnSurfaceVariant` |
| `colorOutline` | `#9AA7A0` | `outline` | `Color.lmcOutline` |
| `colorOutlineVariant` | `#D7DED8` | `outlineVariant` | `Color.lmcOutlineVariant` |
| `colorError` | `#B3261E` | `error` | `Color.lmcError` |
| `colorOnError` | `#FFFFFF` | `onError` | `Color.lmcOnError` |
| `colorSuccess` | `#3B7A3B` | custom extension | `Color.lmcSuccess` |
| `colorOnSuccess` | `#FFFFFF` | custom extension | `Color.lmcOnSuccess` |
| `colorSuccessContainer` | `#E1F3DC` | custom extension | `Color.lmcSuccessContainer` |
| `colorInfo` | `#2B6CA3` | custom extension | `Color.lmcInfo` |
| `colorInfoContainer` | `#DCEEFF` | custom extension | `Color.lmcInfoContainer` |
| `colorScrim` | `#000000` at 32% | `scrim` | `Color.black.opacity(0.32)` |

Recommended Compose shape:

```kotlin
val LmcLightColorScheme = lightColorScheme(
    primary = Color(0xFFB84535),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0D6),
    onPrimaryContainer = Color(0xFF3D0E08),
    secondary = Color(0xFF126B68),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9F1EE),
    onSecondaryContainer = Color(0xFF063432),
    tertiary = Color(0xFF8A6100),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF4D8),
    onTertiaryContainer = Color(0xFF7A4E00),
    background = Color(0xFFFFF8EC),
    onBackground = Color(0xFF202523),
    surface = Color.White,
    onSurface = Color(0xFF202523),
    surfaceVariant = Color(0xFFF3F7F1),
    onSurfaceVariant = Color(0xFF4F5E58),
    outline = Color(0xFF9AA7A0),
    outlineVariant = Color(0xFFD7DED8),
    error = Color(0xFFB3261E),
    onError = Color.White,
)
```

Theme mode rule:

- MVP may use `LmcLightColorScheme` for both light and dark system modes.
- Do not use Material3 `darkColorScheme()` or default SwiftUI dark colors as a fallback; they are not part of this design system.
- If dark mode is added later, define a full semantic dark table with the same token names and rerun contrast checks.

## Typography Tokens

| Token | Size | Line height | Weight | Compose mapping | SwiftUI suggestion |
| --- | ---: | ---: | ---: | --- | --- |
| `typeDisplaySmall` | 32 | 40 | 700 | `displaySmall` | `.title.bold()` custom size 32 |
| `typeHeadlineLarge` | 28 | 36 | 700 | `headlineLarge` | custom size 28 bold |
| `typeHeadlineMedium` | 24 | 32 | 700 | `headlineMedium` | custom size 24 bold |
| `typeTitleLarge` | 22 | 30 | 700 | `titleLarge` | custom size 22 bold |
| `typeTitleMedium` | 18 | 26 | 700 | `titleMedium` | custom size 18 bold |
| `typeBodyLarge` | 18 | 28 | 400 | `bodyLarge` | `.body` custom size 18 |
| `typeBodyMedium` | 16 | 24 | 400 | `bodyMedium` | custom size 16 |
| `typeLabelLarge` | 16 | 22 | 700 | `labelLarge` | custom size 16 bold |
| `typeLabelMedium` | 14 | 20 | 600 | `labelMedium` | custom size 14 semibold |
| `typeReadingHanziSmall` | 22 | 36 | 500 | custom extension | custom size 22 medium |
| `typeReadingHanziMedium` | 26 | 42 | 500 | custom extension | custom size 26 medium |
| `typeReadingHanziLarge` | 30 | 48 | 500 | custom extension | custom size 30 medium |
| `typeReadingPinyinSmall` | 13 | 22 | 400 | custom extension | custom size 13 |
| `typeReadingPinyinMedium` | 15 | 24 | 400 | custom extension | custom size 15 |
| `typeReadingPinyinLarge` | 17 | 28 | 400 | custom extension | custom size 17 |

Font family tokens:

| Token | Value |
| --- | --- |
| `fontChineseUi` | `Noto Sans SC`, `PingFang SC`, system sans |
| `fontChineseReading` | `Noto Serif SC`, `Source Han Serif SC`, `Songti SC`, fallback sans |
| `fontPinyin` | `Inter`, `SF Pro Rounded`, `Roboto`, system sans |
| `fontEnglishUi` | `Inter`, `SF Pro`, `Roboto`, system sans |

## Spacing Tokens

| Token | Value |
| --- | ---: |
| `space0` | 0 |
| `space1` | 4 |
| `space2` | 8 |
| `space3` | 12 |
| `space4` | 16 |
| `space5` | 20 |
| `space6` | 24 |
| `space8` | 32 |
| `space10` | 40 |
| `space12` | 48 |
| `space16` | 64 |

Layout tokens:

| Token | Value | Use |
| --- | ---: | --- |
| `screenPaddingPhone` | 16 | Default phone horizontal inset |
| `screenPaddingExpanded` | 24 | Large phone, tablet, foldable |
| `readingMaxWidth` | 720 | Reading content max width |
| `gridMaxWidth` | 960 | Library/report grid max width |
| `bottomActionHeight` | 72 | Bottom safe action area excluding system inset |
| `minTouchTarget` | 48 | Minimum interactive target |
| `storyCoverList` | 96 | Library card cover |
| `storyCoverHeroMin` | 140 | Today cover minimum |
| `storyCoverHeroMax` | 180 | Today cover maximum |
| `progressHeight` | 8 | Linear progress indicators |

## Radius Tokens

| Token | Value |
| --- | ---: |
| `radiusXs` | 6 |
| `radiusSm` | 8 |
| `radiusMd` | 12 |
| `radiusLg` | 16 |
| `radiusXl` | 24 |
| `radiusFull` | 999 |

## Elevation Tokens

Use low elevation. The app should feel like layered paper, not floating glass.

| Token | Android | iOS | Use |
| --- | ---: | ---: | --- |
| `elevationNone` | 0dp | none | Background sections |
| `elevationCard` | 1dp | shadow opacity 0.08, y 1, blur 4 | Story/vocab cards |
| `elevationAppBar` | 0dp | none | App bars; use divider if needed |
| `elevationModal` | 6dp | shadow opacity 0.16, y 4, blur 16 | Parent gate, bottom sheets |

## Component Tokens

| Token | Value |
| --- | --- |
| `buttonPrimaryHeight` | 56 |
| `buttonPrimaryRadius` | 16 |
| `buttonPrimaryPaddingX` | 20 |
| `buttonSecondaryHeight` | 48 |
| `chipHeight` | 40 |
| `chipRadius` | 999 |
| `quizOptionMinHeight` | 56 |
| `quizOptionRadius` | 12 |
| `cardRadius` | 12 |
| `cardPadding` | 16 |
| `readingPanelRadius` | 16 |
| `readingPanelPadding` | 16 |
| `bottomNavHeight` | 80 |
| `topAppBarHeight` | 64 |
| `iconButtonSize` | 48 |
| `iconSize` | 24 |
| `coverAspectRatio` | `1:1` |

## Motion Tokens

| Token | Value | Use |
| --- | ---: | --- |
| `motionFast` | 120ms | Toggle, selected state |
| `motionMedium` | 180ms | Screen element enter |
| `motionSlow` | 240ms | Completion progress |
| `motionEasingStandard` | platform standard | General |

Respect Android animator duration scale and iOS Reduce Motion. If reduce motion is enabled, use instant state changes or fades only.

## Resource Key Guidance

These are naming recommendations for platform resources, not shared model fields.

| Area | Prefix | Examples |
| --- | --- | --- |
| Navigation | `nav_` | `nav_today`, `nav_library`, `nav_parent`, `nav_settings` |
| Common actions | `action_` | `action_start_reading`, `action_continue`, `action_next`, `action_submit` |
| Reading controls | `reading_` | `reading_pinyin`, `reading_font_size`, `reading_audio` |
| Quiz | `quiz_` | `quiz_question_count`, `quiz_correct`, `quiz_try_next` |
| Parent report | `parent_` | `parent_report_title`, `parent_stories_read`, `parent_privacy_note` |
| Settings | `settings_` | `settings_language`, `settings_pinyin_default`, `settings_privacy` |

Story JSON fields such as `title_zh`, `title_en`, `paragraphs.text`, `paragraphs.pinyin`, `vocab.meaning`, and `questions.prompt` are content data and may be rendered directly.
