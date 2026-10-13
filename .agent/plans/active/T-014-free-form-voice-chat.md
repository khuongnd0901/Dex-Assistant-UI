> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

> Scheduling override 2026-10-13: owner requests Windows simulator tests through WSL during continuation. All remaining physical Z Fold3 execution is consolidated in [T-020](T-020-consolidated-fold3-acceptance.md), one prepared campaign. This overrides older immediate/separate device-run instructions, preserves acceptance gates and does not imply PASS.

# T-014 — Free-form AI voice conversation sessions

> 2026-10-10 owner architecture override: IN_PROGRESS, Android Chat voice/TTS/privacy retained; outstanding server Chat/provider/resource acceptance CANCELLED and direct-provider integration transfers to T-018. See [T-018](T-018-direct-ai-final-fold3-acceptance.md) and [task board](../../TASKS.md). Earlier backend requirements/progress are historical and do not schedule new backend work.

## Outcome and boundaries

Add an Android voice-chat mode that supports multiple user/assistant turns in a
single bounded conversation, sends conversational replies to Android TTS, and
allows follow-up speech. Keep chat and device control as separate request paths:
chat responses cannot execute tools; supported device commands continue through
the current local-first router and guarded `DEVICE_COMMAND` path.

In scope: additive backend protocol, conversation/session management, bounded
context, free-form model responses, Android voice/TTS lifecycle, local time and
time-zone context, privacy and limits, compatibility and end-to-end acceptance.
Out of scope until separately approved: unrestricted AI tool execution, raw-audio
upload, unbounded/persistent transcript storage, and changing existing action
confirmation or capability policy.

## Inputs and constraints

- Canonical task: `.agent/TASKS.md` T-014; acceptance criteria govern completion.
- Current wire/action contract: `docs/PROTOCOL.md`, `docs/DEVICE_COMMANDS_V1.md`.
- Current local routing/privacy behavior: `docs/LOCAL_CAPABILITIES_V1.md`.
- Android repository: `/mnt/d/dex-assistant-ui`; voice service and WebSocket
  protocol are owned there, while backend protocol/planner/store are owned here.
- Android Command sends the fixed `conversationId` `primary`; backend Command
  retains bounded turns in an in-memory store and its planner requires tool
  calls. Chat has a separate session UUID and text-only backend path. Android
  does not yet speak Chat replies; TTS currently speaks fixed status strings.
- Preserve unfinished T-011/T-003/T-006/T-009/T-010 acceptance and pre-existing
  working-tree changes. Backend implementation was verified in T-014-04 and
  Android session/protocol implementation in T-014-05; voice/TTS wiring has not
  started.
- Owner accepted separate Chat and Command modes and session-only volatile
  context (D-030). On 2026-10-06 owner specified one user, at most one active
  Chat session, one conversation ID per session, existing voice-command flow
  with explicit start/end phrases, and no action dispatch from Chat. Owner accepted idle 15 minutes, disconnect/end and fresh reconnect, Stop/Pause
  termination, 200 turns/session and one in-flight turn. Owner also accepted
  20 pairs/8,000 prompt tokens, 2,000 input code points, and 1,024 output
  tokens or 4,000 output code points. Provider-policy review is out of scope
  per owner direction.

## Decision questions and evidence

1. **Session lifecycle:** session-only volatile memory with no long-term Chat
   history is accepted in D-030. Release context on end/reset/expiry and do not
   restore it after app/backend restart. Do not persist Chat text in logs or
   existing Command history. Define start, idle timeout, reconnect behavior,
   isolation per installation/user, turn/token caps, and eviction. Acceptance:
   policy reflected in privacy/operational docs before storage implementation;
   tests verify cleanup, restart behavior and absence of durable Chat content.
