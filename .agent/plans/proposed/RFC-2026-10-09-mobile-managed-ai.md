> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# RFC — Android Direct Free-Tier AI (no backend / no Firebase)

- **Owner direction: CONFIRMED on 2026-10-09.**
- **Implementation: NOT_STARTED.** Document-only migration plan; no Android, DPC, backend runtime, credentials, signing, voice pipeline or deployed instance changed.
- Canonical repo: `khuongnd0901/DeX-Assistant`; Android repo: `khuongnd0901/Dex-Assistant-UI`.
- Goal: one-owner Samsung Z Fold3 / DeX Assistant, cloud AI invoked **directly from Android**, with **only demonstrably free-tier APIs** and no always-running, serverless, Firebase or reverse proxy dependency. Target infrastructure cost: 0/month (not a guarantee of third-party availability).

## Owner's explicit scope / threat-model choice

The owner explicitly chooses direct calls from Android and accepts the API-key exposure risk for a personal sideloaded device. **Do not require or add Firebase AI Logic, App Check, EC2, Cloud Run, Cloudflare Worker, Lambda or Spring Boot as the call path** unless the owner separately changes the requirement.

- Provider keys are manually entered in Android Settings; store with existing Android Keystore/encrypted preferences style. No raw keys committed, included in APK/BuildConfig, logged, backed up, sent to third parties or shown in screenshots. Encrypted-at-rest **does not** prevent extraction from a compromised/running client. Key revocation/rotation stays possible.
- Do not ship preloaded keys. Key exposure is an accepted tradeoff, not an assumption of confidentiality.
- Do not activate paid tiers/auto-recharge, or call a model unless free-tier eligibility of that provider/account/model has been verified. Fail closed when eligibility is unknown; report insufficient access and let user change model.
- Provider-defined RPM/RPD/TPM/TPD and terms apply; rotating keys to bypass provider quotas is out of scope. Actual quotas are account/project-specific and can change.
- Gemini's free tier can permit provider use of submitted content for product improvement: retain existing on-device `CommandPrivacy`/sensitive-input policy. No Wi-Fi passwords, bearer tokens, phonebook dump, UI tree, screenshot/audio or local address/identity store sent by default.

## Mandatory UI-managed provider credentials (owner clarified 2026-10-09)

**API keys must be runtime-editable values in the Android app UI**; they must not be hardcoded in Kotlin/Java, Gradle, BuildConfig, Android resources, sample config, APK, Git or CI. Users never need to rebuild/reinstall to add, rotate, switch or delete a key.

**Settings > AI Providers** (extend existing Compose Settings, currently only WebSocket endpoint and bearer-token provisioning in `MainActivity.kt`):
1. Individual provider entries for **Gemini** and **Groq** with enabled switch, provider name, editable free-tier model selection (validated against actual eligibility), and masked key entry. No default key. Key can be pasted manually at runtime; don't preload values or print their content. Existing saved key is shown as `Configured`, not echoed in cleartext.
2. Per-provider buttons **Save / Update key**, **Test connection**, **Delete key**; an explicit indicator `Not configured / Configured / Testing / Connected / Invalid key / Quota limited / Error` grounded in actual provider responses, not a synthetic success. Test is a minimal real provider request, consumes applicable free quota, never triggers device actions and must remain opt-in.
3. Save key locally using a **provider-separated Android Keystore-backed encrypted store**, e.g. `AiCredentialStore` / `AiSettingsRepository`, inspired by existing `security/CredentialStore.kt`; do not overwrite the existing WebSocket bearer key needed for rollback. No credentials in `rememberSaveable`/saved-instance-state, history, crash logs, metrics, notifications or backups. Disable Android backup/data extraction for these credentials. The provider key is sent only in the authorized provider's HTTPS authentication header.
4. **Delete** removes stored provider secret and invalidates/clears pending credentials for new calls; **Update** must take effect for subsequent requests without rebuilding; cancel or safely finish in-flight request on provider change. Avoid showing any previously saved key in the input UI.
5. Provider can be selected/disabled without a backend. Missing key means fail locally with `AI_PROVIDER_NOT_CONFIGURED` (mapped to a sensible user-visible message); do not silently call another unknown or paid provider. Key format is treated as untrusted until a real authenticated response succeeds.
6. Verify process restart and device reboot retain settings; uninstall/clear app data is allowed to remove local keys, but test suites must **never** wipe real Fold3 data. Test no key leaks in APK, Git, logcat, crash reports, history, network payload body or backup exports.

