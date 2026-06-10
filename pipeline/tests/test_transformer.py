import tempfile
import unittest
import wave
from pathlib import Path

from transformer import generate
from transformer.audio import (
    char_timings_from_alignment,
    even_char_timings,
    generate_audio_manifest,
    split_paragraph_to_sentences,
)
from transformer.generate import build_story, pinyin_for_text
from transformer.story_data import STORY_DRAFTS


class TransformerTest(unittest.TestCase):
    def test_has_ten_initial_stories(self):
        self.assertEqual(10, len(STORY_DRAFTS))

    def test_pinyin_uses_context_for_changban(self):
        self.assertIn("cháng bǎn pō", pinyin_for_text("长坂坡上，大家保持安静。"))

    def test_pinyin_cells_for_text_aligns_one_cell_per_character(self):
        self.assertTrue(hasattr(generate, "pinyin_cells_for_text"))

        cells = generate.pinyin_cells_for_text("长坂坡上，A1")

        self.assertEqual(
            [
                {"c": "长", "p": "cháng"},
                {"c": "坂", "p": "bǎn"},
                {"c": "坡", "p": "pō"},
                {"c": "上", "p": "shàng"},
                {"c": "，", "p": ""},
                {"c": "A", "p": ""},
                {"c": "1", "p": ""},
            ],
            cells,
        )

    def test_build_story_includes_cells_for_each_paragraph(self):
        story = build_story(STORY_DRAFTS[0])

        for paragraph in story["paragraphs"]:
            self.assertIn("cells", paragraph)
            self.assertEqual(len(paragraph["text"]), len(paragraph["cells"]))
            self.assertEqual(
                [{"c": char, "p": cell["p"]} for char, cell in zip(paragraph["text"], paragraph["cells"])],
                paragraph["cells"],
            )

    def test_sentence_splitting_keeps_consecutive_sentence_punctuation(self):
        text = "孩子们很激动。她问：你会吗？！你敢吗？？"
        self.assertEqual(
            ["孩子们很激动。", "她问：你会吗？！", "你敢吗？？"],
            split_paragraph_to_sentences(text),
        )

    def test_sentence_splitting_keeps_ellipsis_as_unit(self):
        text = "他停下来想了想……继续往前走。"
        self.assertEqual(
            ["他停下来想了想……", "继续往前走。"],
            split_paragraph_to_sentences(text),
        )

    def test_sentence_splitting_ignores_blank_segments(self):
        text = "第一句。   第二句。    \n\n第三句；"
        self.assertEqual(["第一句。", "第二句。", "第三句；"], split_paragraph_to_sentences(text))

    def test_sentence_splitting_keeps_closing_quotes_and_parentheses(self):
        text = "他说：“好！”大家点头。再看（可以吗？）"
        self.assertEqual(
            ["他说：“好！”", "大家点头。", "再看（可以吗？）"],
            split_paragraph_to_sentences(text),
        )


class CharTimingTest(unittest.TestCase):
    def test_even_char_timings_align_one_slot_per_character(self):
        timings = even_char_timings("好，A", 900)

        self.assertEqual(["好", "，", "A"], [item["c"] for item in timings])
        self.assertEqual(0, timings[0]["startMs"])
        self.assertEqual(900, timings[-1]["endMs"])
        # Monotonic and contiguous boundaries.
        for previous, current in zip(timings, timings[1:]):
            self.assertLessEqual(previous["startMs"], previous["endMs"])
            self.assertEqual(previous["endMs"], current["startMs"])

    def test_even_char_timings_empty_text_returns_empty(self):
        self.assertEqual([], even_char_timings("", 1000))

    def test_char_timings_from_alignment_backfills_skipped_punctuation(self):
        # Aligner only emits spoken hanzi; the comma must be back-filled in order.
        aligned = [
            {"c": "好", "startMs": 0, "endMs": 400},
            {"c": "吗", "startMs": 600, "endMs": 1000},
        ]
        timings = char_timings_from_alignment("好，吗", aligned, 1000)

        self.assertEqual(["好", "，", "吗"], [item["c"] for item in timings])
        self.assertEqual(0, timings[0]["startMs"])
        self.assertEqual(400, timings[0]["endMs"])
        # The skipped comma is interpolated between its neighbours.
        self.assertEqual(400, timings[1]["startMs"])
        self.assertEqual(600, timings[1]["endMs"])
        self.assertEqual(1000, timings[2]["endMs"])

    def test_char_timings_from_alignment_falls_back_when_unmatched(self):
        timings = char_timings_from_alignment("好吗", [{"c": "X", "startMs": 0, "endMs": 5}], 800)

        self.assertEqual(["好", "吗"], [item["c"] for item in timings])
        self.assertEqual(800, timings[-1]["endMs"])

    def test_generate_audio_manifest_mock_emits_aligned_char_timestamps(self):
        story = build_story(STORY_DRAFTS[0])
        with tempfile.TemporaryDirectory() as raw_dir:
            story_dir = Path(raw_dir)
            manifest = generate_audio_manifest(story, story_dir, use_mock=True)

        self.assertTrue(manifest)
        for entry in manifest:
            chars = entry.get("chars")
            self.assertIsNotNone(chars, entry)
            self.assertEqual(len(entry["text"]), len(chars))
            self.assertEqual(
                [char for char in entry["text"]],
                [cell["c"] for cell in chars],
            )
            self.assertEqual(0, chars[0]["startMs"])
            self.assertEqual(entry["durationMs"], chars[-1]["endMs"])
            for previous, current in zip(chars, chars[1:]):
                self.assertLessEqual(previous["endMs"], current["startMs"])


if __name__ == "__main__":
    unittest.main()
