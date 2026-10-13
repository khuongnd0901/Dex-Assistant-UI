#!/usr/bin/env python3
"""Validate private consented reference/audio pairs without emitting transcripts."""
import argparse
import csv
import json
import re
import wave
from pathlib import Path

GROUPS = {"system": 40, "dex_ui": 40, "media": 40, "maps": 30, "multi_step": 20, "noisy": 30}

def validate(audio, references):
    with references.open(newline="", encoding="utf-8-sig") as stream:
        reader = csv.DictReader(stream)
        required = {"sampleId", "reference", "group", "consent", "source"}
        if not required <= set(reader.fieldnames or []):
            raise ValueError("Reference CSV requires " + ",".join(sorted(required)))
        records = list(reader)
    if not records or len(records) > 200:
        raise ValueError("Expected 1–200 reference rows")
    counts = dict.fromkeys(GROUPS, 0)
    seen = set()
    for row in records:
        identity = row["sampleId"]
        if not re.fullmatch(r"sample[0-9]{3}", identity) or identity in seen:
            raise ValueError("Expected unique opaque sampleNNN IDs")
        seen.add(identity)
        if not row["reference"].strip() or row["group"] not in GROUPS:
            raise ValueError("Missing reference or invalid group")
        if row["consent"] != "yes" or row["source"] != "human_recording":
            raise ValueError("Acceptance corpus requires consent=yes and source=human_recording")
        with wave.open(str(audio / (identity + ".wav")), "rb") as wav:
            if (wav.getnchannels(), wav.getsampwidth(), wav.getframerate(), wav.getcomptype()) != (1, 2, 16000, "NONE"):
                raise ValueError("Expected PCM16 mono 16k WAV")
            if not 160 <= wav.getnframes() <= 960000 or len(wav.readframes(wav.getnframes())) != wav.getnframes() * 2:
                raise ValueError("Invalid WAV duration or truncated audio")
        counts[row["group"]] += 1
    actual = {p.stem for p in audio.glob("*.wav")}
    if actual != seen:
        raise ValueError("Audio/reference IDs must match exactly")
    return {"samples": len(seen), "groups": counts,
            "coverageComplete": all(counts[k] >= n for k, n in GROUPS.items()),
            "note": "Validation checks declarations; actual consent and annotation require owner review"}

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("audio", type=Path)
    p.add_argument("references", type=Path)
    a = p.parse_args()
    print(json.dumps(validate(a.audio, a.references), indent=2))

if __name__ == "__main__":
    main()
