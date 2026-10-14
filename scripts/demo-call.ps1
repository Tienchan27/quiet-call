param([string]$Number, [switch]$Hangup)
$ErrorActionPreference = 'Stop'
$projectPath = Split-Path -Parent $PSScriptRoot
$adbPath = Join-Path $projectPath '.tooling\sdk\platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $adbPath)) { throw 'Chua co SDK. Mo run-local.cmd truoc.' }
if ($Number) {
    if ($Number -notmatch '^\+?[0-9]{8,15}$') { throw 'So demo can 8-15 chu so, co the bat dau bang +.' }
    $operation = if ($Hangup) { 'cancel' } else { 'call' }
    & $adbPath -s emulator-5554 emu gsm $operation $Number
    exit $LASTEXITCODE
}
Write-Host 'QUIETCALL - CUOC GOI ANDROID GIA LAP' -ForegroundColor Green
Write-Host 'Mo run-local.cmd truoc. Chi gia lap tren emulator, khong goi so ngoai doi.'
$activeNumber = $null
while ($true) {
    Write-Host "`n1. Aster: 0900000001 (bi chan neu bat quy tac)"
    Write-Host '2. Aster: 0900000002 (so thu hai cung thuong hieu)'
    Write-Host '3. Aster: 0900000003 (whitelist trong ban demo)'
    Write-Host '4. Nova: 0900000011 (cho phep trong ban demo)'
    Write-Host '5. Ngat cuoc goi demo hien tai'
    Write-Host '0. Thoat'
    $choice = Read-Host 'Chon'
    if ($choice -eq '0') { break }
    if ($choice -notin @('1', '2', '3', '4', '5')) { continue }
    if ($activeNumber) {
        & $adbPath -s emulator-5554 emu gsm cancel $activeNumber 2>$null | Out-Null
        $activeNumber = $null
    }
    if ($choice -eq '5') { continue }
    $activeNumber = switch ($choice) { '1' { '0900000001' }; '2' { '0900000002' }; '3' { '0900000003' }; '4' { '0900000011' } }
    & $adbPath -s emulator-5554 emu gsm call $activeNumber
    Write-Host 'Quan sat cua so Android va tab Lich su cua QuietCall.'
}
if ($activeNumber) { & $adbPath -s emulator-5554 emu gsm cancel $activeNumber 2>$null | Out-Null }
