::[Bat To Exe Converter]
::
::YAwzoRdxOk+EWAjk
::fBw5plQjdCyDJGyX8VAjFCpVSBaLAE+1EbsQ5+n//NaBo1sUV+0xOMeJk/qHI+9z
::YAwzuBVtJxjWCl3EqQJgSA==
::ZR4luwNxJguZRRnk
::Yhs/ulQjdF+5
::cxAkpRVqdFKZSzk=
::cBs/ulQjdF+5
::ZR41oxFsdFKZSDk=
::eBoioBt6dFKZSDk=
::cRo6pxp7LAbNWATEpCI=
::egkzugNsPRvcWATEpCI=
::dAsiuh18IRvcCxnZtBJQ
::cRYluBh/LU+EWAnk
::YxY4rhs+aU+JeA==
::cxY6rQJ7JhzQF1fEqQJQ
::ZQ05rAF9IBncCkqN+0xwdVs0
::ZQ05rAF9IAHYFVzEqQJQ
::eg0/rx1wNQPfEVWB+kM9LVsJDGQ=
::fBEirQZwNQPfEVWB+kM9LVsJDGQ=
::cRolqwZ3JBvQF1fEqQJQ
::dhA7uBVwLU+EWDk=
::YQ03rBFzNR3SWATElA==
::dhAmsQZ3MwfNWATElA==
::ZQ0/vhVqMQ3MEVWAtB9wSA==
::Zg8zqx1/OA3MEVWAtB9wSA==
::dhA7pRFwIByZRRnk
::Zh4grVQjdCuDJHyc90c8FCpVSBaLfFi/FKMZ+qb+9+/n
::YB416Ek+ZG8=
::
::
::978f952a14a936cc963da21a135fa983
@echo off
title 🚀 Qadri Store - Cloudflare Tunnel
color 0B
mode con: cols=100 lines=30

:: ===== GO TO FOLDER =====
cd /d D:\Excel

:: ===== SHOW INFO =====
echo.
echo ╔══════════════════════════════════════════╗
echo ║     QADRI STORE - CLOUDFLARE TUNNEL      ║
echo ╠══════════════════════════════════════════╣
echo ║  Starting Time: %date% %time%            ║
echo ║  Folder: %cd%                            ║
echo ╚══════════════════════════════════════════╝
echo.

:: ===== CHECK PYTHON =====
where python >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Python nahi mila!
    echo Please Python install karein
    pause
    exit /b 1
)

:: ===== RUN SCRIPT (WINDOW OPEN RAHEGA) =====
echo [INFO] Tunnel start ho rahi hai...
echo [INFO] Link dhundhne mein 5-10 seconds lagenge...
echo [INFO] Jab tak ye window open hai, tab tak tunnel chalegi!
echo.
echo ---------------------------------------------------------------

python qadristore.py

:: ===== SHOW EXIT STATUS =====
echo.
echo ---------------------------------------------------------------
if %errorlevel% equ 0 (
    echo [✅] Tunnel normally closed at %time%
) else (
    echo [❌] Tunnel closed with error: %errorlevel%
    echo     Reason: User ne band kiya / Crash hua
)
echo.

:: ===== LOG TO FILE =====
echo [%date% %time%] Exit Code: %errorlevel% >> D:\Excel\tunnel_log.txt

:: ===== KEEP WINDOW OPEN =====
echo Tunnel band ho gayi hai.
echo Press any key to exit...
pause >nul