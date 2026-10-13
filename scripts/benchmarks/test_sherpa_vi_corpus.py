"""Acceptance-data guards; generated silent WAVs test shape, not speech accuracy."""
import csv
import tempfile
import unittest
import wave
from pathlib import Path
import validate_sherpa_vi_corpus as corpus

class CorpusAcceptanceGuardsTest(unittest.TestCase):
    def write_audio(self, root, identity):
        with wave.open(str(root / (identity + '.wav')), 'wb') as wav:
            wav.setparams((1, 2, 16000, 0, 'NONE', 'not compressed'))
            wav.writeframes(b'\0' * 640)

    def references(self, root, groups):
        path = root / 'references.csv'
        with path.open('w', newline='', encoding='utf-8') as stream:
            writer = csv.writer(stream)
            writer.writerow(['sampleId', 'reference', 'group', 'consent', 'source'])
            for number, group in enumerate(groups, 1):
                identity = f'sample{number:03}'
                self.write_audio(root, identity)
                writer.writerow([identity, 'Bật Wi-Fi', group, 'yes', 'human_recording'])
        return path

    def test_two_hundred_samples_require_each_group_not_only_total(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            refs = self.references(root, ['system'] * 200)
            self.assertFalse(corpus.validate(root, refs)['coverageComplete'])
            groups = [group for group, count in corpus.GROUPS.items() for _ in range(count)]
            refs = self.references(root, groups)
            result = corpus.validate(root, refs)
            self.assertEqual(200, result['samples'])
            self.assertTrue(result['coverageComplete'])

    def test_orphan_audio_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            refs = self.references(root, ['system'])
            self.write_audio(root, 'sample002')
            with self.assertRaisesRegex(ValueError, 'match exactly'):
                corpus.validate(root, refs)

    def test_truncated_audio_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            refs = self.references(root, ['system'])
            audio = root / 'sample001.wav'
            audio.write_bytes(audio.read_bytes()[:-2])
            with self.assertRaisesRegex(ValueError, 'truncated'):
                corpus.validate(root, refs)