2. **Entry/routing UX:** separate Chat and Command modes are accepted in D-030.
   Accepted voice entry: “Hey hey” then “Bắt đầu trò chuyện”. Subsequent turns
   need no wake phrase; after TTS completes, reopen listening with an audible
   cue. “Kết thúc trò chuyện” works without the wake phrase, including during
   TTS: locally stop playback/pending work, close the session, release context
   and return to Command wake-word standby. Include an on-screen stop control;
   define its exact semantics and preserve direct mic-button entry.
   Chat text, including command-like text, cannot dispatch actions;
   unmatched commands cannot automatically enter Chat. Acceptance: deterministic
   route table and voice UX reviewed against mode switches, local commands,
   confirmations, cancel, and ambiguous requests.
3. **Wire contract:** additive chat message types and schema without overloading
   action plans. Acceptance: strict server/client fixtures, malformed/oversized
   rejection, and old V1 command compatibility tests pass.
4. **Conversation model:** role-based user/assistant history vs current request /
   result turns; backend must answer without requiring tool calls. Acceptance:
   multi-turn context test demonstrates pronoun/follow-up resolution, truncation,
   and conversation isolation.
5. **TTS lifecycle:** long response chunking, interruption, stop/cancel, playback
   completion, and follow-up listening. Acceptance: callback epochs prevent late
   response/TTS from speaking into a newer session; no overlapping playback or
   duplicate turn.
6. **Time-sensitive response:** source local time/time zone from Android and
   pass as explicit bounded context. Acceptance: deterministic request fixture
   has local timestamp and IANA zone; test story request after time question.
7. **Privacy/provider limits:** transcript retention, sensitive text redaction,
   output length, rate/token budget, provider failure and no-network UX.
   Acceptance: approved limits and data flow documented; logs/history exclude
   raw audio, secrets, and unapproved full transcripts.

## Implementation design for review — 2026-10-05

This section was the 2026-10-05 implementation proposal. D-030 owns accepted
product behavior; later owner decisions and the authoritative protocol replace
its provisional numerical values. T-014-04 backend implementation is now
verified; Android and host/AVD integration remain. Physical speech acceptance
still requires the real device.

### Routing and state transitions

| Mode/state | Input/event | Planned result |
| --- | --- | --- |
| Command standby | Hey hey | Existing command listening |
| Command listening | Whole utterance “Bắt đầu trò chuyện” | Cancel prior interaction; create Chat session; listening cue |
| Command listening | Other utterance | Existing guarded local/backend command route; no Chat fallback |
| Chat listening | Whole utterance “Kết thúc trò chuyện” | Local session end; stop work/TTS; clear context; Command standby |
| Chat listening | Ordinary final text, including command-like text | One Chat request; wait for assistant reply; no action dispatch |
| Chat speaking | TTS completes | Listening cue and wake-free next turn |
| Chat waiting/speaking | User says “Kết thúc trò chuyện” | Local end; invalidate callbacks; stop pending work/playback; clear context |
| Chat active | Dedicated “Kết thúc trò chuyện” button | Proposed: same terminal behavior as spoken exit |
| Chat active | Existing service Pause/Stop | Proposed: end Chat before existing pause/stop behavior |
| Chat active | Idle expiry or unrecoverable session/network loss | Proposed: end Chat truthfully; clear context; never replay a turn |

Match session-control phrases as whole utterances, never substrings in a story
or quoted sentence. Mode changes cancel pending command confirmations and
numbered overlays before Chat starts. Return to Command standby after exit
requires the next Hey hey for a command. A new Chat starts with fresh context;
proposed v1 reset UX is end then start again, with no separate reset phrase.

Keep a session generation plus request identity across WebSocket, TTS and STT
callbacks. Only the active generation may publish state, append context, speak
or reopen listening. Close invalidates it immediately, including when backend
cancellation cannot interrupt provider work. Delete local context immediately;
backend end-message failure relies on disconnect cleanup/expiry and must not be
presented as confirmed immediate remote deletion.

### Proposed wire and backend design

