#!/usr/bin/env python3
"""Read sanitized process memory snapshots from one explicitly verified Windows AVD."""
import argparse
import datetime
import json
import re
import subprocess
import time

AVDS = ("DeX_Command_Test_API35", "ZFold3_API35")
PACKAGE = re.compile(r"[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+")


def parse_meminfo(output):
    """Only App Summary values, never view/activity/private diagnostic strings."""
    summary = output.split("App Summary", 1)[-1] if "App Summary" in output else ""
    fields = {
        "javaHeap": "Java Heap", "nativeHeap": "Native Heap", "code": "Code",
        "stack": "Stack", "graphics": "Graphics", "privateOther": "Private Other",
        "system": "System", "totalPss": "TOTAL PSS", "totalRss": "TOTAL RSS",
        "totalSwapPss": "TOTAL SWAP PSS",
    }
    values = {}
    for key, label in fields.items():
        match = re.search(r"\b" + label + r":\s*(\d+)\b", summary)
        values[key] = int(match[1]) if match else None
    return values


def snapshot(adb, serial, avd, packages, label):
    if not re.fullmatch(r"(?:emulator-\d+|127\.0\.0\.1:\d+)", serial):
        raise ValueError("An explicit emulator or localhost AVD serial is required")
    if avd not in AVDS or not packages or any(not PACKAGE.fullmatch(p) for p in packages):
        raise ValueError("Invalid AVD or package selection")
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,40}", label):
        raise ValueError("Invalid snapshot label")
    prefix = [adb, "-P", "5038", "-s", serial, "shell"]

    def query(*args, optional=False):
        result = subprocess.run(prefix + list(args), capture_output=True, text=True,
                                timeout=30, check=False)
        if result.returncode and not optional:
            raise RuntimeError("ADB resource query failed; inspect the selected AVD locally")
        return result.stdout.strip() if result.returncode == 0 else ""

    # Never collect application data from a non-matching device.
    if query("getprop", "ro.boot.qemu.avd_name") != avd:
        raise RuntimeError("Selected device is not the requested AVD")
    if query("getprop", "ro.build.version.sdk") != "35":
        raise RuntimeError("Selected AVD is not API 35")
    started = time.monotonic()
    processes = []
    for package in dict.fromkeys(packages):
        pid_text = query("pidof", package, optional=True)
        pids = [p for p in pid_text.split() if p.isdigit()]
        if not pids:
            processes.append({"package": package, "running": False})
        for pid in pids:
            meminfo = query("dumpsys", "meminfo", "-s", pid)
            status = query("cat", f"/proc/{pid}/status", optional=True)
            threads = re.search(r"^Threads:\s*(\d+)\s*$", status, re.M)
            rss = re.search(r"^VmRSS:\s*(\d+)\s*kB", status, re.M)
            processes.append({
                "package": package, "pid": int(pid), "running": True,
                "memoryKb": parse_meminfo(meminfo),
                "procRssKb": int(rss[1]) if rss else None,
                "threads": int(threads[1]) if threads else None,
            })
    return {
        "label": label, "target": "AVD", "avd": avd, "api": 35,
        "timestampUtc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "collectionMs": round((time.monotonic() - started) * 1000, 3),
        "processes": processes,
        "note": "Collect outside latency windows; missing values stay null. AVD memory is not Fold3 acceptance.",
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", default="/mnt/d/Android/Sdk/platform-tools/adb.exe")
    parser.add_argument("--serial", required=True)
    parser.add_argument("--avd", choices=AVDS, default=AVDS[0])
    parser.add_argument("--package", action="append", dest="packages")
    parser.add_argument("--label", default="snapshot")
    args = parser.parse_args()
    try:
        result = snapshot(args.adb, args.serial, args.avd,
                          args.packages or ["com.khuongnd.dexassistant.ui"], args.label)
    except (ValueError, RuntimeError, subprocess.TimeoutExpired, OSError) as error:
        # Do not include stdout/stderr from arbitrary device diagnostics.
        reason = str(error) if isinstance(error, (ValueError, RuntimeError)) else type(error).__name__
        parser.exit(1, f"Resource snapshot failed: {reason}\n")
    print(json.dumps(result, indent=2))
