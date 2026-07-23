@echo off
REM ---------------------------------------------------------------------------
REM Builds a SIGNED, installable "preview" APK and copies it to dist\.
REM Install on any phone by transferring the file and tapping it (enable
REM "Install unknown apps" for your file manager). No Developer Options needed.
REM ---------------------------------------------------------------------------
setlocal

REM Use the JDK bundled with Android Studio.
if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%LOCALAPPDATA%\Programs\Android Studio\jbr"
if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
if not defined JAVA_HOME (
  echo ERROR: Could not find the Android Studio JDK ^(jbr^). Set JAVA_HOME manually.
  exit /b 1
)
echo Using JAVA_HOME=%JAVA_HOME%

call gradlew.bat :composeApp:assembleRelease --no-daemon
if errorlevel 1 (
  echo BUILD FAILED.
  exit /b 1
)

if not exist dist mkdir dist
copy /Y "composeApp\build\outputs\apk\release\composeApp-release.apk" "dist\panorama-preview.apk" >nul

echo.
echo ============================================================
echo  Preview APK ready:  dist\panorama-preview.apk
echo  Transfer it to your phone and tap to install.
echo ============================================================
endlocal
