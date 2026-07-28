@echo off
REM ---------------------------------------------------------------------------
REM Builds the SIGNED Android App Bundle (.aab) to upload to Google Play and
REM copies it to dist\. Requires keystore.properties + release-keystore.jks in
REM the project root (see RUNNING.md section 3.2).
REM ---------------------------------------------------------------------------
setlocal

if not exist "keystore.properties" (
  echo ERROR: keystore.properties not found. Copy keystore.properties.sample,
  echo        fill in your upload keystore details, and try again.
  exit /b 1
)

REM Prefer the JDK bundled with Android Studio, then JAVA_HOME, then a JDK 17/21.
if exist "%LOCALAPPDATA%\Programs\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%LOCALAPPDATA%\Programs\Android Studio\jbr"
if exist "%ProgramFiles%\Android\Android Studio\jbr\bin\java.exe" set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
if not exist "%JAVA_HOME%\bin\java.exe" (
  if exist "%ProgramFiles%\Java\jdk-21.0.11\bin\java.exe" set "JAVA_HOME=%ProgramFiles%\Java\jdk-21.0.11"
)
if not exist "%JAVA_HOME%\bin\java.exe" (
  if exist "%ProgramFiles%\Java\jdk-17\bin\java.exe" set "JAVA_HOME=%ProgramFiles%\Java\jdk-17"
)
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo ERROR: No JDK 17+ found. Set JAVA_HOME manually.
  exit /b 1
)
echo Using JAVA_HOME=%JAVA_HOME%

call gradlew.bat :composeApp:bundleRelease --no-daemon
if errorlevel 1 (
  echo BUILD FAILED.
  exit /b 1
)

if not exist dist mkdir dist
copy /Y "composeApp\build\outputs\bundle\release\composeApp-release.aab" "dist\panorama-release.aab" >nul

echo.
echo ============================================================
echo  Play bundle ready:  dist\panorama-release.aab
echo  Upload it in Play Console - Release - Testing - Internal testing.
echo  Remember to bump versionCode in composeApp\build.gradle.kts
echo  before every new upload.
echo ============================================================
endlocal
