// Настройка репозиториев плагинов во время обработки Gradle settings.
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

// Подключение плагинов settings, необходимых для разрешения toolchain.
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

// Определяем репозитории для всех модулей и запрещаем репозитории на уровне отдельных проектов.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://jitpack.io")
        mavenCentral()
    }
}

// Метаданные корневого проекта и подключённые модули.
rootProject.name = "Mutlaboc's notes"
include(":app")
