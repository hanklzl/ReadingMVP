import unittest

from transformer import generate
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


if __name__ == "__main__":
    unittest.main()
