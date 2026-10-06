# Dex Assistant UI agent guide

Use `.agent/STATUS.md` as the entry point for every Android-agent session. The
backend repository remains the canonical source for product-wide tasks,
decisions, requirements, and protocol.

This repository contains the Android client. The canonical backend and protocol
repository is `https://github.com/khuongnd0901/DeX-Assistant` and is checked out locally at `/home/kunrg/DeX-Assistant` on the Fold3 test host.

Before changing WebSocket messages, authentication, or action/result types,
inspect the backend's `docs/PROTOCOL.md` and corresponding protocol tests. Keep
tokens, signing material, local SDK paths, and environment-specific endpoints
out of Git.

Use JDK 17 and the Gradle wrapper. The baseline targets Android 15/API 35 and
supports API 30+ in the current scaffold. The physical Fold3 has been verified as Android 15/API 35; use canonical
D-011 and T-006 evidence for the accepted assistant test baseline.

## Android test target on this host

The canonical test selector is `/mnt/d/DeX-Assistant/scripts/android/select-test-target.sh`.
Run it with `dex` for desktop/freeform tests or `foldable` for hinge/layout tests.
It prefers a connected Galaxy Z Fold3 `SM-F926*` on API 35 and otherwise starts
the matching Windows AVD under `D:\Android\AVD`. Use the exact reported serial
for every device command. `D:\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat`
manages SDK packages; ADB is `D:\Android\Sdk\platform-tools\adb.exe`. For WSL
Gradle builds use JDK 17 and `/home/khuongnd/Android/Sdk`. The AVD approximates
Android 15 foldable/DeX layout but cannot verify Samsung DeX or real mic/button
behavior. Full setup and limitations: backend `scripts/android/README.md`.
Use Linux `adb` for a Fold3 visible to WSL. The Windows AVD uses Windows ADB
server port 5038 (`adb.exe -P 5038`); port 5037 may connect to the separate
Linux ADB server. Follow the selector's `ADB` and `SERIAL` output.

## Agent ownership

The Android agent owns UI, connection/reconnect behavior, WebSocket client
serialization, local token provisioning and Keystore use, native actions,
on-device policy and confirmation, wake word/STT/TTS, Accessibility execution,
device measurements, and Android tests.

Do not edit the canonical backend repository from the Android-agent role. Do not redefine
protocol envelopes, authentication, action/result types, error codes,
deduplication, or reconnect semantics independently. Return shared changes to
the main agent, who coordinates both repositories.

## Context loading

1. Read this file and `.agent/STATUS.md`.
2. Read Gradle configuration and only the Android source/tests relevant to the
   assigned task.
3. For integration work, read backend `.agent/STATUS.md`, the relevant T-003 or
   T-004 task/plan, decisions D-003/D-004/D-006/D-007, and
   the canonical backend `docs/PROTOCOL.md`.
4. Read BRD/SRS only when changing product or platform behavior claims.

## Validation

Run `./gradlew test`, `./gradlew lint`, and `./gradlew assembleDebug` as
applicable, then `git diff --check`. Use JDK 17 and a configured Android SDK.
Device-specific claims require device/ADB evidence; compilation is not proof of
Galaxy Z Fold3 behavior.
