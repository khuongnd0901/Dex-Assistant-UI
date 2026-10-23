# DeX Assistant — Android

## ChatGPT Subscription (T-025)

AI Command and Chat use the account's **ChatGPT subscription** through native OAuth
and the public Responses endpoint. Gemini/Groq and other API-key adapters and their
settings UI have been removed. Existing encrypted API keys are left untouched and
are never used as fallback. No backend, Codex CLI or copied login files are required.

Open **More (⋮) → Settings**, choose **Continue with ChatGPT**, load and select an
account model, run **Test inference** and **Test function calling** (no action executes),
then explicitly select Subscription for Chat/AI Command. Account/model proofs and
Android action confirmation remain required. Browser login starts only from Settings.
Subscription requests use the account's shared limits; this is not unlimited API access.

Native OAuth, inference and structured planning previously passed on Fold3. Full
natural voice, interruption and performance acceptance remains **IN_PROGRESS**.
Disconnect clears subscription tokens while retaining host/registration identity;
if revocation cannot be confirmed, disconnect the app in ChatGPT Settings. Saved data
and legacy preferences are preserved; an old Gemini selection prompts subscription
setup instead of making an API-key request. See [continuation evidence](.agent/evidence/T-025-subscription-only-2026-10-23.md)
and the [ExecPlan](.agent/plans/active/T-025-chatgpt-subscription-migration.md).
Adapted authentication retains its [MIT notice](third-party/siwc-poc-LICENSE).

**English** · [Tiếng Việt](README.vi.md)

Single-user, voice-first Android assistant designed for the **Samsung Galaxy Z Fold3 using Samsung DeX on an external display**. The current app is **local-first**, with direct AI provider calls for requests that need language understanding. **Spring Boot, a custom WebSocket proxy, Firebase and an EC2 instance are not required by the current Android Command/Chat runtime.**

> **2026-10-13:** This Android repository is the canonical development/test workspace. Backend development is retired. Start with [AGENTS](AGENTS.md) and [current status](.agent/STATUS.md); open [Android workspace](Dex-Assistant.code-workspace). T-021 Windows simulator validation passed; actual Fold3/DeX gates remain IN_PROGRESS in T-020. No cloud resources were deleted.

## What it does

- **Offline wake word:** "Hey Hey" with locally packaged sherpa-onnx / KWS assets. Vietnamese speech-to-text uses Android's `SpeechRecognizer` and can require network access or send audio to the configured recognition service. Offline wake detection does **not** mean fully offline speech recognition.
- **Local commands first:** A config-based Vietnamese command catalog matches commands for apps, volume, media, YouTube, Maps/navigation, screen actions and other supported device operations. Recognized local commands do not call an AI provider.
- **Direct AI Command:** Unmatched/complex commands go from Android to a configured eligible AI provider via `ChatGptSubscriptionProvider`. Provider function calls are decoded and validated into typed commands before reaching the **same Android `ActionRouter`**. Unsupported actions, bad parameters and unsafe requests are rejected; the model cannot bypass device permissions or confirmation.
- **Direct AI Chat:** Say **"bắt đầu trò chuyện"** to enter conversation mode, and **"kết thúc trò chuyện"** to leave it. Android owns the isolated in-memory session; `ChatGptSubscriptionProvider` sends only text/context, **without device action tools**. The session retains up to 20 prior turn pairs, uses bounded input/output and expires after 15 minutes of inactivity.
- **DeX-oriented interaction:** Accessibility-assisted controls and a numbered 6 × 4 screen grid for cases where app UI nodes are unavailable. The design operates on the external DeX display, not as a dual-screen phone-controller workflow.
- **Device Policy Controller (DPC):** A separate Android module for narrowly guarded device operations, subject to actual Android permissions and Device Owner setup.

## Runtime architecture

```text
Voice / mic → local KWS ("Hey Hey") → Vietnamese STT → Command Router
                                                    │
                ┌───────────────────────────────────┼───────────────────┐
                │                                   │                   │
             LOCAL                              AI required          Rejected
                │                                   │
       LocalCommandEngine                  Subscription provider (Android)
                │                                   │
                │                         ChatGPT OAuth / Responses SSE
                │                                   │
                │                       strict function-call parsing
                │                                   │
                └──────────────→ ActionRouter ←─────┘
                                   │
                      capability / confirmation / DPC
                                   │
                               Android action

Chat start → Android-owned chat session → Subscription → Responses SSE
                       │
                  text + bounded context only
                  NO device tools / backend WebSocket
```

