from __future__ import annotations

import argparse
import json
import re
import wave
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable

from jsonschema import Draft202012Validator

from transformer.audio import WAV_DURATION_TOLERANCE_MS, build_sentence_plan, read_audio_manifest, wav_duration_ms
from transformer.polyphone_overrides import (
    DE_FULL_TONE_WORDS as _DE_FULL_TONE_WORDS,
    DE_MUST_WORDS as _DE_MUST_WORDS,
    DE_NEUTRAL_WORDS as _DE_NEUTRAL_WORDS,
    DI_NOUN_WORDS as _DI_NOUN_WORDS,
    phrase_guardrail_errors,
)


HANZI_RE = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]")
HANZI_CHAR_RE = re.compile(r"^[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]$")


@dataclass(frozen=True)
class ValidationResult:
    story_id: str
    path: Path | None
    passed: bool
    errors: list[str]
    hanzi_count: int
    paragraph_count: int
    vocab_count: int
    question_count: int
    audio_entry_count: int


def count_hanzi(text: str) -> int:
    return len(HANZI_RE.findall(text))


def is_hanzi_char(char: str) -> bool:
    return bool(HANZI_CHAR_RE.fullmatch(char))


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


# --- Lightweight polyphone (多音字) lint -------------------------------------
# A small, high-confidence regression guard for the most common pinyin mistakes
# in children's 《三国演义》 retellings. It intentionally does NOT try to cover
# every heteronym; it only fires on patterns that are almost always wrong so it
# stays free of false positives. See docs/specs reading-platform-design §6.

# Structural particles that should always carry the neutral tone in this corpus.
_NEUTRAL_PARTICLE_READINGS = {
    "的": {"de"},
    "地": {"de", "dì"},   # 地 is also the noun "ground/place" (dì); both allowed, lint checks position.
    "得": {"de", "dé", "děi"},  # full-tone 得 only allowed inside known words (below).
    "了": {"le", "liǎo"},
    "着": {"zhe", "zháo", "zhāo", "zhuó"},
}


