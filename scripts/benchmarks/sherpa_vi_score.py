#!/usr/bin/env python3
"""T-017 offline-WAV report summarizer. Never sends audio/transcripts to network.

Usage:
  python3 scripts/benchmarks/sherpa_vi_score.py device-results.json references.csv --output score.json

references.csv columns: sampleId,reference. Only local, consented samples.
The output intentionally excludes raw transcripts and sample identifiers.
"""
import argparse
import csv
import json
import math
import statistics
import unicodedata
from collections import Counter
from pathlib import Path


def normalize(text: str) -> str:
    # Do not fold accents or collapse numbers: these are meaningful Vietnamese command slots.
    value = unicodedata.normalize("NFC", text).casefold()
    value = "".join(ch if ch.isalnum() or ch.isspace() else " " for ch in value)
    return " ".join(value.split())


def edit_distance(reference, hypothesis):
    prev = list(range(len(hypothesis) + 1))
    for index, expected in enumerate(reference, start=1):
        curr = [index]
        for j, observed in enumerate(hypothesis, start=1):
            curr.append(min(
                prev[j] + 1,
                curr[-1] + 1,
                prev[j - 1] + (expected != observed),
            ))
        prev = curr
    return prev[-1]


def percentile(values, p):
    if not values:
        return None
    ordered = sorted(values)
    return ordered[max(0, math.ceil(len(ordered) * p / 100) - 1)]


def summarize(report: dict, references: dict) -> dict:
    rows = report.get("measurements", [])
    if not rows:
        raise ValueError("No measured rows; cannot report performance")
    groups = {}
    for row in rows:
        identity = (str(row["sampleId"]), int(row["threads"]))
        groups.setdefault(identity, []).append(row)
    output = {
        "status": "WAV_ONLY_NOT_FOLD3_ACCEPTANCE",
        "metadata": report.get("metadata", {}),
        "methods": {
            "WER": "corpus micro-average, NFC/casefold/punctuation stripping; preserve accents and digits",
            "CER": "corpus micro-average, non-space normalized characters",
            "percentiles": "nearest rank; only valid measured samples",
            "recognition": "first measured repetition; changes across repetitions are counted",
            "intentSlotAccuracy": "NOT_MEASURED (no Kotlin dry-run command parser results)",
        },
        "byThreads": {},
    }
    thread_ids = sorted({thread for _, thread in groups})
    for thread in thread_ids:
        selected = {sample: sorted(group, key=lambda row: row["repeat"])[0]
                    for (sample, t), group in groups.items() if t == thread}
        all_rows = [row for (sample, t), group in groups.items() if t == thread for row in group]
        disagreement = sum(
            len({normalize(str(row["hypothesis"])) for row in group}) > 1
            for (sample, t), group in groups.items() if t == thread
        )
        word_edits = word_total = char_edits = char_total = exact = 0
        scored = 0
        for sample, row in selected.items():
            if sample not in references:
                continue
            scored += 1
            expected = normalize(references[sample])
            observed = normalize(str(row["hypothesis"]))
            if not expected:
                raise ValueError(f"Empty ground truth for {sample}")
            ref_words = expected.split()
            hyp_words = observed.split()
            word_edits += edit_distance(ref_words, hyp_words)
            word_total += len(ref_words)
            ref_chars = expected.replace(" ", "")
            hyp_chars = observed.replace(" ", "")
            char_edits += edit_distance(ref_chars, hyp_chars)
            char_total += len(ref_chars)
            exact += expected == observed
        decode = [float(row["decodeMs"]) for row in all_rows]
        rtf = [float(row["rtf"]) for row in all_rows]
        pss = [float(row["processPssAfterKb"]) for row in all_rows]
        output["byThreads"][str(thread)] = {
            "audioSamples": len(selected),
            "inferenceMeasurements": len(all_rows),
            "groundTruthSamplesScored": scored,
            "repeatTranscriptionDisagreements": disagreement,
            "loadMs": {"first": float(all_rows[0]["modelLoadMs"])},
            "decodeMs": {"median": statistics.median(decode), "p95": percentile(decode, 95),
                         "p99": percentile(decode, 99)},
            "rtf": {"median": statistics.median(rtf), "p95": percentile(rtf, 95)},
            "processPssAfterKb": {"median": statistics.median(pss), "maxObserved": max(pss)},
            "wer": word_edits / word_total if word_total else None,
            "cer": char_edits / char_total if char_total else None,
            "exactTranscriptRate": exact / scored if scored else None,
        }
    return output


def references_from_csv(path: Path) -> dict:
    with path.open(newline="", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        if not {"sampleId", "reference"} <= set(reader.fieldnames or []):
            raise ValueError("CSV requires sampleId,reference columns")
        records = {}
        for row in reader:
            sample_id = row["sampleId"].strip()
            if not sample_id or sample_id in records:
                raise ValueError("Missing or duplicate sampleId in reference CSV")
            records[sample_id] = row["reference"]
        return records


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("results", type=Path)
    parser.add_argument("references", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    report = json.loads(args.results.read_text(encoding="utf-8"))
    summary = summarize(report, references_from_csv(args.references))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(summary, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Scored WAV benchmark: {len(summary['byThreads'])} thread profiles; "
          f"summary without raw transcripts: {args.output}")


if __name__ == "__main__":
    main()
