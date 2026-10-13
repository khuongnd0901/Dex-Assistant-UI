> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# Android agent role

The durable Android-agent entry point is
`/mnt/d/dex-assistant-ui/AGENTS.md`. This backend-side summary exists so the
main agent can assign work without loading the Android implementation first.

Android owns UI, connection/reconnect behavior, WebSocket serialization,
Keystore-backed provisioning, native actions, user confirmation, wake word,
STT/TTS, Accessibility execution, device markers, and mobile tests. It does not
own provider credentials, server planning, token issuance, or canonical wire
contracts.

The Android agent must not edit the backend repository or independently change
protocol/authentication/action semantics. Shared changes return to the main
agent for coordination. Standard validation is `./gradlew test`,
`./gradlew lint`, `./gradlew assembleDebug`, device checks when required, and
`git diff --check`, all with JDK 17 and an installed Android SDK.
