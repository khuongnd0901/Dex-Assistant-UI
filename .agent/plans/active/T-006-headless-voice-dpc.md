> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

> Scheduling override 2026-10-13: owner requests Windows simulator tests through WSL during continuation. All remaining physical Z Fold3 execution is consolidated in [T-020](T-020-consolidated-fold3-acceptance.md), one prepared campaign. This overrides older immediate/separate device-run instructions, preserves acceptance gates and does not imply PASS.

# T-006 headless voice and app-scoped DPC ExecPlan

> 2026-10-10 owner architecture override: IN_PROGRESS, Android scope retained; outstanding backend credential/reconnect/E2E gates CANCELLED. See [T-018](T-018-direct-ai-final-fold3-acceptance.md) and [task board](../../TASKS.md). Earlier backend requirements/progress are historical and do not schedule new backend work.

## Outcome and boundaries

Deliver a service-first Android client for the dedicated Fold3. Android binds it
as the provisioned default assistant after boot; the main app starts its own
microphone FGS, listens offline for “Hey Hey” (D-019), performs
STT, and sends final text through unchanged WebSocket protocol V1. Retain one
minimal administration Activity. This milestone reuses `LAUNCH_APP` and
`TOGGLE_WIFI`; broader system control, Accessibility, and split-screen remain
outside scope. The DPC never holds backend credentials or grants permissions to
other packages.

## Inputs and constraints

- T-003/T-004 and decisions D-003, D-004, D-007, D-009 govern the existing
  client, Device Owner boundary, credentials, and protocol.
- DPC and main app remain separate APKs signed by the same certificate; compile
  and target API 35, minimum API 30.
- Android platform restrictions must be reported truthfully. Device Owner may
  grant eligible runtime permissions but not signature/privileged or all
  special-access capabilities. MediaProjection always remains per-session.
- Physical Fold3 acceptance follows automated work and must not be inferred from
  emulator/build success.

## Decision questions and evidence

1. Classify each requested app permission from package metadata as `GRANTED`,
   `USER_ACTION_REQUIRED`, `UNSUPPORTED`, or `FAILED`; verify with unit tests and
   later PackageManager/AppOps/device evidence.
2. Provision the main app as `android.app.role.ASSISTANT`; start its microphone
   FGS from the system-bound `VoiceInteractionService` after readiness checks.
   Verify manifest/type/notification in tests and on five Fold3 cold boots.
3. Historical D-010 baseline (not Vietnamese feasibility): pin sherpa-onnx
   1.13.8 and the English int8 GigaSpeech 3.3M KWS model in the
   APK with no runtime model download or vendor credential. Verify dependency,
   packaged assets, checksums, and offline initialization.
4. D-016 selects system SpeechRecognizer vi-VN with online processing allowed,
   including sensitive command audio. No per-command consent; retain credential
   text guards from backend and history redaction. Measure real recognition.
5. Keep protocol V1 unchanged and send only final text. Verify no audio storage,
   upload, replay, or duplicate command on reconnect.

## Milestones

1. Record T-006 and accepted service-first/security decisions.
2. Add app-scoped DPC permission/capability IPC plus boot/update recovery.
3. Add voice foreground lifecycle, state machine, KWS/STT/TTS, notification
   actions, reconnect and command-in-flight policy, diagnostics, and tests.
4. Replace the daily chat workflow with the minimal administration surface and
   device-credential gate for sensitive configuration.
5. Run backend and Android automated validation and inspect APK signatures,
   permissions, assets, and secrets.
6. After T-003/T-004 device readiness, run the Fold3 reboot, screen/DeX/network,
   wake-word, soak, and 100-command acceptance matrix. Keep the task open if any
   firmware behavior is unverified or blocks microphone startup.

## Validation

- Backend: `./mvnw verify`.
- Android: `./gradlew test lint assembleDebug` with JDK 17/API 35.
- Unit coverage: permission classification/capabilities, voice states/timeouts,
  reconnect and one-in-flight behavior, and risk policy.
- Instrumentation/device coverage: notification actions, Keystore provisioning,
  health UI, invalid-signature IPC rejection, five cold boots, process/app
  update recovery, screen and DeX modes, network/backend loss, and no duplicate.
- Wake acceptance: 100 quiet calls >=95%, 100 moderate-noise calls >=90%, and
  eight-hour soak with at most one false activation.
- End-to-end: 100 commands, >=99% action/result completion, and p95 from
  `stt.final` to `action.start` below two seconds; report wake and STT latency
  separately.

## Risks and escalation

- Samsung firmware may bind the assistant yet reject its post-boot microphone
  FGS. If observed, keep T-006 `IN_PROGRESS` and record the blocker.
