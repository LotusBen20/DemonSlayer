@echo off
echo Starting build...
call mvn clean package
if errorlevel 1 (
    echo Build failed!
    pause
    exit /b 1
)
echo Build successful! Starting bot...
java -jar target/telegram-business-bot-1.0-SNAPSHOT-jar-with-dependencies.jar
pause 