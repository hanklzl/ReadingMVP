import tempfile
import unittest
import wave
import json
from unittest import mock
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
    def test_has_fifteen_mvp_stories(self):
        self.assertEqual(15, len(STORY_DRAFTS))

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

    def test_polyphone_overrides_win_over_backend_output(self):
        wrong_backend_tokens = [
            "dà",
            "jiā",
            "zǒu",
            "dé",
            "hěn",
            "màn",
            "，",
            "màn",
            "màn",
            "dì",
            "shuō",
            "huà",
            "，",
            "wǒ",
            "dí",
            "shū",
            "，",
            "zhǎng",
            "bǎn",
            "pō",
        ]
        text = "大家走得很慢，慢慢地说话，我的书，长坂坡"

        with mock.patch.object(generate, "_backend_tokens_for_text", return_value=wrong_backend_tokens):
            cells = generate.pinyin_cells_for_text(text)

        by_char = [(cell["c"], cell["p"]) for cell in cells]
        self.assertIn(("得", "de"), by_char)
        self.assertIn(("地", "de"), by_char)
        self.assertIn(("的", "de"), by_char)
        self.assertEqual("cháng", cells[text.index("长")]["p"])

    def test_pinyin_overrides_handle_de_di_de_particles(self):
        examples = [
            ("大家走得很慢", "得", "de"),
            ("桃花开得正好", "得", "de"),
            ("恭恭敬敬地说明", "地", "de"),
            ("有礼貌地坚持", "地", "de"),
            ("我的书", "的", "de"),
            ("扫地的人", "地", "dì"),
            ("地方很大", "地", "dì"),
            ("记得恩情", "得", "de"),
            ("得到帮助", "得", "dé"),
        ]

        for text, char, expected in examples:
            with self.subTest(text=text):
                cells = generate.pinyin_cells_for_text(text)
                self.assertEqual(expected, cells[text.index(char)]["p"])

    def test_pinyin_cells_raise_when_backend_token_count_mismatches_text(self):
        with mock.patch.object(generate, "_backend_tokens_for_text", return_value=["táo"]):
            with self.assertRaisesRegex(ValueError, "pinyin token count 1 does not match text length 2"):
                generate.pinyin_cells_for_text("桃园")

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

    def test_gen_then_align_split_backfills_real_timings(self):
        # Simulates the two-venv flow: generate (mock TTS, placeholder timings)
        # then align (injected aligner standing in for qwen-asr) rewrites chars[].
        from transformer.audio import align_audio_manifest, read_audio_manifest

        story = build_story(STORY_DRAFTS[0])
        with tempfile.TemporaryDirectory() as raw_dir:
            story_dir = Path(raw_dir)

            # gen-only: audio + placeholder (even) timings, schema-valid already.
            gen_manifest = generate_audio_manifest(story, story_dir, use_mock=True, align=False)
            self.assertTrue(gen_manifest)
            for entry in gen_manifest:
                self.assertEqual(len(entry["text"]), len(entry["chars"]))

            class StubAligner:
                def __init__(self):
                    self.calls = []

                def align(self, text, audio_path, duration_ms):
                    self.calls.append((text, Path(audio_path).name, duration_ms))
                    # Distinctive timings so we can prove a real align pass ran.
                    return [
                        {"c": ch, "startMs": min(i, duration_ms), "endMs": min(i + 1, duration_ms)}
                        for i, ch in enumerate(text)
                    ]

            stub = StubAligner()
            aligned = align_audio_manifest(story_dir, aligner=stub)

            # Every available sentence was aligned and rewritten on disk.
            self.assertEqual(len(gen_manifest), len(stub.calls))
            on_disk = read_audio_manifest(story_dir / "audio.json")
            self.assertEqual(len(on_disk), len(aligned))
            for entry in on_disk:
                chars = entry["chars"]
                self.assertEqual(len(entry["text"]), len(chars))
                self.assertEqual([c for c in entry["text"]], [cell["c"] for cell in chars])
                # stub's signature timing: chars[i] -> startMs=i, endMs=i+1 (clamped)
                self.assertEqual(0, chars[0]["startMs"])
            self.assertEqual(2, chars[1]["endMs"])


class TransformerCliTest(unittest.TestCase):
    def test_sync_app_resources_pure_copy_does_not_rewrite_story_json(self):
        with tempfile.TemporaryDirectory() as raw_dir:
            root = Path(raw_dir)
            stories_dir = root / "content" / "stories"
            app_stories_dir = root / "app" / "resources" / "stories"
            story_dir = stories_dir / "sample-story"
            audio_dir = story_dir / "audio"
            audio_dir.mkdir(parents=True)
            source_story = {"id": "sample-story", "sentinel": "hand-fixed-pinyin"}
            source_story_bytes = json.dumps(source_story, ensure_ascii=False, indent=2).encode("utf-8") + b"\n"
            (story_dir / "story.json").write_bytes(source_story_bytes)
            (story_dir / "audio.json").write_text('{"sentences": []}\n', encoding="utf-8")
            (audio_dir / "p1_s1.wav").write_bytes(b"wav-bytes")

            target_dir = app_stories_dir / "sample-story"
            target_dir.mkdir(parents=True)
            (target_dir / "story.json").write_text('{"id":"sample-story","sentinel":"stale"}\n', encoding="utf-8")
            (target_dir / "audio").mkdir()
            (target_dir / "audio" / "old.wav").write_bytes(b"old")

            with mock.patch.object(generate, "generate_all", side_effect=AssertionError("sync must not regenerate")):
                exit_code = generate.main(
                    [
                        "--sync-app-resources",
                        "--stories-dir",
                        str(stories_dir),
                        "--app-stories-dir",
                        str(app_stories_dir),
                        "--ids",
                        "sample-story",
                    ]
                )

            self.assertEqual(0, exit_code)
            self.assertEqual(source_story_bytes, (story_dir / "story.json").read_bytes())
            self.assertEqual(source_story_bytes, (target_dir / "story.json").read_bytes())
            self.assertEqual(b"wav-bytes", (target_dir / "audio" / "p1_s1.wav").read_bytes())
            self.assertFalse((target_dir / "audio" / "old.wav").exists())


if __name__ == "__main__":
    unittest.main()
