#!/usr/bin/env python3
"""Compare the synthetic T-021 AVD workload; never claim natural/Fold3 latency."""
import argparse
import json
import math
import re
import statistics

WORKLOAD = {"commandTransport": 120, "recognizerDiscovery": 120, "wakeFrame": 500}


def percentile(values):
    values = sorted(values)
    return {"samples": len(values), "p50Ms": statistics.median(values) if values else None,
            "p95Ms": values[math.ceil(.95 * len(values)) - 1] if values else None}


def summarize(lines):
    trials = {kind: {block: {} for block in range(3)} for kind in WORKLOAD}
    allocations = {}
    duplicates = invalid = duplicate_allocations = 0
    for line in lines:
        if "DexT021" not in line:
            continue
        fields = dict(re.findall(r"(\w+)=([^\s,]+)", line))
        kind, block_text = fields.get("kind"), fields.get("block", "")
        if kind not in WORKLOAD or block_text not in ("0", "1", "2"):
            continue
        block = int(block_text)
        trial = fields.get("trial", "")
        if trial:
            duration = fields.get("durationUs", "")
            if not trial.isdigit() or int(trial) >= WORKLOAD[kind] or not duration.isdigit():
                invalid += 1
                continue
            if int(trial) in trials[kind][block]:
                duplicates += 1
                continue
            trials[kind][block][int(trial)] = int(duration) / 1000
        elif "allocatedBytes" in fields:
            key = (kind, block)
            if key in allocations:
                duplicate_allocations += 1
                continue
            value = fields["allocatedBytes"]
            allocations[key] = int(value) if value.isdigit() else None
    workloads = {}
    for kind, expected in WORKLOAD.items():
        blocks = []
        for block in range(3):
            values = list(trials[kind][block].values())
            blocks.append({"block": block, **percentile(values), "expectedSamples": expected,
                           "missingSamples": expected - len(values),
                           "allocatedBytes": allocations.get((kind, block))})
        pooled = [value for block in trials[kind].values() for value in block.values()]
        workloads[kind] = {**percentile(pooled), "blocks": blocks,
                           "complete": all(b["missingSamples"] == 0 for b in blocks)}
    return {"workloads": workloads, "duplicateTrials": duplicates,
            "invalidTrials": invalid, "duplicateAllocationRows": duplicate_allocations,
            "note": "Synthetic offline Windows AVD components only. Allocation includes fixture/logging overhead; absent stats remain null."}


def compare(baseline, candidate):
    result = {}
    for kind in WORKLOAD:
        before, after = baseline["workloads"][kind], candidate["workloads"][kind]
        clean = (before["complete"] and after["complete"] and
                 all(report[key] == 0 for report in (baseline, candidate)
                     for key in ("duplicateTrials", "invalidTrials", "duplicateAllocationRows")))
        gain = before["p95Ms"] - after["p95Ms"] if before["p95Ms"] is not None and after["p95Ms"] is not None else None
        block_before = [block["p95Ms"] for block in before["blocks"]]
        block_after = [block["p95Ms"] for block in after["blocks"]]
        noise = consistent = None
        if None not in block_before + block_after:
            noise = (max(block_before) - min(block_before)) + (max(block_after) - min(block_after))
            consistent = all(a < b for b, a in zip(block_before, block_after))
        allocated_before = [block["allocatedBytes"] for block in before["blocks"]]
        allocated_after = [block["allocatedBytes"] for block in after["blocks"]]
        allocation_delta = (statistics.median(allocated_after) - statistics.median(allocated_before)
                            if clean and None not in allocated_before + allocated_after else None)
        result[kind] = {
            "completeCleanSamples": clean,
            "baselineP95Ms": before["p95Ms"], "candidateP95Ms": after["p95Ms"],
            "p95ImprovementMs": gain,
            "p95ImprovementPercent": gain / before["p95Ms"] * 100 if gain is not None and before["p95Ms"] else None,
            "blockSpreadBudgetMs": noise, "allPairedBlocksFaster": consistent,
            "latencySignal": "IMPROVED" if clean and gain is not None and noise is not None
                             and gain > noise and consistent else "INCONCLUSIVE",
            "medianBlockAllocationDeltaBytes": allocation_delta,
        }
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("baseline", help="Filtered baseline DexT021 log, one instrumentation process")
    parser.add_argument("candidate", nargs="?", help="Matching candidate workload log")
    args = parser.parse_args()
    with open(args.baseline, encoding="utf-8", errors="replace") as source:
        baseline = summarize(source)
    report = {"baseline": baseline}
    if args.candidate:
        with open(args.candidate, encoding="utf-8", errors="replace") as source:
            candidate = summarize(source)
        report.update(candidate=candidate, comparison=compare(baseline, candidate))
    print(json.dumps(report, indent=2))