The owner accepts the limitation that client-held secrets can be extracted at runtime. This is not grounds to reintroduce Firebase or a self-hosted service.

## First-party verification as of 2026-10-09

- **Gemini Developer API** has Free Tier access to selected models, including free input/output token pricing for eligible models; usable REST `generateContent` API via `x-goog-api-key` header. Account's active eligibility and quotas must be checked in Google AI Studio before picking a model.
  - https://ai.google.dev/gemini-api/docs/pricing
  - https://ai.google.dev/gemini-api/docs/rate-limits
  - https://ai.google.dev/gemini-api/docs/billing
  - https://ai.google.dev/api/generate-content
- **Groq Cloud API** documents a Free Plan, a documented request/token limit table for each model and OpenAI-compatible `chat/completions` endpoint. Check supported free model list and tool-calling behavior on actual account.
  - https://console.groq.com/docs/rate-limits
  - https://console.groq.com/docs/quickstart
  - https://console.groq.com/docs/text-chat
- **OpenAI API** has no generally guaranteed free monthly developer API quota; do not enable as a zero-cost provider just because the model name or ChatGPT subscription mentions OpenAI. Free-credit offers are account dependent.
  - https://help.openai.com/en/articles/8264644-how-can-i-set-up-prepaid-billing

Suggested initial ranking: Gemini free eligible model **PRIMARY**, Groq free eligible tool-capable model **FALLBACK**. Runtime model IDs come from verified account-eligible model list, not an unvalidated constant. Configure maximum output tokens, prompt budget, timeout, and local attempt budget conservatively. Groq's free availability does not guarantee command/schema compatibility; test it rather than assuming.

## Change request: five configurable AI providers (2026-10-09)

User requests per-provider Enable, manually configured API key and selected default model for Gemini, Groq, OpenAI (ChatGPT API), Grok (xAI) and DeepSeek. UI also provides authenticated model discovery (model IDs), saved local settings, key delete and a preferred provider. Grok and Groq are separate services.

Model list visibility is NOT proof of free inference, billing protection or typed tool-call support. During this pilot, only Gemini and Groq have direct Command adapters. Other providers are configuration-only until independently verified for free eligibility and safe function calling. Direct mode is OFF by default; choosing an unimplemented default must fail closed, never silently use a paid fallback. WebSocket Chat stays until its own migration gate.

Evidence: .agent/evidence/mobile-direct-M3-proxy-multiprovider-2026-10-09.md.

## Current code baseline (read before changing anything)

Android:
- `app/src/main/java/com/khuongnd/dexassistant/ui/AssistantController.kt` owns routing and backend `AssistantWebSocket` correlation; `Route.BACKEND_REQUIRED` currently sends WS.
- `routing/LocalRouting.kt`, `routing/LocalCommandEngine.kt`, `protocol/DeviceCommandProtocol.kt`, `routing/ActionCatalog.kt`, `execution/ActionRouter.kt` implement deterministic on-device route, typed action decoding and guarded execution.
- `protocol/ChatSession.kt` and `voice/ChatVoiceRouting.kt` rely on WS chat today.
- `security/CredentialStore.kt` uses Android Keystore; adapt with **separate credential aliases** and version migration; preserve existing backend token for rollback.
Backend reference-only:
- `application/CommandService.java`, `application/ClientPlanPolicy.java`, `application/PrivacyGate.java`
- `application/chat/ChatService.java`, `application/ConversationStore.java`
- `adapter/out/llm/DeviceCommandTools.java`, `adapter/out/llm/SafeToolCallMapper.java`
- `adapter/out/llm/MeteredPlannerProxy.java`, `adapter/out/llm/JdbcQuotaService.java`.
- `docs/PROTOCOL.md`, `docs/contract-fixtures/`.
- **D-007**, T-009 and T-015's previous server-side boundary is superseded **as a requested target architecture**, not as a statement of current implemented or accepted behavior. Main agent must reconcile decision/task docs when implementation proceeds.