- Extend the existing authenticated V1 envelope additively with proposed types
  `chat.session.start`, `chat.session.started`, `chat.turn.request`,
  `chat.turn.response`, `chat.session.end`, `chat.session.ended`, and a Chat
  error response. Final names/codes become authoritative only in PROTOCOL and
  shared fixtures during milestone2. Reuse message/correlation identity; do
  not carry Chat output in `action.plan` or `DEVICE_COMMAND`.
- Android generates a random session UUID per new Chat. Backend binds it to the
  authenticated principal and active connection; never trust a client user ID.
  Same UUID under another principal/connection must not expose context. Current
  single-device authentication identity must be inventoried before defining
  any installation-binding guarantee.
- Proposed v1 disconnect rule: close the session and release its context,
  invalidate client work, reconnect normally, and require explicit Chat restart.
  No transparent session resurrection, automatic turn replay or transcript
  resend. Duplicate request IDs in a live session never cause a second provider
  call or TTS playback; define the bounded duplicate cache with fixture tests.
- At most one active turn per session. Commit a user/assistant pair only when
  the response completes successfully for an active session. Provider errors,
  cancelled/expired work and late completions cannot append partial pairs.
- Separate bounded Chat store from existing Command context; use role-based
  pairs and evict whole oldest pairs. No provider tools registered for Chat.
  Tool-call output is rejected rather than routed to Android actions.
- Reuse the existing metered proxy/admission boundary with no provider/model,
  quota, billing or timeout increase. Inventory whether the existing provider
  clients and quota accounting can handle text-only output before implementing
  the conversational adapter. No unmetered side channel.
- Send final recognized text only, plus an explicit timestamp/IANA zone when
  relevant. Use supplied time context for the time/story demo; no implied live
  web access or factual freshness. STT provider audio processing remains the
  existing Android policy, not a new backend raw-audio feature.

### Proposed configurable bounds and response policy

| Bound | Candidate default / rule | Status |
| --- | --- | --- |
| Session idle expiry | 15 minutes; refreshed on valid user activity, not synthetic callbacks | Accepted in T-014-02 |
| Context | 20 user/assistant pairs AND 8,000 prompt tokens, whichever binds first | Accepted in T-014-02; backend uses a conservative token bound |
| Session capacity | One active Chat session for the single user; each session has one conversation ID | Owner direction; enforce and verify in implementation |
| Input/output | 2,000 input / 4,000 output Unicode code points; 1,024 output tokens; also enforce 16 KiB UTF-8 frame limit | Accepted in T-014-02; backend enforces both code-point and conservative token bounds |
| Output generation | Complete response first; no streaming in v1 | Engineering proposal; verify provider-backed latency |
| TTS | Short default answer; a story may use the bounded longer output; split at sentences within runtime engine limits | Engineering proposal; no automatic second AI request |
| Failure | Do not speak partial provider output; give bounded local status and clear pending turn | Engineering proposal; define recoverable error vs session end in fixtures |

Token enforcement must use verified model/runtime token accounting or a
conservative documented bound; a character count alone is not a token cap.
Provider context/output must remain inside existing available budget. Limits
above record accepted limits where marked; they are not measured provider
capacity or latency promises.
Transcript logging/storage is disabled regardless of configurable numeric caps.

### Spoken interruption prototype and acceptance split

The accepted exit-during-TTS requirement needs an Android prototype before the
voice lifecycle is finalized. Inspect the actual AudioRecord/KWS/system-STT
ownership and focus rules; avoid two application recognizers competing for one
capture path. Prototype a local exit detector while TTS is playing, with echo
rejection and cancellation epochs. Do not silently substitute a wake phrase,
button-only exit, new speech provider/model, or a full voice/UI migration.
If the current pipeline cannot meet the requirement, record the measured
limitation and a bounded alternative for owner review before changing behavior.
Injected exit callbacks prove lifecycle correctness only. Real TTS loopback,
spoken exit, quiet/noisy rooms and selected USB/built-in/Bluetooth routes need
physical evidence. The button permits demo recovery but does not satisfy the
spoken interruption gate.

## Milestones

1. Record accepted session/privacy/entry/product choices in D-030 and update
   SRS/BRD where needed.
