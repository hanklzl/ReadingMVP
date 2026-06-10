from __future__ import annotations

import hashlib
import json
import math
import os
import wave
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Protocol
from urllib.parse import urljoin

import requests

SENTENCE_END_PUNCTUATION = "。！？；…"
SENTENCE_TRAILING_PUNCTUATION = "”’」』》〉）)"


def split_paragraph_to_sentences(text: str) -> list[str]:
    """Split a Chinese paragraph by sentence-ending punctuation.

    Rules:
    - split on the sentence-ending punctuation: 。！？；…
    - keep consecutive ending punctuation together, including ellipsis runs like "……"
    - ignore whitespace-only sentence chunks
    """

    if not isinstance(text, str):
        return []

    content = text
    if not content:
        return []

    sentence_end_positions: list[tuple[int, int]] = []
    index = 0
    length = len(content)

    while index < length:
        current = content[index]
        if current in SENTENCE_END_PUNCTUATION:
            end = index + 1
            while end < length and (
                content[end] in SENTENCE_END_PUNCTUATION
                or content[end] in SENTENCE_TRAILING_PUNCTUATION
            ):
                end += 1

            sentence_end_positions.append((index, end))
            index = end
            while index < length and content[index].isspace():
                index += 1
            continue

        index += 1

    segments: list[str] = []
    start = 0
    for _, end in sentence_end_positions:
        segment = content[start:end].strip()
        if segment:
            segments.append(segment)
        start = end

    tail = content[start:].strip()
    if tail:
        segments.append(tail)

    if not segments and content.strip():
        segments.append(content.strip())

    return segments


