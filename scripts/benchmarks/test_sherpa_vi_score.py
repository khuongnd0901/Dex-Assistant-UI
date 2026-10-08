"""Host-only tests for T-017 benchmark scoring (no device or voice assets needed)."""
import unittest

try:
    from .sherpa_vi_score import edit_distance, normalize, percentile, summarize
except ImportError:
    from sherpa_vi_score import edit_distance, normalize, percentile, summarize


class SherpaScoreTest(unittest.TestCase):
    def test_preserve_vietnamese_accents_and_numbers(self):
        self.assertEqual(normalize("BẬT Wi-Fi, 50%!"), "bật wi fi 50")
        self.assertNotEqual(normalize("bật"), normalize("bắt"))
        self.assertEqual(normalize("a\u0300"), "à")

    def test_edit_distance(self):
        self.assertEqual(edit_distance("bật wifi".split(), "tắt wifi".split()), 1)
        self.assertEqual(edit_distance("xin".split(), "xin".split()), 0)
        self.assertEqual(edit_distance(list("abc"), list("ab")), 1)

    def test_percentile(self):
        self.assertIsNone(percentile([], 95))
        self.assertEqual(percentile([1, 2, 4, 7, 8], 95), 8)
        self.assertEqual(percentile([1, 2, 4, 7, 8], 50), 4)

    def test_no_references_never_invents_accuracy(self):
        payload = {"metadata": {"device": "SM-F926B"}, "measurements": [
            {"sampleId": "s1", "threads": 1, "repeat": 1, "hypothesis": "bật wifi",
             "decodeMs": 80, "rtf": .08, "modelLoadMs": 500, "processPssAfterKb": 50000},
            {"sampleId": "s1", "threads": 1, "repeat": 2, "hypothesis": "bật wifi",
             "decodeMs": 60, "rtf": .06, "modelLoadMs": 500, "processPssAfterKb": 51000},
        ]}
        s = summarize(payload, {})
        one = s["byThreads"]["1"]
        self.assertEqual(one["audioSamples"], 1)
        self.assertIsNone(one["wer"])
        self.assertIsNone(one["cer"])
        self.assertEqual(one["decodeMs"]["p95"], 80)
        self.assertNotIn("bật wifi", repr(s))

    def test_micro_averaged_wer_cer_and_repeat_disagreement(self):
        payload = {"measurements": [
            {"sampleId": "a", "threads": 2, "repeat": 1, "hypothesis": "tắt wifi",
             "decodeMs": 10, "rtf": .1, "modelLoadMs": 100, "processPssAfterKb": 100},
            {"sampleId": "a", "threads": 2, "repeat": 2, "hypothesis": "bật wifi",
             "decodeMs": 9, "rtf": .09, "modelLoadMs": 100, "processPssAfterKb": 100},
            {"sampleId": "b", "threads": 2, "repeat": 1, "hypothesis": "mở maps",
             "decodeMs": 20, "rtf": .2, "modelLoadMs": 100, "processPssAfterKb": 110},
        ]}
        s = summarize(payload, {"a": "bật wifi", "b": "mở maps"})
        r = s["byThreads"]["2"]
        self.assertEqual(r["groundTruthSamplesScored"], 2)
        self.assertEqual(r["repeatTranscriptionDisagreements"], 1)
        self.assertAlmostEqual(r["wer"], 0.25)
        self.assertAlmostEqual(r["exactTranscriptRate"], 0.5)
        self.assertGreater(r["cer"], 0)

    def test_no_measurements_is_not_success(self):
        with self.assertRaises(ValueError):
            summarize({"measurements": []}, {})


if __name__ == "__main__":
    unittest.main()
