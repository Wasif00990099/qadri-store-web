@echo off
:: ============================================
:: TASK SCHEDULER INSTALLER
:: Run this ONCE to setup automatic startup
:: ============================================

title Cloudflare Tunnel - Auto Install
color 0B
echo.
echo ============================================
echo   CLOUDFLARE TUNNEL AUTO-START SETUP
echo ============================================
echo.

:: Check admin permissions
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ ERROR: Please RUN AS ADMINISTRATOR!
    echo Right-click this file → Run as administrator
    echo.
    pause
    exit /b 1
)

echo ✅ Admin privileges confirmed
echo.

:: Delete old task if exists
schtasks /delete /tn "CloudflareTunnel" /f >nul 2>&1
echo 🗑️  Cleaned old task (if any)

:: Create new task with EXACT settings
echo.
echo 📋 Creating scheduled task...
echo.

schtasks /create /tn "CloudflareTunnel" /tr "wscript.exe \"D:\Excel\tunnel_hidden.vbs\"" /sc onstart /ru SYSTEM /rl HIGHEST /f

if %errorlevel% equ 0 (
    echo.
    echo ✅✅✅ SUCCESS! ✅✅✅
    echo.
    echo ============================================
    echo   TASK CREATED SUCCESSFULLY!
    echo ============================================
    echo.
    echo 📌 Task Name: CloudflareTunnel
    echo 📌 Trigger: At system startup (before login!)
    echo 📌 Runs as: SYSTEM (no login needed)
    echo 📌 Mode: HIDDEN (no window)
    echo 📌 Privileges: Highest (admin rights)
    echo.
    echo 🚀 Your tunnel will now AUTOMATICALLY START:
    echo    → When PC turns on
    echo    → Before anyone logs in
    echo    → Completely hidden (no windows)
    echo    → With full admin rights
    echo.
    echo ============================================
    echo.
    
    :: Test immediately option
    set /p test_now="Do you want to TEST NOW? (Y/N): "
    if /i "%test_now%"=="Y" (
        echo.
        echo 🧪 Testing task...
        schtasks /run /tn "CloudflareTunnel"
        echo ✅ Task started! Check D:\Excel\hidden_launcher.log for status
        timeout /t 5
    )
    
) else (
    echo.
    echo ❌ FAILED to create task!
    echo Error code: %errorlevel%
    echo.
    echo Try running as Administrator again.
)

echo.
pause