2. Define additive chat protocol, schemas, limits, and compatibility fixtures.
3. Implement backend conversational service, bounded context and model response
   path independent of tool calls, with tests for context and isolation.
4. Implement Android chat session ID/lifecycle and protocol parsing while
   keeping command/action routing isolated.
5. Connect assistant text to TTS and implement interruption, next-turn capture,
   cancellation, timeout and errors with lifecycle regressions.
6. Host/AVD integration: exercise a runnable three-turn time/story/follow-up
   demo through actual WebSocket Chat messages with deterministic provider/time
   fixtures and Android TTS. Verify command compatibility, no action dispatch,
   session cleanup, cancellation/reconnect and transcript non-persistence.
   Record APK/JAR/fixture identities and whether TTS is real or injected.
7. Provider-backed host/AVD demo under existing budgets: record first-reply
   latency, output size/duration and incremental backend/Android resource use;
   compare resource use to T-013 without treating its fake-planner latency as
   a real-provider baseline. Document account/provider policy and remaining
   unknowns. Mark unavailable credentials/quota explicitly, not as a pass.
8. Fold3 acceptance when available: natural wake/start, three wake-free turns,
   listening cues and spoken exit during listening/waiting/TTS; TTS echo must
   not close its own session. Test selected microphone routes, session cleanup,
   response timing/resources and Command regression separately. Keep task open
   until its required runnable voice demo and applicable gates are verified.

Each milestone updates its checkbox/evidence only after its completion rule
passes. Finish protocol centrally before backend/Android implementations; if
repository agents are later used, give non-overlapping ownership and shared
fixtures. T-014-01 is a read-only inventory; it does not start source
implementation.

## Validation

- Backend: Java 25 `./mvnw verify`; protocol fixture and focused conversation
  tests for session isolation, bounded context, no-tool replies and failure paths.
- Android: JDK 17 `./gradlew test lint assembleDebug assembleDebugAndroidTest`;
  focused voice tests for TTS, multiple turns, cancellation, interruption,
  timeout, reconnect, and no stale callback publication.
- Both repositories: protocol fixture parity and `git diff --check`.
- Retention: verify Chat content is excluded from durable history/logging,
  context is released on end/reset/expiry, and restarts cannot restore it.
- Voice UX: verify wake-plus-start entry, wake-free follow-up listening/cue,
  wake-free exit while listening and while TTS is playing, context cleanup and
  return to Command standby. Confirm exit is handled locally, app TTS cannot
  trigger it, and late responses cannot resume playback. Verify spoken
  interruption/echo handling on physical hardware separately from injected tests.
- Runnable acceptance: start a voice chat, ask current time, request a child-safe
  story appropriate to that local time, ask a follow-up about the story, and
  hear all assistant responses spoken; separately verify chat cannot dispatch
  actions and a supported device command still uses the guarded action route.
- Record provider-backed latency, response duration/size and resource use. Do
  not equate fake-planner or injected-TTS tests with physical acceptance.

## Risks and escalation

- A persistent transcript changes privacy, storage, account-isolation and
  operational commitments; do not choose persistence or retention silently.
- Long TTS can block follow-up, be interrupted by wake/STT, or outlive its
  command epoch. Keep explicit cancel/stop and generation-gated callbacks.
- Free-form prompts can resemble device commands. Chat must have no action tools;
  command dispatch remains separately validated on Android.
- Provider context/output increases cost and latency. Require owner approval
  before changing provider, budget, cost, latency or data-retention commitments.
- Current backend context is in-memory and may be lost on restart; never describe
  it as durable chat history.

## Progress and findings

- [x] 2026-10-06: T-014-01 backend/Android integration inventory completed;
  see [inventory evidence](../../evidence/T-014-01-inventory-2026-10-06.md)
  and the [execution tracker](../../tasks/T-014-free-form-voice-chat-tasks.md).
  Backend package baseline passes. Android build baseline currently fails at
  resource merge on this host; source implementation remains unstarted.
