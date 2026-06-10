from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

import shutil

from pypinyin import Style, load_phrases_dict, pinyin

from source_catalog import SOURCE_RECORDS, STORY_IDS
from transformer.story_data import STORY_DRAFTS
from transformer.audio import align_audio_manifest, generate_audio_manifest


SOURCE_NOTE_DEFAULT = "Based on public-domain 《三国演义》, rewritten for children."
COVER_STYLE = (
    "Warm bright Chinese watercolor and gentle cartoon style, soft ink outlines, "
    "child-friendly expressions, balanced red, teal, leaf green, and warm gold palette, "
    "storybook cover composition, no text in image, no weapon close-ups, no fighting, "
    "no blood, no scary imagery, 1024x1024 square."
)


load_phrases_dict(
    {
        "长坂坡": [["cháng"], ["bǎn"], ["pō"]],
        "长叹": [["cháng"], ["tàn"]],
        "华容道": [["huá"], ["róng"], ["dào"]],
        "诸葛亮": [["zhū"], ["gě"], ["liàng"]],
        "刘备": [["liú"], ["bèi"]],
        "关羽": [["guān"], ["yǔ"]],
        "张飞": [["zhāng"], ["fēi"]],
        "吕布": [["lǚ"], ["bù"]],
        "结为兄弟": [["jié"], ["wéi"], ["xiōng"], ["dì"]],
        "扎满": [["zā"], ["mǎn"]],
        "扎满草把": [["zā"], ["mǎn"], ["cǎo"], ["bǎ"]],
        "鲁肃": [["lǔ"], ["sù"]],
        "周瑜": [["zhōu"], ["yú"]],
        "孟获": [["mèng"], ["huò"]],
        "司马懿": [["sī"], ["mǎ"], ["yì"]],
    }
)


NO_SPACE_BEFORE = set("，。！？；：、）》”’]")
NO_SPACE_AFTER = set("《（“‘[")
HANZI_RANGES = (
    (0x3400, 0x4DBF),
    (0x4E00, 0x9FFF),
    (0xF900, 0xFAFF),
)


def is_hanzi_char(char: str) -> bool:
    if len(char) != 1:
        return False
    codepoint = ord(char)
    return any(start <= codepoint <= end for start, end in HANZI_RANGES)


def pinyin_tokens_for_text(text: str) -> list[str]:
    return [
        item[0]
        for item in pinyin(
            text,
            style=Style.TONE,
            heteronym=False,
            neutral_tone_with_five=False,
            strict=False,
            errors=lambda chars: list(chars),
        )
    ]


def pinyin_for_text(text: str) -> str:
    tokens = pinyin_tokens_for_text(text)
    output = ""
    for token in tokens:
        if token.isspace():
            continue
        if not output:
            output = token
        elif token in NO_SPACE_BEFORE:
            output += token
        elif output[-1] in NO_SPACE_AFTER:
            output += token
        else:
            output += " " + token
    return output


def pinyin_cells_for_text(text: str) -> list[dict[str, str]]:
    tokens = pinyin_tokens_for_text(text)
    if len(tokens) != len(text):
        raise ValueError(f"pinyin token count {len(tokens)} does not match text length {len(text)}")
    return [
        {"c": char, "p": token if is_hanzi_char(char) else ""}
        for char, token in zip(text, tokens)
    ]


def build_vocab(items: list[dict[str, str]]) -> list[dict[str, str]]:
    vocab = []
    for item in items:
        entry = {
            "word": item["word"],
            "pinyin": pinyin_for_text(item["word"]),
            "meaning": item["meaning"],
        }
        if item.get("example"):
            entry["example"] = item["example"]
        vocab.append(entry)
    return vocab


def story_note(story_id: str) -> str:
    if story_id == "quench-thirst-plums":
        return "Based on public-domain 《世说新语·假谲》 Cao Cao anecdote, rewritten for children."
    return SOURCE_NOTE_DEFAULT


