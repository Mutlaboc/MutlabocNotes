package app.homenotes.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ThemeRegressionTest {

    @Test
    fun editNoteScreen_doesNotOverrideFieldColorsWithFixedBlackOrGray() {
        val editNoteSources = mainSourceFiles()
            .filter { it.name.startsWith("EditNote") || it.name == "DeadlinePicker.kt" }
            .joinToString("\n") { it.readText() }

        assertFalse(editNoteSources.contains("Color.Black"))
        assertFalse(editNoteSources.contains("Color.Gray"))
        assertFalse(editNoteSources.contains("outlinedTextFieldColors"))
    }

    @Test
    fun homeInfoValidation_usesThemeErrorColor() {
        val editHomeInfoCardScreen = projectFile(
            "app/src/main/java/app/homenotes/android/EditHomeInfoCardScreen.kt"
        ).readText()

        assertFalse(editHomeInfoCardScreen.contains("Color.Red"))
        assertTrue(editHomeInfoCardScreen.contains("MaterialTheme.colors.error"))
    }

    @Test
    fun mainUiSources_doNotUseMaterial3() {
        val material3Imports = projectFile("app/src/main/java")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { source ->
                source.readLines().filter { line ->
                    line.contains("androidx.compose.material3")
                }
            }
            .toList()

        assertTrue(material3Imports.isEmpty())
    }

    @Test
    fun noteItem_usesSharedCategoryColors() {
        val noteItem = projectFile(
            "app/src/main/java/app/homenotes/android/NoteItem.kt"
        ).readText()
        val categoryTheme = projectFile(
            "app/src/main/java/app/homenotes/android/NoteCategoryTheme.kt"
        )

        assertTrue(categoryTheme.isFile)
        assertTrue(noteItem.contains("noteCategoryColors(note.category)"))
        assertFalse(noteItem.contains("Color(0xFFD9F0FF)"))
        assertFalse(noteItem.contains("Color(0xFFFFE2E2)"))
        assertFalse(noteItem.contains("Color(0xFFE1F5E3)"))
    }

    private fun mainSourceFiles(): Sequence<File> {
        return projectFile("app/src/main/java")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
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