- [x] 2026-10-06: T-014-02 lifecycle/limits decision record updated in
  [evidence](../../evidence/T-014-02-policy-review-2026-10-06.md).
  Owner keeps personal scope, current Gemini-through-metered-proxy path and
  BRD $0/month target, and directed skipping provider-policy review. Owner also
  specified one user/session, one conversation ID per session, existing voice
  command flow with start/end and no action dispatch. Owner accepted all lifecycle/concurrency/context/input/output limits recorded
  in evidence. T-014-02 is DONE.
- [x] 2026-10-06: T-014-03 additive Chat V1 contract and shared examples added
  to `docs/PROTOCOL.md` and `docs/contract-fixtures/chat-session-schema-1.json`;
  SRS linked. JSON/declared structure check and CRLF-aware diff check PASS; see
  [protocol evidence](../../evidence/T-014-03-protocol-2026-10-06.md). No
  server/client decoder or runtime compatibility test was claimed.
- [x] 2026-10-06: T-014-04 backend WebSocket Chat dispatch, volatile bounded
  session/context and text-only metered Gemini path implemented. Java25
  `./mvnw -q verify` PASS 71 tests/22 suites, 0 failures/errors; JAR built.
  Backend-agent recheck with Java25.0.4.1 also PASS 71/22 with zero skips;
  JAR 118,663,424 bytes, CRLF-aware diff check PASS and no source/test edits.
  See [backend evidence](../../evidence/T-014-04-backend-chat-2026-10-06.md).
  At that point no Android client or provider-backed demo had run; the prior
  stop before T-014-05 was superseded by the current owner request.
- [x] 2026-10-06: split milestones into 20 execution/acceptance subtasks in
  [the status tracker](../../tasks/T-014-free-form-voice-chat-tasks.md).
  T-014-00 decomposition is DONE; T-014-01 through T-014-03 are DONE;
  T-014-04 is DONE; T-014-05 was completed in this work cycle; later subtasks
  remain PLANNED.
  Update subtask evidence there and keep this checklist/canonical memory aligned.

- [x] Capture request as T-014; backend implementation completed in T-014-04.
- [x] Resolve T-014-02 product/lifecycle/content-limit decisions and extend D-030; provider-policy review skipped per owner direction.
- [x] Owner accepted separate Chat and Command routing; recorded in D-030.
- [x] Owner accepted session-only volatile Chat context without long-term
  history; recorded in D-030.
- [x] Owner accepted wake-plus-start entry, wake-free follow-ups and exit,
  including spoken exit during TTS, plus an on-screen stop control (D-030).
  Android stop-control behavior and physical identity isolation remain
  open. Owner directed keeping Gemini via the existing metered API proxy and
  skipping provider-policy comparison; no compliance claim is made.
- [x] Planning refinement: route/state table, proposed session/wire/output
  design, interruption prototype, host/AVD milestones and physical gates added.
- [x] Finalize lifecycle/limits bundle in D-030. Provider-policy review is out
  of scope per owner direction; retain current Gemini-through-proxy path and
  $0/month target.
- [x] Milestone2: authoritative protocol/limits and compatibility fixtures
  documented; runtime compatibility remains pending implementation.
- [x] Milestone3: backend text-only Chat service and bounded isolated store;
  host unit/WebSocket/Command regression and Java25 build pass. Live-provider
  calibration and provider-backed demo remain milestone7.
- [x] Milestone4: T-014-05 Android session/protocol and Command isolation;
  final-source JDK17 app/DPC tests, lint and APK builds pass. See
  [Android evidence](../../evidence/T-014-05-android-chat-client-2026-10-06.md).
  Voice/TTS integration remains open in T-014-06; T-014-07 host fixture demo
  was subsequently authorized and completed.
