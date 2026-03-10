# PowerShell script to update package names from com.example.shoppy to com.example.blood_bud

# Define the root directory
$rootDir = "c:\Users\Conne\AndroidStudioProjects\BloodBud"

# Find all Kotlin and Java files
$files = Get-ChildItem -Path $rootDir -Recurse -Include *.kt, *.java, *.xml, *.gradle*

# Count of files that will be modified
$fileCount = 0

foreach ($file in $files) {
    $content = Get-Content -Path $file.FullName -Raw
    
    # Check if the file contains the old package name
    if ($content -match "com\.example\.shoppy") {
        $fileCount++
        Write-Host "Updating: $($file.FullName)"
        
        # Replace the old package name with the new one
        $newContent = $content -replace "com\.example\.shoppy", "com.example.blood_bud"
        
        # Write the updated content back to the file
        $newContent | Set-Content -Path $file.FullName -NoNewline -Encoding UTF8
    }
}

Write-Host "\nUpdated $fileCount files to use the new package name."
