@echo off
REM =========================================================
REM Build script for LinkBrowser with JavaFX jars in "JavaFX"
REM THIS DOES NOT WORK! do not use this unless this text is gone!
REM =========================================================

SET BUILD_TYPE=STABLE

IF NOT "%1"=="" (
   SET BUILD_TYPE=%1
)

echo ========== BUILD SCRIPT ==========
echo Build Type: %BUILD_TYPE%
echo ----------------------------------

IF NOT EXIST bin (
   mkdir bin
)

echo Generating sources list...
dir /b /s src\*.java > sources.txt

IF ERRORLEVEL 1 (
   echo [ERROR] Could not locate Java sources in src directory!
   goto :EOF
)

echo Compiling source files...
javac ^
  --module-path JavaFX/lib ^
  --add-modules javafx.controls,javafx.graphics,javafx.web ^
  -d bin ^
  @sources.txt

IF ERRORLEVEL 1 (
   echo [ERROR] Compilation failed!
   goto :EOF
)

echo [OK] Compilation succeeded.

echo Creating LinkBrowser.jar...
jar cvfe LinkBrowser.jar com.linkbrowser.LinkBrowser -C bin .

IF ERRORLEVEL 1 (
   echo [ERROR] Jar creation failed!
   goto :EOF
)

echo [OK] Jar created: LinkBrowser.jar

echo ----------------------------------
echo To run the browser with build.type=%BUILD_TYPE%, use:
echo.
echo   java --module-path JavaFX ^
echo        --add-modules javafx.controls,javafx.graphics,javafx.web ^
echo        -Dbuild.type=%BUILD_TYPE% -jar LinkBrowser.jar
echo.
echo [Done]
