# SFX Generator (Task #24)

This folder contains local synthesis for child-friendly P0 sound effects.

Run from repo root:

```bash
cd /Users/zili/code/android/ReadingMVP
pipeline/.venv312-tts/bin/python pipeline/sfx/generate_sfx.py
```

Generated files are written to:

- `apps/reader/shared/src/commonMain/resources/sfx/`

The generator produces the following assets:
- `story_complete_chime.wav`
- `quiz_correct.wav`
- `quiz_try_again.wav`
- `streak_milestone_3.wav`
- `streak_milestone_7.wav`
- `streak_milestone_14.wav`
- `sound_toggle_preview.wav`

The script is deterministic and only requires `numpy` + Python standard library.
