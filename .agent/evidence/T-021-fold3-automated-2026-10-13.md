# T-021 — Resumed Fold3 automated checkpoint, 2026-10-13

Owner authorized priority-order execution and per-task commit/push, resumed
Fold3 testing, then explicitly skipped speaking for now. T-021 remains
IN_PROGRESS: no natural sessions, matched acoustic A/B or eight-hour soak added.

## Candidate and device

Source app/test baseline: `5472dff` (clean before collector/tool changes).
JDK17 `/home/kunrg/.local/opt/jdk17`, SDK `/home/kunrg/Android/Sdk`:
`./gradlew test lint assembleDebug assembleDebugAndroidTest --offline --max-workers=1 --no-watch-fs`
PASS201tasks/6s, existing source checks up to date. Log outside Git:
`/tmp/dex-priority-validation-20261013.log`.

Physical SM-F926B/API35, Linux ADB5037, Samsung EXTERNAL_DEX display2,
1920×1080. Original app/test APKs retained before replace installs at
`/tmp/dex-t021-fold3-20261013/`. Original and candidate app/test certificates
verified: SHA256 `9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`.
No DPC install, reset, uninstall, role/grant/settings change or provider call.

| Artifact | SHA256 |
| --- | --- |
| Original app | `0d9e529013a1654852f223453b9658803043822d31719e1963e60894c99b7fb4` |
| Original test | `c538dbc5d8b2c67d99a0ec2227f980650cd5f6531e3ed5c69b0136cfbbc1adf0` |
| Candidate app | `3d14c12844b027c78839b2ef6afaeb2fc64ca59aed3f9bd9415050a5153e9e9f` |
| Candidate test | `25fb0892c93b63f37314d26708d4214a56d755a5fd2facc47346d2b28bf733e4` |

Candidate app installed hash matches local signed artifact. Original installed
app is an older October10 build, not the retained T-021 instrumented Windows
baseline. `/mnt/d/dex-t021-evidence` is unavailable on this Linux host; do not
compare this candidate with Windows measurements as a physical A/B.

## Executed checks

- `ResponsePerformanceDeviceTest`: PASS2/2,4.91s. Same synthetic offline
  fixture, no mic/actual action/provider. Three blocks:360Command transport,
  360recognizer discovery,1500warm native wake frames. All complete, no duplicate
  or invalid trials.
- `DirectAiFaultDeviceTest` plus three applicable `ResponseMetricsDeviceTest`
  methods: PASS17/17,3.969s. Metrics methods: first-status/action captured
  Intent, EMPTY/queued cancellation, recognizer invalidation. The fourth
  raw-screen method requires missing Samsung desktop and was not selected.
- Perf tooling host suite: PASS26/26. New physical collector verifies explicit
  ADB server, SM-F926*/API35/non-emulator before diagnostics; allowlisted numeric
  fields only, absent values null. Tests cover identity refusal, argument
  injection, unavailable fields, diagnostic errors and redaction.
- Benchmark summary accepts explicit `--target Fold3 --single`, avoiding a
  candidate-only report labelled baseline/Windows. Cross-hardware comparison
  cannot pass the improvement rule.

| Synthetic component | p50 ms | p95 ms | Allocation bytes/block |
| --- | ---: | ---: | --- |
| Offline Command | 8.560 | 12.174 | 21,348,352 /21,331,968 /21,315,584 |
| Discovery | 0.018 | 0.028 | not measured |
| Native wake frame | 0.024 | 8.213 | 147,456 each500frames |

No improvement claim without a matching physical baseline. These component
numbers do not establish speech-end→action, live-provider or wake accuracy.

Commands use `adb -P5037 -s <confirmed-serial> shell am instrument -w -r -e class
<selected-classes/methods> com.khuongnd.dexassistant.ui.test/androidx.test.runner.AndroidJUnitRunner`.
Filtered `DexT021/DexLatency/DexVoiceTiming` log only; stopped collector after
checks. Summarize with:

```bash
python3 scripts/perf/summarize-response-benchmark.py --target Fold3 --single /tmp/dex-t021-fold3-20261013/timing-logcat.txt
python3 scripts/perf/snapshot-fold3-resources.py --port 5037 --serial '<confirmed-serial>' --label after-regressions
```

## Resources and postflight limits

Before update: PSS83,209KiB/RSS181,196KiB,23threads. After regression process:
PSS60,748KiB/RSS163,780KiB,14threads. Different process/workload/capture states:
**not a memory-savings comparison**. A snapshot collected during regression
startup is excluded from any idle claim. No actual capture/soak proof.

Assistant role, enabled Accessibility configuration, Device Owner components,
app UID/data-directory inode, permission grant rows, voice-policy file,
provider settings and encrypted credentials unchanged. Accessibility service
is present in postflight diagnostics; natural readiness is unverified.
History ciphertext hash changed after app startup; no before plaintext snapshot
was captured, so plaintext equality/retention is **unmeasured**, not claimed.
Fixtures use isolated histories and captured Intents, not owner records/actions.

Next: T-014 lifecycle/long-response work, then recover expanded T-017 collector
from the discovered local dirty worktree. Physical natural A/B, acoustic metrics,
100sessions and8hsoak stay open under T-020; owner currently skips speaking.
