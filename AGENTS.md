# Dex Assistant UI agent guide

Use `.agent/STATUS.md` as the entry point for every Android-agent session. The
backend repository remains the canonical source for product-wide tasks,
decisions, requirements, and protocol.

This repository contains the Android client. The canonical backend and protocol
repository is `https://github.com/khuongnd0901/DeX-Assistant` and is commonly
checked out locally at `/mnt/d/dex-assistant`.

Before changing WebSocket messages, authentication, or action/result types,
inspect the backend's `docs/PROTOCOL.md` and corresponding protocol tests. Keep
tokens, signing material, local SDK paths, and environment-specific endpoints
out of Git.

Use JDK 17 and the Gradle wrapper. The baseline targets Android 15/API 35 and
supports API 30+ in the current scaffold. This is not an accepted product
baseline: backend task T-004 proposes API 36 and still requires owner approval
and an actual Galaxy Z Fold3 API-level check.

## Agent ownership

The Android agent owns UI, connection/reconnect behavior, WebSocket client
serialization, local token provisioning and Keystore use, native actions,
on-device policy and confirmation, wake word/STT/TTS, Accessibility execution,
device measurements, and Android tests.

Do not edit `/mnt/d/dex-assistant` from the Android-agent role. Do not redefine
protocol envelopes, authentication, action/result types, error codes,
deduplication, or reconnect semantics independently. Return shared changes to
the main agent, who coordinates both repositories.

## Context loading

1. Read this file and `.agent/STATUS.md`.
2. Read Gradle configuration and only the Android source/tests relevant to the
   assigned task.
3. For integration work, read backend `.agent/STATUS.md`, the relevant T-003 or
   T-004 task/plan, decisions D-003/D-004/D-006/D-007, and
   `/mnt/d/dex-assistant/docs/PROTOCOL.md`.
4. Read BRD/SRS only when changing product or platform behavior claims.

## Validation

Run `./gradlew test`, `./gradlew lint`, and `./gradlew assembleDebug` as
applicable, then `git diff --check`. Use JDK 17 and a configured Android SDK.
Device-specific claims require device/ADB evidence; compilation is not proof of
Galaxy Z Fold3 behavior.
