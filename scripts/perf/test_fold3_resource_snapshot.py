import importlib.util
from pathlib import Path
import subprocess
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("fold3_resources", Path(__file__).with_name("snapshot-fold3-resources.py"))
resource = importlib.util.module_from_spec(spec)
spec.loader.exec_module(resource)
PACKAGE = "com.khuongnd.dexassistant.ui"


def outputs(*values):
    return [subprocess.CompletedProcess([], 0, value, "") for value in values]


class Fold3ResourceSnapshotTest(unittest.TestCase):
    def test_invalid_arguments_never_touch_adb(self):
        with patch.object(resource.subprocess, "run") as run:
            for port, serial, package, label in [(5039, "device", PACKAGE, "idle"),
                    (5037, "device;reboot", PACKAGE, "idle"),
                    (5037, "device", PACKAGE + ";reboot", "idle"),
                    (5037, "device", PACKAGE, "private text")]:
                with self.assertRaises(ValueError):
                    resource.snapshot("adb", port, serial, [package], label)
            run.assert_not_called()

    def test_wrong_device_emulator_and_api_never_query_app(self):
        for values in [("Pixel",), ("SM-F926B", "1"), ("SM-F926B", "0", "34")]:
            with self.subTest(values=values), patch.object(resource.subprocess, "run", side_effect=outputs(*values)) as run:
                with self.assertRaises(RuntimeError):
                    resource.snapshot("adb", 5037, "phone", [PACKAGE], "idle")
                self.assertTrue(all("getprop" in c.args[0] for c in run.call_args_list))

    def test_redacted_snapshot_keeps_missing_values_and_correct_server(self):
        values = outputs("SM-F926B", "0", "35", "4321",
                         "private text\nApp Summary\n TOTAL PSS: 1234 TOTAL RSS: 2000",
                         "Name: private text\nThreads: 17\n")
        with patch.object(resource.subprocess, "run", side_effect=values) as run:
            report = resource.snapshot("adb", 5038, "phone", [PACKAGE, PACKAGE], "idle")
            self.assertTrue(all(c.args[0][:6] == ["adb", "-P", "5038", "-s", "phone", "shell"] for c in run.call_args_list))
        process = report["processes"][0]
        self.assertEqual(process["memoryKb"]["totalPss"], 1234)
        self.assertEqual(process["threads"], 17)
        self.assertIsNone(process["procRssKb"])
        self.assertIsNone(process["memoryKb"]["graphics"])
        self.assertEqual(len(report["processes"]), 1)
        self.assertNotIn("private text", str(report))

    def test_absent_process_is_missing_not_zero(self):
        with patch.object(resource.subprocess, "run", side_effect=outputs("SM-F926B", "0", "35", "")):
            report = resource.snapshot("adb", 5037, "phone", [PACKAGE], "stopped")
        self.assertEqual(report["processes"], [{"package": PACKAGE, "running": False}])

    def test_diagnostics_failure_does_not_expose_raw_output(self):
        failed = subprocess.CompletedProcess([], 1, "private payload", "secret")
        with patch.object(resource.subprocess, "run", return_value=failed):
            with self.assertRaisesRegex(RuntimeError, "^Fold3 resource query failed$"):
                resource.snapshot("adb", 5037, "phone", [PACKAGE], "idle")


if __name__ == "__main__":
    unittest.main()
