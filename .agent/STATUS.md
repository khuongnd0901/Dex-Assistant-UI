# Android repository status

Last synchronized: 2026-10-01

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

## Relevant canonical tasks

- Backend `T-004` is `IN_PROGRESS`: owner decisions are accepted; actual Fold3
  firmware and Device Owner evidence remain.
- Backend `T-003` is `IN_PROGRESS`: automated implementation is complete, while
  on-device provisioning, actions, and latency remain unverified.
- Backend `T-005` establishes the main/backend/Android agent workflow.

## Blockers

No Fold3 is visible through ADB in the current environment. Device Owner
provisioning can require a clean/factory-reset device and is the remaining
acceptance blocker.

## Next action

Back up and provision the dedicated Fold3, install DPC before the main app, run
the remote FakePlanner round trip for launch and Wi-Fi on/off, and capture API,
owner state, results, and latency evidence.
