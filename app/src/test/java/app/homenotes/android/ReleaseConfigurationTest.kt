package app.homenotes.android

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
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
        assertFalse(releaseBuildTypeBlock(buildGradle).contains("signingConfigs.getByName(\"debug\")"))
    }

    @Test
    fun devFlavorHasCompileSafeOauthPlaceholders() {
        val buildGradle = projectFile("app/build.gradle.kts").readText()

        assertTrue(buildGradle.contains("DEV_GOOGLE_WEB_CLIENT_ID_PLACEHOLDER"))
        assertTrue(buildGradle.contains("dev-google-placeholder.apps.googleusercontent.com"))
        assertTrue(buildGradle.contains("DEV_YANDEX_CLIENT_ID_PLACEHOLDER"))
        assertTrue(buildGradle.contains("dev-yandex-placeholder"))
        assertTrue(buildGradle.contains("googleWebClientIdFallback = DEV_GOOGLE_WEB_CLIENT_ID_PLACEHOLDER"))
        assertTrue(buildGradle.contains("yandexClientIdFallback = DEV_YANDEX_CLIENT_ID_PLACEHOLDER"))
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
    fun androidSmokeWorkflowDoesNotRequireOauthSecretsForDevBuilds() {
        val workflow = projectFile(".github/workflows/android-unit-tests.yml").readText()

        assertTrue(workflow.contains("./gradlew assembleDevDebug testDevDebugUnitTest lintDevDebug"))
        assertFalse(workflow.contains("secrets.DEV_GOOGLE_WEB_CLIENT_ID"))
        assertFalse(workflow.contains("secrets.DEV_YANDEX_CLIENT_ID"))
        assertFalse(workflow.contains("vars.DEV_GOOGLE_WEB_CLIENT_ID"))
        assertFalse(workflow.contains("vars.DEV_YANDEX_CLIENT_ID"))
    }

    @Test
    fun changelogDefinesFirstAndroidReleaseAndTagPolicy() {
        val changelog = projectFile("CHANGELOG.md").readText()

        assertTrue(changelog.contains("1.0.1"))
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

    private fun releaseBuildTypeBlock(buildGradle: String): String {
        val start = buildGradle.indexOf("getByName(\"release\")")
        check(start >= 0) { "release build type block not found" }
        val nextSection = buildGradle.indexOf("\n    compileOptions", start)
        check(nextSection > start) { "release build type block end not found" }
        return buildGradle.substring(start, nextSection)
    }
}
