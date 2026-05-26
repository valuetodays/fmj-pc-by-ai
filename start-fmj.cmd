@echo off
cd /d "%~dp0"
call mvn compile -q
if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)
set LOG4J_JAR=%USERPROFILE%\.m2\repository\log4j\log4j\1.2.17\log4j-1.2.17.jar
java -classpath target\classes;%LOG4J_JAR% hz.cdj.game.fmj.GameView
if errorlevel 1 pause
