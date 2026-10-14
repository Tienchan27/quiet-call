@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\demo-call.ps1" %*
if errorlevel 1 pause
