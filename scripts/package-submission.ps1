param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$projectPath = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $projectPath
try {
    $localJdk = 'C:\Program Files\Java\jdk-21.0.10'
    if (Test-Path -LiteralPath $localJdk) { $env:JAVA_HOME = $localJdk }
    $localGradle = Join-Path $projectPath '.tooling\gradle-8.9\bin\gradle.bat'
    if (Test-Path -LiteralPath $localGradle) {
        $env:GRADLE_USER_HOME = Join-Path $projectPath '.tooling\gradle-home'
        $gradlePath = $localGradle
    } else { $gradlePath = Join-Path $projectPath 'gradlew.bat' }
    if (-not $SkipBuild) {
        & $gradlePath assembleDebug lintDebug --no-daemon
        if ($LASTEXITCODE -ne 0) { throw 'Build or Android Lint failed.' }
    }
    $metadata = Get-Content -LiteralPath 'app\build\outputs\apk\debug\output-metadata.json' -Raw | ConvertFrom-Json
    $version = [string]$metadata.elements[0].versionName
    if ($version -notmatch '^\d+\.\d+\.\d+$') { throw 'Unexpected version name.' }
    $apkSource = Join-Path $projectPath ('app\build\outputs\apk\debug\' + $metadata.elements[0].outputFile)
    if (-not (Test-Path -LiteralPath $apkSource)) { throw 'APK is missing.' }
    $outputPath = Join-Path $projectPath 'deliverables'
    New-Item -ItemType Directory -Path $outputPath -Force | Out-Null
    $baseName = "QuietCall-$version"
    $apkName = "$baseName-debug.apk"
    $sourceName = "$baseName-source.zip"
    $bundleName = "$baseName-project.zip"
    Copy-Item -LiteralPath $apkSource -Destination (Join-Path $outputPath $apkName) -Force
    Copy-Item -LiteralPath (Join-Path $projectPath 'README.md') -Destination (Join-Path $outputPath 'README.md') -Force

    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    function Write-Zip([string]$Destination, [object[]]$Files) {
        $stream = [System.IO.File]::Open($Destination, [System.IO.FileMode]::Create)
        $archive = New-Object System.IO.Compression.ZipArchive($stream, [System.IO.Compression.ZipArchiveMode]::Create)
        try {
            foreach ($item in $Files) {
                [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($archive, $item.Path, $item.Entry, [System.IO.Compression.CompressionLevel]::Optimal) | Out-Null
            }
        } finally { $archive.Dispose(); $stream.Dispose() }
    }
    # Explicit allowlist prevents local databases, caches or signing keys entering the archive.
    $rootFiles = @('.gitignore', 'LICENSE', 'README.md', 'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew', 'gradlew.bat', 'run-local.cmd', 'demo-call.cmd', 'package-submission.cmd', 'app\build.gradle.kts')
    $sourceFiles = @($rootFiles | ForEach-Object { Get-Item -LiteralPath (Join-Path $projectPath $_) })
    foreach ($folder in @('app\src', 'gradle\wrapper', '.github', 'scripts', 'docs', 'samples')) {
        $sourceFiles += @(Get-ChildItem -LiteralPath (Join-Path $projectPath $folder) -Recurse -File -Force)
    }
    $entries = @($sourceFiles | Sort-Object FullName -Unique | ForEach-Object {
        $relative = $_.FullName.Substring($projectPath.Length + 1).Replace('\', '/')
        if ($relative -match '(^|/)(\.git|\.tooling|\.gradle|\.kotlin|build)(/|$)|\.(db|jks|keystore)$|(^|/)local\.properties$') { throw "Forbidden source entry: $relative" }
        @{ Path = $_.FullName; Entry = "QuietCall/$relative" }
    })
    Write-Zip (Join-Path $outputPath $sourceName) $entries
    $checksums = @($apkName, $sourceName) | ForEach-Object {
        $hash = (Get-FileHash -LiteralPath (Join-Path $outputPath $_) -Algorithm SHA256).Hash.ToLowerInvariant()
        "$hash  $_"
    }
    Set-Content -LiteralPath (Join-Path $outputPath 'SHA256.txt') -Value $checksums -Encoding ascii
    $bundleFiles = @($apkName, $sourceName, 'README.md', 'SHA256.txt') | ForEach-Object { @{ Path = (Join-Path $outputPath $_); Entry = $_ } }
    Write-Zip (Join-Path $outputPath $bundleName) $bundleFiles
    $bundleHash = (Get-FileHash -LiteralPath (Join-Path $outputPath $bundleName) -Algorithm SHA256).Hash.ToLowerInvariant()
    Set-Content -LiteralPath (Join-Path $outputPath 'PACKAGE-SHA256.txt') -Value "$bundleHash  $bundleName" -Encoding ascii
    Write-Output "Ready: $outputPath"
    Get-ChildItem -LiteralPath $outputPath -File | Select-Object Name, Length
} finally { Pop-Location }
