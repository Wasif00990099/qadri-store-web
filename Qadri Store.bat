::[Bat To Exe Converter]
::
::YAwzoRdxOk+EWAjk
::fBw5plQjdCuDJGiF8FA5aChAQxaHfE+7ErQgwev04daOoUITGus8d+8=
::YAwzuBVtJxjWCl3EqQJgSA==
::ZR4luwNxJguZRRnk
::Yhs/ulQjdF+5
::cxAkpRVqdFKZSTk=
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
::Zh4grVQjdCuDJHyc90c8FCpVSBaLfFiuCacZpu3j6oo=
::YB416Ek+ZG8=
::
::
::978f952a14a936cc963da21a135fa983
@echo off
title QadriStore Builder
color 0A

echo Navigating to D:\Excel...
cd /d D:\Excel

echo Cleaning old files...
del *.class 2>nul
rmdir /s /q temp_jar 2>nul

echo Compiling Java File...
javac -cp ".;mssql-jdbc-13.2.1.jre11.jar" QadriStore.java

if %errorlevel% neq 0 (
    color 0C
    echo.
    echo === COMPILATION FAILED! ===
    echo Check your QadriStore.java for errors.
    echo.
    pause
    exit
)

echo Creating temporary folder...
mkdir temp_jar
cd temp_jar

echo Extracting JDBC Driver...
jar xf ..\mssql-jdbc-13.2.1.jre11.jar

echo Removing Security Signatures...
del /s /q META-INF\*.SF
del /s /q META-INF\*.RSA
del /s /q META-INF\*.DSA

echo Copying Class Files...
copy ..\*.class . >nul

echo Building Final JAR...
jar cfm ..\QadriStore.jar ..\Manifest.txt *

echo Cleaning up temporary files...
cd ..
rmdir /s /q temp_jar
del *.class >nul

color 0A
echo.
echo ====================================
echo       BUILD SUCCESSFUL!
echo ====================================
echo.

echo Starting Application...
start "" javaw -jar QadriStore.jar