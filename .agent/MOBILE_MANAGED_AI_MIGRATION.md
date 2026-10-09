# Android AI migration proposal (cross-repository handoff)

- Status: **PROPOSED; not implemented** (2026-10-09).
- Canonical RFC: https://github.com/khuongnd0901/DeX-Assistant/blob/main/.agent/plans/proposed/RFC-2026-10-09-mobile-managed-ai.md
- This file only points Android Codex to the canonical backend-owned proposal; it does not supersede `AGENTS.md`, D-007, T-009 or current `.agent/STATUS.md`.

## Target

Use existing Android local-first routing and ActionRouter, replacing Spring Boot AI planning and non-action Chat with Firebase AI Logic (Gemini) + App Check if proven secure on the signed Z Fold3. Keep backend fallback until physical acceptance. Never embed cloud API keys in APK, and never reduce action schema/confirmation/device-policy safeguards to accommodate an SDK.

## Android implementation handoff after owner approval

1. M0/M1: audit `AssistantController.kt`, `AssistantWebSocket.kt`, `ChatSession.kt`, `DeviceCommandProtocol.kt`, `routing/`, `ActionRouter.kt`; create validated Kotlin contracts and backend fixture parity.
2. M2: non-production Firebase/Play Integrity App Check spike; verify actual signed sideloaded Fold3 with strict `functionCalls` mapping; do not include raw keys in build artifacts.
3. M3/M4: route `BACKEND_REQUIRED` and Chat through injectable ports with a default-off Firebase feature flag; never alter voice/DPC/Accessibility pipeline.
4. M5/M6: local estimates explicitly marked as such; instrument network and real voice/command Chat reliability and regression; no simulated acceptance claims.
5. M7: make Firebase default and remove WebSocket dependency only after canonical gate approval; retain rollback and do not delete user/device state.

Required validation: `./gradlew test lint assembleDebug`, shared protocol fixtures, APK/secret scan, bounded response validation, Android 15 AVD and physical Galaxy Z Fold3; keep current signing identity and install in place.
