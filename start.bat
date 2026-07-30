@echo off
title Qadri Store Servers
echo Compiling all modules...
javac -encoding UTF-8 -cp .;mssql-jdbc-13.2.1.jre8.jar QadriWebServer.java QadriPOS.java QadriPriceUpdate.java QadriStock.java

if %errorlevel% neq 0 (
    color 0C
    echo.
    echo ❌ Compilation Failed!
    pause
    exit /b
)

color 0A
echo ✅ Compilation Successful!
echo Starting Main Server (Includes POS, Stock & Price Update)...
java -cp .;mssql-jdbc-13.2.1.jre8.jar QadriWebServer
pause