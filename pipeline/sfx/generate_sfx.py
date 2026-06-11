#!/usr/bin/env python3
"""
Synthesize child-friendly, non-licensed SFX assets for P0.

The script generates short mono WAV files (44.1kHz) for local bundling and
normalises each output to approximately -3 dBFS with short fades to avoid clicks.
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path
import math
import wave

import numpy as np


SAMPLE_RATE = 44_100
TARGET_PEAK = 10 ** (-3 / 20)  # -3 dBFS
DEFAULT_OUT_DIR = Path(__file__).resolve().parents[2] / "apps/reader/shared/src/commonMain/resources/sfx"


@dataclass(frozen=True)
class ToneEvent:
    start_s: float
    duration_s: float
    freq: float | tuple[float, float] | list[float]
    amplitude: float
    attack_s: float = 0.01
    release_s: float = 0.08


def render_envelope(n: int, attack_s: float, release_s: float) -> np.ndarray:
    attack = max(1, int(SAMPLE_RATE * attack_s))
    release = max(1, int(SAMPLE_RATE * release_s))
    sustain_len = max(1, n - attack - release)

    attack_seg = np.linspace(0.0, 1.0, attack, endpoint=False)
    sustain_seg = np.ones(sustain_len, dtype=np.float64)
    release_seg = np.linspace(1.0, 0.0, release, endpoint=False)
    return np.concatenate((attack_seg, sustain_seg, release_seg))


def render_tone(duration_s: float, freq: float | tuple[float, float] | list[float], amplitude: float) -> np.ndarray:
    n_samples = int(SAMPLE_RATE * duration_s)
    if n_samples <= 0:
        return np.zeros(0, dtype=np.float64)

    t = np.arange(n_samples, dtype=np.float64) / SAMPLE_RATE

    def single_tone(f: float) -> np.ndarray:
        phase = 2.0 * math.pi * f * t
        return np.sin(phase)

    def gliding_tone(f0: float, f1: float) -> np.ndarray:
        k = (f1 - f0) / max(duration_s, 1e-6)
        phase = 2.0 * math.pi * (f0 * t + 0.5 * k * t * t)
        return np.sin(phase)

    if isinstance(freq, list):
        # Chord: sum tones and average power.
        if len(freq) == 0:
            return np.zeros(n_samples, dtype=np.float64)
        sig = np.zeros(n_samples, dtype=np.float64)
        for f in freq:
            sig += single_tone(float(f))
        sig /= len(freq)
        return np.clip(sig * amplitude, -1.0, 1.0)

    if isinstance(freq, tuple):
        base = gliding_tone(float(freq[0]), float(freq[1]))
    else:
        base = single_tone(float(freq))

    return np.clip(base * amplitude, -1.0, 1.0)


def add_tone(buffer: np.ndarray, event: ToneEvent, harmonic_boost: float = 0.2) -> None:
    n_event = int(SAMPLE_RATE * event.duration_s)
    if n_event <= 0:
        return
    i0 = int(event.start_s * SAMPLE_RATE)
    i1 = min(len(buffer), i0 + n_event)
    if i1 <= 0 or i0 >= len(buffer):
        return

    n = i1 - i0
    tone = render_tone(event.duration_s, event.freq, event.amplitude)
    tone = tone[:n]

    envelope = render_envelope(n, event.attack_s, event.release_s)
    tone *= envelope

    # Add a subtle overtone layer for brightness without harsh edges.
    if isinstance(event.freq, list):
        overtone_freqs = [float(f) * 2.0 for f in event.freq]
    elif isinstance(event.freq, tuple):
        # keep sweep direction and character while moving the overtone slightly lower.
        overtone_freqs = (float(event.freq[0]) * 1.98, float(event.freq[1]) * 1.98)
    else:
        overtone_freqs = float(event.freq) * 1.98
    overtone = render_tone(
        event.duration_s,
        overtone_freqs,
        event.amplitude * harmonic_boost * 0.45,
    )[:n]
    overtone *= 0.35
    overtone = overtone * envelope

    if isinstance(event.freq, tuple):
        # Slightly softer sweep for gliding tones.
        base_amp = event.amplitude * 0.15
        gliding_faint = render_tone(
            event.duration_s,
            (float(event.freq[0]) * 0.995, float(event.freq[1]) * 0.995),
            1.0,
        )[:n]
        overtone = np.clip(gliding_faint * base_amp * envelope, -1.0, 1.0)

    buffer[i0:i1] += tone + overtone


def normalise_to_target_peak(audio: np.ndarray, target_peak: float = TARGET_PEAK) -> np.ndarray:
    max_amp = float(np.max(np.abs(audio)))
    if max_amp < 1e-12:
        return audio
    return np.clip(audio * (target_peak / max_amp), -1.0, 1.0)


def write_wav(path: Path, audio: np.ndarray) -> None:
    audio = normalise_to_target_peak(audio)
    pcm = np.clip(np.rint(audio * np.iinfo(np.int16).max), -32768, 32767).astype(np.int16)
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "wb") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(SAMPLE_RATE)
        f.writeframes(pcm.tobytes())


def build_story_complete() -> np.ndarray:
    total = 1.32
    n_total = int(SAMPLE_RATE * total)
    audio = np.zeros(n_total, dtype=np.float64)
    events = [
        ToneEvent(0.00, 0.20, 392.0, 0.35),
        ToneEvent(0.14, 0.22, (523.0, 587.0), 0.31),
        ToneEvent(0.30, 0.20, 659.0, 0.30),
        ToneEvent(0.46, 0.20, (784.0, 880.0), 0.30),
        ToneEvent(0.62, 0.24, 988.0, 0.27, attack_s=0.007, release_s=0.10),
        ToneEvent(0.75, 0.48, [523.0, 659.0, 784.0], 0.14, attack_s=0.02, release_s=0.12),
    ]
    for event in events:
        add_tone(audio, event, harmonic_boost=0.22)
    return audio


def build_quiz_correct() -> np.ndarray:
    total = 0.44
    n_total = int(SAMPLE_RATE * total)
    audio = np.zeros(n_total, dtype=np.float64)
    events = [
        ToneEvent(0.00, 0.18, 659.0, 0.40),
        ToneEvent(0.12, 0.20, 880.0, 0.33),
    ]
    for event in events:
        add_tone(audio, event)
    return audio


def build_quiz_try_again() -> np.ndarray:
    total = 0.52
    n_total = int(SAMPLE_RATE * total)
    audio = np.zeros(n_total, dtype=np.float64)
    events = [
        ToneEvent(0.00, 0.36, (196.0, 168.0), 0.34, attack_s=0.02, release_s=0.12),
        ToneEvent(0.10, 0.28, 130.0, 0.12, attack_s=0.04, release_s=0.15),
    ]
    for event in events:
        add_tone(audio, event, harmonic_boost=0.16)
    return audio


def build_streak_milestone_3() -> np.ndarray:
    total = 1.02
    n_total = int(SAMPLE_RATE * total)
    audio = np.zeros(n_total, dtype=np.float64)
    events = [
        ToneEvent(0.00, 0.16, 523.0, 0.30, 0.009, 0.07),
        ToneEvent(0.16, 0.16, 659.0, 0.26, 0.009, 0.07),
        ToneEvent(0.31, 0.16, 784.0, 0.22, 0.008, 0.07),
        ToneEvent(0.49, 0.18, 523.0, 0.22, 0.010, 0.08),
        ToneEvent(0.63, 0.17, [587.0, 740.0], 0.18, 0.010, 0.09),
        ToneEvent(0.78, 0.22, [880.0, 1046.5], 0.14, 0.012, 0.10),
    ]
    for event in events:
        add_tone(audio, event, harmonic_boost=0.20)
    return audio


def build_streak_milestone_7() -> np.ndarray:
    total = 1.23
    n_total = int(SAMPLE_RATE * total)
    audio = np.zeros(n_total, dtype=np.float64)
    events = [
        ToneEvent(0.00, 0.17, (392.0, 523.0), 0.24),
        ToneEvent(0.15, 0.16, (523.0, 587.0), 0.22),
        ToneEvent(0.28, 0.16, 659.0, 0.21),
        ToneEvent(0.42, 0.17, (587.0, 659.0), 0.18),
        ToneEvent(0.58, 0.17, [784.0, 880.0], 0.17),
        ToneEvent(0.74, 0.20, [1046.5, 659.0], 0.16),
        ToneEvent(0.96, 0.27, [523.0, 659.0, 784.0], 0.14, attack_s=0.02, release_s=0.12),
    ]
    for event in events:
        add_tone(audio, event, harmonic_boost=0.21)
    return audio


def build_streak_milestone_14() -> np.ndarray:
    total = 1.32
    n_total = int(SAMPLE_RATE * total)
    audio = np.zeros(n_total, dtype=np.float64)
    events = [
        ToneEvent(0.00, 0.17, (392.0, 494.0), 0.27),
        ToneEvent(0.16, 0.18, 523.0, 0.22),
        ToneEvent(0.29, 0.16, 659.0, 0.20),
        ToneEvent(0.44, 0.18, (784.0, 880.0), 0.19),
        ToneEvent(0.59, 0.15, 988.0, 0.17),
        ToneEvent(0.72, 0.18, (587.0, 740.0), 0.16),
        ToneEvent(0.89, 0.17, 1046.5, 0.14),
        ToneEvent(1.04, 0.28, [523.0, 659.0, 784.0, 987.8], 0.12, attack_s=0.02, release_s=0.11),
    ]
    for event in events:
        add_tone(audio, event, harmonic_boost=0.23)
    return audio


def build_sound_toggle_preview() -> np.ndarray:
    total = 0.52
    n_total = int(SAMPLE_RATE * total)
    audio = np.zeros(n_total, dtype=np.float64)
    events = [
        ToneEvent(0.00, 0.21, 330.0, 0.26),
        ToneEvent(0.14, 0.22, (392.0, 440.0), 0.24),
        ToneEvent(0.24, 0.28, [659.0], 0.09, attack_s=0.020, release_s=0.12),
    ]
    for event in events:
        add_tone(audio, event, harmonic_boost=0.20)
    return audio


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate local WAV SFX assets for P0.")
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=DEFAULT_OUT_DIR,
        help="Output directory for generated WAV assets.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print what would be generated without writing files.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    specs = {
        "story_complete_chime": build_story_complete,
        "quiz_correct": build_quiz_correct,
        "quiz_try_again": build_quiz_try_again,
        "streak_milestone_3": build_streak_milestone_3,
        "streak_milestone_7": build_streak_milestone_7,
        "streak_milestone_14": build_streak_milestone_14,
        "sound_toggle_preview": build_sound_toggle_preview,
    }

    print(f"Generating SFX into {args.output_dir}")
    for name, factory in specs.items():
        audio = normalise_to_target_peak(factory())
        duration = len(audio) / SAMPLE_RATE
        peak_dbfs = 20 * math.log10(max(1e-12, np.max(np.abs(audio))))
        if args.dry_run:
            print(f"{name}.wav  ->  {duration:.3f}s  peak={peak_dbfs:.2f} dBFS")
            continue
        out = args.output_dir / f"{name}.wav"
        write_wav(out, audio)
        print(f"{name}.wav  ->  {duration:.3f}s  peak={peak_dbfs:.2f} dBFS")


if __name__ == "__main__":
    main()
