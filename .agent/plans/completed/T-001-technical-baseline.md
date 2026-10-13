> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-001 — Technical baseline validation

- Owner: project agent / product owner for approvals
- Status: `COMPLETE`
- Started: 2026-09-30
- Task: `.agent/TASKS.md` (`T-001`)

## Outcome and boundaries

Produce an evidence-backed initial technical baseline for DeX Assistant before
any application scaffolding. The baseline must resolve Spring Boot/Java, LLM
provider and tool calling, Android Wi-Fi and split-screen feasibility, wake-word
licensing and operation, OCI hosting, transport authentication, and the
sub-two-second action-latency target.

This plan covers research, small disposable measurements or API probes if later
needed, decision records, and requirement corrections. It excludes backend and
Android project scaffolding, production infrastructure, account creation,
credential handling, and implementation of product features.

## Inputs and constraints

- Product scope: `docs/BRD.md`.
- Current requirements and assumptions: `docs/SRS.md`.
- Original delivery sequence: `docs/PLAN.md`.
- Stable constraints and known uncertainties: `.agent/CONTEXT.md`.
- Task acceptance: findings cite current primary documentation; every accepted
  choice is recorded in `.agent/DECISIONS.md`; BRD/SRS conflicts require owner
  approval; no source code is scaffolded first.
- Preserve the single-user scope, encrypted and authenticated transport, desired
  action latency below two seconds after speech ends, and zero-monthly-cost goal
  unless the product owner explicitly approves a change.

## Decision questions and evidence plan

For every topic, capture the official page URL, publisher, relevant product or
version, access date, quoted/paraphrased constraint, and whether the conclusion
is a documented fact, measurement, or inference.

### 1. Spring Boot and Java baseline

- Questions: Is Spring Boot 4 suitable for the target deployment, which exact
  Java LTS/runtime versions are supported, and which Spring AI or LangChain4j
  release provides stable structured output/tool calling for the chosen LLM?
- Primary sources: Spring Boot system requirements and support policy; Spring AI
  reference/API compatibility documentation; LangChain4j official docs and
  release notes; OpenJDK/Oracle Java release and support material.
- Acceptance rule: select pinned, mutually compatible versions with an available
  ARM64 runtime and a maintained integration path. Prefer an LTS Java release
  unless a non-LTS feature has a documented necessity.
- Evidence/output: compatibility table and a proposed decision covering JDK,
  Spring Boot, and one LLM integration library.

### 2. LLM provider and tool calling

- Questions: Which currently available Gemini model/API supports structured tool
  calls, what are its free-tier eligibility and rate/usage limits, where is data
  processed/retained, and can credentials and quotas be safely operated for one
  user? Compare an alternative only if Gemini fails a mandatory constraint.
- Primary sources: Google AI for Developers Gemini model, function-calling,
  structured-output, pricing, rate-limit, data-use, and API-key documentation;
  the official provider documentation for any fallback.
- Acceptance rule: the chosen model must be currently available, support the
  required typed tool-call exchange, fit a documented $0 usage envelope for the
  expected load, and have acceptable data/security terms. Treat limits as quotas,
  not latency guarantees.
- Evidence/output: model and API choice, quota/cost calculation with assumptions,
  payload/response constraints, data-handling note, and fallback condition.

### 3. Android Wi-Fi control

- Questions: On the Galaxy Z Fold3's supported Android/API levels, can a normal
  app toggle Wi-Fi or connect to a supplied SSID without privileged/device-owner
  status, and what user approval UI or suggestion lifecycle is mandatory?
- Primary sources: Android Developers documentation for `WifiManager`, Wi-Fi
  network suggestions/specifiers, permissions, behavior changes by Android
  version, and foreground-service restrictions; Samsung developer material only
  for documented device-specific behavior.
- Acceptance rule: describe only flows available to an ordinary distributable
  app under documented permissions. Reframe or remove silent toggle/connect
  promises that require system, device-owner, or manual-user privileges.
- Evidence/output: capability matrix by API level and deployment privilege, with
  the exact user interaction and permission requirements.

### 4. Android split-screen / Samsung DeX

- Questions: Can an ordinary app reliably launch two arbitrary third-party apps
  into split screen using public APIs, and how do multi-window, activity
  resizability, launch-adjacent flags, task state, and DeX affect the result?
- Primary sources: Android multi-window and activity/task documentation, Android
  compatibility behavior, and Samsung DeX/multi-window developer documentation.
- Acceptance rule: distinguish guaranteed public behavior from best-effort or
  accessibility-assisted behavior. The contract must expose unsupported apps and
  launch failures rather than promise deterministic placement.
- Evidence/output: supported scenarios, prerequisites, known exclusions, and a
  proposed tool result/error contract.

### 5. Wake word

- Questions: Are PocketSphinx or Porcupine technically maintained and legally
  usable for continuous on-device listening at $0 for this personal deployment;
  what licenses, account/key requirements, usage limits, offline guarantees, CPU
  architecture support, and microphone/foreground-service constraints apply?
- Primary sources: maintainers' official repositories/docs and license files;
  Picovoice licensing/pricing/terms and platform documentation; Android
  microphone foreground-service and background-execution documentation.
- Acceptance rule: choose only an on-device option whose current license permits
  the intended use and whose Android/ARM64 operation is documented. If no option
  satisfies $0 and always-on requirements, record the gap for owner approval.
- Evidence/output: candidate comparison, license conclusion, runtime constraints,
  and a later-device-test plan; wake word remains after the text-first slice.

### 6. OCI hosting

