> Migrated to Android on 2026-10-13 ([D-037](../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# DeX Assistant — Android-only Command + Chat migration

- Date: 2026-10-09
- Scope: Source migration away from custom Spring Boot/WebSocket; local device controls unchanged.
- Status: SOURCE PUBLISHED, CI/DEVICE GATES OPEN until their own evidence is recorded.

## Implementation

- Kotlin DirectAiChat posts chat messages to Gemini `generateContent` or Groq `chat/completions` using manually configured device-held API keys; uses no device-action tools in chat.
- DirectChatState stores a bounded session (max 20 prior pairs, max 200 turns, 2,000 input code points, 4,000 output, 15-minute inactivity) in memory only. Privacy gate rejects sensitive chat text.
- AssistantCoordinator.startChat/sendChat/endChat now starts local session and sends async direct requests; late callbacks are ignored after cancellation/end using session/epoch/request identifiers. VoiceAssistantService retains Chat start/end speech and TTS callbacks.
- AI Commands: LocalCommandEngine unchanged, non-local commands always use MobileAiProxy + DeviceCommandProtocol.decode + ActionRouter, with device-side confirmation/revalidation.
- Removed AssistantWebSocket instantiation, backend provisioning/connection and WebSocket listener logic from active AssistantCoordinator. Voice Assistant Service no longer connects to the backend. Removed endpoint/bearer provisioning and backend quota UI.
- Existing protocol/WebSocket classes and test fixtures may remain in repository as inert reference code; they are not used by the live Command/Chat execution path. Backend repository retained as source rollback reference, not deleted.
- No paid provider call is automatically activated. Existing confirmed-Free-Tier setting and enabled/key/model are required. Gemini/Groq implemented; OpenAI, Grok, DeepSeek remain configuration-only pending proven API free-tier and adapter tests.

## Required gates (NOT implied by source commits)

- Host: GitHub Actions JDK17: `testDebugUnitTest lint assembleDebug` plus artifact upload. Capture exact commit and PASS/fail logs.
- Physical: One controlled real Fold3/API35 DeX acceptance run with currently installed matching signing certificate and no uninstall/clear data or Device Owner changes. Do not install generic CI debug APK on the Device Owner installation; use verified matching-key device-test artifact.
- Manual Settings: enter own API keys inside app, save/update/delete, verify Gemini/Groq list-model probe, set explicit eligible free model, select provider and opt in as needed. Don't print keys.
- Device functional: local command with Wi-Fi off; direct command via Gemini (and Groq if free & available); unsupported and malformed plan rejection; Chat three consecutive turns with context; wake/STT/TTS; Chat END by voice and UI; canceled/late responses; provider invalid key, 429, timeout; privacy; role/accessibility/DPC intact.
- Compare actual p50/p95 command+Chat time, failure counts and background retention, measure without including user-confirmation wait. No inference about physical performance from emulator or CI.
- Decommission deployed EC2, DNS/EBS only after physical evidence and explicit user authorization. This coding session cannot access physical Fold3 or AWS running instances.

## CI

CI workflow: https://github.com/khuongnd0901/Dex-Assistant-UI/actions/workflows/android-ci.yml

## References

Gemini REST multi-turn: https://ai.google.dev/gemini-api/docs/generate-content/text-generation
Groq chat: https://console.groq.com/docs/text-chat