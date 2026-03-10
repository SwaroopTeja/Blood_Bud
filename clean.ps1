# Clean script for BloodBud project
Write-Host "Cleaning project..."

# Remove build directories
if (Test-Path "app\build") {
    Remove-Item -Recurse -Force "app\build"
}
if (Test-Path "build") {
    Remove-Item -Recurse -Force "build"
}

# Remove .gradle directory
if (Test-Path ".gradle") {
    Remove-Item -Recurse -Force ".gradle"
}

# Remove .idea/libraries
if (Test-Path ".idea\libraries") {
    Remove-Item -Recurse -Force ".idea\libraries"
}

Write-Host "Project cleaned successfully!"
Write-Host "Please sync project with Gradle files in Android Studio."
