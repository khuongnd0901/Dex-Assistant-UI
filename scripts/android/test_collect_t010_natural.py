import importlib.util
import pathlib
import json
import subprocess
import sys
import tempfile
import unittest
spec = importlib.util.spec_from_file_location('collector', pathlib.Path(__file__).with_name('collect-t010-natural.py'))
collector = importlib.util.module_from_spec(spec)
spec.loader.exec_module(collector)

class MetadataPrivacyTest(unittest.TestCase):
    def test_safe_timings_coordinates_and_request_metric(self):
        for line in ['I/DexVoiceTiming: epoch=5 stage=stt_final atMs=20 status=',
                     'I/DexNaturalTap: count=1 displayId=6 x=40.5 y=10.0',
                     'I/DexLatency: requestId=12345678-1234-1234-1234-123456789abc path=local metric=speechEndToActionStartLatency durationUs=1500']:
            self.assertIsNotNone(collector.safe_metadata(line))
    def test_raw_transcript_secret_and_metadata_suffix_rejected(self):
        for line in ['DexVoiceTiming transcript=private',
                     'DexLatency epoch=5 metric=sttLatency durationUs=20 apiKey=private',
                     'DexVoiceTiming epoch=5 stage=stt_final atMs=20 text=private',
                     'DexNaturalTap count=1 displayId=6 x=40 y=10 transcript=private']:
            self.assertIsNone(collector.safe_metadata(line))

class SummaryTest(unittest.TestCase):
    def test_correlated_terminal_and_recognizer_latency_are_not_acceptance(self):
        request = '12345678-1234-1234-1234-123456789abc'
        lines = ['DexVoiceTiming epoch=7 stage=wake_detected atMs=20',
                 'DexVoiceTiming epoch=7 stage=stt_ready atMs=70',
                 'DexVoiceTiming epoch=7 stage=command_dispatch atMs=150',
                 'DexVoiceTiming epoch=7 stage=speech_end atMs=100',
                 'DexVoiceTiming epoch=7 stage=stt_final atMs=140',
                 'DexVoiceTiming epoch=7 stage=command_terminal atMs=160 status=SCREEN_CONTINUE',
                 f'DexLatency epoch=7 requestId={request} stage=correlated',
                 f'DexLatency requestId={request} path=local terminalReason=SCREEN_CONTINUE']
        result = collector.summarize(lines + lines)
        self.assertEqual(1, result['correlatedTerminalEpochs'])
        self.assertEqual(40, result['latency']['recognizerSpeechEndToFinalMs']['p95'])
        self.assertFalse(result['naturalAcceptanceCertified'])
        self.assertEqual(50, result['latency']['wakeToRecognizerReadyMs']['p50'])
        self.assertEqual(10, result['latency']['finalToDispatchMs']['p50'])
        self.assertEqual(50, result['latency']['recognizerSpeechEndToDispatchMs']['p50'])

    def test_incomplete_epoch_and_touch_never_prove_terminal(self):
        result = collector.summarize(['DexVoiceTiming epoch=1 stage=stt_final atMs=5',
                                     'DexNaturalTap count=1 displayId=2 x=5 y=5'])
        self.assertEqual(0, result['correlatedTerminalEpochs'])
        self.assertEqual(1, result['fixtureTapEvents'])
        self.assertEqual({}, result['latency'])

    def test_parent_resume_keeps_repeated_epochs_in_separate_runs(self):
        with tempfile.TemporaryDirectory() as root:
            for name in ['run-a', 'run-b']:
                run = pathlib.Path(root) / name
                run.mkdir()
                (run / 'timing.log').write_text('DexVoiceTiming epoch=1 stage=stt_final atMs=5\n')
            result = subprocess.run([sys.executable, str(pathlib.Path(collector.__file__)),
                                     '--summarize', '--output', root], capture_output=True,
                                    text=True, check=True)
            summary = json.loads(result.stdout)
            self.assertEqual(2, summary['captureRuns'])
            self.assertEqual(1, summary['runs']['run-a']['finalEpochs'])
            self.assertEqual(1, summary['runs']['run-b']['finalEpochs'])
            self.assertEqual(0, summary['correlatedTerminalCandidatesAcrossRuns'])

    def test_logcat_pid_envelope_and_embedded_private_prefix(self):
        self.assertEqual('DexVoiceTiming epoch=5 stage=stt_final atMs=20',
                         collector.safe_metadata('I/DexVoiceTiming( 123): epoch=5 stage=stt_final atMs=20'))
        for line in ['private DexVoiceTiming epoch=5 stage=stt_final atMs=20',
                     'I/DexLatency: secret epoch=5 metric=sttLatency durationUs=20',
                     'I/DexVoiceTiming: epoch=5 stage=stt_final atMs=20\nsecret']:
            self.assertIsNone(collector.safe_metadata(line))

if __name__ == '__main__':
    unittest.main()