- Bundled model size, ABI support, and KWS integration can materially affect APK
  and reliability; do not replace the pinned runtime/model without owner review.
- Never move microphone or backend credentials into the DPC, claim privileged
  permissions, or broaden automatic grants to other packages.

## Progress and findings

- [x] Owner supplied scope, acceptance thresholds, and locked assumptions.
- [x] Task and ExecPlan created on 2026-10-01.
- [x] DPC permission/capability IPC and default-assistant lifecycle foundation.
- [x] Voice pipeline and administration UI implemented in subsequent slices:
  pinned offline Hey Hey KWS, system vi-VN STT, bounded status TTS, credential
  gating/Settings, masked RAM transcript, non-executing diagnostic and adaptive
  one-page dashboard. See `../../evidence/T-006-hybrid-voice-2026-10-02.md`
  and `../../evidence/T-006-voice-dashboard-2026-10-03.md`.
  This checkbox records implementation, not natural-speech acceptance.
- [ ] Natural Hey Hey accuracy (100 quiet/100 noisy), cue audibility, STT/wake
  timing and repeated re-arm reliability on the current APK.
- [ ] Current physical 100-command completion/latency/resource gates and
  eight-hour false-activation soak; Bluetooth wheel/SCO acceptance is separate
  in `T-006-hardware-voice-local-commands.md`.
- [x] Automated cross-repository validation for the current foundation.
- [x] Publish the verified app and DPC debug APKs as a GitHub prerelease for
  physical Fold3 acceptance without committing generated binaries.
- [x] Historical physical Fold3 slice: verified release artifacts installed without
  build/reset/source changes, Device Owner provisioned, and assistant/microphone
  foundation observed after reboot.
- [ ] Full Fold3 acceptance. Five full reboot cycles and process crash/rebind
  recovery pass; current natural-voice/backend-dependent matrix remains open.

Checklist reconciled 2026-10-05; Fold3 is currently unavailable. Historical
foundation results below are retained as dated evidence, not current blockers.
The later T-009 Java/Gemini evidence supersedes the old no-test-backend blocker;
T-011 stability fixes have host/AVD validation, not full T-006 device acceptance.

On 2026-10-01, backend `./mvnw verify` passed all 17 tests. Android
`./gradlew test lint assembleDebug` passed 150 tasks after a lifecycle correction
that keeps the internal state machine in `PAUSED` when model assets are absent.
Both APKs use certificate SHA-256
`9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`.
No placeholder KWS dependency/model was added: the verified sherpa-onnx 1.13.8
AAR and official GigaSpeech archive are not present locally, so the service does
not open the microphone and reports the blocker. T-006 remains `IN_PROGRESS`.

T-007 emulator evidence added two concrete blockers. On API 35, the DPC was
successfully provisioned as Device Owner and granted the app's runtime permission
profile, but a DPC-initiated start of the separate main app's microphone FGS was
denied (`mAllowStart...=DENIED`, `startForegroundCount=0`). After cold reboot,
`RecoveryReceiver` ran but the explicit main-app service resolved as `not found`
before and after unlock. The separate-package Device Owner exception therefore
cannot be treated as inherited by the app UID; boot/package visibility and the
service-first privilege boundary require redesign and physical Fold3 validation.

On 2026-10-01, D-011 implemented that redesign. The main APK declares a
qualified `VoiceInteractionService`, session service, and required recognition
service. Unit-tested readiness gates cover role, unlocked storage, permissions,
policy, and repeated `onReady()`. DPC recovery no longer starts/stops voice.
An idempotent ADB script provisions Device Owner and assistant role, verifies
binding and permissions, reboots, and verifies persistence without unsupported
fallbacks. On the API 35 AVD, cold reboot produced `READY`, `LISTENING`, a real
successful `AudioRecord` probe, a microphone FGS (`startForegroundCount=1`,
type `0x80`), and no background-start exception. This is not Fold3 proof.

On 2026-10-01, Android `test lint assembleDebug` passed 150 tasks at commit
`9a34c2e`. The resulting app and DPC debug APKs were published in GitHub
prerelease `fold3-device-test-2026-10-01` with their SHA-256 checksums and shared
signing-certificate fingerprint recorded in the release notes. Physical-device
acceptance remains open.

## Physical Fold3 continuation — 2026-10-01

