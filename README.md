# DeX Assistant — Android

## ChatGPT Subscription migration (T-025)

The feature branch adds native **Continue with ChatGPT**, independent OAuth PKCE/OIDC,
Keystore-encrypted session storage, authorized model discovery and an explicit inference
test. ChatGPT Subscription is separate from the OpenAI API-key provider. No backend,
CLI, copied credentials or separate OpenAI API key is required for this connection.
Subscription requests consume the consenting account's plan allowance.

**IN_PROGRESS:** target-app authentication, inference, real function calling and Fold3
voice/security/performance acceptance are pending. Default subscription routing and
subscription device commands remain locked. Gemini/Groq and saved keys remain intact;
local commands continue unchanged. In AI Providers, choose ChatGPT Subscription,
explicitly sign in while safely stationary, load/select an account model and run the
harmless inference test. The browser never launches from a voice command.

Rollback: retain/select the existing Gemini/Groq provider and its configured model;
no automatic subscription-to-API fallback is added. Disconnect clears subscription
credentials while retaining its installation/registration identity. If remote revocation
cannot be confirmed, disconnect the app in ChatGPT Settings. Do not uninstall or clear
app data. See the [migration report](.agent/evidence/T-025-chatgpt-subscription-migration.md)
and [ExecPlan](.agent/plans/active/T-025-chatgpt-subscription-migration.md) for gates and
limitations. Adapted POC code retains its [MIT notice](third-party/siwc-poc-LICENSE).


**English** · [Tiếng Việt](README.vi.md)

Single-user, voice-first Android assistant designed for the **Samsung Galaxy Z Fold3 using Samsung DeX on an external display**. The current app is **local-first**, with direct AI provider calls for requests that need language understanding. **Spring Boot, a custom WebSocket proxy, Firebase and an EC2 instance are not required by the current Android Command/Chat runtime.**

> **2026-10-13:** This Android repository is the canonical development/test workspace. Backend development is retired. Start with [AGENTS](AGENTS.md) and [current status](.agent/STATUS.md); open [Android workspace](Dex-Assistant.code-workspace). T-021 Windows simulator validation passed; actual Fold3/DeX gates remain IN_PROGRESS in T-020. No cloud resources were deleted.

## What it does

- **Offline wake word:** "Hey Hey" with locally packaged sherpa-onnx / KWS assets. Vietnamese speech-to-text uses Android's `SpeechRecognizer` and can require network access or send audio to the configured recognition service. Offline wake detection does **not** mean fully offline speech recognition.
- **Local commands first:** A config-based Vietnamese command catalog matches commands for apps, volume, media, YouTube, Maps/navigation, screen actions and other supported device operations. Recognized local commands do not call an AI provider.
- **Direct AI Command:** Unmatched/complex commands go from Android to a configured eligible AI provider via `MobileAiProxy`. Provider function calls are decoded and validated into typed commands before reaching the **same Android `ActionRouter`**. Unsupported actions, bad parameters and unsafe requests are rejected; the model cannot bypass device permissions or confirmation.
- **Direct AI Chat:** Say **"bắt đầu trò chuyện"** to enter conversation mode, and **"kết thúc trò chuyện"** to leave it. Android owns the isolated in-memory session; `DirectAiChat` sends only text/context, **without device action tools**. The session retains up to 20 prior turn pairs, uses bounded input/output and expires after 15 minutes of inactivity.
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
       LocalCommandEngine                  MobileAiProxy (Android)
                │                                   │
                │                         Gemini REST / Groq API
                │                                   │
                │                       strict function-call parsing
                │                                   │
                └──────────────→ ActionRouter ←─────┘
                                   │
                      capability / confirmation / DPC
                                   │
                               Android action

Chat start → Android-owned chat session → DirectAiChat → Gemini / Groq
                       │
                  text + bounded context only
                  NO device tools / backend WebSocket
