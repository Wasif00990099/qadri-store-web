@echo off
title Qadri All-In-One Auto Sync & Server
cd /d "C:\Users\Qadri\Desktop\Qadri Store Data"

:: ==============================================
:: 1. JAVA SERVER KO BACKGROUND ME CHALANA
:: ==============================================
:: Ye line aapka Java Server chup chap background me chalayegi
start /b "" cmd /c "java -cp .;mssql-jdbc-13.2.1.jre8.jar QadriWebServer 2>> server_log.txt"

:: Server ko start hone aur DB se connect hone ke liye 15 second wait
timeout /t 15 /nobreak >nul

:: ==============================================
:: 2. HAR 2 MINUTE ME DATA PUSH KARNE KA LOOP
:: ==============================================
:LOOP
:: Data files ko add karein
git add data.json 2>> sync_log.txt
git add index.html 2>> sync_log.txt
git add new_invoice.json 2>> sync_log.txt

:: Check karein ke koi change hua hai ya nahi
git diff --cached --quiet
if %errorlevel% neq 0 (
    :: Agar naya data hai toh commit aur push karein
    git commit -m "Auto Data Sync: %date% %time%" 2>> sync_log.txt
    git push origin main 2>> sync_log.txt
)

:: 120 seconds (2 minutes) ka wait
timeout /t 120 /nobreak >nul

:: 2 minute baad wapas :LOOP par chala jayega
goto LOOP