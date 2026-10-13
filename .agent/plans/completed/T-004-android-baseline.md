> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-004 — Android technical baseline validation

- Owner: project agent / product owner for approvals
- Status: `DONE` (baseline documentation; downstream device acceptance remains open)
- Started: 2026-10-01
- Task: `.agent/TASKS.md` (`T-004`)

## Outcome and boundaries

Produce an evidence-backed Android baseline for the Galaxy Z Fold3 before any
Android scaffolding. Resolve the supported OS/API target, Wi-Fi and multi-window
capabilities, microphone foreground-service constraints, wake-word licensing,
Keystore token provisioning, and the device-side latency measurement protocol.

This plan covers first-party research, capability/security matrices, proposed
decisions, and requirement corrections after owner approval. It excludes Android
source beyond the approved T-003 slice, privileged/root-only implementation,
production EMM enrollment, production credentials, and claims that require an
unperformed device test. Development Device Owner enrollment is now in scope.

## Inputs and constraints

- Task and acceptance criteria: `.agent/TASKS.md` (`T-004`).
- Product scope and requirements: `docs/BRD.md` and `docs/SRS.md`.
- Existing architecture/security decisions: `.agent/DECISIONS.md`, especially
  D-004, D-006, and D-007.
- Preserve the single sideloaded-device scope, $0/month goal, WSS/token boundary,
  and p95 action-start target below two seconds unless the owner approves a
  conflict.
- Treat Fold3 firmware availability, vendor UI behavior, and performance as
  measurements until verified on the actual device.

## Decision questions and evidence

For each topic record first-party URL, publisher, access date, version scope,
documented fact versus inference, and any device test still required.

### 1. Fold3 OS/API target

- Question: which Android versions Samsung officially shipped/supports for the
  Fold3, and which compile/min/target SDK baseline should T-003 use?
- Sources: Samsung security/update material and Android SDK requirements.
- Acceptance: choose a decision-complete SDK matrix without claiming an update
  is installed until the device reports it.

### 2. Wi-Fi capability

- Question: what can an ordinary app, user-mediated flow, device owner, or
  privileged/system app do for toggle, add, and connect operations?
- Sources: Android `WifiManager`, network suggestion/specifier, Settings Panel,
  permissions, and device-policy documentation.
- Acceptance: matrix includes API scope, mandatory user interaction, and a
  supported failure/result contract; silent ordinary-app control is not assumed.

### 3. Split-screen and DeX

- Question: when can public multi-window/task APIs place arbitrary third-party
  apps, and which behavior remains best-effort or Samsung-specific?
- Sources: Android multi-window/task docs and Samsung DeX developer docs.
- Acceptance: matrix distinguishes guarantees, prerequisites, unsupported apps,
  and accessibility-assisted fallback.

### 4. Foreground service and microphone

- Question: which service types, permissions, start restrictions, and user
  affordances apply to persistent microphone use at the selected target?
- Sources: Android foreground-service, while-in-use permission, microphone, and
  background-execution docs.
- Acceptance: define a compliant user-started lifecycle and explicitly exclude
  prohibited background starts.

### 5. Wake word

- Question: do PocketSphinx or Porcupine meet current Android/ARM64, offline,
  maintenance, key/account, and $0 personal-use licensing constraints?
- Sources: official repositories, licenses, vendor docs, pricing, and terms.
- Acceptance: select a legally and technically supportable candidate or record
  the owner decision needed; implementation remains after the text-first slice.

### 6. Token provisioning and Keystore

- Question: how is the existing opaque bearer token imported, protected, used,
  rotated, and revoked without hardcoding it in the APK?
- Sources: Android Keystore, cryptography, backup, and secure-storage guidance.
- Acceptance: define an implementable bootstrap/lifecycle and its residual risk;
  no token or universal secret enters source or backup.

### 7. Device-side latency measurement

- Question: how will T-003 measure speech-end through action-start and attribute
  STT, network, backend/LLM, dispatch, and device execution?
- Sources: Android monotonic-clock and performance guidance plus the existing
  backend protocol/budget.
- Acceptance: specify clock, event markers, representative matrix, warm-up,
  sample count, p95 calculation, timeout, and evidence format. The target remains
  unverified until run on the Fold3.

## Milestones

1. **Evidence ledger complete** — all seven topics have current first-party
   evidence, version scope, facts/inferences, and residual device checks.
2. **Candidate baseline complete** — capability/security matrices and latency
   protocol satisfy every acceptance rule.
3. **Owner review complete** — product/security/platform conflicts are accepted,
   rejected, or explicitly block implementation.
4. **Repository synchronized** — accepted decisions and SRS updates are recorded;
   T-003 has a decision-complete input and T-004 is closed or accurately blocked.

