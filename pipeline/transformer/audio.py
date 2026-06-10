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
        chars: list[dict[str, Any]] | None = None,
    ) -> None:
        self.paraIndex = paraIndex
        self.sentIndex = sentIndex
        self.text = text
        self.audioPath = audioPath
        self.durationMs = durationMs
        self.unavailable = unavailable
        self.chars = chars or []

    def as_dict(self) -> dict[str, Any]:
        payload = {
            "paraIndex": self.paraIndex,
            "sentIndex": self.sentIndex,
            "text": self.text,
            "audioPath": self.audioPath,
            "durationMs": self.durationMs,
        }
        if self.chars:
            payload["chars"] = self.chars
        if self.unavailable:
            payload["unavailable"] = True
        return payload


class AudioProvider(Protocol):
    def synthesize(self, text: str, output_path: Path) -> int:
        ...


class CharAligner(Protocol):
    """Produces per-character (text, audio) timestamps in milliseconds.

    Implementations return a list aligned 1:1 with the Unicode characters of
    ``text`` (non-hanzi characters keep a placeholder slot), each item being
    ``{"c": <char>, "startMs": int, "endMs": int}`` with monotonic, in-range
    timings (``0 <= startMs <= endMs <= duration_ms``).
    """

    def align(self, text: str, audio_path: Path, duration_ms: int) -> list[dict[str, Any]]:
        ...


def even_char_timings(text: str, duration_ms: int) -> list[dict[str, Any]]:
    """Synthesize per-character timings by dividing duration evenly across chars.

    Used by the mock provider and as a deterministic fallback whenever a real
    forced aligner is unavailable. Every Unicode character (including punctuation
    and whitespace) gets one slot so the array aligns 1:1 with ``text``; this lets
    the app's karaoke highlight and the manifest schema be verified end to end
    without any heavyweight model.
    """

    chars = list(text)
    count = len(chars)
    if count == 0:
        return []

    total = max(0, int(duration_ms))
    timings: list[dict[str, Any]] = []
    for index, char in enumerate(chars):
        start_ms = round(total * index / count)
        end_ms = round(total * (index + 1) / count)
        if end_ms < start_ms:
            end_ms = start_ms
        timings.append({"c": char, "startMs": start_ms, "endMs": end_ms})

    # Pin the final boundary to the clip duration so highlight never overruns.
    if timings:
        timings[-1]["endMs"] = total
        if timings[-1]["startMs"] > total:
            timings[-1]["startMs"] = total
    return timings


def char_timings_from_alignment(
    text: str,
    aligned_segments: list[dict[str, Any]],
    duration_ms: int,
) -> list[dict[str, Any]]:
    """Map forced-aligner character segments onto the per-character manifest array.

    ``aligned_segments`` is a list of ``{"c", "startMs", "endMs"}`` produced from a
    forced aligner (which typically only emits timings for spoken/aligned units).
    Non-spoken characters (punctuation, spaces, Latin, digits) that the aligner
    skipped are back-filled by interpolating between their neighbours so the
    output stays 1:1 with ``text`` and monotonic. If alignment cannot be matched
    to the text, falls back to :func:`even_char_timings`.
    """

    chars = list(text)
    if not chars:
        return []

    total = max(0, int(duration_ms))
    aligned_by_char: list[dict[str, Any]] = [
        segment for segment in aligned_segments if isinstance(segment, dict)
    ]

    # Greedily consume aligner units in text order, matching on the character.
    timings: list[dict[str, Any] | None] = [None] * len(chars)
    cursor = 0
    for segment in aligned_by_char:
        unit_text = str(segment.get("c", ""))
        if not unit_text:
            continue
        while cursor < len(chars) and chars[cursor] != unit_text[0]:
            cursor += 1
        if cursor >= len(chars):
            break
        start_ms = _coerce_ms(segment.get("startMs"), total)
        end_ms = _coerce_ms(segment.get("endMs"), total)
        if end_ms < start_ms:
            end_ms = start_ms
        # Spread a multi-character aligned unit across its characters.
        unit_len = max(1, len(unit_text))
        for offset in range(unit_len):
            if cursor + offset >= len(chars):
                break
            slot_start = round(start_ms + (end_ms - start_ms) * offset / unit_len)
            slot_end = round(start_ms + (end_ms - start_ms) * (offset + 1) / unit_len)
            timings[cursor + offset] = {
                "c": chars[cursor + offset],
                "startMs": slot_start,
                "endMs": max(slot_start, slot_end),
            }
        cursor += unit_len

    if all(slot is None for slot in timings):
        return even_char_timings(text, duration_ms)

    return _backfill_char_timings(chars, timings, total)


