package com.example.mutlabocsnotes

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// Регрессионные проверки DI-wiring без Robolectric: читаем исходники и манифест как обычные файлы.
class AppContainerWiringTest {

    @Test
    fun manifest_registersCustomApplication() {
        val manifest = projectFile("app/src/main/AndroidManifest.xml").readText()

        assertTrue(
            manifest.contains("""android:name=".MutlabocNotesApplication"""")
        )
    }

    @Test
    fun appContainer_ownsSharedDependencyCreation() {
        val appContainer = projectFile(
            "app/src/main/java/com/example/mutlabocsnotes/AppContainer.kt"
        ).readText()

        assertTrue(appContainer.contains("val sessionManager: SessionManager by lazy"))
        assertTrue(appContainer.contains("val authRepository: AuthSessionRepository by lazy"))
        assertTrue(appContainer.contains("val notesRepository: NotesRepository by lazy"))
        assertTrue(appContainer.contains("val homeInfoRepository: HomeInfoRepository by lazy"))
        assertTrue(appContainer.contains("val deadlineNotificationScheduler: DeadlineNotificationScheduler by lazy"))
        assertTrue(appContainer.contains("class MutlabocNotesViewModelFactory"))
    }

    @Test
    fun myApp_usesFactoryAndDoesNotCreateSessionManagerDirectly() {
        val mainActivity = projectFile(
            "app/src/main/java/com/example/mutlabocsnotes/MainActivity.kt"
        ).readText()
        val myAppBody = mainActivity.substringAfter("fun MyApp(")

        assertTrue(myAppBody.contains("viewModelFactory: ViewModelProvider.Factory"))
        assertTrue(myAppBody.contains("viewModel(factory = viewModelFactory)"))
        assertFalse(myAppBody.contains("SessionManager("))
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
