> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-021 — Windows simulator response/RAM preparation, 2026-10-13

Owner authorizes T-021 implementation in priority order, testing Windows
simulator first and deferring all Z Fold3 execution. Simulator preparation is verified below. T-021 remains IN_PROGRESS because
physical acceptance belongs to T-020 and is explicitly deferred by the owner.

## Scope and provenance

- Android base HEAD `3f2bea85cd7dd8215639ae2080f0d2d856e738a9`; pre-existing
  dirty source/EOL/JVM logs retained, no commit/push performed for this work.
- Exact target: Windows `DeX_Command_Test_API35`, Android15/API35,
  serial `127.0.0.1:5555`, Windows ADB server5038. Selection used
  `bash scripts/android/select-test-target.sh dex --emulator-only`.
- Instrumented baseline APK/test APK installed in place with original matching
  signing SHA256 `9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`.
  No Fold3 commands/install, reset/uninstall, owner/grant/role/signing changes,
  live provider calls, quota use or cloud operations.
- APKs/source identities/raw logs/private dirty patch preserved outside Git at
  `/mnt/d/dex-t021-evidence/`; generated files are not project artifacts.

## M0 — Baseline before optimization

Baseline contains timing instrumentation, without performance edits. Android
JDK17/Linux SDK `test assembleDebug assembleDebugAndroidTest` PASS130tasks,
2m47s. The identical synthetic workload remains the comparison basis.

Windows instrumentation of `com.khuongnd.dexassistant.ui.ResponsePerformanceDeviceTest`
PASS2/2,12.115s. Offline mocked transport, isolated UUID preferences/credential
aliases, harmless PCM, no natural speech or actual action execution. Three
blocks each contain120Command requests,120discovery trials and500warm native
wake frames; first30transport calls/100native frames warm up separately.

| Component | Samples | Median ms | p95 ms | Per-block p95 ms |
| --- | ---: | ---: | ---: | --- |
| Offline Command plan/transport | 360 | 18.736 | 44.701 | 48.624 /39.689 /32.136 |
| Recognizer service discovery | 360 | 1.254 | 3.267 | 3.402 /3.543 /2.309 |
| Warm native wake frame | 1500 | 0.033 | 12.935 | 12.846 /12.935 /13.021 |

Whole-fixture allocation bytes/block: Command38,928,384/39,661,664/39,583,744
per120requests; wake819,200/884,736/819,200 per500frames. ART allocation includes
fixture/logging/Java wrapper overhead, not just input conversion. Wake p95 mixes
native decode frames with cheaper nondecode frames; conversion is not the
entire recognizer. Figures are AVD component measurements, not Fold3 latency.

Targets recorded before edits: investigate ≥20%Command allocation reduction
and latency improvement exceeding inter-block noise; eliminate500normalizing
FloatArrays/block while keeping native p95 within a conservative10%noise budget.
Discovery is a smaller cost; do not alter production endpoint hints/recognizer.

Root summary of actual baseline logs:360direct PLANNED terminals,
0incomplete/0origin conflicts. Separate baseline `DirectAiFaultDeviceTest`
PASS9/9,3.857s; coordinator Chat trace has5requests,3REPLIED/2CANCELLED,
0incomplete/conflicts. Standalone adapter fault tests need not emit coordinator
correlation. All content stays in fixtures, never in metric logs.

Postfixture resource snapshot verifies the exact AVD but app process is absent.
This is missing PSS, not zero RAM or an idle-memory result. AVD RAM scope is
allocation/resource-bounding; production idle PSS and natural accuracy are
deferred to T-020.

Baseline APK SHA256:

| Artifact | SHA256 |
| --- | --- |
| app-debug.apk | `ecd8923d8ac6910290aad778d64343feb945ea759fd7d777ed747ab375f09655` |
| app-debug-androidTest.apk | `ba63b0bb2d25e9e18b87689b68ef8ba612e875a1e7ba51300a3e8b210e107c29` |

## Implemented changes verified on host/AVD

1. Add monotonic admission/status/action/worker/provider/Chat/TTS/rearm timing,
   retained original route, terminal/missing accounting. Actual screen dispatch
   hook replaces the prior early mark before service validation; failed guards
   must not emit action-start. User final/partial/confirmation semantics remain.
