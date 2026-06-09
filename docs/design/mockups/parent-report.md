# Mockup: Parent Report

Route: `parent-report`

Purpose: show parents local/anonymous reading progress without collecting child personal information.

## Parent Gate Wireframe

```text
+------------------------------------------------+
| Parent Area / 家长区                            |
+------------------------------------------------+
| This area is for grown-ups.                    |
|                                                |
| [ simple parent gate control ]                 |
|                                                |
|                                      [Continue]|
+------------------------------------------------+
```

## Report Wireframe

```text
+------------------------------------------------+
| [back] Parent Report / 家长报告          [gear] |
+------------------------------------------------+
| This week / 本周                                |
| +----------------+ +----------------+          |
| | Stories read   | | Reading days   |          |
| | 4              | | 3              |          |
| +----------------+ +----------------+          |
| +----------------+ +----------------+          |
| | Quiz correct   | | Words reviewed |          |
| | 9 / 12         | | 18             |          |
| +----------------+ +----------------+          |
|                                                |
| Story progress / 故事进度                       |
| 桃园三结义      [==========] completed          |
| 草船借箭        [======----] 60%                |
| 空城计          [----------] not started        |
|                                                |
| Privacy / 隐私                                  |
| No child name or personal details are required. |
+------------------------------------------------+
```

## Layout

- Parent report can be reached from Today, bottom nav, or settings.
- A lightweight parent gate is recommended before showing report details.
- Report uses aggregate metrics: stories read, reading days, quiz correct count, words reviewed.
- Story progress list uses titles and local progress, not child identity.
- Privacy note is visible and concise.
- On tablet, metric tiles can be a 4-column row and story progress can sit beside weekly summary.

## Components

- `ParentGate`
- `MetricTile`
- `StoryProgressRow`
- `ProgressBar`
- `PrivacyNotice`
- `TopAppBar`

## Interactions

- Parent gate must be accessible and not require child personal information.
- Tap story progress row -> story detail or reading page.
- Settings icon -> Settings page.
- Metrics update from `ProgressService` / report aggregation use case.

## Bilingual Considerations

- Parent-facing UI must be fully localized in English and zh-Hans.
- Numeric values should use locale-aware formatting.
- Long English labels wrap inside metric tiles; tile heights can grow.

## Safety And Privacy Notes

- Do not ask for child name, birthday, grade, school, photo, email, or location.
- If parent account is added later, keep child profile optional and non-identifying.
- Analytics, if any, must be anonymous and contain no child PII.
- Avoid social comparison, rankings, or public sharing.
