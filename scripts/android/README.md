# Android-local Windows simulator tooling

Run every command here from the Android repository root. No backend checkout,
server or backend scripts are required. Current memory: [STATUS](../../.agent/STATUS.md).

```bash
bash scripts/android/select-test-target.sh dex --emulator-only
# Generic foldable layout instead:
bash scripts/android/select-test-target.sh foldable --emulator-only
```

`--emulator-only` skips physical Fold3 discovery/queries on both ADB servers.
It selects or starts only the named API35 Windows AVD; it does not install APKs.
Keep the exact reported `ADB`, `PORT` and `SERIAL` for all following commands.
Windows ADB uses port5038; Linux ADB uses port5037. A loopback serial such as
`127.0.0.1:5555` is valid only after the reported AVD-name/API checks.
Without `--emulator-only`, the selector prefers a connected API35 SM-F926* Fold3;
physical execution is coordinated under [T-020](../../.agent/plans/active/T-020-consolidated-fold3-acceptance.md).

Windows SDK is `D:\Android\Sdk`, AVD home `D:\Android\AVD`; WSL paths are
`/mnt/d/Android/...`. SDK Manager is `cmdline-tools/latest/bin/sdkmanager.bat`;
ADB is `platform-tools/adb.exe`; Emulator is `emulator/emulator.exe`.
Use JDK17 and Linux SDK `/home/khuongnd/Android/Sdk` for WSL Gradle builds.

The selector's local PowerShell dependencies are [desktop starter](start-dex-test.ps1)
and [foldable starter](start-zfold3.ps1). Desktop `DeX_Command_Test_API35` is a
marked disposable AOSP/API35 1920x1080/160dpi freeform approximation. Foldable
`ZFold3_API35` uses the generic 7.6-inch fold-in profile. Neither verifies Samsung
One UI/DeX display category, natural mic/USB/STT/TTS, thermals/radio or Fold3 PSS.
Do not close physical acceptance from simulator evidence. No reset/provision/
install helpers were moved as current tooling; historical setup is in
[archived guide](../../docs/archive/backend-android-tooling-README.md).

Host validation from Android root:

```bash
python3 -m unittest discover -s scripts/android/tests -p 'test_*.py' -v
python3 -m unittest discover -s scripts/perf -p 'test_*.py' -v
python3 scripts/project/validate_project_memory.py
bash -n scripts/android/select-test-target.sh
```

Performance collectors/summarizers:

```bash
python3 scripts/perf/snapshot-avd-resources.py --help
python3 scripts/perf/summarize-command-latency.py --help
python3 scripts/perf/summarize-response-benchmark.py --help
```

The snapshot tool only queries sanitized process values on an explicit matching
AVD/API35; missing processes/fields stay missing. Save logs/JSON/raw artifacts
outside Git. See [T-021 measurements](../../.agent/evidence/T-021-windows-response-performance-2026-10-13.md)
for exact accepted source/APK hashes, paired benchmark methods and limits.

Build and test app changes using task-relevant Gradle checks before any authorized
scoped AVD install. Preserve existing signatures, history, credentials and owner
settings; broad legacy instrumentation can write owner policy/history. Original
signing/Device Owner/Assistant role/Accessibility/grants and sole wireless ADB
must stay intact. No physical or live provider actions are implied by this guide.

Physical T-021 resource snapshots (owner resumed device testing on 2026-10-13):

```bash
python3 scripts/perf/snapshot-fold3-resources.py --port 5037 --serial '<confirmed-Fold3-serial>' --label preflight
```

Select the actual ADB server/serial, then verify SM-F926*/API35 before any
process query. This separate read-only collector rejects other models/emulators,
exports only numeric App Summary/proc fields, and keeps unavailable values null.
Store reports outside Git. Single snapshots and offline instrumentation do not
satisfy natural speech, matched A/B, 100-session or eight-hour soak gates.
