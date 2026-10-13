[CmdletBinding()]
param(
    [switch]$WipeData,
    [int]$BootTimeoutSeconds = 240
)

$ErrorActionPreference = "Stop"
$env:ADB_SERVER_PORT = '5038'
$AvdName = "ZFold3_API35"
$SdkRoot = "D:\Android\Sdk"
$AvdHome = "D:\Android\AVD"
$Adb = Join-Path $SdkRoot "platform-tools\adb.exe"
$Emulator = Join-Path $SdkRoot "emulator\emulator.exe"

function Get-TargetSerial {
    $lines = & $Adb -P 5038 devices 2>$null
    foreach ($line in $lines) {
        if ($line -match '^(emulator-\d+)\s+device') {
            $serial = $Matches[1]
            $name = (& $Adb -P 5038 -s $serial emu avd name 2>$null | Select-Object -First 1).Trim()
            if ($name -eq $AvdName) { return $serial }
        }
    }
    return $null
}

function Test-TargetProcess {
    return [bool](Get-CimInstance Win32_Process -ErrorAction SilentlyContinue | Where-Object {
        $_.Name -in @('emulator.exe', 'qemu-system-x86_64.exe') -and
        $_.CommandLine -match '(?i)-avd\s+ZFold3_API35'
    } | Select-Object -First 1)
}

if (-not (Test-Path $Adb) -or -not (Test-Path $Emulator)) {
    throw "Windows Android SDK is incomplete under $SdkRoot"
}
if (-not (Test-Path (Join-Path $AvdHome "$AvdName.ini"))) {
    throw "AVD $AvdName does not exist in $AvdHome"
}

$env:ANDROID_AVD_HOME = $AvdHome
$serial = Get-TargetSerial
if ($serial) {
    if ($WipeData) { throw "$AvdName is running as $serial; stop it before a wipe-data start" }
    Write-Output "AVD already running: $serial"
} else {
    if (Test-TargetProcess) {
        throw "$AvdName has a Windows process but is not ready in ADB; stop it before starting another instance"
    }
    $arguments = @('-avd', $AvdName, '-cores', '2', '-memory', '3072', '-no-audio', '-no-boot-anim', '-no-snapshot-load', '-gpu', 'auto')
    if ($WipeData) { $arguments += '-wipe-data' }
    $logBase = Join-Path $AvdHome $AvdName
    Start-Process -FilePath $Emulator -ArgumentList $arguments `
        -RedirectStandardOutput "$logBase.stdout.log" `
        -RedirectStandardError "$logBase.stderr.log" | Out-Null

    $deadline = (Get-Date).AddSeconds($BootTimeoutSeconds)
    do {
        Start-Sleep -Seconds 2
        $serial = Get-TargetSerial
    } until ($serial -or (Get-Date) -ge $deadline)
    if (-not $serial) { throw "Timed out waiting for $AvdName to appear in Windows ADB" }
}

$deadline = (Get-Date).AddSeconds($BootTimeoutSeconds)
do {
    $booted = (& $Adb -P 5038 -s $serial shell getprop sys.boot_completed 2>$null).Trim()
    if ($booted -eq '1') { break }
    Start-Sleep -Seconds 2
} until ((Get-Date) -ge $deadline)
if ($booted -ne '1') { throw "Timed out waiting for Android boot on $serial" }

Write-Output "AVD=$AvdName"
Write-Output "SERIAL=$serial"
Write-Output "BOOT_COMPLETED=1"
