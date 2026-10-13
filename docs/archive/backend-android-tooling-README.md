> Historical backend tooling guide. Provision/install/reset commands and omitted backend helpers are obsolete and unavailable in this Android migration. Use [current local guide](../../scripts/android/README.md). Current owner data/signing/roles must be preserved.

# Windows Fold3-like Android emulator

## Prerequisites

- Windows SDK: `D:\Android\Sdk`
- AVD storage: `D:\Android\AVD`
- AVD: `ZFold3_API35`, Android 15/API 35 Google APIs x86_64
- PowerShell and WSL interoperability enabled

## Chọn thiết bị test

Windows SDK đã có ở `D:\Android\Sdk`. `sdkmanager.bat` tại
`D:\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat` **cài/gỡ SDK**; ADB để
kiểm tra thiết bị là `D:\Android\Sdk\platform-tools\adb.exe`, còn chương trình
máy ảo là `D:\Android\Sdk\emulator\emulator.exe`. Từ WSL dùng các đường dẫn
`/mnt/d/Android/Sdk/...` tương ứng. Không lấy `sdkmanager.bat` làm lệnh ADB.

Trên máy WSL này, ADB Linux dùng cổng 5037; các script Windows dùng ADB server
riêng ở **5038** để `adb.exe` không kết nối nhầm. Lệnh tay tương đương:
`/mnt/d/Android/Sdk/platform-tools/adb.exe -P 5038 devices -l`. Trong
PowerShell, đặt `$env:ADB_SERVER_PORT='5038'` trước khi gọi `adb.exe`.

Chạy từ gốc repo trước khi test Android:

```bash
bash scripts/android/select-test-target.sh dex
```

Khi chỉ kiểm thử simulator, chạy `bash scripts/android/select-test-target.sh dex --emulator-only` (hoặc `foldable --emulator-only`). Chế độ này bỏ qua Fold3 trên cả ADB Linux và Windows, chỉ chọn AVD đúng tên/API 35; không truy vấn thiết bị thật.

Script ưu tiên Fold3 `SM-F926*` đang kết nối, Android 15/API 35. Nếu không có,
nó dùng hoặc khởi động AVD Windows `DeX_Command_Test_API35` và in `SERIAL`.
Chọn `foldable` thay `dex` để dùng `ZFold3_API35` cho kiểm thử gập/mở. Luôn
truyền serial đã in bằng `adb -s SERIAL ...` khi `ADB=linux`, hoặc
`adb.exe -P 5038 -s SERIAL ...` khi `ADB=windows`. ADB Linux dùng được cho
Fold3 mà WSL nhìn thấy; thử trực tiếp trên host này cho thấy nó không kết nối
được tới AVD Windows tại `127.0.0.1:5555`, nên AVD dùng ADB Windows. Script chỉ chọn/khởi động,
không cài APK lên Fold3. Nếu Fold3 được kết nối sau đó, chạy lại để chọn máy thật.
Trên một số lần khởi động, Windows ADB đăng ký AVD bằng serial loopback
`127.0.0.1:5555` thay vì `emulator-5554`. Bộ chọn xác minh tên AVD bằng
`ro.boot.qemu.avd_name` trước khi trả serial; luôn dùng đúng serial được in ra.

| Điều kiện gần Fold3 DeX | AVD thay thế | Giới hạn |
| --- | --- | --- |
| Android 15/API 35, Google APIs x86_64 | Cả hai AVD | Không có One UI/DeX thật |
| DeX ngoài màn hình 1920×1080, hai cửa sổ 960×1080 | `DeX_Command_Test_API35`: 1920×1080/160 dpi, landscape, freeform/resizable | Chỉ mô phỏng layout; không có Samsung desktop display ID/category |
| Màn hình trong Fold3 7,6 inch 1768×2208/420 dpi, bản lề | `ZFold3_API35`: generic 7.6in Foldable, trạng thái gập/mở | Vùng gập 884×2208 khác màn hình ngoài thật 832×2268; guest có thể không đổi metrics |

