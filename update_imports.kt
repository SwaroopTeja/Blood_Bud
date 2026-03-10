import java.io.File

fun main() {
    val rootDir = File("c:/Users/Conne/AndroidStudioProjects/BloodBud/app/src")
    
    // Find all Kotlin and Java files
    val files = rootDir.walk()
        .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
        .toList()
    
    var updatedFiles = 0
    
    files.forEach { file ->
        val content = file.readText()
        if (content.contains("com.example.shoppy")) {
            val newContent = content.replace("com.example.shoppy", "com.example.blood_bud")
            file.writeText(newContent)
            updatedFiles++
            println("Updated: ${file.absolutePath}")
        }
    }
    
    println("\nUpdated $updatedFiles files.")
}
