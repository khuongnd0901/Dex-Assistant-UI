#!/usr/bin/env bash
# Read-only physical Z Fold3/API35 preflight. No APK installs, permission/role changes or secrets.
set -euo pipefail
ADB=${ADB:-adb}
if [ -z "${ANDROID_SERIAL:-}" ]; then echo 'Set ANDROID_SERIAL to the physical Fold3 serial'; exit 2; fi
SERIAL="$ANDROID_SERIAL"
"$ADB" -s "$SERIAL" get-state | grep -qx device || exit 2
MODEL=$("$ADB" -s "$SERIAL" shell getprop ro.product.model | tr -d '\r')
API=$("$ADB" -s "$SERIAL" shell getprop ro.build.version.sdk | tr -d '\r')
case "$MODEL" in SM-F926*) ;; *) echo "Expected Fold3, got $MODEL"; exit 2 ;; esac
[ "$API" = 35 ] || { echo "Expected API35, got $API"; exit 2; }
OUT=${EVIDENCE_FILE:-$HOME/.local/share/dex-benchmarks/fold3-direct-ai-preflight.txt}
mkdir -p "$(dirname "$OUT")"
umask 077
{
 echo "Physical model: $MODEL / API: $API"
 echo 'Assistant role:'
 "$ADB" -s "$SERIAL" shell cmd role get-role-holders --user 0 android.app.role.ASSISTANT || true
 echo 'App package path:'
 "$ADB" -s "$SERIAL" shell pm path com.khuongnd.dexassistant.ui || true
 echo 'Display descriptors:'
 "$ADB" -s "$SERIAL" shell dumpsys display | grep -E -m 6 'SAMSUNG_DESKTOP|displayId=' || true
} > "$OUT"
echo "Read-only preflight output saved locally: $OUT"
echo 'Run T-018 controlled manual voice/direct AI Chat + Command acceptance after confirming matching certificate.'
echo 'This does not establish physical device acceptance on its own.'