Không dùng kết quả AVD để đóng tiêu chí yêu cầu mic/nút USB thật, nhận dạng
giọng nói, DPC/One UI, hiển thị DeX được chọn hoặc hiệu năng Fold3. AVD có thể
kiểm tra build, parser, lifecycle, UI, freeform và các regression không phụ
thuộc Samsung. Dùng JDK 17 và Linux SDK `/home/khuongnd/Android/Sdk` khi chạy
Gradle trong WSL; Windows SDK ở D: dùng cho emulator/ADB. Không xóa dữ liệu
AVD hoặc cài đè app trên Fold3 nếu bài test không yêu cầu.

This is a Google APIs/AOSP-compatible foldable emulator using the 7.6-inch
fold-in profile. It is not Samsung firmware or One UI.

## Provision headless default assistant

After building both Android APKs, connect one clean API 35 device/AVD and run
`./scripts/android/provision-headless-assistant.sh`. The idempotent script
provisions the DPC, installs the main app, assigns `android.app.role.ASSISTANT`
with `cmd role`, verifies role/user/microphone permission/system binding, reboots, and
verifies persistence. It stops clearly if firmware rejects shell role assignment;
it never writes secure settings or clicks screen coordinates. Positional APK
arguments and `DEX_ASSISTANT_UI_DIR` can override the defaults.

### Test the published APKs on a Fold3 without building

The `fold3-device-test-2026-10-01` prerelease contains the two debug APKs built
from Android commit `9a34c2e`. The physical-device test computer runs Ubuntu
and already has working `adb`/`scrcpy` access to the Fold3 over Wi-Fi. Android
Studio, Gradle, the Android SDK platform, and the Android source repository are
not required.

