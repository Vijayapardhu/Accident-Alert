@echo off
echo Testing Crash Alert Safety Android Application Build...
echo.

REM Clean any previous build artifacts
if exist "app\build" rmdir /s /q "app\build"
if exist "build" rmdir /s /q "build"

echo Cleaned previous build artifacts.
echo.

REM Try to build the project
echo Attempting to build the project...
echo.

REM Use gradlew if available, otherwise show instructions
if exist "gradlew.bat" (
    echo Using gradlew.bat...
    gradlew.bat build --no-daemon --stacktrace
) else (
    echo gradlew.bat not found. Please use Android Studio to build the project.
    echo.
    echo Instructions:
    echo 1. Open Android Studio
    echo 2. Open the project from: %CD%
    echo 3. Wait for Gradle sync to complete
    echo 4. Build the project (Build ^> Make Project)
    echo.
    pause
    exit /b 0
)

echo.
echo Build completed! Check the output above for any errors.
pause
