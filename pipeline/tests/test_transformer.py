import unittest

from transformer import generate
from transformer.audio import split_paragraph_to_sentences
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


if __name__ == "__main__":
    unittest.main()
