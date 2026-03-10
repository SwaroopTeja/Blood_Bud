pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://androidx.dev/storage/compose-compiler/repository/") }
    }
}

rootProject.name = "BloodBud"
include(":app")

// Configure build cache (optional but recommended for build performance)
buildCache {
    local {
        // Use the default Gradle cache directory
        directory = File(rootDir, "build-cache")
    }
}
