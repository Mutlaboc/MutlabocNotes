// Configure plugin repositories used during Gradle settings evaluation.
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Register settings plugins required for toolchain resolution.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

// Define repositories for all modules and forbid per-project repositories.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Root project metadata and included modules.
rootProject.name = "Mutlaboc's notes"
include(":app")