def _coerce_ms(value: Any, total: int) -> int:
    try:
        ms = int(round(float(value)))
    except (TypeError, ValueError):
        return 0
    return max(0, min(ms, total))


def _backfill_char_timings(
    chars: list[str],
    timings: list[dict[str, Any] | None],
    total: int,
) -> list[dict[str, Any]]:
    last_end = 0
    for index, char in enumerate(chars):
        slot = timings[index]
        if slot is None:
            # Find the next aligned boundary to interpolate towards.
            next_start = total
            for forward in range(index + 1, len(chars)):
                if timings[forward] is not None:
                    next_start = int(timings[forward]["startMs"])
                    break
            start_ms = last_end
            end_ms = max(start_ms, next_start)
            timings[index] = {"c": char, "startMs": start_ms, "endMs": end_ms}
        else:
            slot["startMs"] = max(int(slot["startMs"]), last_end)
            slot["endMs"] = max(int(slot["endMs"]), slot["startMs"])
        last_end = int(timings[index]["endMs"])

    resolved = [slot for slot in timings if slot is not None]
    if resolved:
        resolved[-1]["endMs"] = max(int(resolved[-1]["startMs"]), total)
    return resolved


class NoOpCharAligner:
    """Char aligner that always falls back to evenly divided timings."""

    def align(self, text: str, audio_path: Path, duration_ms: int) -> list[dict[str, Any]]:
        return even_char_timings(text, duration_ms)


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


class QwenAudioProvider:
    """Local Qwen3-TTS provider (open-source, CPU-capable).

    Wraps the ``qwen-tts`` package's ``Qwen3TTSModel`` (model id
    ``Qwen/Qwen3-TTS-12Hz-1.7B-CustomVoice`` by default, speaker ``Serena``). The
    model id, voice/speaker, reference audio (for voice cloning) and language are
    all read from config / env so nothing is hard-coded. ``torch``/``qwen-tts``
    are imported lazily so
    this module still loads (and the mock path still runs) when the heavyweight
    dependencies are absent.

    Two synthesis modes are supported, matching the two published checkpoints:
      - voice clone (``*-Base``): pass a reference clip via ``ref_audio``/``ref_text``
        so the whole series keeps one warm storytelling voice.
      - custom voice (``*-CustomVoice``): pass a named ``speaker`` instead.
    """

    sample_rate = 24000

    def __init__(
        self,
        *,
        model_id: str,
        voice: str | None,
        language: str,
        ref_audio: str | None,
        ref_text: str | None,
        device: str,
    ) -> None:
        self.model_id = model_id
        self.voice = voice
        self.language = language
        self.ref_audio = ref_audio
        self.ref_text = ref_text
        self.device = device
        self._model: Any | None = None
        self._soundfile: Any | None = None

    def _ensure_model(self) -> Any:
        if self._model is not None:
            return self._model

        try:
            import torch  # type: ignore
            import soundfile  # type: ignore
            from qwen_tts import Qwen3TTSModel  # type: ignore
        except ImportError as exc:  # pragma: no cover - exercised only with real deps
            raise RuntimeError(
                "Qwen TTS provider requires 'qwen-tts', 'torch' and 'soundfile'. "
                "Install with: pip install -U qwen-tts soundfile torch"
            ) from exc

        dtype = torch.float32 if self.device == "cpu" else torch.bfloat16
        self._model = Qwen3TTSModel.from_pretrained(
            self.model_id,
            device_map=self.device,
            dtype=dtype,
        )
        self._soundfile = soundfile
        return self._model

    def synthesize(self, text: str, output_path: Path) -> int:  # pragma: no cover - needs model
        model = self._ensure_model()
        output_path.parent.mkdir(parents=True, exist_ok=True)

        if self.ref_audio:
            wavs, sample_rate = model.generate_voice_clone(
                text=text,
                language=self.language,
                ref_audio=self.ref_audio,
                ref_text=self.ref_text or "",
            )
        else:
            wavs, sample_rate = model.generate_custom_voice(
                text=text,
                language=self.language,
                speaker=self.voice,
            )

        self._soundfile.write(str(output_path), wavs[0], sample_rate)
        with wave.open(str(output_path), "rb") as wav_file:
            frames = wav_file.getnframes()
            rate = wav_file.getframerate() or sample_rate or self.sample_rate
            return round(frames / rate * 1000)


