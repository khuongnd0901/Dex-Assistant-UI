> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-009 — Local-first commands and metered AI proxy

> 2026-10-10 owner architecture override: CANCELLED: original backend proxy/quota/monitoring outcome superseded; Android local/direct-provider verification transfers to T-018. See [T-018](T-018-direct-ai-final-fold3-acceptance.md) and [task board](../../TASKS.md). Earlier backend requirements/progress are historical and do not schedule new backend work.

- Owner: main agent; backend and Android agents own their repositories
- Status: `IN_PROGRESS`
- Started: 2026-10-02

## Outcome and boundaries

Make DeX Assistant useful without a running backend for commands Android can
classify and execute deterministically. Route only requests outside that local
capability to the backend. Put all backend LLM traffic through a metered proxy
that supports multiple free-tier providers and exposes truthful quota health to
Android. Keep a bounded local diagnostic history for optimization.

In scope: capability/routing policy, offline behavior, protocol additions,
Gemini/OpenAI adapters behind one proxy, configurable rate and daily budgets,
quota snapshots, monitoring UI, sanitized local request/outcome history, tests,
and migration of existing flows. Out of scope: embedding provider credentials
in Android, mobile-to-provider calls, paid-tier billing, centralized collection
of Android logs, or promising limits the provider cannot report reliably.

## Inputs and constraints

- Governing documents: `docs/BRD.md`, `docs/SRS.md`, `docs/PROTOCOL.md`, D-003
  through D-007, D-012, and D-013.
- Existing Device Owner, native-action, privacy, authentication, and typed-result
  boundaries remain in force.
- Provider free-tier limits are external and may vary by model, account, region,
  and date. They must be researched from current first-party documentation and
  represented as configuration, not hard-coded product truth.
- “Local command” means deterministically classified and executable on-device;
  it does not authorize heuristic execution of ambiguous natural language.
- Commands are spoken/recognized in Vietnamese. Initial local scope is app
  launch, Wi-Fi, Bluetooth, volume, brightness, media controls, and other
  deterministic API/DPC actions. Media phrases such as `nhạc lái xe` map to a
  YouTube search intent for titles related to the phrase, not a fixed playlist.
- Diagnostic history stores original recognized command text encrypted locally
  and remains user-clearable. It expires at the earlier configurable limit:
  seven days or 100 commands by default. It must not contain wake audio,
  bearer/API credentials, Wi-Fi passwords, or raw provider payloads.
- For each backend AI request, select randomly among providers considered
  available with remaining budget. If the selected call fails or quota is
  exhausted, try another eligible provider at most once. Counters and refill
  windows remain independent per provider; the UI exposes no manual reset.

## Decision questions and evidence

1. Specify the accepted Vietnamese local-command grammar and capability matrix.
   Include phrase normalization and deterministic YouTube search-intent mapping
   separately from requests requiring content reasoning. For every
   command, record required privilege, confirmation policy, observable success,
   and behavior when unavailable. Acceptance: identical input and capability
   state produce the same routing result and existing T-003 actions are covered.
2. Decide whether local routing needs a protocol revision or only Android-side
   events plus new read-only monitoring messages. Acceptance: request IDs,
   duplicate handling, reconnect behavior, error codes, and compatibility are
   explicit in `docs/PROTOCOL.md` and tested in both repositories.
3. Verify current Gemini and OpenAI free-tier/model limits from first-party
   documentation. Acceptance: URL/access date and request/token dimensions are
   recorded; unknown provider state is never shown as remaining quota.
4. Design proxy accounting, fallback, and concurrency semantics. Acceptance:
   atomic
   reservation/finalization, timeout/error charging policy, reset windows,
   persistence/restart behavior, random selection among eligible providers,
   eligible failure classes, one-attempt-per-provider loop prevention, and
   all-providers-exhausted responses are specified and tested. Decide whether
   persistence is required before implementation.
5. Define monitoring authorization and transport. Acceptance: Android sees only
   necessary provider aliases and aggregate usage, never provider credentials
   or other sensitive configuration; stale snapshots are visible as stale.
