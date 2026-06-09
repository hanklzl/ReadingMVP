import unittest

from transformer.generate import pinyin_for_text
from transformer.story_data import STORY_DRAFTS


class TransformerTest(unittest.TestCase):
    def test_has_ten_initial_stories(self):
        self.assertEqual(10, len(STORY_DRAFTS))

    def test_pinyin_uses_context_for_changban(self):
        self.assertIn("cháng bǎn pō", pinyin_for_text("长坂坡上，大家保持安静。"))


if __name__ == "__main__":
    unittest.main()
