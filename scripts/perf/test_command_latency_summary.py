import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

spec = importlib.util.spec_from_file_location(
    "latency_summary", Path(__file__).with_name("summarize-command-latency.py"))
summary = importlib.util.module_from_spec(spec)
spec.loader.exec_module(summary)
REQUEST = "00000000-0000-0000-0000-000000000001"


class CommandLatencySummaryTest(unittest.TestCase):
    def test_pairs_epoch_and_request_and_preserves_missing_intervals(self):
        report = summary.summarize([
            "DexLatency epoch=4 metric=wakeLatency durationUs=2000",
            f"DexLatency epoch=4 requestId={REQUEST} stage=correlated",
            f"DexLatency requestId={REQUEST} path=local terminalReason=CANCELLED",
            f"command_planning_latency requestId={REQUEST} durationUs=3000 outcome=failure reason=AI_UNAVAILABLE",
            "DexLatency commandText=SECRET metric=unknown durationUs=5",
        ])
        self.assertEqual(report["requests"], 1)
        self.assertEqual(report["metrics"]["wakeLatency"]["requestsMissingMetric"], 0)
        self.assertIsNone(report["metrics"]["backendLatency"]["p95Ms"])
        self.assertEqual(report["metrics"]["planningLatency"]["samples"], 1)
        self.assertEqual(report["terminals"], [{"path": "local", "reason": "CANCELLED", "count": 1}])
        self.assertNotIn("SECRET", str(report))

    def test_execution_segments_sum_without_confirmation_wait(self):
        report = summary.summarize([
            f"DexLatency requestId={REQUEST} metric=executionLatency durationUs=1000",
            f"DexLatency requestId={REQUEST} metric=executionLatency durationUs=3000",
            f"DexLatency requestId={REQUEST} metric=confirmationWaitLatency durationUs=1000000",
        ])
        self.assertEqual(report["metrics"]["executionLatency"]["samples"], 1)
        self.assertEqual(report["metrics"]["executionLatency"]["p95Ms"], 4)
        self.assertEqual(report["metrics"]["confirmationWaitLatency"]["p95Ms"], 1000)

    def test_nearest_rank_and_negative_durations(self):
        lines = [f"DexLatency metric=routeLatency durationUs={n * 1000}" for n in range(1, 21)]
        lines.append("DexLatency metric=routeLatency durationUs=-999")
        result = summary.summarize(lines)["metrics"]["routeLatency"]
        self.assertEqual(result["samples"], 20)
        self.assertEqual(result["p50Ms"], 10.5)
        self.assertEqual(result["p95Ms"], 19)


    def test_path_filter_keeps_correlated_voice_and_initial_route(self):
        result = summary.summarize([
            "DexLatency epoch=4 metric=sttLatency durationUs=2000",
            f"DexLatency epoch=4 requestId={REQUEST} stage=correlated",
            f"DexLatency requestId={REQUEST} path=local metric=routeLatency durationUs=1000",
            f"DexLatency requestId={REQUEST} path=fallback terminalReason=VERIFIED",
        ], "fallback")
        self.assertEqual(result["metrics"]["sttLatency"]["samples"], 1)
        self.assertEqual(result["metrics"]["routeLatency"]["samples"], 1)
        self.assertEqual(result["requests"], 1)

    def test_direct_and_chat_errors_without_completed_metrics_are_retained(self):
        second = "00000000-0000-0000-0000-000000000002"
        report = summary.summarize([
            f"DexLatency requestId={REQUEST} origin=direct path=direct terminalReason=AI_AUTH_FAILED",
            f"DexLatency requestId={second} origin=chat path=chat terminalReason=CANCELLED",
        ])
        self.assertEqual(report["requests"], 2)
        self.assertEqual(report["incompleteRequests"], 0)
        self.assertEqual(report["requestsMissingOrigin"], 0)
        self.assertEqual(report["metrics"]["providerRequestLatency"]["requestsMissingMetric"], 2)
        self.assertIsNone(report["metrics"]["providerRequestLatency"]["p95Ms"])
        self.assertEqual(sum(t["count"] for t in report["terminals"]), 2)

    def test_origin_keeps_semantic_execution_and_incomplete_requests(self):
        second = "00000000-0000-0000-0000-000000000002"
        report = summary.summarize([
            "DexLatency epoch=7 metric=sttLatency durationUs=11000",
            f"DexLatency epoch=7 requestId={REQUEST} origin=direct stage=correlated",
            f"DexLatency requestId={REQUEST} origin=direct path=direct metric=providerQueueLatency durationUs=2000",
            f"DexLatency requestId={REQUEST} origin=direct path=semantic terminalReason=VERIFIED",
            f"DexLatency requestId={second} origin=direct path=direct stage=admitted",
            "DexLatency requestId=00000000-0000-0000-0000-000000000003 origin=local path=local terminalReason=VERIFIED",
        ], origin="direct")
        self.assertEqual(report["requests"], 2)
        self.assertEqual(report["incompleteRequests"], 1)
        self.assertEqual(report["metrics"]["sttLatency"]["p95Ms"], 11)
        self.assertEqual(report["metrics"]["providerQueueLatency"]["requestsMissingMetric"], 1)
        self.assertEqual(report["originTerminals"], [
            {"origin": "direct", "path": "semantic", "reason": "VERIFIED", "count": 1},
        ])

    def test_chat_provider_metrics_and_private_contents_never_enter_report(self):
        report = summary.summarize([
            f"DexLatency requestId={REQUEST} origin=chat path=chat metric=providerFirstByteLatency durationUs=1200 transcript=PRIVATE",
            f"DexLatency requestId={REQUEST} origin=chat path=chat metric=chatReplyLatency durationUs=9000 token=SECRET",
            f"DexLatency requestId={REQUEST} origin=chat path=chat metric=providerBodyLatency durationUs=-1",
            f"DexLatency requestId={REQUEST} origin=chat path=chat terminalReason=REPLIED",
            "DexLatency requestId=not-a-uuid origin=chat path=chat terminalReason=CANCELLED",
        ], "chat", "chat")
        self.assertEqual(report["metrics"]["providerFirstByteLatency"]["p95Ms"], 1.2)
        self.assertEqual(report["metrics"]["chatReplyLatency"]["p95Ms"], 9)
        self.assertIsNone(report["metrics"]["providerBodyLatency"]["p95Ms"])
        for private in ("PRIVATE", "SECRET", REQUEST, "not-a-uuid"):
            self.assertNotIn(private, json.dumps(report))

    def test_origin_is_not_inferred_and_conflicting_origin_is_inconclusive(self):
        report = summary.summarize([
            f"DexLatency requestId={REQUEST} origin=direct path=direct stage=admitted",
            f"DexLatency requestId={REQUEST} origin=local path=semantic terminalReason=VERIFIED",
            "DexLatency requestId=00000000-0000-0000-0000-000000000002 path=direct terminalReason=CANCELLED",
        ])
        self.assertEqual(report["originConflicts"], 1)
        self.assertEqual(report["requestsMissingOrigin"], 2)
        self.assertEqual(report["originTerminals"], [])
        self.assertEqual(summary.summarize([
            f"DexLatency requestId={REQUEST} path=direct terminalReason=CANCELLED",
        ], origin="direct")["requests"], 0)

    def test_cli_accepts_direct_origin_and_chat_path(self):
        with tempfile.TemporaryDirectory() as folder:
            log = Path(folder, "sanitized.log")
            log.write_text(f"DexLatency requestId={REQUEST} origin=chat path=chat terminalReason=CANCELLED\n")
            result = subprocess.run([
                sys.executable, str(Path(__file__).with_name("summarize-command-latency.py")),
                "--path", "chat", "--origin", "chat", str(log),
            ], capture_output=True, text=True, check=True)
        report = json.loads(result.stdout)
        self.assertEqual(report["requests"], 1)
        self.assertEqual(report["terminals"], [{"path": "chat", "reason": "CANCELLED", "count": 1}])

    def test_provider_parse_phases_sum_by_request_without_merging_transport_attempts(self):
        report = summary.summarize([
            f"DexLatency requestId={REQUEST} origin=direct metric=providerParseLatency durationUs=1000",
            f"DexLatency requestId={REQUEST} origin=direct metric=providerParseLatency durationUs=3000",
            f"DexLatency requestId={REQUEST} origin=direct metric=providerRequestLatency durationUs=2000",
            f"DexLatency requestId={REQUEST} origin=direct metric=providerRequestLatency durationUs=5000",
            f"DexLatency requestId={REQUEST} origin=direct path=direct terminalReason=VERIFIED",
        ])
        self.assertEqual(report["metrics"]["providerParseLatency"]["samples"], 1)
        self.assertEqual(report["metrics"]["providerParseLatency"]["p95Ms"], 4)
        self.assertEqual(report["metrics"]["providerRequestLatency"]["samples"], 2)


if __name__ == "__main__":
    unittest.main()