## Validation

- Check all material conclusions against current first-party sources and record
  access date 2026-10-01.
- Trace each T-004 acceptance item to a matrix, lifecycle, or measurement rule.
- Verify proposed behavior uses public APIs for the chosen privilege class.
- Review task/status/decision/SRS consistency and confirm no Android source or
  credentials were added.
- Run repository documentation/link checks if available; otherwise use targeted
  searches and `git diff --check` and record their results.

## Risks and escalation

- The actual Fold3 firmware may differ from Samsung's published support range.
- Ordinary apps cannot silently perform every BRD system-control promise.
- OEM multi-window/DeX behavior may exceed but cannot replace public guarantees.
- Always-on microphone policy and wake-word license terms may change.
- Hardware/network measurements may miss the two-second tail-latency target.
- Owner approval is required before changing scope, cost, latency, security, or
  representing user-mediated/best-effort behavior as the product contract.

## Progress and findings

- [x] ExecPlan created and T-004 started.
- [x] Evidence ledger complete.
- [x] Capability/security matrices and latency protocol drafted.
- [x] Owner review complete.
- [x] Accepted decisions/SRS/task/status synchronized.
- [x] Closeout reconciliation completed 2026-10-05: baseline exit criteria are
  documented first-party evidence for all seven topics, capability/security
  matrices, owner-accepted choices, synchronized requirements and a defined
  measurement protocol. All are recorded below; this research task introduces
  no source code and requires documentation validation rather than a build.

### Closeout boundary and downstream owners

| Evidence or outcome | Owner / completion rule |
| --- | --- |
| API35/min30, Device Owner/DPC boundary, Keystore and best-effort DeX baseline | T-004: documented matrices and accepted review package below; complete |
| Physical Android15/API35 and Device Owner/assistant boot foundation | Historical T-006 Fold3 and recovery evidence; verified slices, not full voice acceptance |
| Zero-touch Wi-Fi on/off and complete text round trip/latency | T-003: remains IN_PROGRESS |
| Natural speech, microphone routes, wake accuracy, re-arm, resource/soak | T-006: remains IN_PROGRESS |
| Provider/offline/reconnect acceptance | T-009: remains IN_PROGRESS |
| Natural grid/target and 100-session acceptance | T-010: remains IN_PROGRESS |
| Scoped app adapters, app-pair placement and physical matcher p95 | T-011: remains IN_PROGRESS |

Evidence: `../../evidence/T-006-fold3-2026-10-01.md` and
`../../evidence/T-006-fold3-recovery-2026-10-02.md`. Later D-011/D-016/D-019
supersede lifecycle/wake details in the historical candidate baseline. Closing
T-004 accepts the research baseline only; it neither relaxes downstream gates
nor claims Wi-Fi/voice/DeX performance completion.
Validation: local references/required sections, task/status reconciliation and
`git -c core.whitespace=cr-at-eol diff --check` pass in the
2026-10-05 documentation cycle. No new external
research, app build or physical measurement performed.

The approved T-003 implementation has started; automated build evidence is in
its own ExecPlan. Sources below were accessed on 2026-10-01. Items still marked
“Proposed” are deferred beyond the accepted T-003 slice.

### Evidence ledger and candidate baseline

#### Fold3 OS/API target

- Samsung lists the Galaxy Z Fold3 among devices eligible for four generations
  of Android/One UI upgrades and five years of security updates:
  <https://news.samsung.com/global/samsung-sets-the-new-standard-with-four-generations-of-os-upgrades-to-ensure-the-most-up-to-date-and-more-secure-galaxy-experience>.
  Samsung's launch datasheet identifies Android 11, so Android 15 as the fourth
  upgrade is an inference, not proof of the firmware installed on this device:
  <https://image-us.samsung.com/SamsungUS/samsungbusiness/datasheets/Galaxy-Z-Fold3-5G-Datasheet-AT-T.pdf>.
- Android's current Android 16 setup guide specifies `compileSdk = 36` and
  `targetSdk = 36`:
  <https://developer.android.com/about/versions/16/setup-sdk>.
- **Accepted:** T-003 uses compile/target SDK 35 and min SDK 30 for the one-device
  Fold3 scope. Before scaffolding, record `adb shell getprop ro.build.version.sdk`
  and `ro.build.version.release`; test on the actual installed API (expected
  30–35). A device below API 30 is a blocker rather than a reason to silently
  widen support.

#### Wi-Fi capability matrix

