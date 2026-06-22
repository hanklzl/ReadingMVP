#!/usr/bin/env python3
"""Validate packaged audio resources are real WAV files, not Git LFS pointers."""

from __future__ import annotations

import argparse
import io
import sys
import wave
import zipfile
from pathlib import Path


LFS_POINTER_PREFIX = b"version https://git-lfs.github.com/spec/v1"
MIN_WAV_BYTES = 1024


def iter_source_wavs(root: Path):
    if not root.is_dir():
        raise FileNotFoundError(f"source root not found: {root}")
    for path in sorted(root.rglob("*.wav")):
        yield str(path), path.read_bytes()


def iter_zip_wavs(path: Path):
    if not path.is_file():
        raise FileNotFoundError(f"artifact not found: {path}")
    with zipfile.ZipFile(path) as archive:
        for info in sorted(archive.infolist(), key=lambda item: item.filename):
            if not info.filename.endswith(".wav"):
                continue
            yield info.filename, archive.read(info)


def validate_wav(name: str, data: bytes) -> list[str]:
    errors: list[str] = []
    if data.startswith(LFS_POINTER_PREFIX):
        errors.append("is a Git LFS pointer, not audio data")
        return errors
    if len(data) < MIN_WAV_BYTES:
        errors.append(f"is unexpectedly small ({len(data)} bytes)")
    if not (data.startswith(b"RIFF") and data[8:12] == b"WAVE"):
        errors.append("does not have a RIFF/WAVE header")
        return errors

    try:
        with wave.open(io.BytesIO(data), "rb") as wav:
            if wav.getnchannels() <= 0:
                errors.append("has no audio channels")
            if wav.getsampwidth() <= 0:
                errors.append("has invalid sample width")
            if wav.getframerate() <= 0:
                errors.append("has invalid sample rate")
            if wav.getnframes() <= 0:
                errors.append("has no audio frames")
    except wave.Error as error:
        errors.append(f"cannot be parsed as WAV: {error}")

    return errors


def validate_collection(label: str, wavs) -> tuple[int, int, list[str]]:
    count = 0
    total_bytes = 0
    failures: list[str] = []
    for name, data in wavs:
        count += 1
        total_bytes += len(data)
        for error in validate_wav(name, data):
            failures.append(f"{label}: {name} {error}")
    if count == 0:
        failures.append(f"{label}: no .wav files found")
    return count, total_bytes, failures


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--source-root",
        action="append",
        default=[],
        help="Directory tree containing source WAV resources.",
    )
    parser.add_argument(
        "--zip",
        action="append",
        default=[],
        help="APK/AAB/ZIP artifact containing WAV resources.",
    )
    args = parser.parse_args()

    checks: list[tuple[str, object]] = []
    for root in args.source_root:
        path = Path(root)
        checks.append((str(path), iter_source_wavs(path)))
    for artifact in args.zip:
        path = Path(artifact)
        checks.append((str(path), iter_zip_wavs(path)))

    if not checks:
        parser.error("at least one --source-root or --zip is required")

    all_failures: list[str] = []
    for label, wavs in checks:
        count, total_bytes, failures = validate_collection(label, wavs)
        print(f"media checked: {label}: {count} wav files, {total_bytes} bytes")
        all_failures.extend(failures)

    if all_failures:
        for failure in all_failures[:50]:
            print(f"::error::{failure}", file=sys.stderr)
        remaining = len(all_failures) - 50
        if remaining > 0:
            print(f"::error::{remaining} more media validation failure(s)", file=sys.stderr)
        return 1

    print("Media asset validation passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
