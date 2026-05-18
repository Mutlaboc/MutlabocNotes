package com.example.mutlabocsnotes

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseConfigurationTest {

    @Test
    fun gradleReleaseSigningUsesExternalInputsOnly() {
        val buildGradle = projectFile("app/build.gradle.kts").readText()

        listOf(
            "ANDROID_KEYSTORE_FILE",
            "ANDROID_KEYSTORE_PASSWORD",
            "ANDROID_KEY_ALIAS",
            "ANDROID_KEY_PASSWORD"
        ).forEach { propertyName ->
            assertTrue(buildGradle.contains(propertyName))
        }

        assertTrue(buildGradle.contains("providers.environmentVariable(name).orNull"))
        assertTrue(buildGradle.contains("signingConfigs"))
        assertTrue(buildGradle.contains("create(\"release\")"))
        assertTrue(buildGradle.contains("signingConfig = signingConfigs.getByName(\"release\")"))
    }

    @Test
    fun releaseWorkflowIsManualAndBuildsSignedProdArtifacts() {
        val workflow = projectFile(".github/workflows/android-release.yml").readText()

        assertTrue(workflow.contains("workflow_dispatch"))
        assertTrue(workflow.contains("ANDROID_KEYSTORE_BASE64"))
        assertTrue(workflow.contains("ANDROID_KEYSTORE_PASSWORD"))
        assertTrue(workflow.contains("ANDROID_KEY_ALIAS"))
        assertTrue(workflow.contains("ANDROID_KEY_PASSWORD"))
        assertTrue(workflow.contains("PROD_BACKEND_URL"))
        assertTrue(workflow.contains("PROD_GOOGLE_WEB_CLIENT_ID"))
        assertTrue(workflow.contains("PROD_YANDEX_CLIENT_ID"))
        assertTrue(workflow.contains(":app:bundleProdRelease :app:assembleProdRelease"))
        assertTrue(workflow.contains("actions/upload-artifact@v4"))
    }

    @Test
    fun changelogDefinesFirstAndroidReleaseAndTagPolicy() {
        val changelog = projectFile("CHANGELOG.md").readText()

        assertTrue(changelog.contains("1.0.0"))
        assertTrue(changelog.contains("android-v{versionName}"))
        assertTrue(changelog.contains("versionCode"))
        assertTrue(changelog.contains("No crash telemetry SDK"))
    }

    private fun projectFile(path: String): File {
        val userDir = checkNotNull(System.getProperty("user.dir")) {
            "user.dir is not set"
        }
        var directory = File(userDir)
        while (!File(directory, "settings.gradle.kts").exists()) {
            directory = checkNotNull(directory.parentFile) {
                "Could not locate project root"
            }
        }
        return File(directory, path)
    }
}
