import json
import struct
import wave
import tempfile
import unittest
from pathlib import Path

from validator.validate import count_hanzi, validate_story_file
from transformer.audio import build_sentence_plan


REPO_ROOT = Path(__file__).resolve().parents[2]
SCHEMA_PATH = REPO_ROOT / "content" / "schema" / "story.schema.json"


def cells_for_test(text):
    return [{"c": char, "p": "x"} for char in text]


def build_story(**overrides):
    paragraph = "桃园里三个朋友一起读书学习帮助乡亲" * 5
    story = {
        "id": "peach-garden-oath",
        "title_zh": "桃园三结义",
        "title_en": "The Oath of the Peach Garden",
        "level": 1,
        "age_range": "5-8",
        "source_note": "Based on public-domain 《三国演义》, rewritten for children.",
        "source_url": "https://zh.wikisource.org/wiki/三國演義/第001回",
        "cover_image": "stories/peach-garden-oath/cover.png",
        "paragraphs": [
            {"text": paragraph, "pinyin": "tao yuan li san ge peng you", "cells": cells_for_test(paragraph)},
            {"text": paragraph, "pinyin": "tao yuan li san ge peng you", "cells": cells_for_test(paragraph)},
            {"text": paragraph, "pinyin": "tao yuan li san ge peng you", "cells": cells_for_test(paragraph)},
            {"text": paragraph, "pinyin": "tao yuan li san ge peng you", "cells": cells_for_test(paragraph)},
        ],
        "vocab": [
            {"word": "桃园", "pinyin": "táo yuán", "meaning": "peach garden", "example": "桃园里有花。"},
            {"word": "朋友", "pinyin": "péng you", "meaning": "friend", "example": "朋友一起学习。"},
            {"word": "学习", "pinyin": "xué xí", "meaning": "to learn", "example": "他们一起学习。"},
            {"word": "帮助", "pinyin": "bāng zhù", "meaning": "to help", "example": "他们帮助乡亲。"},
            {"word": "乡亲", "pinyin": "xiāng qīn", "meaning": "neighbors", "example": "乡亲很开心。"},
        ],
        "questions": [
            {
                "id": "q1",
                "type": "single_choice",
                "prompt": "朋友们在哪里？",
                "options": ["桃园", "海边", "雪山"],
                "answer": "桃园",
                "explanation": "正文说他们在桃园里。",
            },
            {
                "id": "q2",
                "type": "single_choice",
                "prompt": "他们一起做什么？",
                "options": ["学习", "睡觉", "迷路"],
                "answer": "学习",
                "explanation": "正文说他们一起读书学习。",
            },
            {
                "id": "q3",
                "type": "single_choice",
                "prompt": "他们帮助谁？",
                "options": ["乡亲", "老虎", "浪花"],
                "answer": "乡亲",
                "explanation": "正文说他们帮助乡亲。",
            },
        ],
        "retell_prompt": "说说三个朋友怎样帮助别人。",
    }
    story.update(overrides)
    return story