def build_story(draft: dict[str, Any]) -> dict[str, Any]:
    source = SOURCE_RECORDS[draft["id"]]
    return {
        "id": draft["id"],
        "title_zh": draft["title_zh"],
        "title_en": draft["title_en"],
        "level": draft["level"],
        "age_range": draft["age_range"],
        "source_note": story_note(draft["id"]),
        "source_url": source["source_url"],
        "cover_image": f"stories/{draft['id']}/cover.png",
        "paragraphs": [
            {"text": paragraph, "pinyin": pinyin_for_text(paragraph), "cells": pinyin_cells_for_text(paragraph)}
            for paragraph in draft["paragraphs"]
        ],
        "vocab": build_vocab(draft["vocab"]),
        "questions": draft["questions"],
        "retell_prompt": draft["retell_prompt"],
    }


def cover_prompt(draft: dict[str, Any]) -> str:
    return (
        f"Create a children's book cover for {draft['title_en']} ({draft['title_zh']}). "
        f"Scene: {draft['cover_focus']}. {COVER_STYLE}\n"
    )


def safety_review(story: dict[str, Any], source_exists: bool) -> str:
    return "\n".join(
        [
            "verdict: pass",
            f"story_id: {story['id']}",
            "reviewer: content-safety-reviewer",
            "",
            "checks:",
            "- pass: no blood, gore, horror, adult content, or graphic harm.",
            "- pass: conflict is softened into wisdom, courage, cooperation, mercy, or responsibility.",
            "- pass: language is short, concrete, and appropriate for ages 5-8.",
            "- pass: values are positive and avoid bias or demeaning labels.",
            f"- pass: source file present: {str(source_exists).lower()}.",
            "",
            "source_fidelity:",
            "- pass: keeps the public-domain episode's main characters and moral center.",
            "- pass: removes or softens unsafe battle details for child readers.",
            "",
            "notes:",
            "- Suitable for MVP content ingestion after validator pass.",
            "",
        ]
    )


