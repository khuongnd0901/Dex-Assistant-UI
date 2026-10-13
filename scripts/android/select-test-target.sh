#!/usr/bin/env bash
set -euo pipefail

# Select a connected Galaxy Z Fold3, or start an isolated Windows API 35 AVD.
# This script selects a target only; it does not install apps or change the phone.
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
sdk_root="/mnt/d/Android/Sdk"
adb="$sdk_root/platform-tools/adb.exe"
adb_port=5038
linux_adb="/home/khuongnd/Android/Sdk/platform-tools/adb"
avd_home="D:\Android\AVD"
profile="${1:-dex}"
emulator_only=false
if [[ "${2:-}" == --emulator-only && $# == 2 ]]; then
  emulator_only=true
elif [[ $# -gt 1 ]]; then
  echo "Usage: $0 [dex|foldable] [--emulator-only]" >&2
  exit 2
fi
case "$profile" in
  dex) avd_name="DeX_Command_Test_API35"; start_script="start-dex-test.ps1" ;;
  foldable) avd_name="ZFold3_API35"; start_script="start-zfold3.ps1" ;;
  *) echo "Usage: $0 [dex|foldable] [--emulator-only]" >&2; exit 2 ;;
esac
[[ -x "$adb" ]] || { echo "Missing Windows ADB: $adb" >&2; exit 1; }

# A physical Fold3 may be visible only to the Linux ADB server in WSL.
if [[ "$emulator_only" == false && -x "$linux_adb" ]]; then
  while read -r candidate state detail; do
    [[ "$state" == device && "$candidate" != emulator-* ]] || continue
    model="$("$linux_adb" -s "$candidate" shell getprop ro.product.model 2>/dev/null | tr -d '\r')"
    api="$("$linux_adb" -s "$candidate" shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')"
    if [[ "$model" == SM-F926* && "$api" == 35 ]]; then
      printf 'TARGET=Fold3 ADB=linux SERIAL=%s API=35\n' "$candidate"
      exit 0
    fi
  done < <("$linux_adb" devices -l | tr -d '\r' | tail -n +2)
fi

find_target() {
  local wanted="$1" serial state detail name model api
  while read -r serial state detail; do
    [[ "$state" == device ]] || continue
    if [[ "$wanted" == avd && "$serial" != emulator-* && "$serial" != 127.0.0.1:* ]]; then
      continue
    fi
    api="$("$adb" -P "$adb_port" -s "$serial" shell getprop ro.build.version.sdk 2>/dev/null | tr -d '\r')"
    [[ "$api" == 35 ]] || continue
    if [[ "$wanted" == phone && "$serial" != emulator-* ]]; then
      model="$("$adb" -P "$adb_port" -s "$serial" shell getprop ro.product.model 2>/dev/null | tr -d '\r')"
      [[ "$model" == SM-F926* ]] && { printf '%s\n' "$serial"; return; }
    elif [[ "$wanted" == avd && ( "$serial" == emulator-* || "$serial" == 127.0.0.1:* ) ]]; then
      name="$("$adb" -P "$adb_port" -s "$serial" shell getprop ro.boot.qemu.avd_name 2>/dev/null | tr -d '\r')"
      [[ -n "$name" ]] || name="$("$adb" -P "$adb_port" -s "$serial" emu avd name 2>/dev/null | tr -d '\r' | head -n 1)"
      [[ "$name" == "$avd_name" ]] && { printf '%s\n' "$serial"; return; }
    fi
  done < <("$adb" -P "$adb_port" devices -l | tr -d '\r' | tail -n +2)
  return 0
}

if [[ "$emulator_only" == false ]]; then
  serial="$(find_target phone)"
  if [[ -n "$serial" ]]; then
    printf 'TARGET=Fold3 ADB=windows PORT=5038 SERIAL=%s API=35\n' "$serial"
    exit 0
  fi
fi

"$adb" -P "$adb_port" connect 127.0.0.1:5555 >/dev/null 2>&1 || true
serial="$(find_target avd)"
if [[ -z "$serial" ]]; then
  command -v powershell.exe >/dev/null || { echo 'Windows PowerShell is unavailable' >&2; exit 1; }
  if [[ "$profile" == dex ]]; then
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$(wslpath -w "$script_dir/$start_script")" \
      -SdkRoot 'D:\Android\Sdk' -AvdHome "$avd_home" -JavaHome 'D:\ms-21.0.9' >&2
  else
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$(wslpath -w "$script_dir/$start_script")" >&2
  fi
  "$adb" -P "$adb_port" connect 127.0.0.1:5555 >/dev/null 2>&1 || true
  serial="$(find_target avd)"
fi
[[ -n "$serial" ]] || { echo "AVD $avd_name is unavailable in Windows ADB" >&2; exit 1; }
printf 'TARGET=AVD ADB=windows PORT=5038 PROFILE=%s AVD=%s SERIAL=%s API=35\n' "$profile" "$avd_name" "$serial"