class Qwen3CharAligner:
    """Per-character forced aligner backed by Qwen3-ForcedAligner-0.6B.

    Wraps ``qwen-asr``'s ``Qwen3ForcedAligner`` (model id
    ``Qwen/Qwen3-ForcedAligner-0.6B``). ``align(audio, text, language)`` returns a
    list whose first element is a list of segments, each exposing ``.text``,
    ``.start_time`` and ``.end_time`` in seconds. For Chinese this is
    character-level by default. We convert seconds → ms and reconcile against the
    full ``text`` via :func:`char_timings_from_alignment`. Imports are lazy and any
    failure falls back to evenly divided timings so generation never hard-fails on
    alignment alone.
    """

    def __init__(self, *, model_id: str, language: str, device: str) -> None:
        self.model_id = model_id
        self.language = language
        self.device = device
        self._model: Any | None = None

    def _ensure_model(self) -> Any:  # pragma: no cover - needs model
        if self._model is not None:
            return self._model

        try:
            import torch  # type: ignore
            from qwen_asr import Qwen3ForcedAligner  # type: ignore
        except ImportError as exc:
            raise RuntimeError(
                "Qwen forced aligner requires 'qwen-asr' and 'torch'. "
                "Install with: pip install -U qwen-asr torch"
            ) from exc

        dtype = torch.float32 if self.device == "cpu" else torch.bfloat16
        self._model = Qwen3ForcedAligner.from_pretrained(
            self.model_id,
            dtype=dtype,
            device_map=self.device,
        )
        return self._model

    def align(self, text: str, audio_path: Path, duration_ms: int) -> list[dict[str, Any]]:
        try:  # pragma: no cover - needs model
            model = self._ensure_model()
            results = model.align(
                audio=str(audio_path),
                text=text,
                language=self.language,
            )
            segments = results[0] if results else []
            aligned = [
                {
                    "c": str(getattr(segment, "text", "")),
                    "startMs": round(float(getattr(segment, "start_time", 0.0)) * 1000),
                    "endMs": round(float(getattr(segment, "end_time", 0.0)) * 1000),
                }
                for segment in segments
            ]
            return char_timings_from_alignment(text, aligned, duration_ms)
        except Exception:
            # Alignment is best-effort; never block audio generation on it.
            return even_char_timings(text, duration_ms)


def build_char_aligner(
    *,
    provider: str,
    use_mock: bool,
) -> CharAligner:
    """Select the per-character aligner for the active provider.

    Mock/offline runs and the OpenAI path use evenly divided timings so the
    karaoke schema can be exercised without any model. ``qwen`` uses the real
    forced aligner unless explicitly disabled via ``LMC_TTS_ALIGNER=none``.
    """

    if use_mock:
        return NoOpCharAligner()

    aligner = (os.getenv("LMC_TTS_ALIGNER") or ("qwen" if provider == "qwen" else "none")).strip().lower()
    if aligner == "qwen":
        return Qwen3CharAligner(
            model_id=os.getenv("LMC_TTS_ALIGNER_MODEL", "Qwen/Qwen3-ForcedAligner-0.6B"),
            language=os.getenv("LMC_TTS_LANGUAGE", "Chinese"),
            device=os.getenv("LMC_TTS_DEVICE", "cpu"),
        )
    return NoOpCharAligner()


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

    if configured == "qwen":
        return QwenAudioProvider(
            # Default to the CustomVoice 1.7B checkpoint (preset speakers, no ref
            # audio needed). "Serena" is the warm, gentle young-female Mandarin
            # speaker best suited to telling stories to young children. Override
            # via LMC_TTS_MODEL / LMC_TTS_VOICE; set LMC_TTS_REF_AUDIO to switch
            # to a *-Base voice-clone checkpoint instead.
            model_id=os.getenv("LMC_TTS_MODEL", "Qwen/Qwen3-TTS-12Hz-1.7B-CustomVoice"),
            voice=os.getenv("LMC_TTS_VOICE") or "Serena",
            language=os.getenv("LMC_TTS_LANGUAGE", "Chinese"),
            ref_audio=os.getenv("LMC_TTS_REF_AUDIO") or None,
            ref_text=os.getenv("LMC_TTS_REF_TEXT") or None,
            device=os.getenv("LMC_TTS_DEVICE", "cpu"),
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

    configured_provider = (provider or os.getenv("LMC_TTS_PROVIDER") or "mock").strip().lower()
    provider_impl = build_audio_provider(
        provider=provider,
        use_mock=use_mock,
        allow_missing_provider=allow_missing_provider,
    )
    char_aligner = build_char_aligner(provider=configured_provider, use_mock=use_mock)

    entries: list[AudioSentence] = []
    for expected in build_sentence_plan(paragraphs):
        audio_path = story_dir / expected["audioPath"]
        text = expected["text"]

        unavailable = False
        duration_ms = 0
        chars: list[dict[str, Any]] = []

        try:
            duration_ms = provider_impl.synthesize(text, audio_path)
            if not audio_path.exists() or audio_path.stat().st_size <= 44:
                raise RuntimeError("Generated audio file is empty")
            chars = char_aligner.align(text, audio_path, duration_ms)
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
                chars=chars,
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
