@echo off
setlocal
set "JAVA_HOME=C:\projects\tools\temurin8\jdk8u492-b09"
set "GRADLE_USER_HOME=%~dp0.gradle"
set "PATH=%JAVA_HOME%\bin;%PATH%"
call "%~dp0gradlew.bat" %*
exit /b %ERRORLEVEL%
