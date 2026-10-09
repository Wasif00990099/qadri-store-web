@echo off
title Qadri Auto Data Sync (Running...)
cd /d "C:\Users\Qadri\Desktop\Qadri Store Data"

:LOOP
cls
echo ==============================================
echo       QADRI STORE AUTO DATA SYNC
echo ==============================================
echo Last Check Time: %date% %time%
echo.

:: 1. Naye data ko add karein (Agar file na ho toh bhi error nahi dega)
echo [1/4] Adding files to Git...
git add data.json 2>nul
git add index.html 2>nul
git add new_invoice.json 2>nul

:: 2. Check karein ke koi change hua hai ya nahi
git diff --cached --quiet
if %errorlevel% neq 0 (
    echo [2/4] Changes found! Committing data...
    git commit -m "Auto Data Sync: %date% %time%" 2>nul
    
    echo [3/4] Pushing data to GitHub...
    git push origin main 2>nul
    
    echo [4/4] SUCCESS: Data pushed to GitHub!
) else (
    echo [2/4] No new data changes found.
    echo [4/4] SKIPPED: Push cancelled.
)

echo.
echo ==============================================
echo Waiting for 2 minutes (120 seconds)...
echo Do NOT close this window! It will auto-update.
echo ==============================================

:: 120 seconds (2 minutes) ka wait
timeout /t 120 /nobreak >nul

:: 2 minute baad wapas :LOOP par chala jayega
goto LOOP