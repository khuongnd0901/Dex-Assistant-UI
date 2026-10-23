> Migrated to Android on 2026-10-13 ([D-037](DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Decision log

Record only choices that future work must preserve. Use the next `D-###` ID and
include date, status, context, decision, rationale, and consequences. Supersede
old entries instead of silently rewriting them.

## D-027 — Typed config catalog and shared local/remote voice policy

- Date: 2026-10-05
- Status: accepted architecture from owner-approved T-011; host/AVD validation recorded separately; physical acceptance open
- Context: phrases/aliases were coupled to Kotlin executors; backend tools could not express the selected local catalog. Owner approved JSON recognition and automatic AI fallback with voice-only selection/confirmation.
- Decision: immutable schema-1 JSON registry compiles whole-input patterns/typed slots at startup/reload, preserves original free-text slots, validates accent-folded collisions and retains last-good config. Executors receive typed actions, not raw phrases. Local and remote actions share Android ActionRouter. Optional V1 capability descriptor restricts provider tools to supported actions/configured app IDs; DEVICE_COMMAND params are strictly validated on both sides. Old descriptor-less clients retain legacy V1 behavior; deploy backend first.
- Rationale: synonyms/app aliases can change as data while Kotlin maintains execution and confirmation policy. Ambiguity and composites use backend planning without executing a prefix.
- Consequences: every call/semantic click requires a later whole-input spoken confirmation, selection pagination and recipient/target revalidation. Parent IDs persist through interactions; 10s reply starts after successful TTS, 30s steps, 120s total; consume before dispatch and cancel on error/new command/Pause/Stop. Contacts/place aliases/screens remain local. Settings is scoped by launch display; Back/Home/Notifications remain unavailable without proven scope. DISPATCHED is delivery only. This supersedes D-014 hard-coded grammar and D-013 whole-phrase YouTube query, retaining privacy/history/quota policies. AOSP desktop approximation cannot close Fold3 DeX/voice/performance acceptance.

## D-026 — Guarded hardware voice activation and strict absolute percentages

- Date: 2026-10-04
- Status: accepted within owner-requested implementation; automated Fold3 slice and owner physical mouse validated; wheel/SCO unverified
- Context: mouse one-press mic invokes Android's Bixby/Google chooser. DeX holds the assistant role but lacked a voice activity. Initial VOICE_COMMAND registration did not fix physical chooser; live trace identifies this mouse sends RecognizerIntent.ACTION_WEB_SEARCH. Assistant session was inert; local normalization discarded a trailing percent sign.
- Decision: VOICE_COMMAND/ASSIST and observed RecognizerIntent.ACTION_WEB_SEARCH activity, assistant session and external dedicated ASSIST/VOICE_ASSIST keys share one guarded command-listening entry. Require unlocked/ready/permitted/default-assistant state, honor explicit Pause/Stop and reject busy/repeated triggers. Queue STT after wake recorder stop and release. Preserve unrelated media/headset/mute keys and existing audio routing. Owner selects DeX in the system chooser; no silent preference changes.
- Rationale: the observed chooser establishes a supported system entry without guessing a generic key binding or changing microphone ownership policy. Reuse the existing STT rather than add a second audio pipeline.
- Consequences: direct Bluetooth wheel support is conditional on delivery of a supported invocation; actual wheel and car microphone/SCO remain unverified. During active STT only, a second supported mic-button press submits the latest nonblank partial; repeated presses after routing starts remain rejected. Strict existing-action grammar treats “tăng âm lượng 100%” as absolute SET100; supports 0..100 with percent/Vietnamese units, while relative percentages, malformed/out-of-range, negated/private/compound requests retain guarded routing. No action/protocol/provider migration or app force-stop.

## D-025 — Keep encrypted history off the command terminal path

- Date: 2026-10-04
- Status: accepted under existing owner authorization; measured latency slice validated2026-10-04
- Context: controlled Fold3 three-cycle baseline passes; dispatch→terminal1846/1459/1634ms. Record+read decrypt/re-encrypt72–74 entries synchronously; second cycle spends721+710ms in history, about98% of terminal wait.
- Decision: serialize history operations on one process-wide FIFO worker shared by store instances. Coordinator enqueues immutable redacted records and refreshes history asynchronously; command terminal and feedback do not wait for encryption/IO. Preserve retention, AndroidKeyStore/encrypted storage and read-time legacy sanitization. Order clear with writes, provide a drain barrier for verification/restoration, and reject stale UI snapshot callbacks. History errors are visible independently of command outcome.
- Rationale: measured encrypted storage work is diagnostic persistence and must not delay a completed local action or feedback. One queue protects cross-instance ordering and prevents older queued records reappearing after clear.
- Consequences: persistence is eventual; abrupt process kill may lose queued diagnostics. No action success implies history durability. Failure/clear/privacy/stale-publication and physical latency tests are required before acceptance. Natural spoken latency/soak and broader T-006/T-009 criteria remain open. No protocol/provider/microphone/key-policy change.

## D-024 — Remove avoidable voice-turn waits

- Date: 2026-10-03
- Status: selected under owner authorization to optimize autonomously; validation in progress
- Context: owner reports slow execution after HeyHey and5–10s re-arm after error feedback. Earlier three controlled cycles spent575–595ms reloading the same native model; a new cold boot measured3151ms to model ready. STT also waited up to1500ms for screen image preparation, and canned TTS had a25s watchdog.
- Decision: start the recognizer after KWS recorder release without waiting for screen image preparation; guard preparation against voice epoch cancellation and terminal commands. Keep one worker-owned native wake model for the service lifetime, replacing the stream between epochs so no decoder/audio history survives. Pause stops mic capture; service destruction releases the model on its worker. Bound canned spoken feedback at4s then cancel and re-arm, with stale callbacks unable to restart twice. Execute Google final STT results or a partial explicitly committed by the second hardware-mic press; ordinary partial callbacks remain display-only. Exact unsupported close requests for known app aliases fail locally.
- Rationale: eliminate measured model and preparation costs and a long feedback callback failure bound. Add sanitized monotonic stages for provider readiness, speech-end/final, dispatch/terminal and TTS queued/start/terminal; remaining provider latency must be measured rather than inferred.
- Consequences: no model/threshold/microphone-route/provider or protocol change. A slow canned TTS may be cut off at4s; Settings reports timeout truthfully. An explicit second hardware-mic press may submit the current nonblank partial through the final-text path; ordinary partial callbacks remain display-only. Validate native model reuse/release, real mic re-arm, cancellation/late callbacks, Pause/Stop, and fixture/live screen flows. Natural spoken latency/accuracy remains open while owner is away.

## D-023 — Explicit grid is a desktop coordinate touch

- Date: 2026-10-03
- Status: accepted by explicit owner instruction; implementation in progress
- Context: owner confirms GRID is visible, but selecting a cell fails with screen-changed feedback on live apps. Owner explicitly defines grid cells as a touch at the shown screen coordinates even if app content/windows change.
- Decision: a grid session freezes the coordinate region on one uniquely selected Samsung DeX display. Content, accessible labels, focus and app/window transitions do not invalidate GRID, subdivision or CLICK_CELL. Before dispatch require the same valid unique desktop ID and dimensions, active user-granted Accessibility, unexpired current session and exactly-once gesture gate. Display removal/resize/replacement, revoke, pause/stop/cancel and terminal STT errors still clear. Numbered CLICK_TARGET retains strict current node/window identity and fresh bitmap-patch revalidation.
- Rationale: grid numbers identify desktop coordinates, whereas target numbers identify particular controls. A coordinate touch must not depend on YouTube's changing node metadata.
- Consequences: supersedes D-017/D-022 window/content invalidation only for grid sessions. A cell tap can act on whichever app or popup currently occupies its coordinates; this is the user-requested behavior. Keep no phone fallback or gesture retry. Grid commands still require the normal final-text route; only the explicit second hardware-mic press can promote a captured partial to that route. Validate coordinate delivery after content/window change and live YouTube/another app; update SRS/capability contract.

## D-022 — Stable screen sessions and perceptible readiness cue

- Date: 2026-10-03
- Status: implementation validated on Fold3; owner audibility/post-YouTube visual check pending
- Context: owner reports numbered overlay disappearing during YouTube updates and cannot hear the 80ms notification beep after Hey Hey. TTS failure feedback remains audible.
- Decision: compare bounded current Accessibility node/window snapshot to the frozen numbered session on content/scroll/topology events; preserve session while identities, geometry and window mapping match. Window-state, target/display identity/geometry changes still invalidate, and screenshot target fingerprints are rechecked before every tap. On actual STT ready callback, play one bounded 360ms two-note media sonification cue without audio focus or global volume changes; expose RAM-only visual ready state and cancel audio on session teardown.
- Consequences: YouTube content refresh must not remove stable labels, while stale or occluded taps remain rejected. Verify natural Hey Hey over YouTube and cue audibility on Fold3; no direct app-close action is added.

## D-021 — Automatic voice startup with bounded recovery and explicit stop

- Date: 2026-10-03
- Status: accepted by explicit owner instruction; implementation in progress
- Context: Owner wants automatic continuous listening without pressing Start,
  stable enabled Accessibility, and autonomous verification. Owner is available
  again after initially allowing unattended tests for60minutes.
- Decision: Default autoStart/wakeWord policy stays enabled. Foreground Activity
  and existing system-bound default VoiceInteractionService share permission/
  unlock/desired-state gates and a bounded recovery/start-inflight guard. The
  background default-assistant path additionally requires assistant role and
  permission reconciliation; eligible foreground Activity startup does not.
  Four failed starts trigger a five-minute cooldown, then another bounded burst;
  thirty seconds of stable listening resets the budget.
  Recover absent/error and externally paused pipeline when safe; never restart
  active recognition/routing/feedback. Explicit Pause/emergency Stop persist
  suppression until user Start/Resume; automatic startup never clears it.
- Boundaries: Keep existing DPC/signing and user-mediated Accessibility grant.
  Preserve enabled service in instrumentation/no-restart; show real revoked or
  disconnected state with Settings recovery. Do not write secure settings or
  claim an app can override user/system permission revocation. No boot receiver
  directly starts a microphone FGS; retain VoiceInteractionService startup path.
- Consequences: Extend T-006/T-010 plans and tests; preserve/restore owner policy
  in tests and leave desired listening active after final validation. Physical
  kill/rebind/foreground startup and manual suppression must be measured.

## D-020 — Single-page voice dashboard and transient STT visibility

- Date: 2026-10-03
- Status: accepted; implementation in progress
- Context: Owner reports clipped UI and asks for one page without scrolling,
  visible recognized text, and repair of HeyHey-to-STT PAUSED behavior.
- Decision: Main view prioritizes voice state, capture level, transient partial/
  final transcript and start/resume/pause/stop/diagnostic controls. Adaptive
  phone/DeX layout; configuration/quota/history in dialogs. STT diagnostic
  never executes/sends a command. Partials never dispatch; text is held only
  in RAM and credentials are masked using existing privacy rules. No continuous
  transcript while waiting for wake; no competing app focus during STT.
- Consequences: Extend T-006 and coordinate T-010; V1/backend contract unchanged.
  Physical provider handoff and resized/IME layout need actual verification.

## D-019 — Replace the wake phrase with Hey Hey

- Date: 2026-10-03
- Status: accepted by explicit owner instruction
- Context: owner reports HeyDeX is hard to trigger/pronounce; Live Transcribe
  still receives speech on DeX. Observed USB routing alone does not establish
  unusable microphone audio.
- Decision: use English “Hey Hey” (hây hây), keyword tokens
  `▁HE Y ▁HE Y @HEY_HEY`, replacing HeyDeX. Retain pinned sherpa/model,
  default thresholds, system microphone routing and Vietnamese system STT with
  existing online authorization. Defer D-018 mic selector/PCM-source change.
- Rationale: implement the owner's requested easier phrase before attributing
  failure to input hardware or changing the microphone architecture.
- Consequences: update current source/health/setup/requirements and asset checks;
  preserve historical HeyDeX evidence. No V1/tool/privacy change. Actual HeyHey
  wake accuracy/false activation and100-session gates remain unverified.

## D-018 — App-local microphone selection for DeX docks

- Date: 2026-10-03
- Status: superseded by D-019; prototype not enabled or run
- Context: actual Fold3 wake capture is routed to a USB dock input, while the
  owner confirms the dock/monitor has no microphone. Listening health and
  unsilenced capture do not prove useful audio. T-006/T-010 need real voice.
- Decision: add local Phone/System microphone preference; preserve System for
  existing users. For Phone, request and verify the builtin AudioRecord route
  for wake. On API33+, prototype the same selected local PCM capture through
  system SpeechRecognizer EXTRA_AUDIO_SOURCE for Vietnamese STT; enable only
  with provider-support evidence, fail locally on unsupported input/route.
  Keep provider/online permission from D-016, pinned wake model/threshold,
  microphone handoff/epochs/timeouts and V1 unchanged. No forced global USB
  audio-routing setting or other-app permission change.
- Rationale: selecting the wake mic alone leaves system STT on the unusable
  USB input. Provider optional PCM support must be measured, not assumed.
- Consequences: Android owns bounded pipe/capture cleanup and a meaningful mic
  selector/route health. No audio or image goes to the application backend;
  system STT may process audio online under existing D-016 authorization.
  Validate provider source support, actual phone wake/STT, pause/stop/error and
  backpressure cleanup. T-006/T-009 acceptance stays open.

Owner subsequently confirms Live Transcribe receives speech and requests only
the wake phrase replacement. USB routing remains an observation; the proposed
mic selector/PCM architecture was not implemented, and its test-only prototype
was removed.

## D-017 — Local numbered Samsung DeX screen control

- Date: 2026-10-02
- Status: accepted by owner through implementation-plan request; device evidence pending
- Decision: User-enabled AccessibilityService selects Samsung desktop dynamically,
  reads nodes and bounded RAM-only screenshots, and shows non-intercepting number
  labels before wake readiness. Conservative contour/X candidates supplement nodes;
  6×4 grid is always available, with at most three subdivisions. No OCR/model or
  image upload. Session mapping is immutable and invalidated on screen changes.
- Routing: Vietnamese digit/number tap, grid, subdivision and cancel commands,
  including malformed/compound numbered requests, stay local. Missing permission,
  stale session or unavailable DeX fails locally. Gestures report DISPATCHED;
  popup closure requires independent state evidence. No retry after timeout.
- Consequences: T-010 coordinates T-006/T-009 without completing their remaining
  acceptance. V1/backend tools unchanged. Physical Fold3 DeX and 100-session
  latency/resource verification required before declaring this outcome DONE.

## D-016 — Hybrid English wake and Vietnamese system speech recognition

- Date: 2026-10-02
- Status: accepted; wake phrase superseded by D-019, other boundaries remain in progress
- Context: Owner chose simple commands executed locally and complex/multi-clause
  commands planned by backend, accepted online speech including sensitive audio,
  and explicitly requested implementation of the hybrid voice plan.
- Decision: Offline “Hey DeX” wake using sherpa-onnx 1.13.8 and English
  GigaSpeech KWS 3.3M int8 2024-01-01. After readiness cue, use Android
  SpeechRecognizer with vi-VN and online recognition allowed without per-command
  online consent. Only final text enters existing local/backend routing.
  Installed offline Vietnamese TTS is preferred; network voice may be used.
- Privacy boundary: The system STT provider may receive command audio containing
  credentials. This does not introduce credential actions or sending credentials
  to the application backend/LLM. Retain credential routing guards, full sensitive
  history/log redaction, no app-owned audio persistence, and no wake-audio upload.
- Consequences: Supersedes D-015 wake phrase, offline-first recognition and ban
  on online sensitive command audio. Network loss may prevent spoken local
  commands. Preserve Device Owner/assistant lifecycle, one command in flight,
  no replay and current V1 tool allowlist. No new paid STT/wake provider selected.
  Model/runtime license, native loading and physical speech/resource validation
  must be evidenced before calling the voice milestone complete.

## D-015 — Vietnamese hands-free driving product preferences

- Date: 2026-10-02
- Status: partially superseded by D-016 (wake/STT/sensitive-audio preferences)
- Context: Owner requested natural Vietnamese everyday commands without a
  screen/remote while driving and selected priorities in conversation.
- Decision: First prioritize driving foundations; wake phrase “a lô a lô”;
  offline speech recognition first and explicit voice consent before online
  fallback; target Maps, YouTube/YouTube Music and Zalo. Preserve local-only
  credential handling. The proposed command catalog is backlog, not a current
  support claim. See `docs/VI_VOICE_DRIVING_HANDOFF.md`.
- Rationale: Short voice interactions and local deterministic commands should
  remain useful without backend access while limiting exposure of speech.
- Consequences: Coordinate T-006 voice and T-009 local routing; verify engines,
  wake reliability and integration APIs before locking technical contracts.
  No authorization to upload credential audio or bypass system confirmations.
  Implementation boundary: Wi-Fi provisioning and credential-bearing utterances
  fail closed locally while provisioning is absent; this supersedes D-014’s
  BACKEND_REQUIRED CONNECT_WIFI row. No remote credential action is introduced.

## D-001 — Repository files are the durable project memory

- Date: 2026-09-30
- Status: superseded by D-011
- Context: Long chat sessions make project state expensive to retain and hard to
  resume reliably.
- Decision: Store stable context, task state, decisions, and the current handoff
  in `.agent/`. Start new sessions from `AGENTS.md` and `.agent/STATUS.md` rather
  than relying on conversation history.
- Rationale: Repository state is inspectable, versionable, and available to a
  fresh agent session.
- Consequences: Every meaningful work session must refresh `STATUS.md` and its
  active task before ending. Detailed chronological session logs are avoided.

## D-002 — Java backend baseline

- Date: 2026-09-30
- Status: accepted
- Context: The initial SRS proposed Spring Boot 4 and Java 25 without verifying
  framework, AI integration, or ARM64 compatibility.
- Decision: Build a Maven modular monolith on Java 25, Spring Boot 4.0.8, and
  Spring AI 2.0.1. Keep domain/application code independent from Spring adapters.
  Use Spring MVC and raw JSON WebSocket rather than STOMP or SockJS.
- Rationale: Spring Boot 4.0.8 supports Java 17 through 26, Spring AI 2.0.x
  supports Spring Boot 4.0.x, and Java 25 has a Linux AArch64 distribution. Raw
  WebSocket keeps the single-client protocol small and explicit.
- Consequences: Versions are pinned in the build. Framework, transport, and LLM
  integrations remain replaceable adapters around typed application ports.

## D-003 — Remote tool planning and protocol

- Date: 2026-09-30
- Status: accepted
- Context: Android actions cannot safely execute as ordinary server-side Spring
  AI callbacks, and untyped model JSON is not a trustworthy command contract.
- Decision: Use Spring AI's low-level `ChatModel` response and convert requested
  tool calls into validated, typed `ExecutionPlan` actions. Expose a versioned
  raw WebSocket envelope at `/ws/v1/assistant`; allow one in-flight command per
  session and correlate all results with UUIDs.
- Rationale: The application retains control of authorization, validation,
  remote dispatch, timeouts, and future tool-result loops.
- Consequences: Unknown tools/arguments fail closed. V1 implements a
  deterministic fake planner and a Gemini adapter; it does not execute device
  actions or scaffold Android code.

## D-004 — Single-device authentication

- Date: 2026-09-30
- Status: accepted
- Context: A reusable API key hardcoded in the Android application would be
  difficult to revoke and would expose the backend if extracted.
- Decision: Authenticate the WebSocket HTTP upgrade with a 256-bit opaque bearer
  token provisioned out of band. Store only its SHA-256 digest in the backend
  environment and compare digests in constant time. Production requires WSS;
  cleartext WebSocket is allowed only from loopback in the local profile.
- Rationale: A random high-entropy token is sufficient for the single-device
  scope while remaining easy to rotate and revoke without JWT infrastructure.
- Consequences: The backend includes a token-issuance CLI. Tokens and payloads
  must never be logged. Android will store its token in Android Keystore later.
  Forwarded HTTPS is trusted only from exact proxy IPs configured through
  `DEX_AUTH_TRUSTED_PROXY_ADDRESSES`; the default trust list is empty.

## D-005 — Gemini free-tier privacy boundary

- Date: 2026-09-30
- Status: accepted
- Context: Gemini free-tier content may be used to improve Google's products,
  while the product can receive commands containing credentials or personal
  data.
- Decision: Use the stable `gemini-3.8-flash` model behind a `Planner` port only
  when the `gemini` profile is selected. Apply a privacy gate before provider
  calls, never send or model Wi-Fi passwords, retain only six sanitized turns
  for 30 minutes, and never log prompts/responses. Use a deterministic fake
  planner locally; do not silently fall back to it after a production LLM error.
- Rationale: This preserves the approved zero-cost experiment while making its
  privacy limitation explicit and containing provider coupling.
- Consequences: Sensitive-looking input is rejected with a stable error. Paid or
  local inference remains a future replacement without changing the protocol.

## D-006 — V1 persistence, deployment, and latency boundary

- Date: 2026-09-30
- Status: accepted
- Context: Redis and OCI deployment are not necessary to prove the backend-first
  command slice, and the two-second end-to-end target cannot be proven without
  Android and a real network.
- Decision: Keep conversations and request deduplication in memory for V1; defer
  Redis and OCI provisioning. Produce a non-root multi-architecture container.
  Treat two seconds as a target, with backend overhead excluding LLM at p95 no
  greater than 100 ms and an LLM timeout of 1.2 seconds.
- Rationale: This minimizes early infrastructure while preserving measurable
  boundaries for the first end-to-end slice.
- Consequences: State is lost on restart. End-to-end latency remains unverified
  until T-003 supplies a representative Android/device measurement.

## D-007 — Retain the backend as the AI orchestration boundary

- Date: 2026-09-30
- Status: accepted
- Context: A mobile-only design was evaluated for the single sideloaded Galaxy Z
  Fold3. Calling an LLM directly from Android would reduce infrastructure, but
  would expose provider credentials and move model routing, workflow state,
  policy, retries, and diagnostics into the device application. Firebase AI
  Logic would replace the custom backend with a Google-managed proxy rather than
  eliminate the backend boundary.
- Decision: Continue developing the existing Java backend as the AI planning and
  orchestration service. Android owns wake word, STT/TTS, final device-side
  policy and confirmation, native APIs, and Accessibility execution. The backend
  owns provider credentials, prompt/model routing, typed plan validation,
  workflow/session state, retry and quota controls, and remote diagnostics.
  Simple deterministic commands may later use a local Android fast path, but
  mobile does not call Gemini or OpenAI directly in the baseline architecture.
- Rationale: This preserves provider flexibility and supports multi-step
  plan-act-observe workflows while keeping cloud credentials out of the APK.
  The already validated backend makes this lower risk than migrating to a new
  managed proxy.
- Consequences: T-003 must integrate with the authenticated WebSocket protocol.
  Device actions still require Android and remain subject to platform limits;
  the backend cannot grant additional Android privileges. OCI deployment and
  live Gemini validation remain deferred until the local vertical slice needs
  them.

## D-008 — Coordinate separate repositories through one main agent

- Date: 2026-10-01
- Status: accepted
- Context: Backend and Android source now live in separate Git repositories,
  while end-to-end features share authentication, protocol, and acceptance
  criteria. Fully independent agents would risk contract drift.
- Decision: Use one main agent to own end-to-end outcomes and canonical project
  memory. Use a backend agent for `/mnt/d/dex-assistant` and an Android agent for
  `/mnt/d/dex-assistant-ui` when work can proceed in parallel. Repository agents
  do not independently change shared contracts; the main agent coordinates and
  validates those changes across both repositories.
- Rationale: This retains independent builds and histories while preserving a
  single integration authority and enabling safe parallel implementation.
- Consequences: Sessions should open the checked-in multi-root workspace when
  both repositories are needed. Each agent commits only in its owned repository;
  canonical tasks, decisions, requirements, and protocol remain in the backend
  repository, with concise handoff pointers in the Android repository.

## D-009 — Dedicated-device Android execution boundary

- Date: 2026-10-01
- Status: accepted
- Context: The product owner retained zero-touch Wi-Fi for a single dedicated
  Fold3. Ordinary Android applications cannot toggle Wi-Fi on the supported OS,
  while Device Owner applications are explicitly exempt. Combining backend
  credentials and device-management privilege in one process would enlarge the
  impact of either compromise.
- Decision: Use compile/target API 35 and min API 30. Package the user-facing
  text/WebSocket client and Device Policy Controller as separate APKs signed by
  the same key. The DPC exposes only typed device actions through an explicit
  signature-permission-protected service and never receives backend credentials.
  T-003 proves `LAUNCH_APP` and Device Owner `TOGGLE_WIFI`; voice, Accessibility,
  split-screen, and Wi-Fi network provisioning remain later work.
- Rationale: Device Owner preserves zero-touch through a documented privilege;
  process/package separation minimizes the DPC interface and credential blast
  radius. API 35 matches the accepted sideloaded-device baseline.
- Consequences: The Fold3 must be clean-device provisioned and may require a
  factory reset. Wi-Fi success requires observing the requested device state.
  Losing the owner or signing identity can require reprovisioning. Target SDK
  upgrades are reviewed separately.

## D-010 — Service-first voice operation with app-scoped DPC permissions

- Date: 2026-10-01
- Status: accepted
- Context: The dedicated Fold3 must accept commands without opening the UI or
  pressing Start each day, while Android limits background microphone starts
  and distinguishes runtime permissions from special access and privileged
  permissions.
- Decision: The main app owns an always-available microphone foreground service,
  offline "Hey DeX" detection, Android STT/TTS, backend credentials, and a
  minimal administration UI. The Device Owner DPC manages only
  `com.khuongnd.dexassistant.ui`: it grants eligible requested runtime
  permissions, reports each result truthfully, and explicitly starts/stops the
  voice service after boot or package replacement through signature-protected
  typed IPC. Wake audio is not persisted or sent; only final text uses existing
  WebSocket protocol V1. Special access remains user-mediated and privileged or
  signature-only permissions are not claimed.
- Rationale: This provides headless daily operation without combining device
  policy privilege and cloud credentials, overclaiming Android authority, or
  changing the already validated backend contract.
- Consequences: A persistent notification and microphone privacy indicator are
  expected while listening. Sensitive administration requires device
  credential authentication. Firmware behavior, wake accuracy, soak reliability,
  and latency require physical Fold3 evidence before T-006 can be complete.

## D-011 — Default-assistant voice lifecycle across separate UIDs

- Date: 2026-10-01
- Status: accepted
- Context: API 35 evidence disproved D-010's assumption that the Device Owner
  background-start exemption crosses into the main app's separate UID. The
  two-APK isolation and no-touch cold-boot requirements remain mandatory.
- Decision: Provision the main app once as the `android.app.role.ASSISTANT`
  holder using supported ADB role APIs. Android binds its
  `VoiceInteractionService`; after role, unlocked-user, permission, and policy
  readiness checks, the main app starts its own microphone FGS. DPC recovery
  only reapplies permissions and records health. Do not use secure-settings
  writes, coordinate clicks, hidden APIs, root, or privileged/system packaging.
- Rationale: The system-bound assistant lifecycle supports background voice
  operation while credentials and audio remain isolated from Device Owner
  privilege. API 35 testing confirmed binding, self-started microphone FGS, and
  a real `AudioRecord` probe after cold reboot.
- Consequences: DeX Assistant replaces the existing assistant. Firmware must
  support shell role assignment during one-time USB provisioning or stop for an
  architecture decision. Fold3 evidence remains required, and large voice
  models wait until its microphone probe succeeds.

## D-012 — Local-first routing with a metered backend provider proxy

- Date: 2026-10-02
- Status: accepted
- Context: Core device commands should remain usable when the backend is absent,
  while requests outside deterministic Android capability still need LLM
  planning. Gemini/OpenAI free offerings impose changing request and token
  limits, and the owner needs mobile visibility into consumption and outcomes.
- Decision: Android first classifies commands against an explicit local
  capability set and executes deterministic supported actions locally. It sends
  only out-of-capability or ambiguous requests to the authenticated backend; if
  that backend is unavailable, it returns a typed unavailable result rendered
  as “Khó quá, không làm được”. Android never calls Gemini/OpenAI directly and
  provider credentials remain backend-only. All provider calls pass through a
  backend proxy that applies configurable per-provider budgets and publishes
  sanitized quota snapshots. Android may retain bounded, user-clearable local
  diagnostic records, but never audio, credentials, passwords, or raw provider
  payloads; raw command text remains disabled by default until an explicit
  retention/privacy lifecycle is approved.
- Rationale: The local fast path removes backend availability from basic device
  control while preserving credential isolation, complex planning, provider
  flexibility, and enforceable free-tier budgets.
- Consequences: This extends D-007's anticipated local fast path without moving
  AI orchestration into Android. T-009 must define a versioned routing/protocol
  contract, prevent ambiguous local execution, distinguish estimated from
  provider-reported quota, and validate both repositories before rollout.

## D-013 — Vietnamese local command scope, retained text, and provider fallback

- Date: 2026-10-02
- Status: accepted
- Context: T-009 needed owner choices for its initial local capability set,
  diagnostic retention, behavior after provider quota exhaustion, and whether
  estimated counters require manual correction.
- Decision: The first local-first grammar accepts Vietnamese commands for app
  launch, Wi-Fi, Bluetooth, volume, brightness, media controls, and other
  deterministic API/DPC actions. Vietnamese media phrases may map to app search
  intents: `nhạc lái xe` opens YouTube search for titles related to that phrase,
  rather than a fixed playlist. Requests requiring additional reasoning still
  fall back to the backend. Android stores the original recognized
  command text plus diagnostic metadata locally, encrypted at rest and
  user-clearable, retaining records until the earlier of a configurable age
  limit (default seven days) or count limit (default 100 commands). For each AI
  request, the backend randomly selects among providers currently considered
  available with remaining budget, then tries another eligible provider if the
  call fails or quota is exhausted. Each provider's counters refill only at
  that provider's configured limit window; no manual counter reset is exposed.
- Rationale: Vietnamese aliases make common routines fast and offline; bounded
  command text supports later optimization. Automatic fallback improves
  availability while provider-specific windows prevent one provider's reset
  semantics from affecting another.
- Consequences: This supersedes D-012 only where D-012 disabled raw command-text
  retention and prohibited silent provider switching. Random candidate
  selection, eligible failure classes, one-attempt-per-provider loop prevention,
  privacy compatibility, encryption, and provider/model identity must be
  explicit and testable. Audio and secrets remain excluded from persistence.

## D-014 — T-009 routing, provider accounting, and quota wire contract

- Date: 2026-10-02
- Status: accepted
- Context: Wave 1 found only app-launch and Wi-Fi Android executors, a single
  direct Gemini planner, and no quota transport. Provider limits are dynamic and
  account-specific. Parallel implementation requires one shared contract.
- Decision: Adopt `vi-local-v1` and the deterministic LOCAL,
  BACKEND_REQUIRED, and UNSUPPORTED semantics in
  `docs/LOCAL_CAPABILITIES_V1.md`. Recognized local commands with missing
  privilege fail locally rather than falling back. Observable outcomes
  distinguish verified state from dispatched intents. Add request/response-only
  authenticated V1 quota snapshots with nullable unknowns, explicit source and
  staleness. Every real provider uses one durable atomic metered proxy, random
  eligible selection, eligible bounded fallback, and at most one attempt per
  provider/request. New terminal codes are `AI_QUOTA_EXHAUSTED`, `AI_TIMEOUT`,
  and `AI_UNAVAILABLE`; Android renders them as `Khó quá, không làm được`.
- Rationale: Additive V1 messages preserve existing command compatibility,
  deterministic local failure cannot bypass Android policy, and durable
  pessimistic reservations enforce configured budgets across concurrency and
  restart without pretending provider dashboards are exact.
- Consequences: `docs/PROTOCOL.md`, `docs/LOCAL_CAPABILITIES_V1.md`, and shared
  fixtures are implementation authority. Gemini/OpenAI numeric limits remain
  runtime configuration. Old servers/clients degrade monitoring to unknown;
  no command auto-replay or manual quota reset is introduced.

## D-028 — Side-by-side configured apps and intent-directed voice routing

- Date: 2026-10-05
- Status: accepted implementation direction from owner request; AVD/physical placement acceptance open
- Context: Owner wants two simultaneous DeX apps (for example Maps and YouTube) and continued voice control of the app named by command intent, regardless of which split pane has focus.
- Decision: Add a typed app-pair action that accepts only two distinct APK-configured app IDs and runs through the shared ActionRouter. On Android, both launches must target the selected Samsung desktop; ask Android for adjacent/side-by-side placement and verify two distinct app windows and their bounds before reporting success. Never use a phone-display/global task action. Route explicit config patterns by their action semantics: Maps destination aliases resolve locally; “mở nhạc {query}” enters YouTube's fixed play-query flow; foreground window alone must not override a uniquely matched explicit-intent command. Ambiguous commands fail closed. Arbitrary means any two installed apps declared in APK config, not unrestricted package discovery.
- Rationale: typed app IDs remain allowlisted and backend-compatible, while display-bound launch plus window readback avoids claiming split-screen when the OEM/app rejected placement.
- Consequences: DEVICE_COMMAND schema-1 action set expands additively, requires backend-first rollout, and old clients remain unchanged. Fold3 Samsung DeX placement and spoken selection are separate required acceptance; an AOSP API35 AVD is not evidence of Samsung DeX behavior.
- Follow-up: when Chrome is the selected DeX foreground app, the exact “tìm địa điểm {query}” pattern resolves to WEB_SEARCH and explicitly targets the configured Chrome package after foreground revalidation. The same phrase in Maps resolves to SEARCH_PLACE; in other apps it keeps the existing unscoped Maps search. This preserves named app intent without letting the foreground pane silently redirect explicit Maps/YouTube phrases.

## D-029 — Bound backend conversation contexts

- Date: 2026-10-05
- Status: accepted within owner-authorized T-013 resource tuning
- Context: TTL and per-conversation turn limits did not bound the number of IDs retained during the TTL window.
- Decision: retain at most 1024 conversation contexts by default, configurable through `dex.conversation.max-conversations`; evict the least recently accessed context when inserting beyond capacity. Keep existing six-turn and 30-minute defaults.
- Rationale: bound retained entry count without an extra cleanup thread or changing command/authentication contracts.
- Consequences: evicted IDs start with empty context on their next command. This is not a strict byte bound; provider response size and durable quota retention remain separate concerns. Positive capacity binding and read/write eviction have regression tests.

## D-030 — Separate voice modes and session-only chat context

- Date: 2026-10-05
- Status: accepted for T-014 routing, voice entry/exit and session-only retention; lifecycle limits remain open
- Context: Owner explicitly chose separate Chat and Command modes, then confirmed context exists only within the session without long-term history during T-014 planning.
- Decision: Use explicit, separate Chat and Command modes. Chat utterances go to the conversational path and cannot dispatch device actions, even when their text resembles a supported command. Device commands use the existing guarded local-first route in Command mode. Do not automatically fall back from unmatched commands into Chat; the user must explicitly switch modes before device control from Chat.
- Retention decision: retain bounded Chat context only in volatile memory during the active session. Do not write Chat transcripts to backend/Android databases, files, diagnostic logs or durable command history. Release retained context on session end/reset/expiry; do not restore conversation content after app/backend restart. This applies to Chat content, not the existing Command history policy. External provider retention must be documented separately; this decision is not evidence of provider-side zero retention.
- Voice UX decision: enter Chat with “Hey hey” followed by “Bắt đầu trò chuyện”. Subsequent Chat turns require no wake phrase. After assistant TTS completes, reopen listening with an audible cue. “Kết thúc trò chuyện” requires no wake phrase, including during assistant TTS; stop playback and pending work, close the session, release context and return to wake-word standby in Command mode. Provide an on-screen stop control. Recognition of the exit phrase is a local session control, not a device action or provider request. Prevent the app's own TTS from triggering exit; spoken interruption and echo handling require physical validation.
- Rationale: make the selected interaction predictable, prevent conversational text from becoming a device action, and avoid long-term project-managed Chat history.
- Consequences: define and test explicit mode transitions, truthful mode indication, lifecycle cancellation and context deletion before implementation. Idle timeout, reconnect behavior, identity isolation, turn/token caps, output limits, provider data policy/budgets, reset controls and the detailed behavior of the on-screen stop control remain undecided. These decisions do not schedule implementation or replace T-011/device acceptance.
- 2026-10-06 owner direction: retain personal scope and the existing Gemini API-through-metered-proxy path; keep the BRD $0/month target; add no provider, paid fallback or integration change. Provider-policy comparison is skipped at the owner's direction and no compliance claim is made. The proposed lifecycle/limits bundle remains unapproved; see [T-014-02 decision record](evidence/T-014-02-policy-review-2026-10-06.md).
- 2026-10-06 T-014-08 owner direction: raise the local Gemini request budget `DEX_GEMINI_TEST_RPM` from 2 to 10 requests per minute for the three-turn provider test. This is an application admission limit, not a verified account quota or billing allowance. Keep the configured model, proxy path and 10-second provider timeout.
- 2026-10-06 owner direction: Chat uses the existing voice-command interaction flow with explicit start/end phrases, one user and at most one active session. A single conversation ID identifies one session and a new session gets a new ID. Utterances in Chat are conversational only and must never dispatch device actions. Owner also accepted 15-minute idle expiry, ending on disconnect/Stop/Pause, fresh session on reconnect, at most 200 user/assistant turns per session and one in-flight turn per session. Context/input/output caps remain open for clarification; see [T-014-02 decision record](evidence/T-014-02-policy-review-2026-10-06.md).

## Template

### D-### — Title

- Date: YYYY-MM-DD
- Status: proposed | accepted | superseded
- Context:
- Decision:
- Rationale:
- Consequences:


## D-031 — Transient command subtitles on DeX

- Date: 2026-10-05
- Status: owner-selected UX accepted; implemented under T-006; physical acceptance IN_PROGRESS
- Context: after wake, the owner cannot see what STT heard while using other apps
  and requested movie-style subtitles along the bottom of the screen.
- Decision: show command STT subtitles only on DeX, include short truthful
  preparation/listening/recognizing status, and retain the final transcript for
  three seconds. A new listening turn replaces it; explicit cancellation and
  STT error/timeout clear it. Existing D-020 redaction, RAM-only transcript and
  partial-no-dispatch rules continue to apply.
- Rationale: provide visible recognition feedback without opening the dashboard
  or taking focus away from the current app.
- Consequences: extend T-006 with independent subtitle lifecycle and T-010
  overlay/screenshot regression checks. Existing provider/protocol remain;
  physical partial-before-final behavior is unverified, not guaranteed by the
  partial-results request. An explicit second hardware-mic press during active
  STT submits the latest nonblank partial exactly once through the same route as
  a final callback, including command interaction and Chat. Other partial
  callbacks stay display-only; empty partials do nothing and late recognizer
  callbacks are ignored. See the T-006 ExecPlan for acceptance. This extends
  D-020, not its privacy or dashboard diagnostic policy.

## D-032 — Command-first semantic UI migration and Vietnamese speech

- Date: 2026-10-07
- Status: accepted implementation direction; M1 contract implemented, semantic
  execution and Fold3 acceptance have not begun
- Context: the owner requested a staged plan from the architecture audit,
  maximum practical use of commands and existing Device Owner privileges,
  and Vietnamese STT/TTS throughout.
- Decision: prefer supported Android API, Intent and verified DPC operations
  for device actions; use semantic UI resolution and current DeX-scoped bounds
  gestures when APIs cannot perform the action, with the existing 24-cell grid
  as the final automatic fallback and an explicit user tool. Automate build,
  fixtures and device verification through commands/ADB where practical;
  production action execution must not depend on ADB, root or shell. Preserve
  the common typed Android action policy for local and remote commands.
  STT remains `vi-VN`; TTS and spoken interaction remain Vietnamese, following
  D-016's offline-preferred/network-permitted voice policy. Partial STT remains
  display-only unless the user explicitly submits it with the existing second
  mic press. Device Owner uses only permissions/actions Android actually grants;
  it cannot replace user-mediated Accessibility, assistant role or special access.
- Rationale: reuse the working executor and privileges without bypassing Android
  policy, reduce uncertain coordinate selection, and retain usable Vietnamese
  voice interaction while changes are verified incrementally.
- Consequences: T-015 defines implementation gates and backend-first protocol
  compatibility. This decision sets priorities and boundaries; D-033 records
  the later schema-2 contract. No semantic resolver, new permission or physical
  result is claimed here.

## D-033 — Additive semantic action schema 2 behind executor readiness

- Date: 2026-10-07
- Status: accepted for T-015 M1
- Context: semantic target selection needs a typed action that preserves old
  schema-1 clients and keeps UI/confirmation authority on Android.
- Decision: schema 2 retains existing `DEVICE_COMMAND` actions and adds
  `CLICK_TARGET` with a configured app ID and at least one bounded textual
  selector; optional role/context/ordinal refine matching. Both sides reject
  unknown or policy fields and enforce the advertised version and action list.
  Android continues to advertise schema 1 and reject execution of
  `CLICK_TARGET` until M2 supplies and validates the semantic executor.
- Consequences: backend may accept schema-2 clients now, but no current APK
  requests the new action. M2 must verify DeX scope, ambiguity, confirmation
  and target revalidation before advertising it. See
  [contract](../docs/DEVICE_COMMANDS_V2.md) and
  [M1 evidence](evidence/T-015-M1-contract-2026-10-07.md).

## D-034 — Show stable YouTube selection indexes after opening or searching

- Date: 2026-10-07
- Status: accepted owner follow-up to T-015
- Context: the original T-015 plan covered semantic search/select and numbered
  fallback but did not explicitly require indexes immediately after app launch.
  Owner reported missing video indexes after opening YouTube and only delivery
  feedback after a simple music search.
- Decision: opening YouTube or a simple YouTube search should prepare visible
  1-based selection numbers for accessible video cards on the selected DeX
  window. Search must verify the requested query before presenting its results.
  A spoken index refers to the frozen displayed card map; confirmation and live
  revalidation remain mandatory. App/list/window/display changes invalidate
  that map instead of silently redirecting the number. No blind click or
  automatic grid is introduced when semantic cards cannot be read.
- Consequences: extend T-015 with preview lifecycle and physical acceptance.
  Existing screen lifetime is retained; no new wire schema, provider policy,
  partial auto-dispatch or permission is authorized by this requirement.
  Other app adapters need their own verified target semantics. Implementation
  and device results are recorded separately in the T-015 plan/evidence.

## D-035 — DeX-first dashboard with visible local commands

- Date: 2026-10-08
- Status: accepted owner direction for T-016
- Context: the current dashboard hides manual entry and local commands behind
  a dialog while occupying the main screen with secondary controls.
- Decision: prioritize a DeX desktop layout that shows voice state, primary
  Start/Resume/Pause/Stop controls, transcript, manual entry and grouped local
  command examples immediately. Fold3 remains usable. A sample tap fills entry
  only; Run is an explicit separate action. Provide full supported-command
  access and disclose prerequisites. Move settings, quota, history and voice
  diagnostic to a secondary menu without removing their behavior. Show End Chat
  only during Chat. The page itself does not scroll; the command pane may scroll.
- Consequences: T-016 updates Android UI and tests and the SRS dashboard
  criterion. Local execution, credentials, permissions and wire contracts stay
  governed by their existing decisions.
- Owner follow-up 2026-10-08: “Xem tất cả” must cover every pattern of every
  currently supported local catalog command. Organize by user goal with an
  easy-to-remember primary phrase and folded alternative phrasings, plus search.
  Exclude unsupported actions and preserve contextual/permission notes. This
  extends the T-016 presentation requirement without changing command routing.


## D-036 — Android-only provider orchestration and Wi-Fi execution

- Date: 2026-10-10.
- Status: accepted owner direction; runtime migration and final acceptance not verified by this memory update.
- Decision: Android owns AI/provider proxy orchestration and local Wi-Fi execution through existing guarded Android/DPC capabilities. Spring Boot backend/WebSocket is no longer required for product Command or Chat paths. Existing T-018 defines final post-migration Fold3 acceptance.
- Consequences: cancel obsolete outstanding backend scopes in T-003/T-009 and backend milestones in mixed T-006/T-011/T-014/T-015. Preserve Android work and completed historical evidence; transfer direct-provider integration and Wi-Fi verification to T-018. Supersedes backend-required direction in D-007/D-012/D-014/D-029/D-030/D-032/D-033 only where it mandates server orchestration/protocol. Local execution, confirmation, bounded volatile Chat, privacy, signing and device safety requirements remain.
- This direction does not authorize cloud resource deletion, claim migration complete, or change source/runtime in this documentation task.

## D-037 — Android canonical workspace; backend retired

- Date: 2026-10-13.
- Status: accepted owner direction.
- Decision: stop backend development. This Android repository owns project
  memory, requirements, action/result contracts, task boards, plans, evidence,
  tools and future development/test sessions. Open its single-folder workspace.
- Consequences: move necessary canonical Markdown and local selector/performance
  dependencies here. Freeze backend source/history and redirect old entry points.
  No backend checkout/runtime/protocol-first coordination is required. Historical
  backend requirements/commands are obsolete where they conflict with D-036/D-037.
- Preserve task IDs/states, cancelled scopes, unfinished Fold3 acceptance,
  original signing, settings, credentials and device data. Windows simulator is
  first; actual Fold3 tests stay deferred in T-020. This migration authorizes no
  app behavior changes, deployments, cloud deletion or physical/provider actions.

## D-038 — Public Android repository under MIT

- Date: 2026-10-22
- Status: accepted by explicit owner request (T-024).
- Context: owner requests making the Android GitHub repository public and adding MIT.
- Decision: publish `khuongnd0901/Dex-Assistant-UI` publicly; license original project material under the root MIT LICENSE, copyright 2026 Khuong Duy Nguyen.
- Rationale: follow the owner's distribution and licensing choice.
- Consequences: third-party components/model assets keep their own licenses and notices. Unrelated local changes are excluded from this publication; existing runtime and physical acceptance gates remain open.

## D-039 — ChatGPT Subscription migration behind target acceptance

- Date: 2026-10-22.
- Status: accepted implementation direction by owner; runtime/default activation unaccepted.
- Decision: T-025 adapts the MIT Android POC into a separate DeX Assistant OAuth installation (own host/client/Keystore record). Use documented public OAuth/model/Responses endpoints with user-granted subscription scope, store=false, stream=true and bounded explicit input history. Keep API-key identities/credentials separate; never use CLI files, internal endpoints, proxies or automatic paid fallback.
- Eligibility: D-038 public MIT repository independently verified on GitHub; official SIWC open-source guidance permits implementation. This does not prove the target account's consent/model/tool availability. POC evidence cannot substitute for target testing.
- Boundaries: local-first routing, CommandPrivacy, ActionCatalog/TypedAction, ActionRouter consent/deadlines/dedup and session-only Chat/voice/TTS remain authoritative. Only completed structured namespace function calls may become typed plans; no text-to-action workaround. Existing Gemini/Groq remain operational rollback choices.
- Activation: subscription account/model/inference settings support owner testing. After real target inference/function-calling PASS, allow deliberate manual trial selection only with successful Chat/tools proofs for the exact account/client and selected model. Changing model or signing in/out clears proofs. Automatic default migration remains disabled pending full physical/voice/action acceptance; no provider preference changes implicitly. No model name is hardcoded. HTTP output limits are enforced locally because subscription preview rejects max_output_tokens; the existing 4000-code-point Chat cap remains authoritative, while 1024 tokens is requested in instructions and is not a hard provider guarantee.
- Consequences: [T-025 plan](plans/active/T-025-chatgpt-subscription-migration.md) and [report](evidence/T-025-chatgpt-subscription-migration.md) own remaining gates. Earlier provider constraints are superseded only within this explicitly authorized migration; no existing unfinished task is marked complete.

## D-040 — Subscription-only AI runtime; preserve stored legacy credentials

- Date: 2026-10-23. Status: accepted by owner's renewed migration/cleanup request, using existing GitHub subscription branch.
- Decision: continue T-025 from ba054d3. Remove Gemini/Groq HTTP adapters, other API-key model probes/key editor and unused provider/free-tier/direct-mode configuration. Command and Chat invoke existing ChatGptSubscriptionProvider directly. Retain encrypted legacy keys/preferences without reading, deleting or migrating their secrets.
- Routing: only subscription may serve non-local AI. Preserve explicit selection and account/model proofs; a saved legacy default is inactive and receives setup guidance. No implicit preference/token changes, backend or API-key fallback. Subscription refresh, bounded SSE and typed ActionRouter remain authoritative.
- Consequences: supersedes D-039's operational Gemini/Groq rollback choices; rollback requires an earlier APK with original signing/data retained. D-039's auth/privacy/tool/proof/physical gates remain. Replace removed-provider fixtures with subscription transport/lifecycle/schema/settings tests; synthetic subscription benchmarks are a new series, not comparable to T-021 BYOK timings. Existing physical results are historical, not evidence of this cleanup build.