Detailed, sanitized measured results and residual blockers are in
[the physical-device report](../../evidence/T-006-fold3-2026-10-01.md).
The target is an actual SM-F926B running Android 15/API 35 and One UI 7.0.
Both APKs were obtained with `gh release download` from the named prerelease;
their SHA-256 and extracted shared signing certificate match the release.
No Android build/checkout/source edit or factory reset occurred. The DPC became
Device Owner without reset. Runtime grants, role/system binding, microphone FGS
and real short AudioRecord probe, UI permission refresh, emergency stop and
explicit restart have physical-device evidence. Five full adb reboot cycles
passed with independently captured RUNNING_UNLOCKED, role/owner/grants, binding,
FGS and fresh probe health each time. App process crash/rebind and exact-APK
package replacement also recovered without opening the Activity. DPC main-app
package-update recovery failed: unchanged lastPolicyApply and explicit SKIPPED
PACKAGE_ADDED/PACKAGE_REPLACED delivery due to background execution policy.
Boot recovery is a separate PASS; source fixes remain diagnose-only on this host.
Owner-operated notification Pause/Resume/Stop passed with persisted health and
FGS checks. Final voice state is STOPPED after notification Stop; the assistant
role/system binding and Device Owner remain provisioned. Paused detail still
says “Opening microphone probe” and the administration health snapshot can be
stale; both are diagnosed without changes.
Hardware power-cycle tests remain unperformed.

The original script passed its before/after gates on a completed invocation via
an ignored device-specific ADB wrapper. The wrapper handles changing wireless
ports, non-streaming installs and exact-installed-APK hash caching. Independently
require `dumpsys user` RUNNING_UNLOCKED: Samsung does not implement
`cmd user is-user-unlocked`, and the script's grep accepts the word `unlocked`
in the resulting error. This and stale UI health after stop are diagnosed only.
At this 2026-10-01 checkpoint, the release lacked a complete KWS/STT/TTS,
credential gate, and Settings/diagnostics surface; no test backend was available,
so backend-dependent checks were blocked. Later Android implementation and
T-009 Java backend evidence supersede those availability statements, but not
the broader T-006 voice, physical, and soak acceptance. Historical automated
release results are retained.

## Provisioning/recovery repair slice — 2026-10-01

The owner requested cloning Android and continuing the next repair step after
pushing physical evidence (`c5ec0d4`). Android is now a sibling checkout at
`../Dex-Assistant-UI`; the prior diagnose-only/no-build instruction applied to
the prerelease-only validation phase. Source repairs and new-build validation
are now authorized. Factory reset remains unauthorized.

Repair acceptance:

- Unsupported Samsung unlock commands cannot pass; inspect only user 0's
  RUNNING_UNLOCKED state as fallback. Wireless transport changes retain the
  originally selected device identity, and proof of reboot requires a new boot ID.
- A system-bound assistant reconciles permissions through existing signature
  IPC before microphone readiness. Main-app replacement advances DPC policy
  health; do not rely on blocked cross-package manifest broadcasts or transfer
  microphone ownership to the DPC.
- Administration health follows preference changes while visible; pause detail
  is truthful and cancelled/duplicate probes cannot publish stale results.
- Regression tests, Android test/lint/assemble and matched-certificate physical
  update/reboot checks pass before claiming the repair accepted. A locally
  generated debug key must never replace the provisioned Fold3 packages.

Progress:

- [x] Android checkout and portable workspace paths established.
- [x] Source fixes and mock regressions prepared; SDK/JDK provisioned locally.
- [x] Local Android test/lint/assemble passes; APKs re-signed with recovered original key and verified. CI final signing repair is separate.
- [x] Physical Fold3 update/reboot/live Voice UI and notification verification; negative shell boundaries pass.

Only this foundation repair slice is in scope for the current continuation;
KWS/STT/TTS, credential gating and backend-dependent acceptance stay open.

On 2026-10-02 the owner authorized local key recovery and GitHub backup.
CMS-encrypted recovery completed through Actions run36897677667; encrypted
artifact was deleted, temporary recovery workflow removed, and original signing
certificate verified for both local APKs. An encrypted keystore backup is pushed
in Android `signing/`, passphrase stored separately in an Actions secret.
Both APKs installed over Wi-Fi and pre-reboot readiness passed. The script then
returned `reboot request failed` with transport offline; correct reboot transport
handling was corrected; the second physical run passes with changed boot ID
and Wi-Fi transport recovery. Main-app-only replacement advances DPC policy
health; probe/FGS and live Voice UI stop/start pass. Repaired notification pause/resume/stop and negative shell boundaries pass;
final microphone state STOPPED. Corrected signed CI run36898323611 passes.
See repair evidence report.