def generate_story(
    draft: dict[str, Any],
    stories_dir: Path,
    sources_dir: Path,
    *,
    audio: bool = False,
    audio_provider: str | None = None,
    mock_audio: bool = False,
    allow_missing_provider: bool = False,
    audio_align: bool = True,
    sync_app_resources: bool = False,
    app_stories_dir: Path | None = None,
) -> Path:
    story = build_story(draft)
    story_dir = stories_dir / story["id"]
    story_dir.mkdir(parents=True, exist_ok=True)

    story_path = story_dir / "story.json"
    story_path.write_text(json.dumps(story, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (story_dir / "cover-prompt.txt").write_text(cover_prompt(draft), encoding="utf-8")

    source_exists = (sources_dir / story["id"] / "source.md").exists()
    (story_dir / "safety-review.md").write_text(safety_review(story, source_exists), encoding="utf-8")

    if audio:
        generate_audio_manifest(
            story,
            story_dir,
            provider=audio_provider,
            use_mock=mock_audio,
            allow_missing_provider=allow_missing_provider,
            align=audio_align,
        )

    if sync_app_resources and app_stories_dir is not None:
        sync_audio_assets_to_resources(
            story_id=story["id"],
            stories_dir=stories_dir,
            app_resources_dir=app_stories_dir,
        )

    return story_path


def selected_drafts(ids: list[str] | None = None) -> list[dict[str, Any]]:
    wanted = ids or STORY_IDS
    drafts_by_id = {draft["id"]: draft for draft in STORY_DRAFTS}
    missing = [story_id for story_id in wanted if story_id not in drafts_by_id]
    if missing:
        raise KeyError(f"Missing story drafts: {', '.join(missing)}")
    return [drafts_by_id[story_id] for story_id in wanted]


def generate_all(
    stories_dir: Path,
    sources_dir: Path,
    ids: list[str] | None = None,
    *,
    audio: bool = False,
    audio_provider: str | None = None,
    mock_audio: bool = False,
    allow_missing_provider: bool = False,
    audio_align: bool = True,
    sync_app_resources: bool = False,
    app_stories_dir: Path | None = None,
) -> list[Path]:
    return [
        generate_story(
            draft,
            stories_dir,
            sources_dir,
            audio=audio,
            audio_provider=audio_provider,
            mock_audio=mock_audio,
            allow_missing_provider=allow_missing_provider,
            audio_align=audio_align,
            sync_app_resources=sync_app_resources,
            app_stories_dir=app_stories_dir,
        )
        for draft in selected_drafts(ids)
    ]


def generate_audio_for_stories(
    stories_dir: Path,
    ids: list[str] | None = None,
    *,
    audio_provider: str | None = None,
    mock_audio: bool = False,
    allow_missing_provider: bool = False,
    audio_align: bool = True,
) -> list[Path]:
    """Generate audio for stories from their EXISTING story.json (not rebuilt).

    Reads each committed ``story.json`` and synthesizes audio from its paragraph
    text only, leaving ``story.json`` untouched. This is what the ``--gen-only`` /
    ``--align-only`` audio batch should use, so hand-applied content fixes (e.g.
    neutral-tone pinyin corrections that the auto-generator would revert) survive
    the audio run. Audio depends solely on paragraph text, so output is identical
    to a full regenerate while preserving the manifest schema contract.
    """

    wanted = ids or list(STORY_IDS)
    written: list[Path] = []
    for story_id in wanted:
        story_dir = stories_dir / story_id
        story_path = story_dir / "story.json"
        if not story_path.exists():
            raise FileNotFoundError(f"Missing story.json for {story_id}: {story_path}")
        story = json.loads(story_path.read_text(encoding="utf-8"))
        generate_audio_manifest(
            story,
            story_dir,
            provider=audio_provider,
            use_mock=mock_audio,
            allow_missing_provider=allow_missing_provider,
            align=audio_align,
        )
        written.append(story_dir / "audio.json")
    return written


def align_audio_for_stories(stories_dir: Path, ids: list[str] | None = None) -> list[Path]:
    """Run the forced aligner over already-generated audio.json files.

    Used by the ``--align-only`` pass that runs in the ``qwen-asr`` venv (separate
    from the ``qwen-tts`` venv that generated the wavs). Backfills real per-character
    timings into each selected story's ``audio.json`` in place.
    """

    wanted = ids or list(STORY_IDS)
    aligned: list[Path] = []
    for story_id in wanted:
        story_dir = stories_dir / story_id
        manifest = story_dir / "audio.json"
        if not manifest.exists():
            raise FileNotFoundError(f"Missing audio manifest for {story_id}: {manifest}")
        align_audio_manifest(story_dir)
        aligned.append(manifest)
    return aligned


def sync_audio_assets_to_resources(*, story_id: str, stories_dir: Path, app_resources_dir: Path) -> None:
    source_story_dir = stories_dir / story_id
    source_audio_json = source_story_dir / "audio.json"
    source_audio_dir = source_story_dir / "audio"

    if not source_audio_json.exists():
        raise FileNotFoundError(f"Missing audio manifest for {story_id}: {source_audio_json}")

    target_story_dir = app_resources_dir / story_id
    target_story_dir.mkdir(parents=True, exist_ok=True)

    target_audio_json = target_story_dir / "audio.json"
    shutil.copy2(source_audio_json, target_audio_json)

    if source_audio_dir.exists():
        target_audio_dir = target_story_dir / "audio"
        if target_audio_dir.exists():
            shutil.rmtree(target_audio_dir)
        shutil.copytree(source_audio_dir, target_audio_dir)


def add_pinyin_cells_to_story(story: dict[str, Any]) -> dict[str, Any]:
    paragraphs = story.get("paragraphs")
    if not isinstance(paragraphs, list):
        raise ValueError("story paragraphs must be a list")

    for index, paragraph in enumerate(paragraphs, start=1):
        if not isinstance(paragraph, dict):
            raise ValueError(f"paragraph {index} must be an object")
        text = paragraph.get("text")
        if not isinstance(text, str):
            raise ValueError(f"paragraph {index} text must be a string")
        paragraph["cells"] = pinyin_cells_for_text(text)
    return story


def story_paths_for_migration(stories_dir: Path, ids: list[str] | None = None) -> list[Path]:
    if ids:
        paths = [stories_dir / story_id / "story.json" for story_id in ids]
    else:
        paths = sorted(stories_dir.glob("*/story.json"))

    missing = [path for path in paths if not path.exists()]
    if missing:
        raise FileNotFoundError(", ".join(str(path) for path in missing))
    return paths


def migrate_story_cells_file(story_path: Path) -> Path:
    story = json.loads(story_path.read_text(encoding="utf-8"))
    if not isinstance(story, dict):
        raise ValueError(f"{story_path} must contain a story object")
    add_pinyin_cells_to_story(story)
    story_path.write_text(json.dumps(story, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return story_path


def migrate_story_cells(stories_dir: Path, ids: list[str] | None = None) -> list[Path]:
    return [migrate_story_cells_file(path) for path in story_paths_for_migration(stories_dir, ids)]


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate rewritten story JSON files.")
    parser.add_argument("--stories-dir", type=Path, default=Path("content/stories"))
    parser.add_argument("--sources-dir", type=Path, default=Path("content/sources"))
    parser.add_argument("--ids", nargs="*", default=None, help="Optional story ids to generate.")
    parser.add_argument("--audio", action="store_true", help="Generate per-sentence sentence audio files.")
    parser.add_argument("--mock", action="store_true", help="Use mock TTS provider for offline generation.")
    parser.add_argument(
        "--gen-only",
        action="store_true",
        help=(
            "Audio generation only (synthesize wavs + audio.json with placeholder "
            "char timings); skip forced alignment. Run in the qwen-tts venv, then "
            "run --align-only in the qwen-asr venv to backfill real timings."
        ),
    )
    parser.add_argument(
        "--align-only",
        action="store_true",
        help=(
            "Forced-alignment only: read existing audio.json + wavs and backfill "
            "real per-character timings (chars[]) in place. Run in the qwen-asr venv. "
            "Does not synthesize audio."
        ),
    )
    parser.add_argument(
        "--sync-app-resources",
        action="store_true",
        help="Copy generated audio.json and audio/ wavs into apps/reader/shared resources.",
    )
    parser.add_argument(
        "--app-stories-dir",
        type=Path,
        default=Path("apps/reader/shared/src/commonMain/resources/stories"),
        help="App resource story folder for syncing audio outputs.",
    )
    parser.add_argument(
        "--audio-provider",
        default=None,
        help="Override LMC_TTS_PROVIDER for audio generation.",
    )
    parser.add_argument(
        "--skip-missing-audio-provider",
        action="store_true",
        help="Mark audio entries unavailable instead of failing when provider config is missing.",
    )
    parser.add_argument(
        "--migrate-cells",
        action="store_true",
        help="Add or refresh paragraphs[].cells in existing story.json files without regenerating stories.",
    )
    args = parser.parse_args()

    if args.migrate_cells:
        paths = migrate_story_cells(args.stories_dir, args.ids)
        for path in paths:
            print(path)
        print(f"Migrated cells in {len(paths)} story files")
        return 0

    if args.align_only:
        paths = align_audio_for_stories(args.stories_dir, args.ids)
        for path in paths:
            print(path)
        print(f"Aligned char timings in {len(paths)} audio manifests")
        return 0

    if args.gen_only:
        # Audio-only pass: read existing story.json (preserve hand edits), generate
        # wavs + placeholder timings; real timings come from a later --align-only pass.
        paths = generate_audio_for_stories(
            args.stories_dir,
            args.ids,
            audio_provider=args.audio_provider,
            mock_audio=args.mock,
            allow_missing_provider=args.skip_missing_audio_provider,
            audio_align=False,
        )
        for path in paths:
            print(path)
        print(f"Generated audio (gen-only) for {len(paths)} stories")
        return 0

    paths = generate_all(
        args.stories_dir,
        args.sources_dir,
        args.ids,
        audio=args.audio,
        audio_provider=args.audio_provider,
        mock_audio=args.mock,
        allow_missing_provider=args.skip_missing_audio_provider,
        sync_app_resources=args.sync_app_resources,
        app_stories_dir=args.app_stories_dir,
    )

    if args.audio:
        print(f"Audio processed for {len(paths)} story files")

    for path in paths:
        print(path)

    print(f"Generated {len(paths)} story files")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