- Questions: Does OCI still offer the required Always Free ARM compute and
  associated boot volume/network resources to eligible accounts/regions, what
  capacity/reclamation/idling caveats apply, and can TLS plus the selected JVM
  fit within the allocation?
- Primary sources: Oracle Cloud Free Tier, Always Free resource, compute shape,
  idle-instance reclamation, capacity, networking, and certificate guidance.
- Acceptance rule: document an actually eligible configuration and its operational
  risks; do not equate a published allowance with regional capacity. Identify a
  no-cost fallback or request owner approval if reliable $0 hosting is not
  supportable.
- Evidence/output: resource budget, eligibility/capacity caveats, deployment and
  TLS assumptions, and fallback trigger.

### 7. Authentication and transport security

- Questions: What threat model applies to a public single-user WebSocket, how
  should the Android client authenticate without a hardcoded reusable secret,
  how are credentials provisioned/rotated/revoked, and how are replay, logging,
  rate limiting, and TLS termination handled?
- Primary sources: OWASP WebSocket Security Cheat Sheet, OWASP MASVS/MASTG and
  secrets guidance, Android Keystore documentation, Spring Security WebSocket
  and authentication documentation, and the selected token standard's normative
  specification where applicable.
- Acceptance rule: WSS is mandatory; no credential is committed or embedded as
  a universal static API key; authentication is bound to a provisioned client
  with expiry or rotation and server-side revocation; authorization and abuse
  controls cover handshake and messages.
- Evidence/output: concise threat model, credential lifecycle, handshake/message
  flow, storage rules, failure behavior, and proposed decision. Exact token
  format follows the threat model rather than preceding it.

### 8. End-to-end latency objective

- Questions: Is action start within two seconds after end-of-speech feasible at
  representative network and provider percentiles, and which segments dominate
  STT finalization, network, backend, LLM, dispatch, and Android execution?
- Primary sources: provider/API streaming and latency guidance, Android speech
  recognition documentation, chosen hosting/network documentation, plus measured
  prototypes later. Official docs establish capabilities; measurements establish
  the achievable service objective.
- Acceptance rule: define the measurement boundary and percentile, create a
  segment budget totaling at most 2,000 ms, and specify timeouts/instrumentation.
  Do not accept the objective as verified until an end-to-end representative
  measurement exists; label it a target meanwhile.
- Evidence/output: latency budget, measurement protocol, test matrix, sample-size
  and percentile rule, and escalation threshold if the budget cannot close.

## Milestones

1. **Evidence ledger complete** — all eight topics have dated first-party links,
   version scope, facts, uncertainties, and conflicts with current requirements.
2. **Candidate baseline complete** — compatibility/capability matrices, threat
   model, cost/resource budget, and latency budget satisfy the acceptance rules;
   unresolved alternatives and tradeoffs are explicit.
3. **Owner review complete** — every change to provider, cost, latency, platform,
   or security commitment is either approved or recorded as blocking; rejected
   choices remain traceable.
4. **Repository baseline synchronized** — accepted choices have sequential
   decision records, affected BRD/SRS statements are updated after approval, and
   T-001/STATUS accurately report validation and residual limitations.

## Validation

- Confirm every conclusion cites a current primary source with access date and
  applicable version/region/license scope.
- Check each of the eight sections has a conclusion that meets its acceptance
  rule or an explicit blocker/owner decision.
- Cross-check selected Java, framework, integration library, LLM API, ARM64
  runtime, Android API levels, and OCI resources for mutual compatibility.
- Trace every accepted choice to `.agent/DECISIONS.md` and every approved
  requirements conflict to `docs/BRD.md` or `docs/SRS.md`.
- Verify no application source/build scaffold or credential was added.
- Before completion, record the exact review/probe commands and outcomes here or
  in `.agent/STATUS.md`. Documentation-only validation is expected for this
  planning step; later feasibility claims may require disposable probes and
  representative device/network measurements.

## Risks and escalation

- Free-tier offers, quotas, model names, and OCI capacity may change or vary by
  region/account; preserve access dates and avoid guarantees beyond the source.
- Public Android APIs may not satisfy silent Wi-Fi or deterministic split-screen
  behavior; do not substitute undocumented APIs without a separate security and
  distribution decision.
- Always-on microphone behavior is constrained by Android policy, licensing, and
  vendor firmware even when power is unlimited.
- A static embedded token conflicts with the security acceptance rule.
- The latency goal may conflict with cloud LLM/STT tail latency and the $0 goal.
- Escalate to the product owner before relaxing $0/month, two-second latency,
  Gemini/provider choice, supported Android behavior, or authentication strength.

## Progress and findings

- [x] ExecPlan standard created.
- [x] Research questions, primary-source classes, and acceptance rules defined.
- [x] Official evidence collected for the backend-affecting baseline.
- [x] Candidate backend decisions drafted and cross-checked.
- [x] Product owner approved the proposed backend plan and tradeoffs.
- [x] Accepted decisions D-002 through D-006 and SRS synchronized.
- [x] T-001 backend baseline completion recorded.

Current finding: backend-affecting choices are accepted. Android Wi-Fi,
split-screen, and wake-word device validation remain later delivery concerns and
must be verified by T-004 before their respective implementation tasks.

## Decision and requirement updates

- Add one or more accepted `D-###` records to `.agent/DECISIONS.md`; group choices
  only when their compatibility must be preserved as a unit.
- Update `docs/SRS.md` where verified platform/provider/security behavior replaces
  current assumptions, and `docs/BRD.md` only for owner-approved product-scope or
  goal changes.
- T-001 closed after its backend-affecting decisions were accepted and
  synchronized. The unexecuted Android evidence work was explicitly extracted to
  T-004 so this completed plan does not imply those capabilities were verified.