## Decision and requirement updates
- Owner2026-10-03 requests lower waits and authorizes autonomous implementation
  and hardware testing while unavailable. Measure KWS→STT ready, speech-end→final,
  dispatch→terminal, TTS requested/start/terminal and wake capture using sanitized
  monotonic epochs. Start STT without awaiting bitmap preparation, guard late
  preparation from recreating stale overlays, reuse worker-owned wake model with
  fresh streams to remove measured recurring load, bound short canned TTS wait.
  Known unsupported app-close requests fail locally without a provider timeout.
  Preserve final-only execution, manual suppression and sequential mic ownership.

- Add D-011 to supersede D-010's cross-UID start assumption with a
  default-assistant-owned microphone lifecycle.
- Add D-010 for service-first headless operation, app-scoped DPC permission
  management, offline wake word, and unchanged backend protocol.
- Supersede any earlier operational assumption that daily listening requires a
  manual Start action. Update Android handoff/status and SRS if its normative
  lifecycle or permission statements conflict with the implemented boundary.

## Vietnamese driving continuation — 2026-10-02

D-015 selects “a lô a lô”, offline Vietnamese first, and explicit voice consent
before online STT. Earlier English “Hey DeX”/GigaSpeech pin is historical and
must not be treated as a solution for this phrase. Engine replacement remains
a measured/license-checked decision gate, not an accepted dependency change.

Source and primary-document findings, ordered milestones and evidence gates:
[feasibility report](../../evidence/T-006-vi-driving-feasibility-2026-10-02.md).
First implement fail-closed credential routing/history protection; next measure
vi-VN on-device STT/TTS support and evaluate bundled Vietnamese assets. Real
wake/audio acceptance and physical Fold3 remain open. No wire change is needed
for the privacy guard; later Wi-Fi provisioning is local signature DPC IPC only.

Privacy first slice validated: Android final unit/lint/app+instrumentation build
passes (34 unit tests, 152 tasks), focused AVD runner OK (2 tests) for local
rejection and legacy encrypted-history sanitization. No real audio acceptance,
physical Fold3 measurement or engine replacement is claimed. See feasibility
report for exact commands/test names. Next milestone: device speech support
probe and license/runtime evidence for a Vietnamese bundled prototype.

## Speech capability gate — 2026-10-02

Read-only vi-VN instrumentation probe implemented and validated on the retained
API35 AVD: STT language is downloadable but not installed; offline TTS voices
report missing data. Android build/unit/lint/app+test assembly and runner pass.
Runtime AAR and Vietnamese 2025 int8 archive inspected/hashed; no production
dependency or engine replacement made. License provenance distinguishes 30M
CC-BY-NC-ND from the 2025 upstream Apache-2.0 declaration; converted package
notices/lineage and native loading still require validation.
See [speech gate evidence](../../evidence/T-006-vi-speech-capability-2026-10-02.md).
At that checkpoint the next step was isolated pinned runtime/model loading and
licensed fixture transcription. Later hybrid and physical work is recorded
below; natural-speech and physical acceptance still remain open.

## Accepted hybrid implementation — 2026-10-02 (D-016)

Owner approved the hybrid plan and requested implementation. This supersedes
Vietnamese wake/model-prototype as the immediate milestone. Reuse pinned sherpa
1.13.8 + English GigaSpeech 3.3M int8 2024-01-01 with Hey DeX and one CPU thread.

Milestones/acceptance:
1. Verify runtime/model provenance, checksums/ABIs and package generated assets
   at build time, no runtime download or generated binaries in Git.
2. Replace short probe with continuous KWS; release AudioRecord before STT,
   duck media with audio focus and cue only after ready callback. 30s STT bound;
   cancel/destroy and reject stale callbacks on pause/stop.
3. Shared application coordinator independent of Activity, one command in
   flight, correlated command terminals, sequential current V1 remote tools.
   Final text exactly once, multi-clause whole-input backend routing, no replay.
4. vi-VN TTS installed offline first/network fallback; canned truthful response,
   no credential readback. Health shows unavailable speech rather than success.
5. Android unit/lint/app+instrumentation assembly and AVD native/headless/routing
   tests. Actual Fold3 voice, screen-off/reboot and resource/soak gates remain
   required: wake >=95/100 quiet, >=90/100 moderate noise, <=1 false activation
   in8h; 100 commands and latency criteria above.

Resource evidence: compare Maps+music baseline against wake-enabled CPU/PSS,
pin/thermal state and 100-session release behavior before choosing resource
thresholds. Local debug APKs must not replace physical signing identity. No
reset, paid provider, new remote credential tool or broader app integration.

Progress: implementation started; canonical decision/SRS synchronized. Android
agent owns source/tests; main agent owns contract and integration acceptance.

