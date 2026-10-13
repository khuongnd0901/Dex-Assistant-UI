#!/usr/bin/env python3
"""Read sanitized resource values from an explicitly selected physical Fold3/API35."""
import argparse
import datetime
import importlib.util
import json
from pathlib import Path
import re
import subprocess
import time

_spec = importlib.util.spec_from_file_location("avd_resources", Path(__file__).with_name("snapshot-avd-resources.py"))
_avd = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_avd)


def snapshot(adb, port, serial, packages, label):
    if port not in (5037, 5038) or not re.fullmatch(r"[A-Za-z0-9_.:-]{1,160}", serial):
        raise ValueError("Explicit serial and ADB port 5037 or 5038 required")
    if not packages or any(not _avd.PACKAGE.fullmatch(p) for p in packages):
        raise ValueError("Invalid package selection")
    if not re.fullmatch(r"[A-Za-z0-9_-]{1,40}", label):
        raise ValueError("Invalid snapshot label")
    prefix = [adb, "-P", str(port), "-s", serial, "shell"]

    def query(*args, optional=False):
        result = subprocess.run(prefix + list(args), capture_output=True, text=True,
                                timeout=30, check=False)
        if result.returncode and not optional:
            raise RuntimeError("Fold3 resource query failed")
        return result.stdout.strip() if result.returncode == 0 else ""

    # Identity checks precede all process diagnostics, including on localhost ADB links.
    model = query("getprop", "ro.product.model")
    if not re.fullmatch(r"SM-F926[A-Za-z0-9]*", model):
        raise RuntimeError("Selected device is not a Galaxy Z Fold3")
    if query("getprop", "ro.kernel.qemu") == "1":
        raise RuntimeError("Selected device is an emulator")
    if query("getprop", "ro.build.version.sdk") != "35":
        raise RuntimeError("Selected Fold3 is not API35")
    started = time.monotonic()
    processes = []
    for package in dict.fromkeys(packages):
        pids = query("pidof", package, optional=True).split()
        if not pids:
            processes.append({"package": package, "running": False})
        for pid in pids:
            if not pid.isdigit():
                raise RuntimeError("Unexpected process identity")
            memory = _avd.parse_meminfo(query("dumpsys", "meminfo", "-s", pid))
            status = query("cat", f"/proc/{pid}/status", optional=True)
            threads = re.search(r"^Threads:\s*(\d+)\s*$", status, re.M)
            rss = re.search(r"^VmRSS:\s*(\d+)\s*kB", status, re.M)
            processes.append({"package": package, "pid": int(pid), "running": True,
                              "memoryKb": memory,
                              "procRssKb": int(rss[1]) if rss else None,
                              "threads": int(threads[1]) if threads else None})
    return {"label": label, "target": "Fold3", "model": model, "api": 35,
            "timestampUtc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
            "collectionMs": round((time.monotonic() - started) * 1000, 3),
            "processes": processes,
            "note": "Read-only snapshot outside timing windows. Missing fields remain null; a snapshot is not A/B or soak acceptance."}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--port", type=int, choices=(5037, 5038), required=True)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--package", action="append", dest="packages")
    parser.add_argument("--label", default="snapshot")
    args = parser.parse_args()
    try:
        result = snapshot(args.adb, args.port, args.serial,
                          args.packages or ["com.khuongnd.dexassistant.ui"], args.label)
    except (ValueError, RuntimeError, subprocess.TimeoutExpired, OSError) as error:
        reason = str(error) if isinstance(error, (ValueError, RuntimeError)) else type(error).__name__
        parser.exit(1, f"Resource snapshot failed: {reason}\n")
    print(json.dumps(result, indent=2))
