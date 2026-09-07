@if "%DEBUG%"=="" @echo off

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set CMD_LINE_ARGS=%*

set JAVA_EXE=java.exe
if not "%JAVA_HOME%"=="" (
    if exist "%JAVA_HOME%\bin\java.exe" set JAVA_EXE="%JAVA_HOME%\bin\java.exe"
)

"%JAVA_EXE%" -cp "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %CMD_LINE_ARGS%
