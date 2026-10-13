> Migrated to Android on 2026-10-13 ([D-037](../../DECISIONS.md#d-037--android-canonical-workspace-backend-retired)). Backend-only instructions, paths and commands below are historical and superseded; they are not current prerequisites. Task IDs, measured evidence and unfinished Android/Fold3 gates are retained.

# T-019 — Speech-aware microphone selection

Status: **CANCELLED** by owner on2026-10-10. Remaining implementation/acceptance milestones are withdrawn. Retain historical findings and existing prototype for source publication; cancellation does not establish speech selection, provider handoff or physical multi-mic acceptance.

## Outcome and boundaries
Default to Fold3 built-in microphone, then use the microphone carrying speech when multiple inputs are present. Owner asks whether all external/Bluetooth inputs can be listened to concurrently: verify actual support and explicitly distinguish sequential selection from simultaneous independent capture. Android-only implementation; no backend or provider changes. Preserve prior unfinished acceptance tasks and owner data/settings.

## Inputs and constraints
Canonical T-019 acceptance in TASKS.md. Android source at /home/kunrg/Dex-Assistant-UI. Preserve wake/STT lifecycle, resource bounds, original signing and permissions. Multiple AudioRecord instances do not by themselves prove independent hardware capture.

## Decision questions and evidence
Verify official Android input routing/concurrent capture and SpeechRecognizer audio-source contracts. Record actual routed devices and silencing before claiming multi-mic coverage. Speech energy alone is not voice detection. Selection must avoid oscillation and release all capture resources.

## Milestones
1. Inspect current capture and documented platform constraints.
2. Implement supported selection/fallback with meaningful regression tests.
3. Run JDK17 test/lint/APK build; physical checks where available.
4. Record evidence, limitations and next action.

## Validation
Android ./gradlew test lint assembleDebug assembleDebugAndroidTest --offline --max-workers=1; git diff --check. Physical flow: connect multiple mic inputs, speak near each, verify actual capture routing and wake/STT, disconnect input and repeat. Do not substitute synthetic evidence for natural speech.

## Risks and escalation
HAL may expose inputs but refuse concurrent independent capture; external recognizer may choose its own route. Fall back safely and report limitations. Avoid recording raw speech to persistent files.

## Progress and findings
- Task created; Android investigation/implementation delegated under main-agent role.
- Official docs reviewed2026-10-10: https://developer.android.com/media/platform/sharing-audio-input ; https://developer.android.com/reference/android/media/AudioRecord ; https://developer.android.com/reference/android/speech/RecognizerIntent#EXTRA_AUDIO_SOURCE . Preferred input is a request, actual routed device must be checked. Recognizer PCM support is provider-dependent.
- Read-only Fold3 API35 policy snapshot: currently available physical input ports18(bottom),20(back),21(2mic); no available USB input. Active capture uses bottom. Logical built-in ports do not establish independently selectable microphones. Owner subsequently attached external microphone; verified USB input port61, USB Composite Device, PCM16/16kHz/mono. Sequential route diagnostics can now run; Bluetooth hardware not yet confirmed.
- Prior D-018 prototype was deferred by D-019. T-019 authorizes revisiting local selection, while provider PCM support must be measured before enabling it.

## Decision and requirement updates
Record durable supported selection behavior and platform limits after evidence. Update task/status with exact validation and remaining hardware gates.

- Main coordination: selected-source PCM may be enabled only after known-source physical proof for the exact Google recognition component/version on API33+; unknown providers/versions retain explicit system-route fallback pending proof. This does not establish universal provider support.

- Intermediate frozen source full JDK17 test/lint/debug+test APK build PASS201tasks3m31s (`/tmp/t019-final-build.log`). Additional owner steering (builtin default, Bluetooth checks) and routing/provider/concurrent diagnostics still require final validation. No final acceptance implied.

- Cancelled-task publication checkpoint2026-10-10: owner requests existing source commit/push. Android snapshot `48d676d` includes sequential wake-routing and version-gated command PCM prototype plus tests. Exact isolated snapshot JDK17 full test/lint/app+test APK PASS201tasks9m50s; app190tests/variant (1skip each), DPC5/variant, Python6PASS. Hardware behavior is not accepted; no install. Later concurrent edits remain outside the snapshot.
