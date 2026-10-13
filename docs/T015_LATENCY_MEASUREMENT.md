> Migrated to Android on 2026-10-13 ([D-037](../.agent/DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Android Command and Chat latency measurement

Current runtime is Android-only (D-036). T-021 extends T-015 monotonic metrics
to direct provider Command and text-only Chat. Physical acceptance belongs to
T-020; Windows AVD results establish simulator behavior only.

## Clock, correlation and privacy

Android durations use the monotonic clock in the owning Android process.
Recognition epochs correlate to UUID request IDs; every Chat turn has its own
UUID. Use one process lifetime per input because epochs may repeat after restart.
Never subtract Android/provider/backend wall-clock timestamps.

`DexLatency` carries fixed metric names, durationUs, request ID/epoch and bounded
classification. Do not log recognized text, UI text, prompts, keys, tokens,
screen contents or provider bodies. Summary JSON never exposes request IDs.
Missing endpoints remain missing, never zero.

`path` is the execution/terminal path: local, direct, chat, semantic, fallback.
Historical remote remains readable for old WebSocket evidence. `origin` is the
original route: local, direct, chat (legacy remote also accepted). A direct plan
executed semantically retains origin=direct with path=semantic. Before routing,
omit origin; earlier events are resolved from a later explicit origin for the
same request. A request ending before routing can lack origin and is reported
as missing. Never infer origin from legacy terminal path. Conflicting origins
are counted and excluded from origin-filtered results.

Admitted requests lacking a terminal count as incomplete. Failures, timeouts
and cancellations stay in the terminal table even without a completed interval.
First status means acknowledgement, not successful action.

## Stage definitions

| Metric | Start → end / meaning |
| --- | --- |
| wakeLatency | Wake detected → recognizer ready; excludes acoustic wake-word duration. |
| sttLatency | Recognizer ready → final callback or explicit second mic submit; includes user speaking time. |
| speechEndToFinalLatency | Recognizer end-of-speech callback → final callback, if both exist. |
| firstStatusLatency | Final/typed input accepted → first status acknowledgement. |
| routeLatency | Final text accepted → routing decision. |
| finalToActionStartLatency | Final text accepted → first actual action dispatch; identify confirmation delays separately. |
| providerQueueLatency | Provider task queued → worker begins it. |
| providerRequestLatency | HTTP execute starts → response headers or terminal transport error, once per attempt; not a complete answer. |
| providerFirstByteLatency | HTTP execute starts → first body byte available; absent if no byte arrives. |
| providerBodyLatency | First body byte available → body-read completion/terminal, subject to transport size limit. |
| providerParseLatency | JSON decode and typed-plan/text validation duration. |
| uiResolutionLatency | Semantic resolution/live revalidation starts → decision. |
| executionLatency | ActionRouter invocation → terminal; active segments summed per request excluding confirmation wait. |
| confirmationWaitLatency | Prompt → accepted/rejected/expired answer; user waiting time separate from machine latency. |
| speechEndToActionStartLatency | Recognizer end callback → actual action dispatch, if both exist. |
| chatReplyLatency | Accepted Chat turn → complete validated text reply; not provider first byte. |
| chatTtsStartLatency | Accepted voice Chat turn → TTS playback-start callback, when supplied by engine. |
| rearmLatency | Terminal feedback lifecycle → actual next wake capture or Chat recognizer readiness, if observed. |

These additive fields are supported by the summary tool; runtime coverage and
fixture evidence are recorded in T-021. Listing a field is not proof every
branch emits it. backendLatency/planningLatency remain supported for historical
T-015 logs only; no backend collector/runtime is required for direct Android.

Neither speechEndToFinalLatency nor speechEndToActionStartLatency proves
acoustic endpoint latency: the service may wait before its callback. For the
SRS end-of-speech→action p95 <2 s objective, retain an independently observed
acoustic endpoint when available; otherwise label the result callback-only.
Report missing acoustic references. Chat has no action dispatch: measure first
text/audible answer. DISPATCHED is delivery only, not playback/window success.

## Reproducible host and Windows AVD checks

From the canonical repository:

```bash
python3 -m unittest discover -s scripts/perf -p 'test_command_latency_summary.py' -v
bash scripts/android/select-test-target.sh dex --emulator-only
```

Use the exact AVD serial from the selector and Windows ADB port 5038. Record
source/APK SHA256, signing certificate, AVD/API/build type and workload, with
synthetic/natural and cold/warm classification. Only matching-cert APKs may be
installed in place; preserve settings/data. Mocked-provider fixtures use isolated
preferences/credentials and never call a live provider. Runnable Android
benchmark commands and before/after results belong in T-021 evidence.

Capture filtered timing events without clearing unrelated owner logs:

```bash
/mnt/d/Android/Sdk/platform-tools/adb.exe -P 5038 -s <SERIAL> logcat -d -v brief DexLatency:I DexVoiceTiming:I '*:S' > /tmp/android-timing.log
python3 scripts/perf/summarize-command-latency.py /tmp/android-timing.log
python3 scripts/perf/summarize-command-latency.py --path direct /tmp/android-timing.log
python3 scripts/perf/summarize-command-latency.py --origin direct /tmp/android-timing.log
python3 scripts/perf/summarize-command-latency.py --origin chat /tmp/android-timing.log
```

`--path` selects requests with a matching terminal, excluding incomplete work.
`--origin` selects a consistent explicit original route, including admitted
incomplete requests. Filters can be combined. Correlated epoch events and
earlier request stages remain in the population. Unfiltered output retains
requests without origin/terminal. Use a bounded log window and one process
lifetime; do not mix baseline/candidate runs. Percentiles are median and
nearest-rank p95. Repeated execution segments and provider parse phases sum per request; other repeated
metrics count intervals. Missing counts include inapplicable stages and are
not automatically failures.

## Synthetic response benchmark comparison

The Android `ResponsePerformanceDeviceTest` emits `DexT021` records for three
blocks of 120 offline Command transport requests, 120 service-discovery trials
and 500 native warm wake frames. These are component costs, not user speech or
actual execution. Run the same test APK/workload for baseline and candidate;
retain APK/source identity and capture each instrumentation process separately.

```bash
python3 scripts/perf/summarize-response-benchmark.py /tmp/baseline-timing.log /tmp/candidate-timing.log > /tmp/avd-response-comparison.json
python3 -m unittest discover -s scripts/perf -p 'test_response_benchmark_summary.py' -v
```

The helper reports pooled/per-block median and nearest-rank p95, exact missing
or duplicate trials and whole-fixture allocation bytes. It labels a latency
signal IMPROVED only with clean complete samples, all three paired blocks
faster and pooled p95 gain above the sum of baseline/candidate block-p95
spreads. This is a conservative screening rule, not a confidence interval or
Fold3 acceptance. Missing/negative allocation stats stay null. Native decode
frames and nondecode frames have different costs; include both rather than
claiming conversion latency represents the recognizer as a whole.

## Resource measurement and deferred Fold3 acceptance

For sanitized Windows AVD snapshots outside timing windows:

```bash
python3 scripts/perf/snapshot-avd-resources.py --serial <SERIAL> --label baseline > /tmp/avd-baseline-memory.json
python3 -m unittest discover -s scripts/perf -p 'test_avd_resource_snapshot.py' -v
```

The collector verifies exact AVD identity/API35 before process queries; explicit
emulator/localhost serial and Windows port5038 are required. It reports numeric
App Summary PSS/RSS categories plus optional proc RSS/thread counts, collection
duration and missing values. Add `--package` for another known process; do not
sum external STT/TTS into app-only figures. A stopped process is marked missing,
not zero. This collector intentionally serves simulator preparation; Fold3 uses
the T-020 measurement protocol later.

Sample PSS with `adb -s <SERIAL> shell dumpsys meminfo <PACKAGE>` outside timing
windows. Separate app/DPC Java/native/graphics/code and external STT/TTS
processes where accessible. Frequent PSS/profiler collection can alter latency;
allocation/heap profiling runs separately. Record CPU/thermal, threads/FD and
equivalent idle cleanup states; no forced runtime GC. Keep raw heaps/screens/
private traces outside Git; publish sanitized counts.

T-020 runs matched baseline/candidate blocks on Fold3, ≥30 natural samples per
active route/candidate, ≥100 natural candidate sessions and eight-hour soak.
Separate cold/warm and local/direct Command/direct Chat populations; report all
errors, missing stages and no-response. Reuse only trials actually satisfying
parent gates. Preserve confirmation/display/privacy, mic handoff/rearm and
Vietnamese STT/TTS. AVD cannot prove Fold3 performance or acoustic accuracy.
