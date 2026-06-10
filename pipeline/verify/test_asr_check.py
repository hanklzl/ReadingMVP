import unittest

import asr_check


class AsrCheckTest(unittest.TestCase):
    def test_normalized_match_rate_ignores_punctuation(self):
        expected = asr_check.normalize_for_compare("桃园，三结义。")
        actual = asr_check.normalize_for_compare("桃园三结义")

        alignment = asr_check.align_normalized_text(expected, actual)

        self.assertEqual("桃园三结义", expected.text)
        self.assertEqual(1.0, alignment.match_rate)
        self.assertEqual(0, alignment.edit_count)

    def test_homophone_replacement_is_asr_mishearing(self):
        issue = asr_check.classify_char_mismatch(
            expected_char="涿",
            expected_pinyin="zhuō",
            actual_char="卓",
        )

        self.assertEqual("asr_homophone", issue.kind)
        self.assertFalse(issue.counts_as_tts_error)

    def test_polyphone_alt_reading_is_suspected_tts_error(self):
        issue = asr_check.classify_char_mismatch(
            expected_char="长",
            expected_pinyin="cháng",
            actual_char="掌",
        )

        self.assertEqual("suspected_tts_polyphone", issue.kind)
        self.assertTrue(issue.counts_as_tts_error)

    def test_zero_duration_deleted_char_is_suspected_tts_omission(self):
        issue = asr_check.classify_char_mismatch(
            expected_char="用",
            expected_pinyin="yòng",
            actual_char="",
            expected_duration_ms=0,
        )

        self.assertEqual("suspected_tts_omission", issue.kind)
        self.assertTrue(issue.counts_as_tts_error)


if __name__ == "__main__":
    unittest.main()
