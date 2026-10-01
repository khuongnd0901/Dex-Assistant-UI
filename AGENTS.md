# Dex Assistant UI agent guide

This repository contains the Android client. The canonical backend and protocol
repository is `https://github.com/khuongnd0901/DeX-Assistant` and is commonly
checked out locally at `/mnt/d/dex-assistant`.

Before changing WebSocket messages, authentication, or action/result types,
inspect the backend's `docs/PROTOCOL.md` and corresponding protocol tests. Keep
tokens, signing material, local SDK paths, and environment-specific endpoints
out of Git.

Use JDK 17 and the Gradle wrapper. The baseline targets Android 15/API 35 and
supports API 30+, pending validation against the actual Galaxy Z Fold3.
