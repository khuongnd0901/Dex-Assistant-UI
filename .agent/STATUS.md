# Android repository status

Last synchronized: 2026-10-01

## Canonical project memory

Product-wide status, tasks, decisions, requirements, and protocol are owned by
the backend repository at `/mnt/d/dex-assistant`. Start cross-repository work
from its `.agent/STATUS.md`. This file is a concise Android handoff, not a second
canonical task board.

## Current state

- `main` contains the initial Kotlin/Jetpack Compose scaffold.
- Package: `com.khuongnd.dexassistant.ui`.
- Current build configuration: compile/target API 35, min API 30, JDK 17.
- The placeholder screen has no backend connection, authentication,
  provisioning, action execution, INTERNET permission, or reconnect behavior.
- Gradle wrapper 8.9 runs, but tests/build are unverified because Android SDK
  Platform 35 is not configured locally.

## Relevant canonical tasks

- Backend `T-004` is `IN_PROGRESS`: Android baseline proposals still require
  owner approval and actual Fold3 firmware/API evidence.
- Backend `T-003` is `BACKLOG`: Android client implementation must not proceed
  beyond the scaffold until T-004 is accepted.
- Backend `T-005` establishes the main/backend/Android agent workflow.

## Blockers and mismatch

The scaffold currently targets API 35, while the unaccepted T-004 candidate
baseline proposes compile/target API 36 and min API 30. Do not silently resolve
this mismatch in either direction. The main agent must coordinate the accepted
T-004 decision and then update this repository.

## Next action

Wait for T-004 owner review and device API evidence. Once accepted, let the main
agent create/activate the T-003 implementation plan and assign bounded Android
work with explicit protocol and validation criteria.