def lint_polyphone_readings(para_index: int, cells: list[Any]) -> list[str]:
    """Flag high-confidence polyphone (多音字) reading mistakes.

    Heuristics (deliberately conservative):
    - 的 must be neutral-tone ``de``.
    - 长 in the place name 长坂 must read ``cháng``.
    - 得 between a verb/adjective and its complement (V得C) must read ``de``;
      a full tone is only allowed inside a narrow known word list (得到/获得 …).
    - 地 used as the adverbial particle (X地V, i.e. preceded by a hanzi and
      followed by a hanzi, and not part of a 地-noun word) must read ``de``.
    """
    errors: list[str] = []
    text = "".join(str(cell.get("c", "")) if isinstance(cell, dict) else "" for cell in cells)
    readings = [str(cell.get("p", "")) if isinstance(cell, dict) else "" for cell in cells]
    errors.extend(phrase_guardrail_errors(para_index=para_index, text=text, readings=readings))

    def cell_char(i: int) -> str:
        if 0 <= i < len(cells) and isinstance(cells[i], dict):
            return str(cells[i].get("c", ""))
        return ""

    for i, cell in enumerate(cells):
        if not isinstance(cell, dict):
            continue
        char = str(cell.get("c", ""))
        reading = str(cell.get("p", ""))
        prev_char = cell_char(i - 1)
        next_char = cell_char(i + 1)
        prev_is_hanzi = is_hanzi_char(prev_char)
        next_is_hanzi = is_hanzi_char(next_char)

        # 的卢 is the proper-noun horse name, read dí lú (per MOE dict); exempt from the
        # neutral-tone 的=de rule.
        if char == "的" and text[i : i + 2] == "的卢" and reading == "dí":
            continue

        if char == "的" and reading != "de":
            errors.append(
                f"paragraph {para_index} cell {i + 1} 的 should read neutral-tone 'de', got '{reading}'"
            )

        elif char == "长" and reading == "zhǎng" and next_char == "坂":
            errors.append(
                f"paragraph {para_index} cell {i + 1} 长 in place name 长坂 should read 'cháng', got '{reading}'"
            )

        elif char == "得":
            word_prev = prev_char + char
            word_next = char + next_char
            in_neutral_word = word_prev in _DE_NEUTRAL_WORDS or word_next in _DE_NEUTRAL_WORDS
            in_must_word = word_prev in _DE_MUST_WORDS or word_next in _DE_MUST_WORDS
            in_full_tone_word = word_prev in _DE_FULL_TONE_WORDS or word_next in _DE_FULL_TONE_WORDS

            if in_neutral_word and reading != "de":
                errors.append(
                    f"paragraph {para_index} cell {i + 1} 得 in neutral-tone word should read 'de', "
                    f"got '{reading}' (context: …{prev_char}得{next_char}…)"
                )
            elif in_must_word:
                if reading != "děi":
                    errors.append(
                        f"paragraph {para_index} cell {i + 1} 得 meaning 'must' should read 'děi', "
                        f"got '{reading}' (context: …{prev_char}得{next_char}…)"
                    )
            elif in_full_tone_word:
                if reading != "dé":
                    errors.append(
                        f"paragraph {para_index} cell {i + 1} 得 in full-tone word should read 'dé', "
                        f"got '{reading}' (context: …{prev_char}得{next_char}…)"
                    )
            # V得C structural-complement pattern: hanzi 得 hanzi, not a known 得-word.
            elif reading in {"dé", "děi"} and prev_is_hanzi and next_is_hanzi:
                errors.append(
                    f"paragraph {para_index} cell {i + 1} 得 in V得C complement should read neutral-tone 'de', "
                    f"got '{reading}' (context: …{prev_char}得{next_char}…)"
                )

        elif char == "地" and reading == "dì":
            word_prev = prev_char + char
            word_next = char + next_char
            in_noun_word = word_prev in _DI_NOUN_WORDS or word_next in _DI_NOUN_WORDS
            # Adverbial particle pattern: X地V (hanzi 地 hanzi), not a 地-noun word.
            # An adverbial 地 is followed by a verb, never by 的 (that marks 地 as a
            # noun being modified, e.g. 扫地的人), so 地的 is treated as the noun reading.
            if prev_is_hanzi and next_is_hanzi and not in_noun_word and next_char != "的":
                errors.append(
                    f"paragraph {para_index} cell {i + 1} 地 as adverbial particle should read neutral-tone 'de', "
                    f"got '{reading}' (context: …{prev_char}地{next_char}…)"
                )

    return errors


