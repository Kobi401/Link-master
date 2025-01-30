@echo off
REM Exit immediately if a command exits with a non-zero status
SETLOCAL ENABLEEXTENSIONS ENABLEDELAYEDEXPANSION

REM ------------------------------
REM Configuration
REM ------------------------------

REM Set JAVA_HOME if not already set
IF NOT DEFINED JAVA_HOME (
    SET JAVA_HOME=C:\Program Files\Java\jdk-17
)

REM Path to JavaFX SDK
SET JAVA_FX_PATH=%~dp0JavaFX\lib
REM Path to JavaFX lib directory (JavaFX is in the project directory)

REM Project directories
SET SRC_DIR=C:\Users\Kobi401\Desktop\Link-master\src
SET RESOURCES_DIR=src\main\resources
SET BUILD_DIR=build\classes
SET JAR_NAME=LinkBrowser.jar
SET MAIN_CLASS=ui.LinkBrowser

REM Output directories
SET DIST_DIR=dist
IF NOT EXIST "%DIST_DIR%" (
    mkdir "%DIST_DIR%"
)

REM ------------------------------
REM Verify JavaFX Path
REM ------------------------------
IF NOT EXIST "%JAVA_FX_PATH%" (
    echo JavaFX SDK not found at %JAVA_FX_PATH%
    echo Please ensure the JavaFX SDK is located in the 'JavaFX\lib' directory within your project folder.
    EXIT /B 1
)


REM ------------------------------
REM Compile Java Source Files
REM ------------------------------

echo Compiling Java source files...

IF NOT EXIST "%BUILD_DIR%" (
    mkdir "%BUILD_DIR%"
)

javac --module-path "%JAVA_FX_PATH%" ^
      --add-modules javafx.controls,javafx.web ^
      -d "%BUILD_DIR%" ^
      %SRC_DIR%\**\*.java

REM ------------------------------
REM Copy Resources
REM ------------------------------

echo Copying resource files...

xcopy /E /I /Y "%RESOURCES_DIR%\*" "%BUILD_DIR%\" >nul

REM ------------------------------
REM Create Executable JAR
REM ------------------------------

echo Creating executable JAR...

cd "%BUILD_DIR%"

REM Create manifest file with Main-Class
echo Main-Class: %MAIN_CLASS%> manifest.txt
echo.>> manifest.txt

REM Package JAR
jar cfm "%JAR_NAME%" manifest.txt ^
    ui\*.class ^
    ui\bookmark\*.class ^
    Images\*.*

REM Return to project root
cd ..\..\..

REM ------------------------------
REM Package with jpackage
REM ------------------------------

echo Packaging application with jpackage...

REM Define icon path (optional)
SET ICON_PATH=%RESOURCES_DIR%\Images\linkbrowser.ico

REM Check if icon exists
IF EXIST "%ICON_PATH%" (
    SET ICON_OPTION=--icon "%ICON_PATH%"
) ELSE (
    echo Icon file not found at %ICON_PATH%. Proceeding without icon.
    SET ICON_OPTION=
)

REM Use jpackage to create installer
jpackage ^
  --type exe ^ REM Change to msi for Windows installer, pkg for macOS, dmg for macOS, deb/rpm for Linux
  --input "%BUILD_DIR%" ^
  --name LinkBrowser ^
  --main-jar "%JAR_NAME%" ^
  --main-class "%MAIN_CLASS%" ^
  --module-path "%JAVA_FX_PATH%" ^
  --add-modules javafx.controls,javafx.web ^
  %ICON_OPTION% ^
  --app-version 1.0 ^
  --vendor "Kobi401" ^
  --copyright "Copyright © 2025" ^
  --java-options "--enable-unsafe-exceptions"

echo Packaging completed. Installer is located in the %DIST_DIR% directory.
