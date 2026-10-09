# Direct Free-Tier AI migration — Android handoff

- Owner direction: **CONFIRMED 2026-10-09**. Android app calls **Gemini Free Tier REST directly**, with **Groq Free Plan API as conditional fallback**.
- Implementation: **NOT_STARTED**. Only this documentation changed; app, device, signing, runtime voice/DPC, backend and provider credentials untouched.
- Canonical M0–M7 plan: https://github.com/khuongnd0901/DeX-Assistant/blob/main/.agent/plans/proposed/RFC-2026-10-09-mobile-managed-ai.md
- Do **not** integrate Firebase AI Logic, App Check, EC2, Cloud Run, serverless or new Spring Boot proxy. No API paid fallback.

## Key choice

This is one owner's sideloaded Galaxy Z Fold3, and the owner accepts extractable provider keys. Enter individual keys manually in Settings; store in separate Android Keystore-backed secure preferences (reuse pattern of `security/CredentialStore.kt`), without any hardcoded keys in APK/BuildConfig/source, repository, logs or cloud sync. Preserve backend credential and device app state for rollback. This key tradeoff is explicitly accepted; don't block direct PoC on server-side secret storage.

## Implementation entry points

- `AssistantController.kt`: add injected `AiPlanner` transport; local path unchanged; route only `BACKEND_REQUIRED` through `MobileAiProxy` after an opt-in flag. Preserve requestId, pending/session deadline and cancellation.
- `routing/LocalRouting.kt`, `routing/LocalCommandEngine.kt`, `routing/ActionCatalog.kt`, `protocol/DeviceCommandProtocol.kt`: preserve deterministic local routing and strictly typed action contract. Map provider tool calls to existing `TypedAction`; never let LLM free text click.
- `execution/ActionRouter.kt`: unchanged authority for privilege/capability, confirmation, live target revalidation and execution.
- `protocol/AssistantWebSocket.kt`, `protocol/ChatSession.kt`, `voice/ChatVoiceRouting.kt`: legacy path remains rollback until Chat/Command real Fold3 gates; add local `ChatController` strictly separate from actions.
- Proposed `ai/` modules: `AiProvider`, `MobileAiProxy`, `GeminiRestProvider`, `GroqChatProvider`, `ToolCallMapper`, `PlanValidator`, `ProviderQuotaStore`, `AiSettingsRepository`.
- Network: existing OkHttp; configured Gemini REST `generateContent` with `x-goog-api-key`, Groq `chat/completions` Bearer; TLS and endpoint whitelist; background thread, bounded timeout and cancellation; function calling only, not arbitrary JSON instructions.
- Per-provider free eligibility and rate limits must be checked with current owner credentials/account. Track estimated local quota only. On 429 honor retry information, bounded fallback; never call paid models or rotate keys to circumvent limits.

## Milestone handoff

M0 actual free-account/model/tool-use probe → M1 fixture/parity tests → M2 direct Gemini signed Fold3 PoC → M3 Groq tool-calling/free check → M4 command transport flag → M5 Chat & settings/quota → M6 thorough physical Fold3/regression/latency/secret scan → M7 only then remove backend default after owner's go-ahead.

Use `./gradlew test lint assembleDebug` and real Galaxy Z Fold3 installation with original signing cert/data preserved. Treat physical natural voice, actual Google/Groq quota and timeout evidence separately from host tests; never mark a gate DONE without its required evidence. Cross-repo contract changes and task status require main-agent coordination.

This instruction supersedes the earlier Firebase AI Logic proposal in the Android repo, not the current shipped code.