def validate_story(story: dict[str, Any], schema: dict[str, Any], path: Path | None = None) -> ValidationResult:
    errors: list[str] = []

    schema_validator = Draft202012Validator(schema)
    for error in sorted(schema_validator.iter_errors(story), key=lambda item: list(item.path)):
        field_path = ".".join(str(part) for part in error.absolute_path) or "<root>"
        errors.append(f"{field_path}: {error.message}")

    story_id = str(story.get("id") or (path.parent.name if path else "<unknown>"))
    paragraphs = story.get("paragraphs") if isinstance(story.get("paragraphs"), list) else []
    vocab = story.get("vocab") if isinstance(story.get("vocab"), list) else []
    questions = story.get("questions") if isinstance(story.get("questions"), list) else []
    body = "".join(str(paragraph.get("text", "")) for paragraph in paragraphs if isinstance(paragraph, dict))
    hanzi_count = count_hanzi(body)

    if not (300 <= hanzi_count <= 600):
        errors.append(f"body hanzi count must be 300-600, got {hanzi_count}")

    if not (4 <= len(paragraphs) <= 8):
        errors.append(f"paragraph count must be 4-8, got {len(paragraphs)}")

    if not (5 <= len(vocab) <= 8):
        errors.append(f"vocab count must be 5-8, got {len(vocab)}")

    if len(questions) != 3:
        errors.append(f"questions count must be exactly 3, got {len(questions)}")

    for index, paragraph in enumerate(paragraphs, start=1):
        if not isinstance(paragraph, dict):
            errors.append(f"paragraph {index} must be an object")
            continue
        if not str(paragraph.get("text", "")).strip():
            errors.append(f"paragraph {index} text must be non-empty")
        if not str(paragraph.get("pinyin", "")).strip():
            errors.append(f"paragraph {index} pinyin must be non-empty")
        text = paragraph.get("text") if isinstance(paragraph.get("text"), str) else ""
        cells = paragraph.get("cells")
        if not isinstance(cells, list):
            errors.append(f"paragraph {index} cells must be a list")
            continue

        text_chars = list(text)
        if len(cells) != len(text_chars):
            errors.append(
                f"paragraph {index} cells length must equal text length, got {len(cells)} cells "
                f"for {len(text_chars)} characters"
            )

        for cell_index, expected_char in enumerate(text_chars, start=1):
            if cell_index > len(cells):
                break
            cell = cells[cell_index - 1]
            if not isinstance(cell, dict):
                errors.append(f"paragraph {index} cell {cell_index} must be an object")
                continue

            actual_char = cell.get("c")
            pinyin_value = cell.get("p")
            if actual_char != expected_char:
                errors.append(
                    f"paragraph {index} cell {cell_index} c must equal text character "
                    f"'{expected_char}', got '{actual_char}'"
                )

            if is_hanzi_char(expected_char):
                if not isinstance(pinyin_value, str) or not pinyin_value.strip():
                    errors.append(f"paragraph {index} cell {cell_index} hanzi '{expected_char}' must have non-empty pinyin")
            elif pinyin_value != "":
                errors.append(f"paragraph {index} cell {cell_index} non-hanzi '{expected_char}' must have empty pinyin")

    for key in ("title_zh", "title_en", "source_note", "source_url", "retell_prompt"):
        if not str(story.get(key, "")).strip():
            errors.append(f"{key} must be non-empty")

    audio_errors = validate_audio_manifest(story, path)
    errors.extend(audio_errors)

    for index, paragraph in enumerate(paragraphs, start=1):
        if isinstance(paragraph, dict) and isinstance(paragraph.get("cells"), list):
            errors.extend(lint_polyphone_readings(index, paragraph["cells"]))

    for index, item in enumerate(vocab, start=1):
        if not isinstance(item, dict):
            errors.append(f"vocab {index} must be an object")
            continue
        word = str(item.get("word", "")).strip()
        if word and word not in body:
            errors.append(f"vocab {index} word '{word}' must appear in story text")
        for key in ("word", "pinyin", "meaning"):
            if not str(item.get(key, "")).strip():
                errors.append(f"vocab {index} {key} must be non-empty")

    for index, question in enumerate(questions, start=1):
        if not isinstance(question, dict):
            errors.append(f"question {index} must be an object")
            continue
        question_id = str(question.get("id") or f"question {index}")
        options = question.get("options")
        answer = question.get("answer")
        if isinstance(options, list) and answer not in options:
            errors.append(f"{question_id} answer must equal one of options")

    return ValidationResult(
        story_id=story_id,
        path=path,
        passed=not errors,
        errors=errors,
        hanzi_count=hanzi_count,
        paragraph_count=len(paragraphs),
        vocab_count=len(vocab),
        question_count=len(questions),
        audio_entry_count=count_audio_entries(path),
    )