2. Recognizer service-catalog reuse invalidated by package install/remove/change/
   replace; Secure selected service read each turn. No recognizer-instance or
   endpoint-hint reuse.
3. Bounded immutable serialized tool catalog for current capability/app scope,
   lazy provider schema generation and JSON-quoted dynamic request fields.
   Every provider plan still undergoes strict Android validation; no target,
   action result, credential or command text cached.
4. Worker-owned exact-size PCM buffer reused across frames, with old contents
   erased on replacement/reset/capture-loop exit/close. Short reads cannot
   include stale tails; native close is idempotent. Warm model/threshold/thread
   configuration remains unchanged.
5. Provider response reads bounded before accumulation; Chat preserves65,536
   UTF-16-character limit and charset/BOM behavior. Command now rejects bodies
   above65,536bytes instead of peeking a truncated prefix, preventing a valid
   prefix plus oversized tail from yielding an action plan. Error codes remain
   fail-closed; response ownership closes once.
6. Command cancellation also cancels active HTTP and blocks cancellation
   fallback, retaining stale-result guards; no automatic action replay.
7. First eligible provider key is decrypted once within a single plan/reply,
   then its local slot cleared before HTTP; fallback eligibility checks and
   reload-at-attempt remain. New calls/turns read current keys. No plaintext
   instance/global cache or encrypted-storage/alias change.
8. Explicit Android JSONException handling keeps malformed/empty/oversized
   JSON and validated-output failures mapped to exact fail-closed codes, with
   no fallback for invalid output.