Hybrid source/build milestone implemented:42unit tests pass, final full Android
build/lint/app+test assembly passes153tasks. AVD focused7 runtime/headless/protocol
checks and9 legacy local/history/privacy/inventory checks pass. Explicit vi-VN
recognizer ready callback arrives but no test speech supplied; no transcript
accuracy acceptance. Optional live Gemini dispatch acceptance fails AI_TIMEOUT
in10.891s;10s provider deadline unchanged and no replay. ABI narrowed to
arm64/x86_64; final APK/model/licenses verified. Full details:
[hybrid implementation evidence](../../evidence/T-006-hybrid-voice-2026-10-02.md).
Task stays IN_PROGRESS for real speech/TTS and physical resource/soak gates;
owner confirms no Fold3 in this session. Remaining T-006 credential gate/Settings
and broader diagnostics acceptance is not silently marked complete.

## DeX dock microphone correction — 2026-10-03 (D-018)
During T-010 physical testing, owner reports no HeyDeX response near Fold3.
Health LISTENING/active unsilenced16kHz capture is routed to USB dock input;
owner confirms dock/monitor has no mic. This is a T-006 voice prerequisite
correction, not completion of open voice/soak acceptance.

1. Add app-local Phone/System preference and actual routed-device health; keep
   existing System default. Verify phone AudioRecord preference after start.
2. Prototype API33 system SpeechRecognizer PCM-source support with benign known
   speech, without command dispatch. Only claim selected phone STT on proven
   provider support. No model/threshold/global audio setting change.
3. Implement bounded producer/PFD lifecycle, cancellation/backpressure/epochs
   and wake→STT→wake microphone handoff. Preserve existing online authorization
   and text/backend privacy; no raw audio storage or app-backend audio transport.
4. Run unit/lint/APK plus actual provider/phone mic/voice/error cleanup checks;
   continue real100spoken/wake/soak/resource gates unchanged.
Android agent owns implementation; main coordinates D-018, contracts and device
validation. T-010 hardening candidate builds pass, physical continuation pending.

## Owner wake replacement — 2026-10-03 (D-019)
Owner confirms Live Transcribe still receives speech and asks to replace
HeyDeX with HeyHey. D-018 microphone selector/PCM architecture is deferred;
its unused test prototype removed. Current source/health/asset use
▁HE Y ▁HE Y @HEY_HEY with original pinned runtime/model/threshold and system
mic/STT configuration. Full50unit/lint/APKs PASS153tasks/32s. Actual newphrase
wake/quiet/noise/soak and100spoken acceptance still required. Owner additionally
authorizes source commit/push; task remains IN_PROGRESS.


## Approved follow-up — voice handoff and single-page dashboard (2026-10-03)
Owner authorizes all five plan parts and GitHub commit/push in both repositories.
This is a correction under T-006 coordinated with T-010, not a new task.

### Evidence and acceptance
Fold3 audio focus log repeats app USAGE_ASSISTANT req3 then Google selected
recognizer com.google.android.tts req4; within ~35ms app abandons focus and
voice_health reports PAUSED/AUDIO_FOCUS_LOST. Wake reaches STT invocation;
this failure is not evidence of faulty hardware or incorrect pronunciation.
Existing Column already has verticalScroll; owner reports clipped unusable UI.
CommandRecognizer disables partials and callback discards them.

### Ordered milestones and outputs
1. Fix competing app/recognizer focus ownership, stale focus callbacks and
   pause reason/resume behavior. Output: HeyHey reaches live STT without PAUSED;
   genuine external capture/feedback interruptions still end safely.
2. Add transient bounded partial/final transcript and no-execution diagnostic
   listen path. Output: visible recognition text; partial/diagnostic never dispatch.
   Redact credentials and never persist/log transcript or raw audio; capture
   RMS/route/silenced metadata only in RAM with throttled UI updates.
3. Compact adaptive one-page voice dashboard with all primary controls visible,
   dialog configuration/history/quota and bounded text/details. Output: no main
   scroll or clipped controls across phone/DeX small windows/IME; buttons usable.
4. Unit/lint/app+test build, focused instrumentation and matching-signed Fold3
   installation preserve DPC/data/grants. Physical tests distinguish injected
   deterministic callbacks from actual speech/provider capture acceptance.
5. Review related staged+unstaged changes, diff/secret checks, commit and push
   both main branches without force; record validation and residual gaps.

### Validation and boundaries
JDK17 testDebugUnitTest lint assembleDebug assembleDebugAndroidTest;
focused pause/resume, focus-handoff, partial/final/diagnostic no-execution,
late callbacks and privacy tests; bounded layout tests and actual Fold3 UI.
Manual HeyHey + vi-VN command final text remains required, plus old T-006/T-010
soak/popups/YouTube acceptance. Keep tasks IN_PROGRESS until those are proven.
No V1/backend change, mic routing/model/threshold tuning without evidence,
no global audio setting or Accessibility grant/reset. Main owns canonical memory,
physical sign/install/integration; Android agent exclusively owns Android edits.