6. Define diagnostic history schema and privacy lifecycle. Acceptance: original
   recognized text and metadata are encrypted locally, clearable by the user,
   and removed at the earlier configured age/count limit. Validate defaults of
   seven days and 100 commands and exclusion of audio/secrets/provider payloads.

## Milestones

1. Baseline current Android command executors and backend provider adapters;
   produce the local capability matrix and protocol proposal.
2. Update shared protocol/contracts and add cross-repository contract fixtures.
3. Implement and test Android local routing, offline response, and fallback.
4. Implement and test the backend provider proxy, budgets, accounting, and
   quota snapshot endpoint/messages.
5. Implement and test Android quota monitoring and sanitized local history.
6. Run backend/Android automated suites and offline, online, exhausted-quota,
   reconnect, redaction, and Fold3 acceptance flows.

Milestones 3 and 4 may run in parallel only after milestone 2 is accepted.

## Agent work packages and handoffs

1. **Android inventory (Android agent):** takes D-012/D-013 and current Android
   executors as input; outputs a Vietnamese capability/routing matrix, privilege
   and observable-success rules, YouTube search-intent mapping, encrypted
   history proposal, and Android test cases. No independent protocol changes.
2. **Backend inventory (backend agent):** takes current planner/provider paths
   and D-012/D-013 as input; outputs current first-party provider-limit evidence,
   proxy/accounting/persistence/fallback design, quota snapshot proposal, and
   backend test cases. No Android edits or independent protocol changes.
3. **Contract gate (main agent):** takes both reports; outputs accepted canonical
   protocol messages/errors, capability matrix, shared fixtures, compatibility
   behavior, and two implementation-ready assignments.
4. **Parallel implementation:** Android outputs local routing, monitoring, and
   encrypted bounded history; backend outputs metered random provider selection,
   bounded fallback, durable refill accounting, and quota snapshots. Each agent
   commits only in its owned repository and reports validation evidence.
5. **Integration (main agent):** takes both verified implementations; outputs
   cross-repository automated/device evidence and synchronized task/status/
   protocol records. This is the only completion authority for T-009.

The self-contained next-session startup checklist and exact expected outputs are
in `.agent/NEXT_SESSION.md`.

## Validation

- Backend: `./mvnw verify`; targeted concurrency, reset-window, provider-error,
  redaction, and protocol compatibility tests.
- Android: `./gradlew test lint assembleDebug`; routing, offline fallback,
  stale quota, history retention/redaction, and UI tests.
- Integration: local commands execute with the backend stopped; complex
  commands use the backend when available; unavailable backend and exhausted
  quota yield stable typed failures and the required user-facing response.
- Device: repeat representative native actions and monitoring/history flows on
  the Fold3; record actual capabilities and latency.
- Security/privacy: inspect APK/config/log output and persisted records for
  provider keys, bearer tokens, passwords, audio, and raw provider payloads.

## Risks and escalation

- Natural-language local matching can execute the wrong action; ambiguous
  phrases must fall back or request confirmation rather than guess.
- Client and backend capability versions can drift; shared fixtures and explicit
  versioning are required before parallel implementation.
- Provider dashboards may expose delayed or incomplete usage. The UI must label
  locally estimated, provider-reported, stale, and unknown values distinctly.
- In-memory accounting can reset on restart and violate daily budgets. Escalate
  the persistence/cross-instance choice before claiming hard daily enforcement.
- Persisted command text can expose personal data. D-013 authorizes it only with
  encryption, bounded age/count retention, user deletion, and the exclusions
  above.

## Progress and findings

- [x] Change request captured in BRD/SRS/task memory.
- [x] Architectural direction recorded as D-012.
- [x] Owner choices for Vietnamese scope, retention, and fallback recorded as
  D-013.