Pinned JNI source reviewed 2026-10-13 supports synchronous input-array
consumption before reuse: [v1.13.8 JNI OnlineStream](https://raw.githubusercontent.com/k2-fsa/sherpa-onnx/v1.13.8/sherpa-onnx/jni/online-stream.cc),
[stream implementation](https://raw.githubusercontent.com/k2-fsa/sherpa-onnx/v1.13.8/sherpa-onnx/csrc/online-stream.cc)
and [feature extraction](https://raw.githubusercontent.com/k2-fsa/sherpa-onnx/v1.13.8/sherpa-onnx/csrc/features.cc).
This source inference still requires native simulator smoke and eventual
physical wake/accuracy testing; it does not establish Fold3 performance.

## Canonical tooling validation

Executed from canonical repository:

```bash
python3 -m unittest discover -s scripts/perf -p 'test_*summary.py' -v
python3 -m unittest discover -s scripts/perf -p 'test_avd_resource_snapshot.py' -v
python3 scripts/perf/summarize-response-benchmark.py /mnt/d/dex-t021-evidence/baseline/timing-logcat.txt
python3 scripts/perf/summarize-command-latency.py --origin direct /mnt/d/dex-t021-evidence/baseline/timing-logcat.txt
python3 scripts/perf/summarize-command-latency.py --origin chat /mnt/d/dex-t021-evidence/baseline-chat-fixtures/timing-logcat.txt
python3 scripts/perf/snapshot-avd-resources.py --serial 127.0.0.1:5555 --label baseline-post-fixture
```

Host14summary/comparison tests and5resource tests PASS. Real baseline reports
have complete trial counts with no duplicate/invalid trials. CLI outputs live
outside Git. Comparison signal uses a conservative block-spread screening rule,
not a confidence interval. Missing/conflicting origins, unfinished requests and
unmeasured resource values remain explicit.

## Candidate validation and residual gates

First full candidate run passed200JVMtests/variant and lint but Android test
APK compilation encountered an older class jar after a screen-hook edit during
that build. No candidate APK was installed from that incomplete run. Frozen
source rebuild subsequently PASS201tasks/6m6s (app200tests/variant,0failures/
errors/1expected skip; DPC5/variant), then candidate benchmark PASS2/2,8.788s.

First comparison: discovery p95 3.267→0.043ms and wake12.935→11.497ms exceed
the helper block-spread screening rule. Command44.701→29.119ms is an observed
34.86%reduction but **INCONCLUSIVE** against24.105mscombined block spread.
Command median block allocation39,583,744→32,120,832bytes (−18.85%); wake
819,200→163,840bytes (−80%). Stable wake blocks allocate1normalizing array
instead of500 (499arrays removed), not zero allocation. These are component/
fixture figures, never idle PSS or natural end-to-end response.

Selected AVD regression run:29PASS/4assumption-SKIP/1FAIL,33tests. Failure
exposed Android org.json.JSONException inheriting checked Exception whereas
host library inherits RuntimeException; narrow catches let malformed JSON
escape expected INVALID_LLM_OUTPUT mapping. Primary references checked2026-10-13:
[Android JSONException](https://developer.android.com/reference/org/json/JSONException)
and [pinned host JSON20240303](https://raw.githubusercontent.com/stleary/JSON-java/20240303/src/main/java/org/json/JSONException.java). Fix and exact-mapping fixtures
are required, not test suppression. Dashboard and VoiceRuntime class fixtures
are intentionally omitted because their setup mutates production voice policy
or reads/prunes owner history; no UI layout/endpoint change requires those
unsafe-to-owner-data fixtures. Native/discovery are tested in isolated suites.

The remaining repeated credential decryption (hasKey calls load, then request
calls load again) was removed using **request-local first-provider key
reuse only**; fallback keys still reload at their actual attempt. No instance/
global plaintext cache, alias/encryption/default change. Rotation/deletion/
fallback-freshness and Android JSON mapping regressions now PASS in the final
safe AVD suite below. Earlier candidate artifacts/failures remain preserved
outside Git under candidate-before-key-json-fix; never use them as final APKs.

## Final exact-candidate validation

From `/mnt/d/dex-assistant-ui`:

```bash
env JAVA_HOME=/home/khuongnd/.local/opt/jdk17 ANDROID_HOME=/home/khuongnd/Android/Sdk ./gradlew test lint assembleDebug assembleDebugAndroidTest --offline --max-workers=1 --no-watch-fs
```

Latest frozen source PASS201tasks/6m3s. App200tests each debug/release,
0failures/errors/1expected skip each; DPC5eachPASS. Root independently read
JUnit XML and verified SHA256 for20source/testfiles after the build and all
AVD rounds. Benchmark source hash is identical baseline/candidate; no app/test
edits or builds occurred during the alternating A/B sequence.

Final single benchmark PASS2/2,7.038s. Targeted safe instrumentation PASS
**31/35,4assumption-SKIP,0FAIL**,8.04s. Classes under
`com.khuongnd.dexassistant.ui`: DirectAiFaultDeviceTest,
ResponseMetricsDeviceTest, ConfigCommandDeviceTest, SherpaCorpusPcmTest,
SherpaDryRunScoreTest; methods YoutubeSearchDeviceTest#searchIntentUsesPackageBoundResultsDeepLink
and NumberedScreenDeviceTest#numericAndMalformedFamiliesNeverBecomeBackendCommands.
Invocation uses `adb.exe -P 5038 -s 127.0.0.1:5555 shell am instrument -w -r
-e class CLASS_LIST com.khuongnd.dexassistant.ui.test/androidx.test.runner.AndroidJUnitRunner`.
Native benchmark uses ResponsePerformanceDeviceTest separately.

New verified cases cover cache scope/escaping, body size/Unicode/charset/BOM,
malformed/empty provider output exact mapping, request-local key read counts,
rotation/deletion and fresh fallback keys, HTTP cancellation/no-fallback,
late reply/idle/context isolation, observed local status/action order and
EMPTY/queued-cancel absence of fabricated intervals, recognizer invalidation
and app-stub exclusion, screen CANCEL/INVALID/expired/missing-DeX zero starts.
Scoped preferences/credentials/history only; no actual external action.

Four assumption skips remain3Samsung selected-display/Maps/pair cases and
1private real-corpus scoring. Dashboard and VoiceRuntime class omissions are
not included as skip/pass counts. Count only unique class/test terminal frames,
not custom metadata status0 events. Root independently confirmed35terminal
cases:31code0 and4code−4, no failure/error terminal.

## Alternating paired A/B — accepted simulator comparison

Because standalone baseline/final runs spanned build cycles,3additional
baseline→candidate rounds alternated retained exact matching-cert APK pairs.
Each install reverified emulator-only selector/identity and preserved data/
settings. Six benchmark invocations PASS12test executions total, each with
3clean complete blocks; no duplicate/invalid trials. Per variant:1080Command
requests,1080discovery trials and4500warm native frames. These remain synthetic
component trials; no natural session counter increases.

| Pair | Command p95 baseline→candidate ms | Improvement | Discovery p95 baseline→candidate ms | Wake p95 baseline→candidate ms |
| --- | --- | --- | --- | --- |
| 1 | 34.149→16.073 | 52.93% | 3.194→0.033 | 11.434→11.947 |
| 2 | 36.288→20.446 | 43.66% | 3.715→0.026 | 11.501→11.367 |
| 3 | 35.501→18.268 | 48.54% | 3.173→0.102 | 11.632→12.113 |

Command and discovery improve beyond each pair's conservative block-spread
budget; all9paired Command blocks are faster. Command allocation reduction is
45.85/45.87/45.95%, about39.6MB→21.4MB per120requests. Wake allocation decreases
84/80/80%, median819,200→131,072/163,840/163,840bytes per500frames.

**Native wake speed is INCONCLUSIVE in all3paired rounds**: deviations−1.17%
to+4.49%stay within the predeclared10%noise budget but do not establish a speed
improvement. The earlier standalone10%wake-speed interpretation is withdrawn;
accept allocation reduction only. No idle PSS/RAM footprint or real acoustic/
provider latency improvement is inferred. Command p95 here includes offline
fixture/Keystore/request/validation work, not live Gemini/network response.

Reproduce each pair from canonical repository:

```bash
python3 scripts/perf/summarize-response-benchmark.py /mnt/d/dex-t021-evidence/paired-01-baseline/timing-logcat.txt /mnt/d/dex-t021-evidence/paired-01-candidate/timing-logcat.txt
```

Use paired-02 and paired-03 paths for the other two. Root independently
regenerated all comparisons, checked completeness/counts and matched saved
reports. Helper rules are screening evidence, not confidence intervals.

Final AVD app/test APKs are the optimized candidate, verified by pulling
installed base APKs and matching SHA256. Original certificate retained.

| Final artifact | SHA256 |
| --- | --- |
| app-debug.apk | `c434bfebb73f7615e01ce6810c0e806663b98e21d9aa5c4feac512c13fc0d15d` |
| app-debug-androidTest.apk | `13583c48ae6d7499c8ce8e808f36ee4f5dbb8e3d881e20806bff74e642783f7c` |
| 20-file frozen source manifest | `d68530d49b040f6090edcd25f9065abb6c60c751537bb8cc7d49264a81a60473` |

Raw locations: candidate/key-json-final-validation.log,
candidate-final/{timing-logcat,instrumentation}.txt,
candidate-final-regressions/{timing-logcat,instrumentation}.txt,
paired-01/02/03-baseline/candidate directories and final-installed-identities.json
under `/mnt/d/dex-t021-evidence`. Sanitized canonical report is the durable
handoff; no raw log/APK/model/audio/key added to Git.

Final root integration checks PASS:81local links across canonical task/status/
plan/evidence/measurement docs,8required ExecPlan sections,21unique task IDs,
20source/test hashes, installed app/test APK hash consistency and changed-file
whitespace in both repositories. Pre-existing unrelated dirty files/JVM logs
were excluded from the task check and retained without edits.

## Remaining physical and conditional work

M4 R8/Baseline Profile tuning deferred: no measured startup/ART bottleneck or
matching protected-signing/profileable benchmark evidence; current priorities
are measured per-request work and allocation. Production release configuration
is unchanged. All physical acoustic/STT/USB/TTS/echo-safe Chat exit, actual
Samsung display/grid/semantic, ≥30natural samples/route/candidate, ≥100natural
candidate sessions, production CPU/PSS/thermal/battery and8hsoak remain T-020.
No idle RAM savings or full SRS p95<2s acceptance is claimed from this report.
