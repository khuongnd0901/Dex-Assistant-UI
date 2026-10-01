# Android repository status

Last synchronized: 2026-10-01 (T-006 Android slice)

## Canonical project memory

Product-wide status, tasks, decisions, requirements, and protocol are owned by
the backend repository at `/mnt/d/dex-assistant`. Start cross-repository work
from its `.agent/STATUS.md`. This file is a concise Android handoff, not a second
canonical task board.

## Current state

- The repository contains the Compose client (`com.khuongnd.dexassistant.ui`)
  and separate DPC (`com.khuongnd.dexassistant.dpc`).
- Current build configuration: compile/target API 35, min API 30, JDK 17.
- The client provisions a Keystore-wrapped token, enforces WSS except loopback,
  implements the V1 WebSocket/reconnect flow, launches apps, delegates Wi-Fi to
  the signature-protected DPC, and emits non-sensitive latency markers.
- The DPC verifies Device Owner status and observes Wi-Fi state before success.
- `test`, `lint`, and `assembleDebug` pass for both modules with JDK 17/API 35.
- T-006 now has app-scoped DPC permission enumeration with verified grant
  results, typed permission/capability/voice lifecycle IPC, and Device Owner
  boot/app-update recovery. The DPC target is fixed to
  `com.khuongnd.dexassistant.ui`; it does not receive backend credentials.
- The main APK declares the API 30-35 permission profile and a
  signature-protected microphone foreground service. Its persistent
  notification supports pause/resume, stop, and opening administration; health
  is persisted without command text or credentials.
- The daily chat controls were removed from the Activity. The remaining
  administration surface provisions/connects, shows basic owner/voice/backend
  health, applies the permission profile, and starts/stops voice service.
- Voice lifecycle transitions and permission result classification have unit
  coverage. Protocol V1 was not changed.

## Relevant canonical tasks

- Backend `T-004` is `IN_PROGRESS`: owner decisions are accepted; actual Fold3
  firmware and Device Owner evidence remain.
- Backend `T-003` is `IN_PROGRESS`: automated implementation is complete, while
  on-device provisioning, actions, and latency remain unverified.
- Backend `T-005` establishes the main/backend/Android agent workflow.

## Blockers

- No Fold3 is visible through ADB. Device Owner provisioning, signature IPC
  rejection, five cold boots, microphone FGS behavior, and notification actions
  still require device evidence.
- sherpa-onnx 1.13.8 and the official GigaSpeech 3.3M model were not added: the
  exact model archive was not available locally and no unverified/placeholder
  asset was fabricated. Until those assets and checksums are installed, the
  service reports `PAUSED` / model unavailable and does not open the microphone.
- STT, TTS, real wake detection, WebSocket voice handoff, device-credential
  gating, special-access Settings links, and full diagnostics remain T-006 work.
- API 35 foldable-emulator testing provisioned the DPC as Device Owner and
  verified permission grants, but DPC-started microphone FGS failed across the
  separate package boundary (`mAllowStart...=DENIED`). Boot recovery ran but
  could not resolve/start the main service. The administration layout also clips
  its emergency-stop control, and no AccessibilityService is declared.

## Next action

Obtain and verify the official sherpa-onnx 1.13.8 AAR plus GigaSpeech 3.3M model
archive, then implement KWS -> Vietnamese on-device SpeechRecognizer (explicit
system fallback) -> existing V1 command path and TTS. After that, run the full
Fold3 acceptance matrix; keep canonical T-006 `IN_PROGRESS` until it passes.

## Validation

- Passed: `env JAVA_HOME=/home/khuongnd/.local/opt/jdk17
  ANDROID_HOME=/home/khuongnd/Android/Sdk ./gradlew test lint assembleDebug`
  (`BUILD SUCCESSFUL`, 150 tasks).
- Passed: `git diff --check`.
- APK SHA-256: app
  `a3a25c3e57f0e0e8bd352aacc5525788bc50a0fa521b961b30a19000c21cb426`;
  DPC `e899a415b419884336a744a4b3f8a5313d138e632884863a8dd17acb5ebbebf5`.
- `apksigner verify --print-certs` passed for both APKs; both use certificate
  SHA-256 `9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`.
- Windows `ZFold3_API35` (Google APIs x86_64) booted as Android 15/API 35;
  both APKs installed and MainActivity cold-launched without crash/ANR. The
  emulator reported CLOSED/HALF_OPENED/OPENED but kept 1768x2208 guest metrics.
  Device Owner and runtime permission checks passed before the final clean reset.
