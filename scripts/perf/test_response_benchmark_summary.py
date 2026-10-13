import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location(
    "response_benchmark", Path(__file__).with_name("summarize-response-benchmark.py"))
summary = importlib.util.module_from_spec(spec)
spec.loader.exec_module(summary)


def log(duration, allocation=10000):
    return [line for kind, count in summary.WORKLOAD.items() for block in range(3)
            for line in ([f"DexT021 kind={kind} block={block} trial={trial} durationUs={duration} PRIVATE=SECRET" for trial in range(count)] +
                         [f"DexT021 kind={kind} block={block} samples={count} allocatedBytes={allocation}"])]


class ResponseBenchmarkSummaryTest(unittest.TestCase):
    def test_complete_repeatable_improvement_and_allocation_delta(self):
        before, after = summary.summarize(log(2000)), summary.summarize(log(1000, 7000))
        comparison = summary.compare(before, after)["commandTransport"]
        self.assertEqual(before["workloads"]["commandTransport"]["samples"], 360)
        self.assertEqual(comparison["latencySignal"], "IMPROVED")
        self.assertEqual(comparison["p95ImprovementPercent"], 50)
        self.assertEqual(comparison["medianBlockAllocationDeltaBytes"], -3000)
        self.assertNotIn("SECRET", str(before))

    def test_missing_duplicate_negative_and_unknown_allocations_do_not_pass(self):
        lines = log(1000, -1)
        lines.pop(0)
        lines.append("DexT021 kind=commandTransport block=0 trial=1 durationUs=1000")
        lines.append("DexT021 kind=commandTransport block=0 trial=0 durationUs=-1")
        report = summary.summarize(lines)
        self.assertEqual(report["duplicateTrials"], 1)
        self.assertEqual(report["invalidTrials"], 1)
        self.assertEqual(report["workloads"]["commandTransport"]["blocks"][0]["missingSamples"], 1)
        comparison = summary.compare(summary.summarize(log(2000)), report)["commandTransport"]
        self.assertEqual(comparison["latencySignal"], "INCONCLUSIVE")
        self.assertIsNone(comparison["medianBlockAllocationDeltaBytes"])

    def test_block_noise_can_outweigh_apparent_pooled_improvement(self):
        before = summary.summarize(log(2000))
        candidate_lines = [line.replace("durationUs=1000", "durationUs=3000")
                           if "block=2" in line else line for line in log(1000)]
        after = summary.summarize(candidate_lines)
        comparison = summary.compare(before, after)["wakeFrame"]
        self.assertFalse(comparison["allPairedBlocksFaster"])
        self.assertEqual(comparison["latencySignal"], "INCONCLUSIVE")

    def test_empty_population_is_missing_with_null_percentiles(self):
        report = summary.summarize([])
        self.assertIsNone(report["workloads"]["wakeFrame"]["p95Ms"])
        self.assertEqual(report["workloads"]["wakeFrame"]["blocks"][0]["missingSamples"], 500)
        self.assertEqual(summary.compare(report, report)["wakeFrame"]["latencySignal"], "INCONCLUSIVE")


if __name__ == "__main__":
    unittest.main()