def validate_audio_manifest(story: dict[str, Any], path: Path | None) -> list[str]:
    if path is None:
        return ["audio manifest validation requires story path"]

    manifest_path = path.parent / "audio.json"
    if not manifest_path.exists():
        return [f"{path.parent.name} missing audio.json"]

    try:
        actual_entries = read_audio_manifest(manifest_path)
    except Exception as exc:
        return [f"audio.json in {path.parent.name} is invalid: {exc}"]

    expected_plan = build_sentence_plan(story.get("paragraphs", []) if isinstance(story.get("paragraphs"), list) else [])

    if len(actual_entries) != len(expected_plan):
        return [
            f"{path.parent.name} audio entry count mismatch: expected {len(expected_plan)} "
            f"but got {len(actual_entries)}"
        ]

    errors: list[str] = []
    actual_by_key: dict[tuple[int, int], dict[str, Any]] = {}
    for index, entry in enumerate(actual_entries):
        if not isinstance(entry, dict):
            errors.append(f"{path.parent.name} audio entry #{index} must be an object")
            continue

        para_index = entry.get("paraIndex")
        sent_index = entry.get("sentIndex")

        if not isinstance(para_index, int) or para_index < 0:
            errors.append(f"{path.parent.name} audio entry #{index}: paraIndex must be an integer >= 0")
            continue
        if not isinstance(sent_index, int) or sent_index < 0:
            errors.append(f"{path.parent.name} audio entry #{index}: sentIndex must be an integer >= 0")
            continue

        key = (para_index, sent_index)
        if key in actual_by_key:
            errors.append(
                f"{path.parent.name} audio has duplicate entry for paraIndex={para_index}, sentIndex={sent_index}"
            )
            continue

        actual_by_key[key] = entry

    if errors:
        return errors

    for expected in expected_plan:
        key = (expected["paraIndex"], expected["sentIndex"])
        entry = actual_by_key.get(key)
        if entry is None:
            errors.append(
                f"{path.parent.name} missing audio entry paraIndex={expected['paraIndex']}, sentIndex={expected['sentIndex']}"
            )
            continue

        expected_text = expected["text"]
        actual_text = str(entry.get("text", "")).strip()
        if actual_text != expected_text:
            errors.append(
                f"{path.parent.name} paraIndex={expected['paraIndex']}, sentIndex={expected['sentIndex']} text mismatch: "
                f"expected {expected_text!r}, got {actual_text!r}"
            )

        audio_path = entry.get("audioPath")
        unavailable = entry.get("unavailable", False)
        if unavailable is not False and unavailable is not True:
            errors.append(
                f"{path.parent.name} paraIndex={expected['paraIndex']}, sentIndex={expected['sentIndex']}: "
                "unavailable must be true when present"
            )

        if unavailable:
            continue

        if not isinstance(audio_path, str):
            errors.append(
                f"{path.parent.name} paraIndex={expected['paraIndex']}, sentIndex={expected['sentIndex']} "
                "missing audioPath"
            )
            continue

        rel_path = Path(audio_path)
        if rel_path.is_absolute() or ".." in rel_path.parts:
            errors.append(
                f"{path.parent.name} paraIndex={expected['paraIndex']}, sentIndex={expected['sentIndex']} has invalid audioPath {audio_path!r}"
            )
            continue

        if rel_path.as_posix() != expected["audioPath"]:
            errors.append(
                f"{path.parent.name} paraIndex={expected['paraIndex']}, sentIndex={expected['sentIndex']} "
                f"audioPath mismatch: expected {expected['audioPath']!r}, got {audio_path!r}"
            )

        audio_file = path.parent / rel_path
        if not audio_file.exists() or not audio_file.is_file():
            errors.append(
                f"{path.parent.name} paraIndex={expected['paraIndex']}, sentIndex={expected['sentIndex']} missing audio file {audio_path!r}"
            )

        duration_ms = entry.get("durationMs")
        if not isinstance(duration_ms, int) or duration_ms < 0:
            errors.append(
                f"{path.parent.name} paraIndex={expected['paraIndex']}, sentIndex={expected['sentIndex']} "
                "durationMs must be an integer >= 0"
            )
        elif audio_file.exists() and audio_file.is_file():
            try:
                actual_duration_ms = wav_duration_ms(audio_file)
            except wave.Error as exc:
                errors.append(
                    f"{path.parent.name} paraIndex={expected['paraIndex']}, sentIndex={expected['sentIndex']} "
                    f"audio file {audio_path!r} is not a readable wav: {exc}"
                )
            else:
                if abs(duration_ms - actual_duration_ms) > WAV_DURATION_TOLERANCE_MS:
                    errors.append(
                        f"{path.parent.name} paraIndex={expected['paraIndex']}, sentIndex={expected['sentIndex']} "
                        f"durationMs {duration_ms} does not match wav duration {actual_duration_ms} "
                        f"for {audio_path!r}"
                    )

        errors.extend(
            validate_char_timings(
                story_name=path.parent.name,
                para_index=expected["paraIndex"],
                sent_index=expected["sentIndex"],
                text=expected_text,
                duration_ms=duration_ms if isinstance(duration_ms, int) else None,
                chars=entry.get("chars"),
            )
        )

    expected_keys = {(item["paraIndex"], item["sentIndex"]) for item in expected_plan}
    for extra_key, entry in sorted(actual_by_key.items(), key=lambda item: item[0]):
        key = extra_key
        if key not in expected_keys:
            errors.append(
                f"{path.parent.name} has extra audio entry paraIndex={key[0]}, sentIndex={key[1]}, "
                f"text {entry.get('text', '')!r}"
            )

    return errors


