# Dex Assistant UI

Android client for DeX Assistant. The project is a Kotlin/Jetpack Compose base
targeting Android 15 (API 35), with API 30 as the minimum supported version for
the Galaxy Z Fold3 product scope.

The Spring Boot backend and canonical protocol are maintained in the private
[`DeX-Assistant`](https://github.com/khuongnd0901/DeX-Assistant) repository.

## Prerequisites

- JDK 17
- Android SDK Platform 35
- Android SDK Build Tools

## Build and test

```bash
./gradlew test
./gradlew assembleDebug
```

On Windows, use `gradlew.bat` instead. Open the repository root in Android
Studio if the SDK path needs to be configured through `local.properties`.

## Security

Do not commit backend bearer tokens, signing keys, `local.properties`, or
environment-specific server URLs. Authentication provisioning and the
WebSocket protocol will be implemented after their Android-side contracts are
accepted.