## Target architecture

```text
Galaxy Z Fold3: wake/KWS -> STT -> CommandRouter
                     |-- LOCAL -> existing ActionRouter -> guarded DPC/Accessibility
                     |
                     '-- CLOUD_REQUIRED -> privacy gate / capability descriptor
                           -> MobileAiProxy (Kotlin + existing OkHttp)
                                |-- GeminiRestProvider (Free Tier verified)
                                '-- GroqChatProvider  (Free Plan verified)
                           -> strict common ToolCallMapper + PlanValidator
                           -> existing ActionRouter -> user confirmation -> revalidate + execute

Chat (no device action): local ChatController -> MobileAiProxy (separate prompts/session state)
```

Use a single provider-neutral `AiProvider` interface and `MobileAiProxy` for selection, timeouts, response normalization, quota estimates, provider fallback and cancellation. Keep network calls on background coroutine/executor, never block main/voice thread. No direct `WebSocket` to a custom backend in new primary path. Use TLS and configured approved endpoints only.

Command: map Gemini tool/function-call responses and Groq tools/structured output into an existing **typed `DeviceCommand` schema**, not raw natural language gestures. Reject unexpected fields, unsupported schema/actions, missing required params, invalid ranges, duplicates, >8 actions, fabricated local identities/targets and unsupported tool replies. Full validation, live capability check, user confirmation, revalidation and execution remain in Android's **single existing ActionRouter**. Preserve in-flight ID/epoch gates, no replay after ambiguity and no multiple executors.

Chat: isolated from Command, no device-action tool exposure; bounded in-memory conversation, context token/turn limits, timeouts/cancel/interrupt/rearm, sensitive input rejection. Do not silently persist transcripts.

Quota: Android cannot know exact remote provider usage just from a locally counted request. Use counters explicitly labeled **estimated/local**, read and honor trustworthy `Retry-After` / provider headers where present, implement per-provider rate buckets and reset windows and simple circuit breaker. Do not call paid models as fallback. No rotation among keys/projects to defeat quota rules. Quota exhaustion returns bounded terminal `AI_QUOTA_EXHAUSTED` / `AI_UNAVAILABLE` rather than indefinite retries.

## Sequential milestones with acceptance gates