- [ ] Milestone5 / T-014-06: partially wired whole-utterance entry/exit, isolated
  Chat turns, TTS replies and wake-free follow-up. See
  [T-014-06 evidence](../../evidence/T-014-06-android-voice-chat-2026-10-06.md).
  JDK17 Gradle validation later passed during T-014-07. Spoken exit during
  TTS remains unimplemented because capture ownership is sequential;
  echo-safe interruption prototype and verification remain required.
- [x] Milestone6 / T-014-07: deterministic host integration demo PASS with
  real WebSocket Chat V1, three context-linked fixture replies and injected
  Android TTS completion callbacks. Backend Java25 verify and Android JDK17
  test/lint/APK pass. See [evidence](../../evidence/T-014-07-host-chat-demo-2026-10-06.md).
  AVD, real TTS/mic, deterministic reconnect timing and physical voice flow
  remain unverified; these are not claimed by this host demo.
- [ ] Milestone7 / T-014-08: live WebSocket Chat start PASS, but two metered
  Gemini turns timed out at the configured 10-second model-call boundary.
  Owner-requested retest then passed two independent one-turn sessions in
  2.086 s and 1.135 s without timeout. After local RPM 2→10, one provider-backed
  three-turn time/story/follow-up session passed in 2.876/2.028/1.198 s;
  comparable Android/resource and account evidence remain open. See
  [T-014-08 evidence](../../evidence/T-014-08-provider-chat-2026-10-06.md).
- [ ] Milestone8 / T-014-09: on-screen Chat end control added in Android;
  physical acceptance remains open because this container has no Android SDK
  or `adb`. Spoken exit during TTS remains unsupported. See
  [T-014-09 evidence](../../evidence/T-014-09-fold3-voice-chat-2026-10-06.md).

## Decision and requirement updates

When decisions are accepted, record D-030 in `.agent/DECISIONS.md`; update
`docs/PROTOCOL.md`, `docs/SRS.md`, privacy/operational documentation, and Android
voice/chat documentation. Keep `.agent/TASKS.md` and `.agent/STATUS.md` aligned.


## Windows simulator continuation — 2026-10-13

Continuation 2026-10-13: fixed idle clock before first turn, automatic15min session/context teardown and active HTTP cancellation/no cancellation fallback. Voice service automatic-expiry cleanup hook added. Host exact-boundary test and Windows AVD idle/cancel/restart fixtures PASS; full build/lint PASS. Spoken exit during TTS remains unsupported, requires design and T-020 physical proof. [Evidence](../../evidence/Android-simulator-continuation-2026-10-13.md).

## Priority continuation — 2026-10-13

Owner authorized all open Android tasks in priority order with per-task commit/
push and resumed Fold3 automation, then skipped speaking for now. T-014 remains
IN_PROGRESS. T-014-06 now addresses bounded long-reply playback: Chat allows
4,000 Unicode code points, which may exceed the TTS engine's input-string limit
for supplementary characters. Use the runtime `getMaxSpeechInputLength()` limit,
sentence/word boundaries and intact surrogate pairs. Submit one chunk at a time;
only last completion resumes listening. One start metric per whole reply;
unique chunk/generation IDs suppress duplicate/late/cancelled callbacks.
No new persistent transcript/provider/model/mic ownership or timeout policy.

Primary reference accessed2026-10-13:
[Android TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech#getMaxSpeechInputLength()).
Acceptance: Unicode reconstruction/engine-limit, full-sequence completion,
cancel/same-public-ID replacement and late callbacks pass host tests; build/
lint/APKs pass; opt-in short offline Vietnamese chunk fixture provides actual
TTS support evidence. No fixture mic or network synthesis. The60s Chat watchdog
still bounds total playback; long spoken stories may timeout. Echo-safe exit
while TTS is speaking remains unsupported and cannot be accepted from these
playback-cursor tests. Natural F08 remains open.

Verified chunk checkpoint: full201-task checks PASS, app204tests/variant and
physical offline chunk/cancel fixture1/1 PASS. [Evidence](../../evidence/T-014-tts-chunks-2026-10-13.md).
Spoken echo-safe exit/natural F08 remain open; no whole-task closure.
