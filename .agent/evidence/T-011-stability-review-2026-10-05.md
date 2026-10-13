> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-011 stability and resource review — 2026-10-05

Scope: strengthen existing implementation in both repositories; preserve owner
history, credentials, signing configuration, grants, wireless ADB and unfinished
T-006/T-009/T-010 device acceptance. No commit/release/physical installation.

## Findings and implemented corrections

| Priority | Finding | Correction / regression evidence |
| --- | --- | --- |
| P1 | Fast action results could arrive before backend pending plan registration. Mixed dummy/remote plans waited for dummy results. | Register pending before publishing; wait only for device steps. Deterministic lifecycle tests and real WebSocket integration. |
| P1 | Terminal publication/admission and old worker cleanup could reject or release the next request. Rejected executor submission stranded admission. | Request ownership identity, release before terminal/error publication, idempotent owned cleanup and rejection recovery; immediate-next-command regressions. |
| P1 | Action results consumed command quota, preventing active plans from finishing. | Quota applies to command requests; result validation and UTF-8 frame limits remain. Regression with exhausted command quota. |
| P1 | Queued old WebSocket callbacks/reconnects could change a replacement connection or current command. | Shared lifecycle monitor, cancellable generation-checked reconnect, main callback delivery with socket identity checked at delivery. Two deterministic queued-callback regressions. |
| P1 | Old TTS completion/watchdog could remove the next prompt timer or audio focus. | Playback generation consumed once before cleanup; cancellation invalidates old callbacks. Two feedback guard regressions; ignored replies before successful prompt completion. |
| P1 | Remote actionId could impersonate Chrome's local command ID. | Separate trusted localCommandId supplied only by local resolution; remote IDs no longer select scoped local routing. |
| P2 | Disconnect left provider work/timers alive; absent results kept backend admission indefinitely. | Cancel planning/provider futures, stop provider fallback on interrupt, remove cancelled scheduled tasks; 120s device-result deadline. Network interruption remains best effort. |
| P2 | Expired conversation IDs accumulated; expiry reads could lose a concurrent append. | Short synchronized operations, bounded turns, opportunistic expired-ID sweep at most once/minute, concurrency/expiry regressions. |
| P2 | Foreground checks traversed Accessibility trees; match paths built Unicode maps unnecessarily. | Read active root package for foreground; lazily build offsets for matched slots; reuse normalization/regex/indexes; enforce traversal budgets before fetching children. Unicode regression and matcher benchmark. |
| P2 | Cancelled contact work stayed queued; idle contact worker remained alive. | Provider CancellationSignal, queue purge, exact-name filtering during scan, idle worker timeout30s. Recipient identity revalidation retained. |
| P2 | Plaintext endpoint exception accepted prefix-spoofed loopback hosts. | Parsed URI exact loopback host/no userinfo; regression for localhost.evil.example, 127.0.0.10 and userinfo spoofing. |

## Validation

Android Windows JDK21 / SDK35 command:

```bat
gradlew.bat --max-workers=1 test assembleDebug assembleDebugAndroidTest :app:lintDebug
```

PASS: 117 app debug + 117 release unit cases, zero failures/skips; debug app/DPC
and instrumentation APK assembly; app lint. Host matcher1000 patterns/300 samples:
debug p95=78,000ns (0.078ms), release56,400ns (0.0564ms).
Initial simultaneous build/AVD run exhausted Windows paging-file commit; aborted,
stopped only the disposable AVD, reran one-worker build successfully. Generated
crash logs from that attempt removed. Aggregate DPC lint is unchanged and was
not repeated; earlier stalled analysis remains explicitly incomplete.

A new marked disposable API35 Google APIs x86_64 AVD
`DeX_Stability_API35_20261005` on emulator-5560 used1920×1080/160dpi, 2cores/2GiB,
headless cold boot. Verify AVD identity, then install current app/test APKs only
on that serial. Existing emulator-5556 different-signature app/data untouched.

```text
adb -s emulator-5560 shell am instrument -r -w -e class com.khuongnd.dexassistant.ui.ConfigCommandDeviceTest com.khuongnd.dexassistant.ui.test/androidx.test.runner.AndroidJUnitRunner
```

PASS in2.700s: six executed runtime cases; one explicit selected-DeX fixture
assumption-skipped (runner prints OK7). Includes catalog/app-pair recognition,
strict JSON/reload, call/TTS cancellation and rejection without Samsung desktop.
Matcher1000 patterns/300 samples p95=3,235,469ns (3.235ms). This AVD differs from
the earlier p95=2.060ms environment; no controlled speedup comparison claimed.
No actual call, Wi-Fi, gesture, contacts lookup or permission provisioning.
New AVD stopped after validation to release resources; its marked data retained.
External logs: D:\Android\t011-stability-gradle.log and
D:\Android\t011-stability-instrumentation.log (not repository artifacts).

Backend Java25 `./mvnw verify` PASS57/57 cases, zero failures/errors/skips,
JAR packaging/repackage succeeds (3m57s). Ten lifecycle regressions cover fast
results, mixed dummy/device steps, command quota, pending expiry/disconnect,
executor rejection, UTF-8 byte limits and terminal/admission ordering including
same-message-ID retries. Real WebSocket, AI-enabled context, typed capability/
legacy compatibility, provider interruption/fallback and store races pass.
Log: `/tmp/t011-backend-stability-verify-owned.log` (external).
An earlier full run found scheduler/provider ExecutorService bean ambiguity;
explicit llmExecutor qualifier fixes it and the final full context suite passes.
Scoped whitespace checks pass; broad dirty-tree checks emit existing CRLF noise.

## Remaining acceptance and risks

- Fold3 Android15 natural speech, selected DeX windows/adapters, media transport,
  end-to-end latency, CPU/RAM/battery,100-session/soak and actual recipient safety
  need physical evidence. This review does not establish hardware acceptance.
- Confirmed contact revalidation queries Contacts provider synchronously on main
  immediately before dispatch. Slow provider can still delay UI; retain safety
  and measure on-device before changing the dispatch boundary.
- Conversation expiry sweep is O(n) under the store monitor at most once/minute;
  active IDs within TTL and active sessions have no global cardinality cap.
- Backend admission/dedup cache is per connection; no replay protection across
  reconnects. Android does not automatically replay ambiguous commands.
- Backend120s bounds device-result wait after publication, separately from
  provider planning; Android retains its own total command budget.
- Close/minimize/app-cycle/new-notification speech remain unfinished requested
  features; no unscoped or synthetic implementation was added during this review.
