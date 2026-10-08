import hashlib
import io
import tempfile
import unittest
import wave
from pathlib import Path
from unittest.mock import Mock, patch
import prepare_sherpa_vi_model as model
import validate_sherpa_vi_corpus as corpus

class PreparationTest(unittest.TestCase):
    def test_sha_and_git_blob_integrity(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "tokens.txt"
            p.write_bytes(b"abc")
            sha = hashlib.sha256(b"abc").hexdigest()
            blob = hashlib.sha1(b"blob 3\0abc").hexdigest()
            self.assertEqual(sha, model.verify(p, (3, blob, "git-blob-sha1")))
            self.assertEqual(sha, model.verify(p, (3, sha, "sha256")))
            with self.assertRaises(ValueError): model.verify(p, (3, "0" * 64, "sha256"))
            with self.assertRaises(ValueError): model.verify(p, (4, sha, "sha256"))
    def test_reject_checkout_destination(self):
        with self.assertRaises(ValueError): model.prepare(Path(model.__file__).parent)
    def test_accept_partial_corpus_and_reject_no_consent_or_wrong_audio(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            with wave.open(str(root / "sample001.wav"), "wb") as w:
                w.setparams((1, 2, 16000, 0, "NONE", "not compressed"))
                w.writeframes(b"\0" * 640)
            r = root / "r.csv"
            header = "sampleId,reference,group,consent,source\n"
            r.write_text(header + "sample001,Bật Wi-Fi,system,yes,human_recording\n")
            self.assertFalse(corpus.validate(root, r)["coverageComplete"])
            r.write_text(header + "sample001,Bật Wi-Fi,system,no,human_recording\n")
            with self.assertRaises(ValueError): corpus.validate(root, r)
            r.write_text(header + "sample001,Bật Wi-Fi,system,yes,human_recording\n")
            with wave.open(str(root / "sample001.wav"), "wb") as w:
                w.setparams((2, 2, 16000, 0, "NONE", "not compressed"))
                w.writeframes(b"\0" * 640)
            with self.assertRaises(ValueError): corpus.validate(root, r)

    def test_reject_missing_audio_and_duplicate_ids(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            r = root / "r.csv"
            r.write_text("sampleId,reference,group,consent,source\nsample001,mở nhạc,media,yes,human_recording\n")
            with self.assertRaises(FileNotFoundError): corpus.validate(root, r)
            with wave.open(str(root / "sample001.wav"), "wb") as w:
                w.setparams((1, 2, 16000, 0, "NONE", "not compressed"))
                w.writeframes(b"\0" * 640)
            r.write_text(r.read_text() + "sample001,tắt nhạc,media,yes,human_recording\n")
            with self.assertRaises(ValueError): corpus.validate(root, r)
    def test_no_network_when_cached_files_corrupt(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            (root / "encoder.int8.onnx").write_bytes(b"bad")
            opener = Mock()
            with self.assertRaises(ValueError): model.prepare(root, opener)
            opener.assert_not_called()

    def test_bad_download_not_published_and_partial_removed(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            with patch.object(model, "FILES", {"encoder.int8.onnx": (3, "0" * 64, "sha256")}):
                with self.assertRaises(ValueError): model.prepare(root, lambda *a, **k: io.BytesIO(b"bad"))
            self.assertFalse((root / "encoder.int8.onnx").exists())
            self.assertFalse((root / "encoder.int8.onnx.partial").exists())