- [x] Current implementation inventory and capability matrix completed.
- [x] Provider-limit evidence and proxy design accepted.
- [x] Shared contract updated and JSON fixtures syntax-checked.
- [ ] Backend implementation acceptance complete (core passes; gaps below).
- [ ] Android implementation acceptance complete (core passes; gaps below).
- [x] AVD/Docker integration subset completed; seven device tests and live Gemini planning.
- [ ] Fold3/end-to-end acceptance completed.

Wave 1 completed read-only in both clean repositories. Android currently
executes only app launch and DPC Wi-Fi; backend currently has one direct Gemini
planner and no proxy/counters/snapshot. First-party evidence accessed
2026-10-02 is recorded in `docs/AI_PROVIDER_LIMITS.md`; exact active limits are
account-specific. D-014 and the canonical protocol now lock the shared contract
for parallel source implementation. T-006 remains active and is not replaced.

Wave 3 Android commit `72d0ece` implements local routing/executors, quota UI,
and encrypted bounded history. JDK 17/API 35 `test lint assembleDebug` passed
150 tasks and 64 test executions; a final debug/lint/assemble rerun also passed.
Bluetooth remains truthfully unavailable on the current API 35 path; app/media/
YouTube intent execution is `DISPATCHED`, not synthetic verified success.

Backend now has the H2-backed atomic quota service, metered randomized proxy,
common Gemini/OpenAI tool-call validation, bounded failure classification, and
authenticated quota snapshots. Java 25 `./mvnw verify` passes 23 tests,
including concurrency/restart/reset/fallback and WebSocket monitoring. Runtime
model clients remain secret/environment configuration; simultaneous live
Gemini/OpenAI behavior is not claimed without both clients and account keys.

Historical Wave 4 status as of 2026-10-02: no dedicated credentials/device were
available and physical acceptance had not run. This blocker was superseded by
the 2026-10-04 direct Java JAR/Gemini Fold3 integration and quota/reconnect
continuation recorded below and in the linked evidence. Those slices passed, but
broader T-009 acceptance remains open; do not use the old ADB/key availability
statement or old repository commit IDs as current state.

## Decision and requirement updates

Historical owner follow-up: `docs/VI_VOICE_DRIVING_HANDOFF.md` and D-015 capture
a Vietnamese driving-command backlog. That proposal does not replace the
current task order in `.agent/STATUS.md` or T-009 acceptance below.

The AVD/Docker Wave 4 run is historical evidence for that integration subset:
`.agent/evidence/T-009-avd-docker-2026-10-02.md`. The later Java JAR/Gemini
Fold3 integration and quota/reconnect results are recorded in the 2026-10-04
evidence linked below and supersede its environment/credential availability
notes. Neither run completes T-009; remaining acceptance is tracked here and
in `.agent/TASKS.md`.

Update D-012 if the accepted privacy/persistence or provider-accounting model
changes. Update `docs/PROTOCOL.md`, BRD/SRS, T-003/T-006 dependencies, Android
handoff memory, and both repositories' validation records as milestones land.

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

## D-016 hybrid voice integration — 2026-10-02

Owner approved English offline Hey DeX wake and online-allowed vi-VN system
SpeechRecognizer. T-006 implements audio; T-009 retains whole-input routing,
local-only execution for supported grammar, backend for complex/multi-clause
non-sensitive final text, no replay and sensitive-history guards. This does
not add backend tools or credential transport. Existing open quota/device
acceptance remains unchanged.

Hybrid integration source passes Android build/unit/lint and headless AVD
local/atomic-multiclause/ordered-plan/dedup/no-replay checks. Live Gemini
headless acceptance failed AI_TIMEOUT10.891s; no real cloud action evidence
claimed and provider timeout not changed. See T-006 hybrid evidence; quota,
physical and runtime-provider acceptance remain open.

## Coordinated history latency slice — 2026-10-04

T-006 continuation measures synchronous record+read explaining~98% of controlled
Fold3 command-terminal wait (1846/1459/1634ms for72–74retained entries). D-025
selects process-wide FIFO history scheduling off coordinator terminal path;
redaction precedes enqueue, encryption/retention/read-time legacy scrub remain,
clear orders with writes, stale UI publication cannot override status, failures
are separately visible. Implementation/ordering/privacy/clear/late tests and
physical comparison pending. Existing provider/device acceptance stays open.
See [continuation evidence](../../evidence/T-006-history-latency-2026-10-04.md).