def validate_char_timings(
    *,
    story_name: str,
    para_index: int,
    sent_index: int,
    text: str,
    duration_ms: int | None,
    chars: Any,
) -> list[str]:
    """Validate the per-character karaoke timings for one available sentence.

    Contract (see docs/design/tts-read-along-interaction.md §7.2):
    - ``chars`` is required and 1:1 with the Unicode characters of ``text``
      (non-hanzi characters keep placeholder slots).
    - each ``chars[i].c`` equals ``text[i]``.
    - startMs/endMs are integers, ``0 <= startMs <= endMs`` and monotonic across
      characters (``endMs[i] <= startMs[i+1]``), all within ``[0, durationMs]``.
    """

    prefix = f"{story_name} paraIndex={para_index}, sentIndex={sent_index}"
    text_chars = list(text)

    if chars is None:
        return [f"{prefix} missing chars timing array"]
    if not isinstance(chars, list):
        return [f"{prefix} chars must be a list"]
    if len(chars) != len(text_chars):
        return [
            f"{prefix} chars length must equal sentence text length, "
            f"got {len(chars)} for {len(text_chars)} characters"
        ]

    errors: list[str] = []
    previous_end = 0
    for index, expected_char in enumerate(text_chars):
        cell = chars[index]
        if not isinstance(cell, dict):
            errors.append(f"{prefix} chars[{index}] must be an object")
            continue

        actual_char = cell.get("c")
        if actual_char != expected_char:
            errors.append(
                f"{prefix} chars[{index}] c must equal text character "
                f"'{expected_char}', got '{actual_char}'"
            )

        start_ms = cell.get("startMs")
        end_ms = cell.get("endMs")
        if not isinstance(start_ms, int) or start_ms < 0:
            errors.append(f"{prefix} chars[{index}] startMs must be an integer >= 0")
            continue
        if not isinstance(end_ms, int) or end_ms < start_ms:
            errors.append(f"{prefix} chars[{index}] endMs must be an integer >= startMs")
            continue
        if start_ms < previous_end:
            errors.append(
                f"{prefix} chars[{index}] startMs {start_ms} must not precede previous endMs {previous_end}"
            )
        if duration_ms is not None and end_ms > duration_ms:
            errors.append(
                f"{prefix} chars[{index}] endMs {end_ms} must not exceed durationMs {duration_ms}"
            )
        previous_end = end_ms

    return errors


def count_audio_entries(path: Path | None) -> int:
    if path is None:
        return 0
    manifest_path = path.parent / "audio.json"
    if not manifest_path.exists():
        return 0
    try:
        return len(read_audio_manifest(manifest_path))
    except Exception:
        return 0


def validate_story_file(story_path: Path, schema_path: Path) -> ValidationResult:
    return validate_story(load_json(story_path), load_json(schema_path), story_path)


def story_files(paths: Iterable[Path]) -> list[Path]:
    files: list[Path] = []
    for path in paths:
        if path.is_file():
            files.append(path)
        elif path.is_dir():
            files.extend(sorted(path.glob("*/story.json")))
        else:
            raise FileNotFoundError(path)
    return sorted(files)


def format_result(result: ValidationResult) -> str:
    stats = (
        f"hanzi={result.hanzi_count}, paragraphs={result.paragraph_count}, "
        f"vocab={result.vocab_count}, questions={result.question_count}"
    )
    if result.passed:
        return f"{result.story_id}: PASS ({stats})"
    return f"{result.story_id}: FAIL ({stats})\n  - " + "\n  - ".join(result.errors)


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate Little Mandarin Classics story JSON files.")
    parser.add_argument(
        "paths",
        nargs="*",
        type=Path,
        default=[Path("content/stories")],
        help="Story JSON files or directories containing <id>/story.json files.",
    )
    parser.add_argument(
        "--schema",
        type=Path,
        default=Path("content/schema/story.schema.json"),
        help="Path to story.schema.json.",
    )
    args = parser.parse_args()

    results = [validate_story_file(path, args.schema) for path in story_files(args.paths)]
    for result in results:
        print(format_result(result))

    passed = sum(1 for result in results if result.passed)
    failed = len(results) - passed
    print(f"Summary: {passed} passed, {failed} failed")
    return 0 if failed == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