def build_sentence_plan(paragraphs: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Build expected sentence plan from raw paragraph text."""

    plan: list[dict[str, Any]] = []
    for para_index, paragraph in enumerate(paragraphs):
        if not isinstance(paragraph, dict):
            continue
        text = str(paragraph.get("text", ""))
        for sent_index, sentence in enumerate(split_paragraph_to_sentences(text)):
            plan.append(
                {
                    "paraIndex": para_index,
                    "sentIndex": sent_index,
                    "text": sentence,
                    "audioPath": f"audio/p{para_index + 1}_s{sent_index + 1}.wav",
                }
            )
    return plan


class AudioSentence:
    def __init__(
        self,
        paraIndex: int,
        sentIndex: int,
        text: str,
        audioPath: str,
        durationMs: int,
        unavailable: bool = False,
    ) -> None:
        self.paraIndex = paraIndex
        self.sentIndex = sentIndex
        self.text = text
        self.audioPath = audioPath
        self.durationMs = durationMs
        self.unavailable = unavailable

    def as_dict(self) -> dict[str, Any]:
        payload = {
            "paraIndex": self.paraIndex,
            "sentIndex": self.sentIndex,
            "text": self.text,
            "audioPath": self.audioPath,
            "durationMs": self.durationMs,
        }
        if self.unavailable:
            payload["unavailable"] = True
        return payload


class AudioProvider(Protocol):
    def synthesize(self, text: str, output_path: Path) -> int:
        ...


class MockAudioProvider:
    sample_rate = 22050
    _max_duration_ms = 1500
    _min_duration_ms = 350

    def synthesize(self, text: str, output_path: Path) -> int:
        output_path.parent.mkdir(parents=True, exist_ok=True)

        duration_ms = _mock_duration_ms(text)
        num_frames = int(self.sample_rate * duration_ms / 1000)
        frequency = _mock_frequency(text)
        amplitude = 0.12

        with wave.open(str(output_path), "wb") as wav_file:
            wav_file.setnchannels(1)
            wav_file.setsampwidth(2)
            wav_file.setframerate(self.sample_rate)

            for frame in range(num_frames):
                phase = frame * 2 * math.pi * frequency / self.sample_rate
                sample = int(amplitude * 32767 * math.sin(phase))
                wav_file.writeframesraw(sample.to_bytes(2, "little", signed=True))

        return duration_ms


class UnavailableAudioProvider:
    def __init__(self, reason: str) -> None:
        self.reason = reason

    def synthesize(self, text: str, output_path: Path) -> int:
        raise RuntimeError(self.reason)


class OpenAIAudioProvider:
    def __init__(
        self,
        api_key: str,
        base_url: str,
        model: str,
        voice: str,
        fmt: str,
        timeout_seconds: int,
    ) -> None:
        if fmt.lower() != "wav":
            raise ValueError("OpenAI-compatible provider currently supports wav format only")

        self.api_key = api_key
        self.base_url = base_url.rstrip("/")
        self.model = model
        self.voice = voice
        self.format = fmt
        self.timeout_seconds = timeout_seconds

    def synthesize(self, text: str, output_path: Path) -> int:
        response = requests.post(
            urljoin(f"{self.base_url}/", "audio/speech"),
            json={
                "model": self.model,
                "input": text,
                "voice": self.voice,
                "response_format": self.format,
            },
            headers={
                "Authorization": f"Bearer {self.api_key}",
                "Content-Type": "application/json",
            },
            timeout=self.timeout_seconds,
        )
        if response.status_code != 200:
            raise RuntimeError(
                f"OpenAI-compatible TTS request failed ({response.status_code}): {response.text[:240]}"
            )

        output_path.write_bytes(response.content)
        with wave.open(str(output_path), "rb") as wav_file:
            frames = wav_file.getnframes()
            rate = wav_file.getframerate()
            return round(frames / rate * 1000)


def build_audio_provider(
    *,
    provider: str | None = None,
    use_mock: bool = False,
    allow_missing_provider: bool = False,
) -> AudioProvider:
    configured = (provider or os.getenv("LMC_TTS_PROVIDER") or "mock").strip().lower()

    if use_mock or configured == "mock":
        return MockAudioProvider()

    if configured == "openai":
        key = os.getenv("OPENAI_API_KEY") or os.getenv("LMC_TTS_API_KEY")
        if not key:
            if allow_missing_provider:
                return UnavailableAudioProvider("Missing OPENAI_API_KEY")
            raise RuntimeError("Missing OPENAI_API_KEY")

        return OpenAIAudioProvider(
            api_key=key,
            base_url=os.getenv("LMC_TTS_BASE_URL", "https://api.openai.com/v1"),
            model=os.getenv("LMC_TTS_MODEL", "tts-1"),
            voice=os.getenv("LMC_TTS_VOICE", "alloy"),
            fmt=os.getenv("LMC_TTS_FORMAT", "wav"),
            timeout_seconds=_parse_timeout_seconds(),
        )

    raise ValueError(f"Unsupported LMC_TTS_PROVIDER={configured}")


def generate_audio_manifest(
    story: dict[str, Any],
    story_dir: Path,
    *,
    provider: str | None = None,
    use_mock: bool = False,
    allow_missing_provider: bool = False,
    clear_existing: bool = True,
) -> list[dict[str, Any]]:
    paragraphs = story.get("paragraphs")
    if not isinstance(paragraphs, list):
        raise ValueError("story.paragraphs must be a list")

    audio_dir = story_dir / "audio"
    if clear_existing and audio_dir.exists():
        for old in sorted(audio_dir.glob("*.wav")):
            old.unlink()
    audio_dir.mkdir(parents=True, exist_ok=True)

    provider_impl = build_audio_provider(
        provider=provider,
        use_mock=use_mock,
        allow_missing_provider=allow_missing_provider,
    )

    entries: list[AudioSentence] = []
    for expected in build_sentence_plan(paragraphs):
        audio_path = story_dir / expected["audioPath"]
        text = expected["text"]

        unavailable = False
        duration_ms = 0

        try:
            duration_ms = provider_impl.synthesize(text, audio_path)
            if not audio_path.exists() or audio_path.stat().st_size <= 44:
                raise RuntimeError("Generated audio file is empty")
        except Exception as exc:
            unavailable = True
            if audio_path.exists():
                audio_path.unlink()
            if not allow_missing_provider:
                raise RuntimeError(
                    f"Failed to generate audio for paragraph {expected['paraIndex'] + 1}, sentence "
                    f"{expected['sentIndex'] + 1}: {exc}"
                ) from exc

        entries.append(
            AudioSentence(
                paraIndex=expected["paraIndex"],
                sentIndex=expected["sentIndex"],
                text=text,
                audioPath=expected["audioPath"],
                durationMs=duration_ms,
                unavailable=unavailable,
            )
        )

    manifest = [entry.as_dict() for entry in entries]
    payload = {"sentences": manifest}
    (story_dir / "audio.json").write_text(
        json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    return manifest


def _parse_timeout_seconds() -> int:
    value = os.getenv("LMC_TTS_TIMEOUT_SECONDS", "30")
    try:
        parsed = int(value)
    except ValueError as exc:
        raise ValueError(f"LMC_TTS_TIMEOUT_SECONDS must be an integer, got {value!r}") from exc

    if parsed <= 0:
        raise ValueError(f"LMC_TTS_TIMEOUT_SECONDS must be > 0, got {parsed}")

    return parsed


def _mock_duration_ms(text: str) -> int:
    text_length = len(text.strip())
    return max(_min_or_one(350), min(1500, 350 + text_length * 95))


def _min_or_one(value: int) -> int:
    return max(1, value)


def _mock_frequency(text: str) -> int:
    digest = hashlib.sha1(text.encode("utf-8")).digest()
    return 220 + (digest[0] % 180)


def read_audio_manifest(path: Path) -> list[dict[str, Any]]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if isinstance(data, list):
        return data
    if isinstance(data, dict) and isinstance(data.get("sentences"), list):
        return data["sentences"]
    raise ValueError("audio.json format must be a list or an object with 'sentences'")
