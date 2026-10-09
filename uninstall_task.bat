@echo off
:: ============================================
:: UNINSTALLER - Removes auto-start task
:: ============================================

title Uninstall Tunnel Auto-Start
color 0C
echo.
echo ============================================
echo   REMOVE AUTO-START TASK
echo ============================================
echo.

set /p confirm="Are you sure? (Y/N): "
if /i not "%confirm%"=="Y" (
    echo Cancelled.
    pause
    exit /b 0
)

schtasks /delete /tn "CloudflareTunnel" /f

if %errorlevel% equ 0 (
    echo.
    echo ✅ Task removed successfully!
    echo The tunnel will NO LONGER auto-start.
) else (
    echo ❌ Failed or task didn't exist.
)

echo.
pause