Progress: [x] source/read-only Fold3 diagnosis; [ ] five implementation outputs.


### D-021 continuous automatic startup follow-up (2026-10-03)
Owner explicitly asks always-on support and continuous listening without Start.
Extend existing five-part correction: shared Activity/defaultVoiceInteraction
startup/recovery gate checks unlock/mic/policy/manual suppression, backoff,
startup-inflight timeout. Background default-assistant recovery additionally
requires assistant role/reconciliation; foreground Activity startup does not.
Four-attempt bursts use five-minute cooldown, stable listening30s resets budget.
Existing default policy true; AUTO_START distinct from
user START/RESUME. Persist deliberatePause/Stop suppression; never auto-clear it.
Absent/ERROR/externalPAUSED may recover safely (no call/communication mode);
active STT/routing/speech must not be restarted. Keep direct boot mic-start
restrictions, defaultassistant path, DPC and Accessibility user-mediated grant.
No SecureSettings force enable or battery/global-policy mutation. UI truthfully
shows disabled/disconnected service and Settings entry. Tests preserve/restore
voice_policy; verify auto-open, unintentional service loss, explicitPause/Stop and
multiple completed local finals returning to actual native capture. Distinguish
synthetic final callbacks from real repeated positive KWS/100spoken acceptance.

Prior three-case Fold3 corrected renderer suite PASS3tests5.081s after fixing
STOP-test foreground misuse and preserved-process coroutine TestScope harness.
Owner confirms real HeyHey/transcript/UI fit; later repeat-wake report overlaps
our STOP tests. Readback record resumes unsilenced10:24:09 until testSTOP10:26:16,
so do not infer a YouTube runtime defect without an uninterrupted reproduction.
Six-case numbered follow-up fails5preparations: initial collectSnapshot swallowed
hardening refresh errors as display unavailable. Node exclusion/freshness guards
and sanitized counters are being validated; no weakening tap validation.

### Readiness cue follow-up (owner manual report)
Owner verifies two consecutive HeyHey activations; first opens YouTube and second
shows numbered overlay. No audible ready cue, so command timing is unclear.
Current code uses80ms STREAM_NOTIFICATION tone; audio readback notification/music
not muted and routesHDMI. This does not prove silence due to volume. Improve short
clear media readiness cue on actual STT-ready callback, once per turn, bounded/
stale-safe; no competing focus, global volume/routing or microphone changes.
Build/unit/lint plus realprovider duplicate/late-ready checks; owner audibility
on Fold3/DeX required. Direct tắtYouTube app-close executor is absent; numbered
X command remains supported workflow, no force-stop/BACK pretending closure.

### Wake re-arm delay after failure reply — OPEN (owner 2026-10-03)
Owner reports HeyHey is not detected promptly after spoken “Khó quá, không làm
được”. Do not guess or cache the KWS engine yet: source restarts wake after TTS
terminal and reloads `SherpaWakeEngine` and `AudioRecord` each cycle. Add
sanitized monotonic timing by epoch for feedback terminal, queue, model ready,
AudioRecord recording and first positive read; measure the physical delay before
optimizing. Never log transcript/audio. See `.agent/SESSION_HANDOFF.md`.

Continuation: first timing build installed matching-signed; full host build and
11 Fold3 regressions pass. Controlled final feedback terminal→first mic read is
726ms (model ready after582ms); this is not the owner's natural slow-wake repro.
Absolute monotonic timestamps and requested/terminal feedback duration are being
completed for physical measurement. Do not optimize model/routing from this one
controlled sample. ADB recovered after phone Wi-Fi reconnect; see
`../../evidence/T-006-T-010-handoff-continuation-2026-10-03.md`.

### 2026-10-04 resumable checkpoint (supersedes older next actions)
D-023/D-024 implemented and installed:59units/build/lint;9screen and corrected
5voice/recovery hardware passes; live YouTube coordinate dispatch passes. Settings
result unknown. Warm feedback-terminal→mic150–175ms,4s watchdog verified. Remaining
controlled command-terminal delay1.5–1.6s: measure synchronous encrypted-history
record/read before proposing FIFO background work; no history change accepted yet.
Task remains IN_PROGRESS. See ../../SESSION_HANDOFF.md and continuation evidence
for exact resume steps, artifacts, dirty trees and unverified acceptance.

### Resumed history-latency investigation — 2026-10-04

