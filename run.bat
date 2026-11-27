@echo off
REM Compiles and runs DriveShare (Windows). Requires a JDK 11 or newer.
cd /d "%~dp0"
set CP=lib\sqlite-jdbc-3.46.1.3.jar
if not exist out mkdir out
REM -sourcepath lets javac find and compile every class Main uses (works even if the folder path has spaces)
javac -d out -cp "%CP%" -sourcepath src src\driveshare\Main.java
if errorlevel 1 (
  pause
  exit /b 1
)
java -cp "out;%CP%" driveshare.Main
pause
