# Очистка и сборка
Write-Host "Starting build..."
mvn clean package

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build successful! Starting bot..."
    java -jar target/telegram-business-bot-1.0-SNAPSHOT-jar-with-dependencies.jar
} else {
    Write-Host "Build failed!"
    exit 1
} 