Owner resumed the checkpoint; existing T-006 coordinated with T-009 remains
active. Android owner first adds only monotonic record/read load/persist durations
and retained-entry counts. Main runs the controlled three-final Fold3 test on
matching-signed artifacts before accepting a scheduling change. No speech,
screenshot, ID, ciphertext or credential logging is permitted.

Acceptance for this slice: measurements isolate terminal-path history cost; any
worker change preserves pre-enqueue redaction, encryption/key/retention and
read-time legacy scrubbing, process-wide FIFO ordering across store instances,
ordered clear, tested drain/restoration, truthful failure visibility, and stale
history callbacks cannot overwrite newer command status. Run unit/lint/build,
physical history/privacy and voice/screen regression, then compare controlled
final-to-terminal latency. Natural speech/soak acceptance remains open.

Fold3 reconnects at SDK37 endpoint192.168.1.18:40581 with Accessibility enabled
and bound. Existing Settings live-grid test fails first RAM-only badge check
(1test/0.740s) before tap. Diagnose separately under T-010; no passing claim.

### Final measured history slice — 2026-10-04
D-025 implemented and measured:full64debug+64release units/lint/app/test PASS;
controlled final→terminal1852/1464/1639ms→40/41/38ms. Five isolated history,
four voice andautomatic cases pass. Combined19case18PASS/1late-window canvas
expiry;unchanged focused canvas +9screen rerun pass. Final live YouTube6.460s/
Settings6.480s pass. Preserve failure evidence;no19/19 or natural speech inference.
Matching-original installs preserve DPC/data/Accessibility;finalLISTENING.
See ../../evidence/T-006-history-latency-2026-10-04.md for exact artifact hashes,
commands and remaining acceptance. Task remains IN_PROGRESS.


## DeX STT subtitles (2026-10-05)

### Outcome and boundaries

Show live Vietnamese command recognition as bottom-centered subtitles over any
app on the selected Samsung DeX display. This is a T-006 extension, coordinated
with T-010 overlay/image-target safety; it does not replace other unfinished
acceptance. Source is implemented; acceptance and remaining physical limits are tracked below.

Owner selections (D-031): DeX only; preparation/listening/recognizing status;
retain final text for three seconds. No phone overlay or new STT provider.

### Inputs and constraints

- Existing D-020/SRS transient transcript and privacy rules apply. Reuse
  VoiceLiveState.safeTranscript; partials are presentation only, final dispatch
  remains exactly once, and no subtitle transcript/audio is logged or persisted.
- Android checkout on this host: /home/kunrg/Dex-Assistant-UI. Relevant source:
  voice/VoiceAssistantService.kt, voice/VoiceLiveState.kt and
  screen/DexAccessibilityService.kt under app/src/main/java/com/khuongnd/dexassistant/ui/.
- Existing STT requests EXTRA_PARTIAL_RESULTS and publishes partial/final text
  to the dashboard. The numbered overlay already has API34+ SurfaceControl and
  API30-33 non-focusable/non-touchable Accessibility window renderers.
- Preserve existing dirty work, signing, grants, owner data, microphone policy,
  provider, protocol V1 and backend. Subtitle failure must not stop commands.

### Decision questions and evidence

Owner-selected behavior is settled. Remaining implementation defaults: enabled
on DeX, white 24sp text on a translucent black rounded background; centered at
bottom, width at most 80% of the display, at most two transcript lines showing
its latest tail. Use display density/font scale; position above bottom system
insets with 24dp padding, or 72dp from the bottom if insets are unavailable.
Show a separate compact status line and bound layout to the available display.
No appearance settings or dashboard diagnostic overlay in this slice.