1. While signed in to GitHub, open the
   [Fold3 device-test release](https://github.com/khuongnd0901/Dex-Assistant-UI/releases/tag/fold3-device-test-2026-10-01)
   and download both files into one directory:

   - `dex-assistant-dpc-debug.apk`
   - `dex-assistant-app-debug.apk`

2. On Ubuntu, verify that the downloads are the tested artifacts:

   ```bash
   cd ~/Downloads
   sha256sum dex-assistant-dpc-debug.apk dex-assistant-app-debug.apk
   ```

   Expected SHA-256 values:

   ```text
   17377af836a71fdafc184f33441f6fc0560ef07fadd6440f9fb19ea7074fa76b  dex-assistant-dpc-debug.apk
   d6ada73dca07a823e2833a61908244e891b6a8a6dc64896564052c84ccee07a9  dex-assistant-app-debug.apk
   ```

3. Confirm the existing wireless connection and copy the exact Fold3 serial
   shown in the first column:

   ```bash
   adb devices -l
   adb -s PHONE_IP:DEBUG_PORT get-state
   ```

   Continue only when the intended Fold3 is listed as `device`. If it is not
   connected, use the same Wireless debugging flow that was used for `scrcpy`:

   ```bash
   adb pair PHONE_IP:PAIRING_PORT
   adb connect PHONE_IP:DEBUG_PORT
   adb -s PHONE_IP:DEBUG_PORT get-state
   ```

   `PAIRING_PORT` and `DEBUG_PORT` are often different.

4. Clone only the backend repository, or copy its `scripts/android` directory,
   then run the existing provisioning script directly on Ubuntu. Pass the
   release APKs explicitly so the script cannot fall back to local build
   outputs:

   ```bash
   cd ~/path/to/dex-assistant
   export ANDROID_SERIAL='PHONE_IP:DEBUG_PORT'
   ADB="$(command -v adb)" \
     ./scripts/android/provision-headless-assistant.sh \
     "$HOME/Downloads/dex-assistant-dpc-debug.apk" \
     "$HOME/Downloads/dex-assistant-app-debug.apk"
   ```

   This command installs DPC first, provisions Device Owner, installs the main
   app, assigns the default-assistant role, verifies the microphone/role state,
   reboots the phone, and verifies again. It does not run Gradle or build an APK.

The phone may drop its wireless ADB connection during reboot. The script pins
`ro.serialno` from the explicitly selected device, then searches connected ADB
transports for that same identity. A changed kernel boot ID and completed boot
are required, so a briefly surviving pre-reboot transport cannot pass. The script
uses ADB’s reboot service; a command error or immediate transport disconnect
triggers the same bounded proof check rather than claiming reboot failure or
success before the new boot is observed. It can reconnect the selected mDNS service
instance when its port changes; unrelated mDNS peers are never connected. If
mDNS is unavailable, reconnect the same phone from another terminal using its
current debug port; the script discovers that transport and verifies its identity.
Without `ANDROID_SERIAL`, exactly one connected device is required.

Reconnection/boot and assistant readiness are bounded, respectively by
`DEX_ADB_RECONNECT_TIMEOUT` (180 seconds) and `DEX_ASSISTANT_READY_TIMEOUT`
(120 seconds). Each ADB query is bounded by `DEX_ADB_COMMAND_TIMEOUT` (20
seconds); installs are bounded by `DEX_ADB_INSTALL_TIMEOUT` (240 seconds).
For a slow Wi-Fi link, set `DEX_ADB_INSTALL_TIMEOUT=900`; every timeout must be
a positive whole number of seconds. GNU `timeout` is required. Native
ADB uses non-streaming installs; `adb.exe` retains Windows install behavior and
WSL path conversion. Unlock locally after reboot; the script never requests a
PIN. Samsung's unsupported unlock command falls back to user 0's exact
`RUNNING_UNLOCKED` state, never another profile's state.

The script waits for the assistant role, the DPC-owned `RECORD_AUDIO` grant,
and a bound non-null system voice service. It does not grant permissions from
shell, reset the phone, launch an Activity to bypass binding, or prove live
microphone/wake-word readiness. Keep both APKs from the same release because
their shared signing certificate is required for signature-protected DPC IPC.

Run the mocked regressions without a device or APK build:

```bash
python3 -m unittest discover -s scripts/android/tests -v
```

Device Owner provisioning normally requires a clean device and may require a
factory reset. Back up the Fold3 and obtain owner confirmation before resetting
it. Disable Wireless debugging after the test when it is no longer needed.

## Start emulator

From WSL:

```bash
powershell.exe -ExecutionPolicy Bypass -File scripts/android/start-zfold3.ps1
```

The script is idempotent, uses Windows Emulator/ADB, 2 CPU cores, 3072 MB
requested RAM, automatic hardware graphics, disabled audio/boot animation, and
waits for `sys.boot_completed=1`. Snapshot loading is disabled by default to
avoid stale/offline ADB transports; the emulator may enforce a higher RAM floor
for the foldable profile.

## Stop emulator

```bash
powershell.exe -ExecutionPolicy Bypass -File scripts/android/stop-zfold3.ps1
```

Only the running emulator whose console reports AVD name `ZFold3_API35` is
stopped.

## Reset emulator

```bash
powershell.exe -ExecutionPolicy Bypass -File scripts/android/reset-zfold3.ps1
```

Reset stops only `ZFold3_API35`, starts it with `-wipe-data`, and preserves the
AVD definition/system image. All app and Device Owner data in that AVD is lost.

## Check status

```bash
powershell.exe -ExecutionPolicy Bypass -File scripts/android/status-zfold3.ps1
```

The output includes existence/running state, serial, Android/API/boot state,
fold state, enabled Accessibility services, and device/profile owners.

## Run app test

```bash
bash scripts/android/test-android.sh
```

The WSL runner starts the AVD if needed, locates or builds the Android debug APK
(default sibling repo `/mnt/d/dex-assistant-ui`), installs and launches it, then
captures screenshots, UI XML, logcat, activity/window/accessibility/DPC dumps,
and `test-artifacts/test-report.md`. Override the Android repo with
`DEX_ANDROID_REPO=/path/to/repo`.

## DPC testing notes

Set Device Owner only on a clean AVD before installing the main app. Check
`adb shell dpm list-owners`. Use `reset-zfold3.ps1` when a deliberate clean
provision is required; it destroys this AVD's user data.

## Accessibility testing notes

Enable an Accessibility service through Android Settings when testing real
user-mediated behavior. Inspect with `adb shell dumpsys accessibility`; do not
bypass consent. The APK declares `DexAccessibilityService`; Samsung desktop-scoped actions remain
unavailable on an ordinary AOSP AVD. Do not enable a phone fallback to test them.

## Known limitations

- No Samsung One UI, DeX, Knox, or Samsung background-policy behavior.
- Fold3-like open resolution is 1768x2208; the generic outer-display region is
  884x2208, not the Fold3's exact 832x2268 cover panel.
- Hinge/device-state simulation does not reproduce physical durability,
  microphones, thermals, radios, or OEM lifecycle restrictions.


## Isolated desktop approximation for T-011

SDK Manager installs SDK components; `emulator.exe` runs the AVD. Samsung's
[app testing guide](https://developer.samsung.com/samsung-dex/testing.html)
describes matching DeX display/freeform conditions using Android Emulator.
This is an AOSP approximation, without Samsung One UI or the Samsung desktop
DisplayManager category. Production display guards remain unchanged.

From Windows PowerShell (adjust paths to your SDK/JDK):

```powershell
$env:JAVA_HOME = 'D:\ms-21.0.9'
& 'D:\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat' --version
# Only when the image/emulator is missing:
& 'D:\Android\Sdk\cmdline-tools\latest\bin\sdkmanager.bat' 'emulator' 'system-images;android-35;google_apis;x86_64'
.\scripts\android\start-dex-test.ps1 -SdkRoot 'D:\Android\Sdk' -AvdHome 'D:\Android\AVD' -JavaHome $env:JAVA_HOME
```

The script creates/reuses only `DeX_Command_Test_API35`, marked disposable,
1920×1080 at 160 dpi, keyboard and freeform/resizable flags. It refuses to change
an existing unmarked AVD. No retained `ZFold3_API35` data, Device Owner, assistant
role, permission or history changes are needed. The script reports its selected
serial; use that exact serial, never an unqualified multi-device ADB command.

After `./gradlew test lint assembleDebug assembleDebugAndroidTest` in the Android
repo, install its main/test APKs only onto this dedicated AVD. Example (substitute
the serial printed by the script):

```powershell
$env:ADB_SERVER_PORT = '5038'
$adb = 'D:\Android\Sdk\platform-tools\adb.exe'
$serial = 'emulator-5556'
& $adb -s $serial emu avd name  # must be DeX_Command_Test_API35
& $adb -s $serial install -r 'D:\dex-assistant-ui\app\build\outputs\apk\debug\app-debug.apk'
& $adb -s $serial install -r 'D:\dex-assistant-ui\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk'
& $adb -s $serial shell am instrument -w -e class com.khuongnd.dexassistant.ui.ConfigCommandDeviceTest com.khuongnd.dexassistant.ui.test/androidx.test.runner.AndroidJUnitRunner
```

The default focused suite tests the catalog, platform JSON, cancelled/rejected
call interactions and reports the 1,000-pattern benchmark; it never places a
call, grants permissions, changes Wi-Fi or clears history. The selected-desktop
fixture is skipped unless explicitly opted in on an actual selected DeX display.
Injected TTS callbacks prove cancellation policy, not audible spoken acceptance.
Do not run the whole legacy instrumentation suite against owner data.

For a windowed UI demo on the dedicated AVD:

```powershell
& $adb -s $serial shell am start --windowingMode 5 -n com.khuongnd.dexassistant.ui/.MainActivity
```

Use `adb shell am task resize TASK_ID 80 80 1280 900` with the numeric task ID
from `dumpsys activity activities` if a larger test window is needed. Verify
`dumpsys activity activities` shows `mode=freeform`; the flag is a test
configuration, not evidence of Samsung DeX. Fold3 voice flows, external display
execution, app-version adapters, actual calls and device p95 acceptance remain
separate checks. Stop only this AVD using its confirmed serial with `adb emu kill`.