Android 10 prevents apps targeting API 29+ from enabling/disabling Wi-Fi with
`WifiManager.setWifiEnabled()` and directs apps to a Settings Panel. It also
replaces direct configured-network mutation with suggestions/specifiers:
<https://developer.android.com/about/versions/10/privacy/changes> and
<https://developer.android.com/develop/connectivity/wifi/wifi-infrastructure>.
Apps targeting API 33+ need the runtime `NEARBY_WIFI_DEVICES` permission for
covered APIs; scanning can still require fine location:
<https://developer.android.com/develop/connectivity/wifi/wifi-permissions>.

| Privilege/flow | Toggle Wi-Fi | Join internet SSID | Product contract |
| --- | --- | --- | --- |
| Ordinary app, silent | Unsupported on target OS | Cannot directly mutate saved networks | Return `USER_ACTION_REQUIRED`/`UNSUPPORTED`, never fake success |
| User-mediated | Open Internet/Wi-Fi Settings Panel | `ACTION_WIFI_ADD_NETWORKS` on API 30+ requires system confirmation | Action completes only after observed connection/state change |
| Ordinary suggestion | Not applicable | Suggestion API; approval is per app and selection remains OS-controlled | `SUBMITTED`, then observe post-connection or timeout |
| Specifier | Not applicable | User-approved local/peer connection, not general internet provisioning | Exclude from `connectWifi` internet semantics |
| Device Owner DPC | `setWifiEnabled` and configured-network APIs have documented exemptions | Managed configuration possible after enrollment | Accepted for the dedicated Fold3; separate least-privilege DPC APK |

**Accepted:** `toggleWifi` is zero-touch only through a separately packaged
Device Owner DPC. The main client calls an explicit signature-protected service;
the DPC holds no backend credential and reports success only after observing the
requested state. `connectWifi` remains outside T-003 and passwords remain local.
The Fold3 clean-device enrollment and OEM behavior still require device proof.

#### Split-screen and DeX capability matrix

Android documents `FLAG_ACTIVITY_LAUNCH_ADJACENT` plus `NEW_TASK`, but explicitly
calls adjacent placement best-effort and not guaranteed. Android 12L/API 32+
improves full-screen-to-split behavior; OEMs may override it:
<https://developer.android.com/develop/adaptive-apps/guides/support-multi-window-mode>.
Samsung says DeX apps should support Android multi-window/resizing and runtime
configuration changes:
<https://developer.samsung.com/samsung-dex/modify-optimizing.html>.

| Scenario | Guarantee | T-003 behavior |
| --- | --- | --- |
| Launch one exported/deep-linkable activity | Public supported intent flow | Report resolve/start failure explicitly |
| Request second activity adjacent on API 32+ | Best effort; target app/task/OEM can refuse | Return `REQUESTED`; verify multi-window state where observable |
| Place two arbitrary third-party apps deterministically | Unsupported public contract | Never return guaranteed success |
| User enters split screen from Recents | Supported user-mediated flow when apps allow it | Return `USER_ACTION_REQUIRED` with prompt |
| Accessibility drives Samsung UI | Firmware/UI-fragile fallback | Deferred and separately tested; never baseline guarantee |

**Proposed:** keep `splitScreen` as a best-effort request with results
`REQUESTED`, `USER_ACTION_REQUIRED`, `UNSUPPORTED_APP`, or `FAILED`; remove the
SRS wording that equates the flag with deterministic placement.

#### Foreground service and microphone lifecycle

Android requires microphone foreground-service type for target API 30+, blocks
most background FGS starts from target API 31+, and for target API 34+ checks the
specific FGS permission and active while-in-use microphone permission at service
creation:
<https://developer.android.com/develop/background-work/services/fgs/changes> and
<https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start>.
If started while the Activity is visible, microphone capture may continue after
the app moves to background:
<https://developer.android.com/training/permissions/requesting>.

**Superseded by D-010 for T-006:** the accepted dedicated-device design uses a
Device Owner exception to request post-boot start of the main app's microphone
FGS through explicit signature-protected IPC. The service still declares the
required microphone type/permissions and posts an ongoing notification. This is
not considered verified until measured on the Fold3; if Samsung firmware blocks
it, T-006 remains open rather than falling back silently.

#### Wake-word candidates

| Candidate | Evidence | Conclusion |
| --- | --- | --- |
| PocketSphinx | Android wrapper is BSD-2-Clause and includes `arm64-v8a`, but still uses AGP 3.5.3/JCenter and version `5prealpha`; current PocketSphinx releases state Android support is gone | License fits $0, maintenance/support baseline does not; reject for T-003 baseline |
| Porcupine | Official Android SDK supports API 21+, on-device custom keywords, and requires a Picovoice account/AccessKey that its docs say must remain secret | Technically viable for a later spike, but licensing/quota and client-secret exposure need owner acceptance and a real-device test |

