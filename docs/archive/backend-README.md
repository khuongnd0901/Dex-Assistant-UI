> Retired backend project document; preserved only as historical provenance. Current Android workspace authority is [AGENTS](../../AGENTS.md) and [STATUS](../../.agent/STATUS.md).

# DeX Assistant — Legacy Backend and Architecture Archive

**English** · [Tiếng Việt](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/README.vi.md)

This repository contains the **historical Java/Spring Boot backend**, protocol contracts, fixtures, test harnesses and project-agent plans for **DeX Assistant**.

> **Current architecture — October 9, 2026:** The maintained Android app in [**Dex-Assistant-UI**](https://github.com/khuongnd0901/Dex-Assistant-UI) now has source code for **local-first Command, direct AI Command and direct AI Chat**, without a required Spring Boot/WebSocket proxy. **This Java backend is no longer part of the Android app's active Command/Chat runtime.** The repository remains available for legacy tests, schema history, migration evidence and potential source-level rollback. **Physical Samsung Z Fold3/DeX acceptance is still OPEN**, and removing source dependencies does not automatically stop a deployed EC2 instance or other billable infrastructure.

[Android README (English)](https://github.com/khuongnd0901/Dex-Assistant-UI/blob/main/README.md) · [Android README (Tiếng Việt)](https://github.com/khuongnd0901/Dex-Assistant-UI/blob/main/README.vi.md)

## What is in this repository?

The retained backend implementation provides the former device-control transport:

- **Java 25 / Spring Boot 4.0.8** application and Spring AI integrations.
- Authenticated **JSON WebSocket** protocol, typed device execution plans, Java validation and command contracts.
- Historical **Gemini/OpenAI provider adapters**, metered provider proxy and local/H2 quota tracking.
- Test fixtures for command schemas, capability filtering, WebSocket handling, Chat and quotas.
- Architectural decisions, milestones, test evidence and the current migration / physical-acceptance plan.

These capabilities can still be **built and tested as legacy components**. They are **not Android installation prerequisites** and are not automatically called by the current app.

## Architecture before and after

### Former implementation (retained here)

```text
Android → authenticated WebSocket → Spring Boot → AI provider
                                               ↓
                                        typed execution plan
                                               ↓
                                          Android executor
```

### Current Android source

```text
Voice/STT → Android Command Router
                ├── matched local command → Android ActionRouter
                └── AI-required command  → Android MobileAiProxy
                                             ├── Gemini REST
                                             └── Groq API
                                                   ↓
                                         typed, validated actions
                                                   ↓
                                             ActionRouter

Chat → Android DirectAiChat → Gemini/Groq (text-only, no device tools)
```

The Android project's legacy enum `BACKEND_REQUIRED` denotes **non-local, AI-requiring intent**, even though the current execution path no longer opens a backend WebSocket. Old Java contracts and their Android protocol reference files remain useful for tests and audits.

## AI provider support in the Android app

| Provider | Configure API key and default model in Android | Direct Command/Chat adapter |
|---|---|---|
| Gemini | Yes | Implemented in Android |
| Groq (GroqCloud) | Yes | Implemented in Android |
| OpenAI (ChatGPT API) | Yes | Not yet wired to Android Command/Chat |
| Grok (xAI) | Yes | Not yet wired |
| DeepSeek | Yes | Not yet wired |

**Groq and Grok are separate providers.** Keys are entered through Android Settings and stored by the Android Keystore; **no backend environment variable or server-side secret deployment is needed for the direct Android flow**. Free Tier is model/account dependent and must be explicitly verified; listing a model does not prove it is free. The current Android code does not silently call paid or unsupported adapters.

See the Android [English](https://github.com/khuongnd0901/Dex-Assistant-UI/blob/main/README.md) or [Vietnamese](https://github.com/khuongnd0901/Dex-Assistant-UI/blob/main/README.vi.md) README for current setup and operation.

## Build and run *legacy backend tests only*

Requirements: **JDK 25**, Maven wrapper and the dependencies declared in `pom.xml`.

```bash
./mvnw verify
```

Historical server profiles, token provisioning, Docker Compose, loopback WebSocket tests and AI quota setup are documented in the [archived original README](backend-README_legacy_2026-10-09.md). **Those procedures target the old backend client/protocol and do not configure the current Android app.** Never copy API keys, bearer tokens, credentials, keystores or private data into source control.

Useful references:

| Document | Purpose |
|---|---|
| [`../PROTOCOL.md`](../PROTOCOL.md) | Historic WebSocket messages and correlated execution |
| [`../DEVICE_COMMANDS_V1.md`](../DEVICE_COMMANDS_V1.md) | Shared typed-action V1 contract |
| [`../DEVICE_COMMANDS_V2.md`](../DEVICE_COMMANDS_V2.md) | Additional/semantic contract reference |
| [`https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/docs/contract-fixtures`](https://github.com/khuongnd0901/DeX-Assistant/blob/7d3611e0f505d4750e34f0c47fd873312fb1efb4/docs/contract-fixtures) | Java/Android cross-repository fixtures |
| [`../../.agent/STATUS.md`](../../.agent/STATUS.md) | Historical project status; includes older backend-era snapshots |
| [`../../.agent/plans/proposed/RFC-2026-10-09-mobile-managed-ai.md`](../../.agent/plans/proposed/RFC-2026-10-09-mobile-managed-ai.md) | Direct API migration decisions (read later changes/evidence as well) |
| [`../../.agent/evidence/mobile-direct-M4-command-chat-backendless-2026-10-09.md`](../../.agent/evidence/mobile-direct-M4-command-chat-backendless-2026-10-09.md) | Published source migration and unverified gates |
| [`../../.agent/plans/active/T-018-direct-ai-final-fold3-acceptance.md`](../../.agent/plans/active/T-018-direct-ai-final-fold3-acceptance.md) | Final controlled Fold3 hardware test |

## Migration / decommissioning status

| Item | Status as of 2026-10-09 |
|---|---|
| Backend removed from active Android Command source path | **Implemented in source** |
| Backend removed from active Android Chat source path | **Implemented in source** |
| Android build, unit tests and lint | **PASS** — [CI run `37878690108`](https://github.com/khuongnd0901/Dex-Assistant-UI/actions/runs/37878690108), commit `3715b36` |
| Live account-key direct API inference / tool compatibility | **Not accepted yet** |
| Final signed Galaxy Z Fold3 + DeX one-shot regression | **OPEN — T-018** |
| EC2/EBS/DNS/other deployed-resource shutdown | **Not performed or verified by this repository change** |

**Do not retire or delete deployed infrastructure solely because the source no longer needs it.** First pass the real-device acceptance gate and obtain explicit authorization for production-resource changes. Preserve matching Android/DPC signing identities, app data, Device Owner, Accessibility and user credentials during tests.
