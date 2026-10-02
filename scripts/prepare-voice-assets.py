#!/usr/bin/env python3
"""Prepare official, checksum-pinned offline wake assets (build time only)."""
import argparse
import hashlib
import shutil
import tarfile
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RUNTIME = "sherpa-onnx-1.13.8.aar"
MODEL = "sherpa-onnx-kws-zipformer-gigaspeech-3.3M-2024-01-01"
PINNED = {
    RUNTIME: (
        f"https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/{RUNTIME}",
        "633c24321e06b1fe79feafa03ea16cbc0f8a286641e2da3559bac91bdb13bd96",
    ),
    f"{MODEL}.tar.bz2": (
        f"https://github.com/k2-fsa/sherpa-onnx/releases/download/kws-models/{MODEL}.tar.bz2",
        "f170013b4716e41b62b9bfd809687c207cef798ef9bc6534d524e17af9b6561a",
    ),
}
MODEL_FILES = [
    "tokens.txt",
    "encoder-epoch-12-avg-2-chunk-16-left-64.int8.onnx",
    "decoder-epoch-12-avg-2-chunk-16-left-64.int8.onnx",
    "joiner-epoch-12-avg-2-chunk-16-left-64.int8.onnx",
]


def digest(path):
    with path.open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest() if hasattr(hashlib, "file_digest") else hashlib.sha256(source.read()).hexdigest()


def prepare(offline=False):
    cache = ROOT / ".cache/voice"
    cache.mkdir(parents=True, exist_ok=True)
    for name, (url, expected) in PINNED.items():
        path = cache / name
        if not path.exists():
            if offline:
                raise RuntimeError(f"Missing cached artifact: {name}")
            temporary = path.with_suffix(path.suffix + ".download")
            try:
                with urllib.request.urlopen(url, timeout=60) as response, temporary.open("wb") as output:
                    shutil.copyfileobj(response, output)
                if digest(temporary) != expected:
                    raise RuntimeError(f"Checksum mismatch: {name}")
                temporary.replace(path)
            finally:
                temporary.unlink(missing_ok=True)
        if digest(path) != expected:
            raise RuntimeError(f"Checksum mismatch: {name}; remove corrupt cached file and retry")
    assets = cache / "assets/kws"
    assets.mkdir(parents=True, exist_ok=True)
    with tarfile.open(cache / f"{MODEL}.tar.bz2") as archive:
        # Extract only named regular files; never archive paths or test recordings.
        for name in MODEL_FILES + ["README.md"]:
            member = archive.getmember(f"{MODEL}/{name}")
            if not member.isfile():
                raise RuntimeError(f"Unexpected archive member: {name}")
            target = assets / name if name != "README.md" else cache / "assets/licenses/gigaspeech-model-README.md"
            target.parent.mkdir(parents=True, exist_ok=True)
            with archive.extractfile(member) as source:
                content = source.read()
            if not target.exists() or target.read_bytes() != content:
                target.write_bytes(content)
    tokens = {line.split()[0] for line in (assets / "tokens.txt").read_text().splitlines()}
    keywords = ROOT / "app/src/main/assets/kws/keywords.txt"
    for line in keywords.read_text().splitlines():
        for token in line.split():
            if token.startswith(("@", ":", "#")):
                continue
            if token not in tokens:
                raise RuntimeError(f"Wake keyword token absent from pinned model: {token}")
    print("Pinned sherpa runtime and English KWS assets verified; no runtime download required.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--offline", action="store_true", help="Require cached archives; no network")
    args = parser.parse_args()
    prepare(args.offline)
