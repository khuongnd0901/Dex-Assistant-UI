[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$SdkRoot,
    [Parameter(Mandatory = $true)][string]$AvdHome,
    [string]$JavaHome = $env:JAVA_HOME,
    [int]$BootTimeoutSeconds = 240
)

# A disposable AOSP desktop approximation. This does not install Samsung DeX.
$ErrorActionPreference = 'Stop'
$env:ADB_SERVER_PORT = '5038'
$AvdName = 'DeX_Command_Test_API35'
$AvdPath = Join-Path $AvdHome "$AvdName.avd"
$Marker = Join-Path $AvdPath '.t011-disposable'
$Adb = Join-Path $SdkRoot 'platform-tools\adb.exe'
$Emulator = Join-Path $SdkRoot 'emulator\emulator.exe'
$AvdManager = Join-Path $SdkRoot 'cmdline-tools\latest\bin\avdmanager.bat'
foreach ($tool in @($Adb, $Emulator, $AvdManager)) {
    if (-not (Test-Path $tool)) { throw "Missing SDK tool: $tool" }
}
if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
$env:ANDROID_AVD_HOME = $AvdHome
$env:ANDROID_HOME = $SdkRoot

function Get-TargetSerial {
    # Windows ADB may expose this local AVD as a loopback transport instead of emulator-5554.
    & $Adb -P 5038 connect '127.0.0.1:5555' | Out-Null
    foreach ($line in (& $Adb -P 5038 devices)) {
        if ($line -match '^((?:emulator-\d+|127\.0\.0\.1:\d+))\s+device') {
            $serial = $Matches[1]
            $name = (& $Adb -P 5038 -s $serial shell getprop ro.boot.qemu.avd_name | Select-Object -First 1).Trim()
            if (-not $name -and $serial -like 'emulator-*') {
                $name = (& $Adb -P 5038 -s $serial emu avd name | Select-Object -First 1).Trim()
            }
            if ($name -eq $AvdName) { return $serial }
        }
    }
    return $null
}
function Invoke-TargetShell {
    param([string[]]$Arguments)
    & $Adb -P 5038 -s $script:TargetSerial shell @Arguments
    if ($LASTEXITCODE -ne 0) { throw "ADB shell failed: $Arguments" }
}

if (Test-Path $AvdPath) {
    if (-not (Test-Path $Marker)) { throw 'Existing AVD is not marked disposable; refuse to change it' }
} else {
    if (-not (Test-Path (Join-Path $SdkRoot 'system-images\android-35\google_apis\x86_64\system.img'))) {
        throw 'Install system-images;android-35;google_apis;x86_64 with SDK Manager first'
    }
    New-Item -ItemType Directory -Force -Path $AvdHome | Out-Null
    'no' | & $AvdManager create avd -n $AvdName -k 'system-images;android-35;google_apis;x86_64' --device 'pixel_tablet' -p $AvdPath
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path (Join-Path $AvdPath 'config.ini'))) { throw 'AVD creation failed' }
    Set-Content -Path $Marker -Value 'T-011 isolated test data only' -Encoding ASCII
}
$script:TargetSerial = Get-TargetSerial
if (-not $script:TargetSerial) {
    $alreadyStarting = Get-CimInstance Win32_Process | Where-Object {
        $_.Name -in @('emulator.exe','qemu-system-x86_64.exe') -and $_.CommandLine -match "-avd\s+$AvdName"
    }
    $settings = @{
        'hw.lcd.width' = '1920'; 'hw.lcd.height' = '1080'; 'hw.lcd.density' = '160'
        'hw.initialOrientation' = 'landscape'; 'hw.keyboard' = 'yes'; 'hw.gpu.enabled' = 'yes'
    }
    $configPath = Join-Path $AvdPath 'config.ini'
    $config = [System.IO.File]::ReadAllText($configPath)
    foreach ($key in $settings.Keys) {
        $pattern = '(?m)^' + [regex]::Escape($key) + '=.*$'
        $value = "$key=$($settings[$key])"
        if ([regex]::IsMatch($config,$pattern)) { $config = [regex]::Replace($config,$pattern,$value) }
        else { $config += "`n$value`n" }
    }
    [System.IO.File]::WriteAllText($configPath,$config)
    if (-not $alreadyStarting) {
        Start-Process -FilePath $Emulator -ArgumentList @('-avd',$AvdName,'-cores','2','-memory','3072',
            '-no-boot-anim','-no-snapshot-load','-no-snapshot-save','-gpu','auto') `
            -RedirectStandardOutput (Join-Path $AvdHome "$AvdName.stdout.log") `
            -RedirectStandardError (Join-Path $AvdHome "$AvdName.stderr.log") | Out-Null
    }
}
$deadline = (Get-Date).AddSeconds($BootTimeoutSeconds)
do {
    if (-not $script:TargetSerial) { $script:TargetSerial = Get-TargetSerial }
    if ($script:TargetSerial) {
        $booted = (& $Adb -P 5038 -s $script:TargetSerial shell getprop sys.boot_completed).Trim()
        if ($booted -eq '1') { break }
    }
    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $deadline)
if ($booted -ne '1') { throw 'Timed out waiting for the dedicated test AVD' }

# Only this marked disposable AVD is changed. No root/remount/SELinux changes.
Invoke-TargetShell -Arguments @('settings','put','global','enable_freeform_support','1')
Invoke-TargetShell -Arguments @('settings','put','global','force_resizable_activities','1')
Invoke-TargetShell -Arguments @('wm','density','160')
Invoke-TargetShell -Arguments @('wm','size','1920x1080')
Write-Output "AVD=$AvdName SERIAL=$script:TargetSerial BOOT_COMPLETED=1"
Write-Output 'AOSP desktop approximation: 1920x1080 at 160 dpi; freeform enabled; no Samsung desktop category'
