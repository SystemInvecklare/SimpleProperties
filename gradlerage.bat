@echo off
setlocal enabledelayedexpansion

REM Read project.gradlerage
set "GRADLE_VERSION="
set "JAVA_VERSION="
for /F "usebackq tokens=1,2 delims==" %%A in ("project.gradlerage") do (
    if "%%A"=="gradle" set "GRADLE_VERSION=%%B"
    if "%%A"=="java" set "JAVA_VERSION=%%B"
)

if "%GRADLE_VERSION%"=="" (
    echo gradle version not set in project.gradlerage
    exit /b 1
)

if "%JAVA_VERSION%"=="" (
    echo java version not set in project.gradlerage
    exit /b 1
)

REM Call the jar with the versions as arguments
java -jar GradleRage.jar "%GRADLE_VERSION%" "%JAVA_VERSION%"

@echo off
SET DIR=%~dp0
SET DIR=%DIR:~0,-1%

:: Generate wrapper so jitpack can build

:: Read paths.properties
SET JAVA_BIN=
SET GRADLE_JAR=

FOR /F "usebackq tokens=1,* delims==" %%A IN ("%DIR%\.gradlerage\paths.properties") DO (
    SET KEY=%%A
    SET VALUE=%%B
    IF /I "!KEY!"=="java_path" SET JAVA_BIN=!VALUE!
    IF /I "!KEY!"=="gradle_launcher" SET GRADLE_JAR=!VALUE!
)

:: Validate
IF NOT DEFINED JAVA_BIN (
    ECHO ERROR: java_path not found in paths.properties
    EXIT /B 1
)
IF NOT DEFINED GRADLE_JAR (
    ECHO ERROR: gradle_launcher not found in paths.properties
    EXIT /B 1
)

:: Append actual executable/file names
SET JAVA_BIN=%JAVA_BIN%/java.exe

:: Run Gradle with all passed arguments
"%JAVA_BIN%" -jar "%GRADLE_JAR%" wrapper --gradle-version %GRADLE_VERSION%

:: Launch gradle wrapper with all passed arguments
.\gradlew.bat %*
