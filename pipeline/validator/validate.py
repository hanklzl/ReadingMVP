from __future__ import annotations

import argparse
import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable

from jsonschema import Draft202012Validator


HANZI_RE = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff\uf900-\ufaff]")


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


def count_hanzi(text: str) -> int:
    return len(HANZI_RE.findall(text))


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


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

    for key in ("title_zh", "title_en", "source_note", "source_url", "retell_prompt"):
        if not str(story.get(key, "")).strip():
            errors.append(f"{key} must be non-empty")

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
    )


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