class ValidatorTest(unittest.TestCase):
    def write_audio_manifest(self, story_dir: Path, story: dict, *, unavailable: set[tuple[int, int]] | None = None) -> None:
        if unavailable is None:
            unavailable = set()

        audio_dir = story_dir / "audio"
        audio_dir.mkdir(parents=True, exist_ok=True)

        entries = []
        for expected in build_sentence_plan(story.get("paragraphs", [])):
            key = (expected["paraIndex"], expected["sentIndex"])
            audio_path = story_dir / expected["audioPath"]

            entry = {
                "paraIndex": expected["paraIndex"],
                "sentIndex": expected["sentIndex"],
                "text": expected["text"],
                "audioPath": expected["audioPath"],
                "durationMs": 500,
            }

            if key in unavailable:
                entry["unavailable"] = True
            else:
                self.write_wav(audio_path)

            entries.append(entry)

        (story_dir / "audio.json").write_text(json.dumps({"sentences": entries}, ensure_ascii=False) + "\n", encoding="utf-8")

    def write_wav(self, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        with wave.open(str(path), "wb") as wav:
            wav.setnchannels(1)
            wav.setsampwidth(2)
            wav.setframerate(22050)
            wav.writeframes(b"\x00\x00" * 22050)

    def write_story(self, story):
        temp_dir = tempfile.TemporaryDirectory()
        story_dir = Path(temp_dir.name)
        story_path = story_dir / "story.json"
        story_path.write_text(json.dumps(story, ensure_ascii=False), encoding="utf-8")
        self.write_audio_manifest(story_dir, story)
        self.addCleanup(temp_dir.cleanup)
        return story_path

    def test_valid_story_file_passes(self):
        story_path = self.write_story(build_story())

        result = validate_story_file(story_path, SCHEMA_PATH)

        self.assertTrue(result.passed, result.errors)
        self.assertEqual([], result.errors)
        self.assertGreaterEqual(result.hanzi_count, 300)
        self.assertLessEqual(result.hanzi_count, 600)

    def test_answer_must_be_one_of_options(self):
        story = build_story()
        story["questions"][0]["answer"] = "竹林"
        story_path = self.write_story(story)

        result = validate_story_file(story_path, SCHEMA_PATH)

        self.assertFalse(result.passed)
        self.assertIn("q1 answer must equal one of options", result.errors)

    def test_count_hanzi_ignores_punctuation_and_latin_text(self):
        self.assertEqual(5, count_hanzi("桃园ABC，三结义!"))

    def test_cells_must_align_exactly_with_text_characters(self):
        story = build_story()
        story["paragraphs"][0]["cells"] = story["paragraphs"][0]["cells"][:-1]
        text_length = len(story["paragraphs"][0]["text"])
        cell_length = len(story["paragraphs"][0]["cells"])
        story_path = self.write_story(story)

        result = validate_story_file(story_path, SCHEMA_PATH)

        self.assertFalse(result.passed)
        self.assertIn(
            f"paragraph 1 cells length must equal text length, got {cell_length} cells for {text_length} characters",
            result.errors,
        )

    def test_cells_require_hanzi_pinyin_and_blank_non_hanzi_pinyin(self):
        text = "桃园，A1"
        filler = "桃园里三个朋友一起读书学习帮助乡亲" * 5
        story = build_story(
            paragraphs=[
                {
                    "text": text,
                    "pinyin": "táo yuán，A1",
                    "cells": [
                        {"c": "桃", "p": ""},
                        {"c": "园", "p": "yuán"},
                        {"c": "，", "p": "comma"},
                        {"c": "A", "p": ""},
                        {"c": "1", "p": ""},
                    ],
                },
                {"text": filler, "pinyin": "x", "cells": cells_for_test(filler)},
                {"text": filler, "pinyin": "x", "cells": cells_for_test(filler)},
                {"text": filler, "pinyin": "x", "cells": cells_for_test(filler)},
                {"text": filler, "pinyin": "x", "cells": cells_for_test(filler)},
            ]
        )
        story_path = self.write_story(story)

        result = validate_story_file(story_path, SCHEMA_PATH)

        self.assertFalse(result.passed)
        self.assertIn("paragraph 1 cell 1 hanzi '桃' must have non-empty pinyin", result.errors)
        self.assertIn("paragraph 1 cell 3 non-hanzi '，' must have empty pinyin", result.errors)

    def test_audio_manifest_fails_when_entry_text_is_incorrect(self):
        story = build_story()
        story_path = self.write_story(story)

        with open(story_path.parent / "audio.json", "r+", encoding="utf-8") as handle:
            payload = json.loads(handle.read())
            payload["sentences"][0]["text"] = "被改坏的句子"
            handle.seek(0)
            handle.truncate(0)
            handle.write(json.dumps(payload, ensure_ascii=False, indent=2) + "\n")

        result = validate_story_file(story_path, SCHEMA_PATH)

        self.assertFalse(result.passed)
        self.assertIn("text mismatch", result.errors[0] if result.errors else "")

    def test_audio_manifest_fails_when_file_missing(self):
        story = build_story()
        story_path = self.write_story(story)

        payload = json.loads((story_path.parent / "audio.json").read_text(encoding="utf-8"))
        payload["sentences"][0]["audioPath"] = "audio/missing.wav"
        (story_path.parent / "audio.json").write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

        result = validate_story_file(story_path, SCHEMA_PATH)

        self.assertFalse(result.passed)
        self.assertIn("missing audio file", "\n".join(result.errors))


if __name__ == "__main__":
    unittest.main()