### Final measured history slice — 2026-10-04
D-025 implemented and measured:full64debug+64release units/lint/app/test PASS;
controlled final→terminal1852/1464/1639ms→40/41/38ms. Five isolated history,
four voice andautomatic cases pass. Combined19case18PASS/1late-window canvas
expiry;unchanged focused canvas +9screen rerun pass. Final live YouTube6.460s/
Settings6.480s pass. Preserve failure evidence;no19/19 or natural speech inference.
Matching-original installs preserve DPC/data/Accessibility;finalLISTENING.
See ../../evidence/T-006-history-latency-2026-10-04.md for exact artifact hashes,
commands and remaining acceptance. Task remains IN_PROGRESS.

### Latest backend test setup — 2026-10-04
Owner requests .env configuration and Gemini Free Tier check. Current official
pricing confirms existing model default gemini-3.8-flash Free Tier eligibility;
no account quota/tier or inference verified. Ignored.env scaffold0600 prepared
withblankkey/tokenhash placeholder. Owner adding key locally;FilesCtrl+H shows
dotfiles. Current host lacksDocker/JDK25/backendJAR. Next verifykey/model with
sanitized response metadata,then prepare appropriate runtime and run existing
authenticated backend/planning/quota smoke without committing secrets.

### Direct Java runtime — owner instruction2026-10-04
Use java-jar,notDocker. Key nowpresent;Models.get3.5Flash-LiteHTTP200/346ms.
Prepare verified user-localJava25,run bash ./mvnw verify,launch loopbackJAR with
.env parsed without shell evaluation and isolated quota/publictest token process
overrides. Run readiness/auth/quota/liveGemini planning and safe isolated Fold3
headless multi-action test. Existing Android test must isolate history/context
and avoid provisioning/fixed owner credential alias deletion. Record actual
provider latency/errors,no inferred FreeTier billing or action success.
Evidence: ../../evidence/T-009-java-jar-backend-2026-10-04.md.

Initial direct calls returned HTTP 200, followed by 401 responses and an
isolated Fold3 test failure. These are historical results and are superseded
by the successful rerun below; retain them only to explain the adapter fix.
The wrapped 401/403 and 429 classification fix passed regression coverage.

Ownerupdatedkey2026-10-04: Models.get200;reloadedJavaJARready200UP;sameisolated
Fold3testPASS1/4.186s realGemini compound/headlessDISPATCHED,observer/ownerstate
preservation assertionsPASS. Previous401blockerresolved;negativeauth/quota
readbackPASS. No sourcechange ornewbuildneeded. BroaderT-009 remainsIN_PROGRESS.

### Handoff quota continuation — 2026-10-04
Live Java configured RPM exhaustion and reconnect snapshot continuity PASS;
6 focused quota/fallback tests PASS (6.483s). No new source changes or device
actions. Legacy physical local/history tests require owner-state isolation.
Details and remaining acceptance: ../../evidence/T-009-handoff-quota-2026-10-04.md.

### ADB simulator retest — 2026-10-06
On disposable `DeX_Command_Test_API35` API35, JDK17 unit/test APK build passed.
Local volume/Wi-Fi, offline reasoning and history terminal-independence checks
pass. Offline submit-to-SEND_FAILED state is306ms. The previous10s timeout
polled only the newest history entry for AI_UNAVAILABLE; controller finish writes
SEND_FAILED afterward. Main dashboard exposes the message only in Details, so
immediate visible feedback remains open. UiAutomation disabled Accessibility
during the history test; the user-enabled component/master were restored and
binding verified afterward. App history was restored byte-identically, Wi-Fi
remained enabled, and Device Owner remained. Monitoring/provider/Fold3 acceptance
is still open. See
`../../evidence/T-009-avd-adb-2026-10-06.md`.
