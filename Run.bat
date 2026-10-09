@echo off
title Qadri Store Auto Compiler

REM Ye command current folder ko set karti hai
cd /d "%~dp0"

echo.
echo [Step 1] Cleaning old files...
if exist QadriStore.class del QadriStore.class

echo [Step 2] Compiling Java Code...
javac -cp ".;*" QadriStore.java

REM Agar error aaye to ruk jao
if %errorlevel% neq 0 (
    echo.
    echo ERROR: Compilation Failed! Check code.
    pause
    exit /b
)

echo [Step 3] Starting Application...
echo.
java -cp ".;*" QadriStore

pause