```

`BACKEND_REQUIRED` remains the **name of a legacy routing enum value** for non-local commands; it no longer means an actual backend network call. Some WebSocket/protocol files remain in the repository as **inactive legacy/reference code**.

## AI providers — configuration and runtime support

| Provider | Configure key / enable / default model | List account-visible models | Direct Command | Direct Chat |
|---|---|---|---|---|
| **Gemini** (Google) | Yes | Yes | Implemented | Implemented |
| **Groq** (GroqCloud) | Yes | Yes | Implemented | Implemented |
| **OpenAI** (ChatGPT API) | Yes | Yes | Not yet wired | Not yet wired |
| **Grok** (xAI) | Yes | Yes | Not yet wired | Not yet wired |
| **DeepSeek** | Yes | Yes | Not yet wired | Not yet wired |

**Grok (xAI) and Groq (GroqCloud) are different providers.** A model appearing in the API's model list **does not imply free access, tool-calling support or zero billing**. The current direct adapters are limited to Gemini and Groq, and sending requests requires a configured key, enabled provider, selected model and **explicit confirmation that the model is eligible for a free tier on your account**. If the selected preferred provider has no implemented adapter, the app fails closed instead of silently using another or paid provider. Provider quotas, price plans and free-model availability can change.

### Configure API keys in the Android UI

Open **More (⋮) → Settings → AI Providers**:

1. Choose **Gemini / Groq / OpenAI / Grok / DeepSeek**.
2. Enter the provider's key in the masked **API key** field and tap **Save / Update**. You can **Test API key** (an authenticated model-list request with no AI inference) or **Delete key**.
3. Use **Load models from API**, select or enter a **default Model ID for that provider**, then **Save model**.
4. Enable the provider and, only after verifying account-specific pricing, tick the **Free Tier confirmation** for the saved model.
5. Choose **Set as preferred provider**. For live Command and Chat, choose **Gemini or Groq** until the other adapters are implemented.

Keys are **entered at runtime** and stored in separate provider-scoped **Android Keystore AES-GCM** entries. They are **not hardcoded into source, the APK, BuildConfig, CI or Git**; changing a key/model does not require rebuilding the app. Like any client-side credential, a key may still be extracted from a compromised/running device; the project owner explicitly accepts this risk. Keep provider billing disabled or restricted if zero cost is essential.

If a key/model is absent, the provider is disabled, a quota is reached or a network call fails, AI features can return a visible error. **Local commands remain independent of the AI API** (but STT may still need network). No Spring Boot fallback is provided in the current runtime.

## Repository layout

| Location | Responsibility |
|---|---|
| `app/` | Android Kotlin + Jetpack Compose app; voice, Settings, routing, AI and guarded actions |
| `dpc/` | Companion Device Policy Controller, separately installed/signed APK |
| `app/src/main/java/com/khuongnd/dexassistant/ui/ai/` | `MobileAiProxy`, `DirectAiChat`, `AiKeyProbe`, tool schemas |
| `app/src/main/java/com/khuongnd/dexassistant/ui/security/AiCredentialStore.kt` | Per-provider credential and preference storage |
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

A final **single controlled Samsung Z Fold3 / DeX acceptance run** remains open. It must verify the existing signing certificate and retained device state, local/offline routing, actual direct Gemini/Groq Command, three-turn Vietnamese Chat and cancellation, provider failures, wake/STT/TTS, Device Owner/Accessibility, privacy and p50/p95 timing. No physical PASS or zero-cost usage result is claimed by repository CI.

- [T-018 physical acceptance plan (local canonical copy)](.agent/plans/active/T-018-direct-ai-final-fold3-acceptance.md)
- [Migration evidence](.agent/evidence/mobile-direct-M4-command-chat-backendless-2026-10-09.md)
- [Prior device/voice setup notes](docs/README_legacy_2026-10-09.md) — **historical; includes obsolete backend instructions**

The former [Java/Spring Boot backend repository](https://github.com/khuongnd0901/DeX-Assistant) is retired and frozen for historical source provenance. **Do not assume the deployed EC2/EBS/DNS resources have already been shut down.**

Development/test entry point: [.agent/STATUS.md](.agent/STATUS.md). Windows simulator selection from this root: `bash scripts/android/select-test-target.sh dex --emulator-only`. [Local test tooling](scripts/android/README.md), [T-021 evidence](.agent/evidence/T-021-windows-response-performance-2026-10-13.md) and [T-020 physical campaign](.agent/plans/active/T-020-consolidated-fold3-acceptance.md) are canonical here.

## License

[MIT](LICENSE) © 2026 Khuong Duy Nguyen. Third-party components and model assets retain their respective licenses and notices.
