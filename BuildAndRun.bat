@echo off
setlocal

REM ====================================================
REM Java JDK Path
REM ====================================================
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.2"
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM Batch file ke folder ko working directory bana do
cd /d "%~dp0"

REM Check jar.exe
if not exist "%JAVA_HOME%\bin\jar.exe" (
    echo.
    echo ERROR: jar.exe not found.
    echo Check JAVA_HOME:
    echo %JAVA_HOME%
    pause
    exit /b 1
)

echo Compiling Java source...
javac -cp ".;mssql-jdbc-13.2.1.jre11.jar" QadriStore.java
if errorlevel 1 goto :error

echo.

echo Creating temp folder...
if exist temp_jar rmdir /s /q temp_jar
mkdir temp_jar

cd temp_jar

echo Extracting JDBC jar...
"%JAVA_HOME%\bin\jar.exe" xf ..\mssql-jdbc-13.2.1.jre11.jar
if errorlevel 1 goto :error

echo Removing signatures...
del /s /q META-INF\*.SF  >nul 2>&1
del /s /q META-INF\*.DSA >nul 2>&1
del /s /q META-INF\*.RSA >nul 2>&1

echo Copying class files...
copy ..\*.class . >nul

echo Creating QadriStore.jar...
"%JAVA_HOME%\bin\jar.exe" cfm ..\QadriStore.jar ..\Manifest.txt *
if errorlevel 1 goto :error

cd ..

echo Cleaning up...
rmdir /s /q temp_jar
del /q *.class

echo.
echo ======================================
echo JAR created successfully.
echo ======================================

echo Running application...
java -jar QadriStore.jar

goto :end

:error
echo.
echo ======================================
echo ERROR: Process failed.
echo ======================================
pause

:end
endlocal