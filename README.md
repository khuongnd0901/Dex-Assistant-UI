# Dex Assistant UI

Android client for DeX Assistant. The project contains a Kotlin/Jetpack Compose
client plus a separate Device Policy Controller. Both target Android 15 (API
35), with API 30 as the minimum supported version for the Galaxy Z Fold3.

The Spring Boot backend and canonical protocol are maintained in the private
[`DeX-Assistant`](https://github.com/khuongnd0901/DeX-Assistant) repository.

## Prerequisites

- JDK 17
- Android SDK Platform 35
- Android SDK Build Tools

## Build and test

```bash
./gradlew test lint assembleDebug
```

On Windows, use `gradlew.bat` instead. Open the repository root in Android
Studio if the SDK path needs to be configured through `local.properties`.

Debug APKs are produced at `app/build/outputs/apk/debug/app-debug.apk` and
`dpc/build/outputs/apk/debug/dpc-debug.apk`.

## Dedicated-device setup

Device Owner provisioning can reset or constrain a device. Use only the
dedicated Fold3 and back it up first. For a clean development device with no
accounts, install the DPC before the main app and provision it through ADB:

```bash
adb install dpc/build/outputs/apk/debug/dpc-debug.apk
adb shell dpm set-device-owner \
  com.khuongnd.dexassistant.dpc/.DexDeviceAdminReceiver
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell getprop ro.build.version.sdk
adb shell getprop ro.build.version.release
adb shell dpm list-owners
```

Start the backend with a generated token digest and remote fake execution:

```bash
DEX_DEVICE_TOKEN_SHA256=<sha256> DEX_FAKE_REMOTE_EXECUTION=true ./mvnw spring-boot:run
adb reverse tcp:8080 tcp:8080
```

Provision `ws://127.0.0.1:8080/ws/v1/assistant` and the corresponding raw token
in the app. Cleartext WebSocket is restricted to loopback; other endpoints must
use WSS.

## Security

Do not commit backend bearer tokens, signing keys, `local.properties`, or
environment-specific server URLs. The main app wraps its bearer token with an
AndroidKeyStore AES-GCM key and excludes app data from backup. The DPC has no
backend credential or network permission; its exported service requires a
signature-level permission, so both APKs must retain the same signing identity.
