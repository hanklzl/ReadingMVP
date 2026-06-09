# Mockup: Settings

Route: `settings`

Purpose: configure language, reading supports, audio, privacy, and parent controls.

## Phone Wireframe

```text
+------------------------------------------------+
| [back] Settings / 设置                         |
+------------------------------------------------+
| Language / 语言                                 |
| +--------------------------------------------+ |
| | English                           [check]  | |
| | 简体中文                                    | |
| +--------------------------------------------+ |
|                                                |
| Reading / 阅读                                 |
| +--------------------------------------------+ |
| | Show pinyin by default              [on]   | |
| | Text size                       [S] [M] [L] | |
| | Audio voice                     System     | |
| +--------------------------------------------+ |
|                                                |
| Parent / 家长                                  |
| +--------------------------------------------+ |
| | Parent report                              | |
| | Privacy                                    | |
| +--------------------------------------------+ |
|                                                |
| Little Mandarin Classics  v1.0                 |
+------------------------------------------------+
```

## Layout

- Settings are grouped into Language, Reading, Parent, and About.
- Use full-width rows with 48dp minimum height.
- Toggles use platform-native components.
- Segmented text size control maps to reading S/M/L tokens.
- Keep About text minimal.

## Components

- `SettingsSection`
- `SettingsRow`
- `LanguagePicker`
- `Switch`
- `FontSizeSegmentedControl`
- `NavigationRow`
- `TopAppBar`

## Interactions

- Language changes app UI locale where platform support permits. Story content remains Chinese.
- Pinyin default controls initial state on Reading page.
- Text size changes reading tokens globally.
- Audio voice opens platform TTS settings or in-app system voice selector if available.
- Parent report row opens parent gate/report.
- Privacy row opens local privacy information.

## Bilingual Considerations

- Language rows should show native language names: "English" and "简体中文".
- Current locale is indicated with check icon and accessible selected state.
- UI labels come from platform resources.
- Long English settings labels wrap; do not truncate critical privacy labels.

## Safety And Privacy Notes

- Settings must not include child name, birthday, school, location, or photo fields.
- Privacy copy should state that MVP does not require child personal details.
- Any future account sign-in must be parent-oriented and reviewed for COPPA/GDPR-K.
