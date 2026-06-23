@echo off
setlocal

set BASE_DIR=%~dp0
set WRAPPER_DIR=%BASE_DIR%.mvn\wrapper
set MAVEN_VERSION=3.9.9
set MAVEN_HOME=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%
set MAVEN_ZIP=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip
set MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip

where java >nul 2>nul
if errorlevel 1 (
  echo Java was not found on PATH. Install JDK 17 or set JAVA_HOME, then run this command again.
  exit /b 1
)

if not exist "%JAVA_HOME%\bin\java.exe" (
  for /f "delims=" %%i in ('where java') do (
    set JAVA_EXE=%%i
    goto foundJava
  )
  :foundJava
  for %%i in ("%JAVA_EXE%\..\..") do set JAVA_HOME=%%~fi
)

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo JAVA_HOME is not pointing to a JDK. Set JAVA_HOME to your JDK 17 folder, for example:
  echo setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17"
  exit /b 1
)

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
  echo Downloading Apache Maven %MAVEN_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%MAVEN_ZIP%'"
  if errorlevel 1 exit /b 1
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; if (Test-Path '%MAVEN_HOME%') { Remove-Item '%MAVEN_HOME%' -Recurse -Force }; Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%WRAPPER_DIR%' -Force"
  if errorlevel 1 exit /b 1
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