- [ ] **M0 — Audit + free-tier feasibility (no code change)**: inventory entire remote Command/Chat/credential/quota/plan/voice dependency; confirm actual account Free Tier, available Gemini and Groq tool-capable models, rate limits, pricing/billing restrictions, key format, tools schemas, and sample request/response. Baseline p50/p95/timeout on signed Fold3 vs current backend. Exit: provider matrix, safe tool-call proof, approved model choices; no claims without live owner-key evidence.
- [ ] **M1 — Kotlin contracts and native policy parity**: common `AiProvider`, `AiRequest`, `AiResult`, `AiUsage`, `MobileAiProxy`, normalized provider error classes, Gemini/Groq response parsers, `PlanValidator`. Port/reuse backend schema fixtures and negative-case tests. No network/action integration. Exit: deterministic validation parity and strict fail closed, bound max eight actions.
- [ ] **M2 — Direct Gemini REST PoC**: OkHttp `generateContent`, configurable verified free model, function declarations based on Android capabilities, request cancellation/timeouts; settings-provisioned key from provider-separated Keystore-backed store; Compose Settings must offer masked input, Save/Update, Test connection, Delete and configured/invalid/quota statuses before any live PoC; zero keys committed/packed. Exit: real signed Z Fold3 direct request, valid multi-action function-call or explicit failure, authenticated 401/403/429/5xx behavior and bounded no-replay.
- [ ] **M3 — Direct Groq free fallback**: implement `chat/completions` adapter, tool schema translation, common typed mapper, check free-model tool support; provider selection **eligible + healthy + budget** not arbitrary random across broken options; at most one attempt/provider/request, fallback only transient failures (quota, 429, network, 5xx, timeout), never after an action is executed. Exit: real Groq function-call parity, confirmed free plan and 429 fallback tests. If unsupported, mark Groq unavailable rather than weaken action safety.
- [ ] **M4 — Migrate Command behind flag**: `AI_TRANSPORT=BACKEND|DIRECT` with `BACKEND` default in test/rollback. `BACKEND_REQUIRED` routes to `MobileAiProxy`, preserving on-device local commands, privacy, consent, 120s command deadline, requestId, epoch cancellation, no replay, confirmation and semantic target revalidation. Exit: stable Fold3 real command test and comparable p50/p95.
- [ ] **M5 — Migrate Chat, settings, local quota/history**: local bounded ChatSessionController without custom WS, voice/TTS cancellation and spoken exit; provider keys/models enable/disable via Settings and retain/rotate/delete without rebuilding; network state, 429 status, estimated quota health, user-clearable encrypted diagnostic history and redaction. Remove dependency on remote quota UI; preserve backend rollback provisioning. Exit: three-turn Vietnamese Chat, interrupt/rearm, 429/timeout/privacy tests on Fold3.
- [ ] **M6 — End-to-end physical acceptance**: `./gradlew test lint assembleDebug`; shared fixture parity; redact/log/APK/key test; no cloud call for supported local grammar; Wi-Fi-off; 401/403/429, timeout, cancellation and delayed responses, malformed tool calls, 100 command sessions, 3-turn natural voice Chat, 8h soak if available, no Device Owner/Accessibility/wake/STT/TTS regression. Benchmark >=30 real Fold3 successful attempts per actual route class, p50/p95 and error distributions; keep separate user confirmation waits. Signing identity and app data must remain intact; in-place install only. Failures remain OPEN.
- [ ] **M7 — Default direct; decommission self-hosted runtime (requires M6 PASS + owner go-ahead)**: switch default to DIRECT, mark backend-specific settings deprecated rather than breaking existing installs, stop EC2 and release billable resources only with explicit authorization, preserve backend source as rollback, update D-007, T-009/T-014/T-015 cross-repo status/docs, record observed $0 provider usage and any unknowns.

## Work split

- **Main agent**: canonical architecture decision and linked tasks, test fixture/contract authority, review Go/No-Go, integration evidence.
- **Android agent**: Kotlin ports, provider-key configuration UI + Keystore, OkHttp, Gemini/Groq adapters, guard/executor integration, device tests; only Android repo changes.
- **Backend agent**: read-only export of contract/policy/schema/Chat golden fixtures, mapping errors and test requirements; do not add backend deployment effort.
- Evidence: `.agent/evidence/mobile-direct-Mx-*.md` in canonical repo; link from Android `.agent/STATUS.md` as gates complete. Each milestone records commit(s), tests, failed/skip cases, real hardware evidence. **No DONE status from unit or emulator alone.**

## Decision

Proceed with M0 → M2 before removing Spring Boot runtime. Android-to-provider direct calls are the accepted target; key confidentiality is explicitly **not** a blocker, while preventing accidental **paid usage** and unvalidated device actions remains mandatory. No runtime change has yet been performed.