`BACKEND_REQUIRED` remains the **name of a legacy routing enum value** for non-local commands; it no longer means an actual backend network call. Some WebSocket/protocol files remain in the repository as **inactive legacy/reference code**.

## AI connection

Only **ChatGPT Subscription** is supported. Models come from the signed-in account's
catalog; a catalog entry alone does not prove inference or function-call entitlement.
Failed, incomplete, malformed or cancelled streams never execute actions or enter
Chat history. Quota/auth/network errors are surfaced without a provider fallback.
Local commands remain independent of AI; Android speech recognition can need network.

## Repository layout

| Location | Responsibility |
|---|---|
| `app/` | Android Kotlin + Jetpack Compose app; voice, Settings, routing, AI and guarded actions |
| `dpc/` | Companion Device Policy Controller, separately installed/signed APK |
| `app/src/main/java/com/khuongnd/dexassistant/ui/ai/` | `ChatGptSubscriptionProvider`, Responses SSE, typed tools, volatile Chat |
| `app/src/main/java/com/khuongnd/dexassistant/ui/security/ChatGptSettings.kt` | Subscription selection/model/proofs; OAuth tokens live separately in `auth/` |
| `app/src/main/java/com/khuongnd/dexassistant/ui/routing/` | Local command matching and privacy restrictions |
| `app/src/main/java/com/khuongnd/dexassistant/ui/execution/ActionRouter.kt` | Device execution and confirmation authority |
| `scripts/prepare-voice-assets.py` | Pinned/hash-verified build-time voice assets |
| `scripts/android/check-direct-ai-fold3.sh` | **Read-only** physical Fold3/API35 ADB preflight |
| `.agent/STATUS.md` | Development history and outstanding acceptance tasks |

## Requirements, build and CI

- **JDK 17**, **Android SDK Platform 35 / Build Tools 35**, Python 3.10+ and Gradle wrapper.
- Android `minSdk 30`, `targetSdk 35` (Android 15 for the dedicated Fold3).
- One-time checksum-verified voice assets, staged in ignored `.cache/voice`; no runtime model download for wake-word recognition.

```bash
python3 scripts/prepare-voice-assets.py
./gradlew --no-daemon --build-cache testDebugUnitTest lint assembleDebug
```

On Windows, use `gradlew.bat` in place of `./gradlew`. Build outputs:

- `app/build/outputs/apk/debug/app-debug.apk`
- `dpc/build/outputs/apk/debug/dpc-debug.apk`

[Android CI](.github/workflows/android-ci.yml) performs the build, unit tests, lint and uploads debug artifacts. The separate [Device-test release workflow](.github/workflows/device-test-release.yml) uses a **verified existing matching signing identity** for dedicated Fold3 upgrades. **Do not install an arbitrary CI/debug-signed APK over a provisioned Device Owner device.** Do not uninstall, clear app data, reset Device Owner or toggle Accessibility to make a test pass.

## Real-device acceptance — not completed

A final **single controlled Samsung Z Fold3 / DeX acceptance run** remains open. It must verify the existing signing certificate and retained device state, local/offline routing, subscription Command, three-turn Vietnamese Chat and cancellation, provider failures, wake/STT/TTS, Device Owner/Accessibility, privacy and p50/p95 timing. No physical PASS or zero-cost usage result is claimed by repository CI.

- [T-018 physical acceptance plan (local canonical copy)](.agent/plans/active/T-018-direct-ai-final-fold3-acceptance.md)
- [Migration evidence](.agent/evidence/mobile-direct-M4-command-chat-backendless-2026-10-09.md)
- [Prior device/voice setup notes](docs/README_legacy_2026-10-09.md) — **historical; includes obsolete backend instructions**

The former [Java/Spring Boot backend repository](https://github.com/khuongnd0901/DeX-Assistant) is retired and frozen for historical source provenance. **Do not assume the deployed EC2/EBS/DNS resources have already been shut down.**

Development/test entry point: [.agent/STATUS.md](.agent/STATUS.md). Windows simulator selection from this root: `bash scripts/android/select-test-target.sh dex --emulator-only`. [Local test tooling](scripts/android/README.md), [T-021 evidence](.agent/evidence/T-021-windows-response-performance-2026-10-13.md) and [T-020 physical campaign](.agent/plans/active/T-020-consolidated-fold3-acceptance.md) are canonical here.

## License

[MIT](LICENSE) © 2026 Khuong Duy Nguyen. Third-party components and model assets retain their respective licenses and notices.
