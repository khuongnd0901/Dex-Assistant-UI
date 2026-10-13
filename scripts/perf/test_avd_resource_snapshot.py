import importlib.util
from pathlib import Path
import subprocess
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location(
    "resource_snapshot", Path(__file__).with_name("snapshot-avd-resources.py"))
snapshot = importlib.util.module_from_spec(spec)
spec.loader.exec_module(snapshot)
SERIAL = "127.0.0.1:5555"
PACKAGE = "com.khuongnd.dexassistant.ui"


class AvdResourceSnapshotTest(unittest.TestCase):
    def test_physical_serial_and_shell_arguments_rejected_before_adb(self):
        with patch.object(snapshot.subprocess, "run") as run:
            for serial, package in [("192.168.1.3:5555", PACKAGE), (SERIAL, PACKAGE + ";reboot")]:
                with self.assertRaises(ValueError):
                    snapshot.snapshot("adb.exe", serial, snapshot.AVDS[0], [package], "baseline")
            run.assert_not_called()

    def test_mismatched_avd_never_queries_application_process(self):
        with patch.object(snapshot.subprocess, "run", return_value=subprocess.CompletedProcess([], 0, "another-avd\n", "")) as run:
            with self.assertRaises(RuntimeError):
                snapshot.snapshot("adb.exe", SERIAL, snapshot.AVDS[0], [PACKAGE], "baseline")
            self.assertEqual(run.call_count, 1)
            self.assertEqual(run.call_args.args[0][-2:], ["getprop", "ro.boot.qemu.avd_name"])

    def test_summary_only_values_unknowns_null_and_private_text_removed(self):
        parsed = snapshot.parse_meminfo("Java Heap: 999\nPRIVATE\nApp Summary\n Java Heap: 123 456\n Native Heap: 24\n TOTAL PSS: 2048 TOTAL RSS: 4096\n")
        self.assertEqual(parsed["javaHeap"], 123)
        self.assertEqual(parsed["totalPss"], 2048)
        self.assertEqual(parsed["totalRss"], 4096)
        self.assertIsNone(parsed["graphics"])
        self.assertNotIn("PRIVATE", str(parsed))
        self.assertTrue(all(value is None for value in snapshot.parse_meminfo("unavailable").values()))

    def test_exact_serial_port_and_per_process_snapshot(self):
        outputs = [snapshot.AVDS[0], "35", "4321", "App Summary\n TOTAL PSS: 1234", "Name: PRIVATE\nVmRSS: 2000 kB\nThreads: 17\n"]
        with patch.object(snapshot.subprocess, "run", side_effect=[subprocess.CompletedProcess([], 0, text, "") for text in outputs]) as run:
            report = snapshot.snapshot("adb.exe", SERIAL, snapshot.AVDS[0], [PACKAGE], "baseline")
            self.assertTrue(all(call.args[0][:6] == ["adb.exe", "-P", "5038", "-s", SERIAL, "shell"] for call in run.call_args_list))
        self.assertEqual(report["processes"][0]["threads"], 17)
        self.assertEqual(report["processes"][0]["procRssKb"], 2000)
        self.assertEqual(report["processes"][0]["memoryKb"]["totalPss"], 1234)
        self.assertNotIn("PRIVATE", str(report))

    def test_stopped_process_is_missing_not_zero_memory(self):
        outputs = [subprocess.CompletedProcess([], 0, snapshot.AVDS[0], ""), subprocess.CompletedProcess([], 0, "35", ""), subprocess.CompletedProcess([], 1, "", "process missing")]
        with patch.object(snapshot.subprocess, "run", side_effect=outputs):
            report = snapshot.snapshot("adb.exe", SERIAL, snapshot.AVDS[0], [PACKAGE], "paused")
        self.assertEqual(report["processes"], [{"package": PACKAGE, "running": False}])


if __name__ == "__main__":
    unittest.main()
