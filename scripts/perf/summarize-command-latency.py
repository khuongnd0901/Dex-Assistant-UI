#!/usr/bin/env python3
"""Summarize filtered Android timing logs without exposing request contents or IDs."""
import argparse
import collections
import json
import math
import re
import statistics

METRICS = (
    "wakeLatency", "sttLatency", "speechEndToFinalLatency", "routeLatency",
    "backendLatency", "planningLatency", "uiResolutionLatency",
    "executionLatency", "confirmationWaitLatency", "speechEndToActionStartLatency",
    "firstStatusLatency", "finalToActionStartLatency", "providerQueueLatency",
    "providerRequestLatency", "providerFirstByteLatency", "providerBodyLatency",
    "providerParseLatency", "chatReplyLatency", "chatTtsStartLatency", "rearmLatency",
)
PATHS = ("local", "remote", "semantic", "fallback", "direct", "chat")
ORIGINS = ("local", "remote", "direct", "chat")
SUM_PER_REQUEST = ("executionLatency", "providerParseLatency")
UUID = re.compile(r"^[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$")


def summarize(lines, path=None, origin=None):
    if path is not None and path not in PATHS:
        raise ValueError("Unknown terminal path")
    if origin is not None and origin not in ORIGINS:
        raise ValueError("Unknown request origin")
    records = [(line, dict(re.findall(r"(\w+)=([^\s,]+)", line))) for line in lines
               if "DexLatency" in line or "command_planning_latency" in line]
    origins = {}
    origin_conflicts = set()
    for line, fields in records:
        request = fields.get("requestId", "")
        candidate = fields.get("origin")
        if "DexLatency" in line and UUID.fullmatch(request) and candidate in ORIGINS:
            if request in origins and origins[request] != candidate:
                origin_conflicts.add(request)
            else:
                origins[request] = candidate
    if path is not None or origin is not None:
        selected = {f["requestId"] for line, f in records
                    if UUID.fullmatch(f.get("requestId", ""))
                    and (path is None or ("DexLatency" in line and f.get("path") == path
                         and re.fullmatch(r"[A-Z][A-Z0-9_]{0,63}", f.get("terminalReason", ""))))
                    and (origin is None or (origins.get(f["requestId"]) == origin
                         and f["requestId"] not in origin_conflicts))}
        selected_epochs = {f.get("epoch") for line, f in records
                           if "DexLatency" in line and f.get("stage") == "correlated"
                           and f.get("requestId") in selected and f.get("epoch", "").isdigit()}
        records = [(line, f) for line, f in records if f.get("requestId") in selected
                   or ("requestId" not in f and f.get("epoch") in selected_epochs)]
    samples = collections.defaultdict(list)
    epochs = collections.defaultdict(dict)
    requests = collections.defaultdict(set)
    terminals = collections.Counter()
    request_totals = {name: collections.Counter() for name in SUM_PER_REQUEST}
    completed_requests = set()
    origin_terminals = collections.Counter()
    for line, fields in records:
        request = fields.get("requestId", "")
        valid_request = bool(UUID.fullmatch(request))
        epoch = fields.get("epoch", "")
        if valid_request:
            requests[request]  # Retain admitted requests even when no interval completes.
        if fields.get("stage") == "correlated" and valid_request and epoch.isdigit():
            epochs[epoch]["request"] = request
        metric = "planningLatency" if "command_planning_latency" in line else fields.get("metric")
        duration = fields.get("durationUs", "")
        if metric in METRICS and duration.isdigit():
            if metric in SUM_PER_REQUEST and valid_request:
                request_totals[metric][request] += int(duration) / 1000
            else:
                samples[metric].append(int(duration) / 1000)
            if valid_request:
                requests[request].add(metric)
            elif epoch.isdigit():
                epochs[epoch].setdefault("metrics", set()).add(metric)
        reason = fields.get("terminalReason", "")
        terminal_path = fields.get("path", "")
        if "DexLatency" in line and valid_request and terminal_path in PATHS and re.fullmatch(r"[A-Z][A-Z0-9_]{0,63}", reason):
            terminals[(terminal_path, reason)] += 1
            completed_requests.add(request)
            if request not in origin_conflicts and request in origins:
                origin_terminals[(origins[request], terminal_path, reason)] += 1
    for entry in epochs.values():
        if "request" in entry:
            requests[entry["request"]].update(entry.get("metrics", set()))
    for metric, totals in request_totals.items():
        samples[metric].extend(totals.values())
    distributions = {}
    for metric in METRICS:
        values = sorted(samples[metric])
        distributions[metric] = {
            "samples": len(values),
            "p50Ms": statistics.median(values) if values else None,
            "p95Ms": values[math.ceil(.95 * len(values)) - 1] if values else None,
            "requestsMissingMetric": sum(metric not in observed for observed in requests.values()),
        }
    return {
        "requests": len(requests),
        "metrics": distributions,
        "terminals": [{"path": path, "reason": reason, "count": count}
                      for (path, reason), count in sorted(terminals.items())],
        "incompleteRequests": len(set(requests) - completed_requests),
        "origins": [{"origin": source, "requests": sum(origins.get(r) == source
                    and r not in origin_conflicts for r in requests)} for source in ORIGINS],
        "originTerminals": [{"origin": source, "path": terminal_path, "reason": reason, "count": count}
                            for (source, terminal_path, reason), count in sorted(origin_terminals.items())],
        "requestsMissingOrigin": sum(r not in origins or r in origin_conflicts for r in requests),
        "originConflicts": len(set(requests) & origin_conflicts),
        "note": "Execution segments (excluding confirmation wait) and provider parse phases are summed per request. Other samples are intervals. Missing includes stages inapplicable to a path. Terminal path filters exclude incomplete requests; origin filters include admitted incomplete requests with explicit consistent origin. Origins are never inferred from legacy terminal paths. Use one process lifetime per input; epochs may repeat after restart.",
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("logs", nargs="+", help="Filtered Android/backend log files for one run")
    parser.add_argument("--path", choices=PATHS,
                        help="Only requests with this terminal path (incomplete requests excluded)")
    parser.add_argument("--origin", choices=ORIGINS,
                        help="Only requests with this original route, including admitted incomplete requests")
    args = parser.parse_args()
    lines = []
    for filename in args.logs:
        with open(filename, encoding="utf-8", errors="replace") as source:
            lines.extend(source)
    print(json.dumps(summarize(lines, args.path, args.origin), indent=2))
