#!/usr/bin/env python3
"""Download pinned upstream files for private noncommercial evaluation only.
No archive extraction, microphone, upload, or production packaging.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import urllib.request

REPO = "csukuangfj2/sherpa-onnx-zipformer-vi-30M-int8-2026-02-09"
REVISION = "83e140db6d23fbb8480fd5fb868f74ab80e7092c"
FILES = {
    "encoder.int8.onnx": (27699063, "8ef5286dd427eb108055c2ddc1982aa31e544706072d5ea228729292dacade68", "sha256"),
    "decoder.onnx": (5165084, "cf2aa385b82c9d5d40cd29c3188af52d0249b3b78f0d4b7eb84ad502d50c7e7f", "sha256"),
    "joiner.int8.onnx": (1033417, "7311d2e17b810ecea515d79c71cc4668af8759256a06fa01d27047772320c821", "sha256"),
    "tokens.txt": (23238, "3544f3798170a7fd783f0c1324f8ce03eb16630d", "git-blob-sha1"),
}

def verify(path, spec):
    size, expected, kind = spec
    data = path.read_bytes()
    if len(data) != size:
        raise ValueError("Unexpected file size: " + path.name)
    digest = (hashlib.sha256(data).hexdigest() if kind == "sha256" else
              hashlib.sha1(b"blob " + str(size).encode() + b"\0" + data).hexdigest())
    if digest != expected:
        raise ValueError("Upstream integrity mismatch: " + path.name)
    return hashlib.sha256(data).hexdigest()

def prepare(destination, opener=urllib.request.urlopen):
    destination = destination.resolve()
    repo_root = Path(__file__).resolve().parents[2]
    if destination == repo_root or repo_root in destination.parents:
        raise ValueError("Use a private directory outside this Git checkout")
    destination.mkdir(parents=True, exist_ok=True, mode=0o700)
    hashes = {}
    for name, spec in FILES.items():
        target = destination / name
        if not target.exists():
            temporary = destination / (name + ".partial")
            try:
                with opener(f"https://huggingface.co/{REPO}/resolve/{REVISION}/{name}", timeout=120) as response:
                    with temporary.open("wb") as output:
                        os.chmod(temporary, 0o600)
                        while chunk := response.read(65536):
                            output.write(chunk)
                verify(temporary, spec)
                temporary.replace(target)
            finally:
                temporary.unlink(missing_ok=True)
        hashes[name] = verify(target, spec)
    manifest = {"repository": REPO, "revision": REVISION, "sha256": hashes,
                "use": "PRIVATE_NONCOMMERCIAL_EVALUATION_ONLY",
                "license": "Origin CC-BY-NC-ND-4.0; redistribution clearance remains open",
                "source": f"https://huggingface.co/api/models/{REPO}/revision/{REVISION}?blobs=true"}
    (destination / "verified-model.json").write_text(json.dumps(manifest, indent=2) + "\n")
    return manifest

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("destination", type=Path)
    parser.add_argument("--private-noncommercial-evaluation", action="store_true", required=True)
    args = parser.parse_args()
    prepare(args.destination)
    print("Verified four pinned model files; no device or production change")

if __name__ == "__main__":
    main()