Android references inspected 2026-10-05:
- [RecognitionListener](https://developer.android.com/reference/android/speech/RecognitionListener#onPartialResults(android.os.Bundle)):
  partial callbacks may occur zero, once or multiple times, depending on provider.
- [Accessibility overlays](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#attachAccessibilityOverlayToDisplay(int,%20android.view.SurfaceControl)):
  API34+ permits display-attached overlays and explicit layer ordering.

Runtime acceptance requires partial text visible before final on the physical
Fold3. If the provider produces no partials, status/final presentation may pass
but realtime acceptance stays open; do not substitute a provider implicitly.

### Milestones

1. Add separate RAM-only subtitle state with turn identity, phase, sanitized
   text and final expiry. Emit events from guarded command-listener callbacks:
   preparing at command entry, listening only on onReadyForSpeech, latest
   non-empty partial replacing the complete previous hypothesis, recognizing
   on onEndOfSpeech while retaining text, final on onResults with a three-second
   monotonic deadline. Later partials can update text while recognizing.
2. Cover wake, hardware/button activation and selection/confirmation listening.
   Keep diagnostic listening in the dashboard only. A new command-listening
   turn replaces old content. Pause/Stop/cancel/STT failure/timeout clear it.
   Normal command completion, TTS and return to wake standby preserve final text
   until its independent deadline. Old events/timers cannot affect a newer turn.
3. Add an independently owned subtitle renderer in DexAccessibilityService,
   reusing the current renderer approach by API level. Resolve exactly one valid
   Samsung desktop, never fall back to the phone, reuse rendering resources and
   redraw only for changed presentation. Explicit layer ordering places the
   subtitle above numbers. Grid preparation/cleanup must not own subtitle state.
4. Suspend subtitles before both preparation and image-target revalidation
   screenshots; wait for compositor detachment before capture. Hold suspension
   across pending captures, including updates arriving during capture; on
   completion/failure/timeout restore only the latest still-valid state. Do not
   extend expiry or restore canceled/stale turns. Capture failures keep existing
   command safety guards. Display resize recalculates layout; display loss,
   ambiguity, Accessibility interrupt/unbind/destroy dispose resources and
   suppress that turn, with a fresh listening turn required to show again.
5. Validate host/AVD behavior, then matching-signed Fold3 DeX natural use and
   T-010 regressions. Update evidence and checklist independently; host success
   cannot close physical realtime acceptance.

### Validation

- Unit tests: phases, revised/empty/duplicate partials, long Vietnamese text,
  credential redaction, final deadline, stale callbacks/timers, new turns,
  cancel/Pause/Stop/error/timeout and normal re-arm retaining final subtitles.
- Instrumentation: no partial dispatch and final exactly once; coexisting grid
  and subtitles; grid cleanup/subdivision remains correct; focus/input passes
  through; image captures exclude subtitles; failed/timed-out captures and
  cancellation cannot restore stale overlays; display/service teardown cleans up.
- Android JDK17: ./gradlew test lint assembleDebug assembleDebugAndroidTest;
  record results, including any existing aggregate-lint limitation. Run focused
  safe device tests rather than broad owner-state-mutating legacy suites.
- Fold3: rediscover device/signature/display; use Maps/YouTube commands, long
  phrases, repeated wake and continuous grid/selection/confirmation turns.
  Verify partial-before-final, three-second retention, app interaction through
  overlay, timeout/error, resize and DeX disconnect. Capture callback/frame
  metadata only; injected callbacks establish lifecycle, not natural streaming.
- Documentation: check local links, required sections, task/plan/status parity
  and git -c core.whitespace=cr-at-eol diff --check.

### Risks and escalation

Provider partial cadence and STT endpoint delay are separate from overlay
rendering. This slice does not promise an STT latency reduction. If physical
partials are absent, record the limitation and seek a separate provider decision.
Premature grid cleanup, overlay screenshot contamination and timer races are
required regression gates. Preserve selected-display and tap freshness guards.

### Progress and findings

- [x] Inspect existing partial/final callbacks, sanitization and DeX renderers.
- [x] Save owner selections, implementation defaults and acceptance checklist.
- [x] Implement subtitle state/events and independent renderer.
- [x] Implement screenshot suspension and lifecycle/display cleanup.
- [x] Pass automated validation and record host/physical fixture evidence.
- [x] Record repaired Fold3 repeat-turn natural realtime and grid confirmation.
- [ ] Complete physical resize/disconnect/selection/confirmation evidence.

State: IN_PROGRESS; source implemented and matching-signed APK installed.
Host124 tests/variant, full lint and APKs PASS. Physical4 subtitle cases PASS;
compact grid separate PASS5.135s. Initial natural first-turn partial led final by
4542ms but repeat turns failed: no-overlay Choreographer barrier stalled in
background. Fixed bypass for absent overlay plus hide-before-detach cleanup
validated by background/production capture regressions. Owner confirms three
repeat turns show subtitles/final three seconds and stale number cleanup; core
UX acceptance PASS. Final disposal RAM cleanup host/physical4-case PASS; natural
owner evidence belongs preceding APK (source delta only drawing closure release).
Physical resize/disconnect/interaction gates remain open. See [acceptance evidence](../../evidence/T-006-dex-stt-subtitles-2026-10-05.md).
Broader T-006 voice/resource/soak criteria remain IN_PROGRESS.

### Decision and requirement updates

D-031 records the three owner selections and extends D-020. T-006 tracks this
slice with verified implementation and outstanding physical milestones. T-010
supplies overlay/grid regression gates. SRS and Android operational guide/status
now distinguish owner-accepted core streaming from remaining physical limits. Keep canonical STATUS/TASKS aligned, attach
validation evidence, and do not mark T-006 DONE from this slice alone.