Primary sources:
<https://github.com/cmusphinx/pocketsphinx-android>,
<https://github.com/cmusphinx/pocketsphinx-android/blob/master/LICENSE>,
<https://github.com/cmusphinx/pocketsphinx/releases>, and
<https://picovoice.ai/docs/quick-start/porcupine-android/>.

**Proposed:** T-003 remains text-first. Do not embed a Picovoice AccessKey or
claim a zero-cost always-on wake word in its acceptance criteria. Add a later
Porcupine device/licensing spike; proceed only after confirming current plan
terms and deciding whether an app-distributed AccessKey is an acceptable vendor
credential risk.

#### Token provisioning and Keystore lifecycle

Android Keystore keeps key material non-exportable and can bind it to TEE or
StrongBox, but compromised code can still ask the device to use a key. StrongBox
is optional and Android advises it is unnecessary for most apps:
<https://developer.android.com/privacy-and-security/keystore>.
Android's backup guidance supports explicit exclusion of sensitive material:
<https://developer.android.com/privacy-and-security/risks/backup-best-practices>.

**Proposed bootstrap:** issue a unique backend bearer token with the existing
CLI; transfer it once over a local owner-controlled channel into an explicit
provisioning screen; generate a non-exportable AES-256-GCM key in
`AndroidKeyStore`; store only ciphertext, IV, and version in app-private storage;
exclude the credential file from cloud and device-to-device backup; erase UI and
clipboard state immediately; never log the token. On 401/revocation, delete the
ciphertext and require reprovisioning. Rotate by provisioning a new token,
confirming a WSS connection, then revoking the old digest server-side.

Keystore protects the wrapping key, not the bearer token while the app decrypts
and uses it. A rooted/compromised device or compromised app process can still
use/capture the credential. StrongBox is preferred only when available without
breaking the required AES mode; TEE fallback is allowed and recorded.

#### Device-side latency measurement protocol

Use `SystemClock.elapsedRealtimeNanos()`, Android's monotonic general-purpose
interval clock that includes deep sleep:
<https://developer.android.com/reference/android/os/SystemClock>.

- Define start as STT final-result callback (the measurable proxy for
  end-of-speech) and finish as the first native action invocation, before any
  user-mediated system panel. Keep wake-word/STT-finalization as separately
  measured lead-in because true acoustic end-of-speech needs audio annotation.
- Emit local structured markers with correlation UUID and monotonic nanoseconds:
  `stt.final`, `ws.send`, `ws.plan_received`, `action.start`, `action.result`.
  The backend already correlates its receive/plan/send stages; wall clocks are
  for diagnostics only and are not subtracted across devices.
- Test FakePlanner and live Gemini separately on Wi-Fi in Fold3 phone and DeX
  modes, cold and warm connection, screen on and service-backgrounded. Use at
  least 10 warm-ups plus 100 recorded commands per representative scenario.
- Sort the 100 durations and report nearest-rank p50/p95/p99 plus failures and
  timeouts; p95 is observation 95. Do not discard errors or reconnects. Pass
  when at least 99% produce an action/result and `stt.final → action.start` p95
  is below 2,000 ms. Also report `ws.send → plan_received` and
  `plan_received → action.start` to localize regressions.
- User-mediated Wi-Fi/split-screen flows measure action start as opening the
  system UI, not completion of the user's interaction. The product must label
  this distinction.

### Owner-review package

The product owner accepted this revised baseline:

1. compile/target SDK 35, min SDK 30, with actual Fold3 API verified by ADB;
2. Wi-Fi toggle is zero-touch through a separate Device Owner DPC; network
   provisioning remains later work;
3. split-screen is best-effort with explicit non-success outcomes;
4. T-003's visible-start microphone assumption is superseded for T-006 by D-010;
   post-boot Device Owner startup remains subject to Fold3 verification;
5. T-003 is text-first; T-006 pins offline sherpa-onnx rather than PocketSphinx
   or Porcupine;
6. bearer token is Keystore-wrapped, excluded from backup, and manually rotated;
7. latency uses the stated Fold3 protocol and remains a target until measured.

Decisions D-009 and D-010 record the accepted SDK, packaging, privilege, T-003
scope, and service-first T-006 lifecycle. The closeout boundary above assigns
remaining device execution/performance evidence to downstream tasks; this
baseline is complete without claiming those outcomes.

## Decision and requirement updates

- Add proposed or accepted sequential records to `.agent/DECISIONS.md` only when
  the evidence meets the acceptance rule; owner-controlled conflicts stay
  proposed pending approval.
- Update `docs/SRS.md` after owner approval where public platform behavior
  contradicts silent Wi-Fi, deterministic split-screen, or unrestricted
  always-on listening assumptions.
- When decision-complete, use this plan as the input for the T-003 Android
  implementation ExecPlan; do not scaffold T-003 earlier.
