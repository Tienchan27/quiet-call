param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$projectPath = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectPath
try {
    $sdkPath = Join-Path $projectPath '.tooling\sdk'
    $jdkPath = 'C:\Program Files\Java\jdk-21.0.10'
    if (-not (Test-Path -LiteralPath "$sdkPath\platform-tools\adb.exe")) {
        throw 'Chua co SDK cuc bo. Xem docs/LOCAL_RUN.md de cau hinh Android Studio.'
    }
    if (Test-Path -LiteralPath $jdkPath) { $env:JAVA_HOME = $jdkPath }
    $env:ANDROID_HOME = $sdkPath
    $env:ANDROID_USER_HOME = Join-Path $projectPath '.tooling\android-user'
    $env:ANDROID_AVD_HOME = Join-Path $projectPath '.tooling\avd'
    $env:GRADLE_USER_HOME = Join-Path $projectPath '.tooling\gradle-home'
    $adbPath = Join-Path $sdkPath 'platform-tools\adb.exe'
    if (-not $SkipBuild) {
        $localGradlePath = Join-Path $projectPath '.tooling\gradle-8.9\bin\gradle.bat'
        if (Test-Path -LiteralPath $localGradlePath) { & $localGradlePath assembleDebug --no-daemon }
        else { & .\gradlew.bat assembleDebug --no-daemon }
        if ($LASTEXITCODE -ne 0) { throw 'Build that bai; xem thong bao Gradle.' }
    }
    $apkPath = Join-Path $projectPath 'app\build\outputs\apk\debug\app-debug.apk'
    if (-not (Test-Path -LiteralPath $apkPath)) { throw 'Chua co ban build cuc bo. Chay lai khong co -SkipBuild.' }
    $deviceSerial = 'emulator-5554'
    $devices = (& $adbPath devices) -join "`n"
    if ($devices -notmatch "$deviceSerial\s+device") {
        # Persist a compact emulator window so the entire phone fits on a laptop screen.
        $windowConfigPath = Join-Path $env:ANDROID_AVD_HOME 'QuietCall.avd\emulator-user.ini'
        $windowConfig = if (Test-Path -LiteralPath $windowConfigPath) { Get-Content -LiteralPath $windowConfigPath } else { @() }
        $windowConfig = @($windowConfig | Where-Object { $_ -notmatch '^window\.(x|y|scale)\s*=' })
        $windowConfig += @('window.x = 20', 'window.y = 20', 'window.scale = 0.250000')
        Set-Content -LiteralPath $windowConfigPath -Value $windowConfig -Encoding ascii
        Start-Process -FilePath "$sdkPath\emulator\emulator.exe" -ArgumentList '-avd', 'QuietCall', '-port', '5554', '-gpu', 'swiftshader', '-no-boot-anim', '-no-snapshot', '-no-audio' -RedirectStandardOutput "$projectPath\.tooling\emulator.stdout.log" -RedirectStandardError "$projectPath\.tooling\emulator.stderr.log"
    } else {
        $avdName = ((& $adbPath -s $deviceSerial emu avd name) | Select-Object -First 1).Trim()
        if ($avdName -and $avdName -ne 'QuietCall') { throw "Cong 5554 dang dung boi may ao $avdName. Dong may ao do roi chay lai." }
    }
    Write-Output 'Dang cho Android khoi dong...'
    $bootDeadline = (Get-Date).AddMinutes(3)
    do {
        Start-Sleep -Seconds 2
        # Windows PowerShell 5 treats redirected native stderr as an ErrorRecord while ADB is offline.
        $savedErrorPreference = $ErrorActionPreference
        try {
            $ErrorActionPreference = 'Continue'
            $bootState = (& $adbPath -s $deviceSerial shell getprop sys.boot_completed 2>$null) -join ''
        } finally { $ErrorActionPreference = $savedErrorPreference }
        if ((Get-Date) -gt $bootDeadline) { throw 'Android chua khoi dong xong sau 3 phut. Xem .tooling/emulator.stderr.log.' }
    } until ($bootState.Trim() -eq '1')
    & $adbPath -s $deviceSerial install -r $apkPath
    if ($LASTEXITCODE -ne 0) { throw 'Khong cai duoc ban build vao may ao.' }
    & $adbPath -s $deviceSerial shell am start -n vn.quietcall/.MainActivity
    if ($LASTEXITCODE -ne 0) { throw 'Khong mo duoc QuietCall.' }
    Write-Output 'QuietCall da mo tren may ao Android. Co the dung chuot nhu tren dien thoai.'
} finally { Pop-Location }
