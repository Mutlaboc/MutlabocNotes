package com.example.mutlabocsnotes

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ThemeRegressionTest {

    @Test
    fun editNoteScreen_doesNotOverrideFieldColorsWithFixedBlackOrGray() {
        val editNoteScreen = projectFile(
            "app/src/main/java/com/example/mutlabocsnotes/EditNoteScreen.kt"
        ).readText()

        assertFalse(editNoteScreen.contains("Color.Black"))
        assertFalse(editNoteScreen.contains("Color.Gray"))
        assertFalse(editNoteScreen.contains("outlinedTextFieldColors"))
    }

    @Test
    fun homeInfoValidation_usesThemeErrorColor() {
        val homeInfoScreens = projectFile(
            "app/src/main/java/com/example/mutlabocsnotes/HomeInfoScreens.kt"
        ).readText()

        assertFalse(homeInfoScreens.contains("Color.Red"))
        assertTrue(homeInfoScreens.contains("MaterialTheme.colors.error"))
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
        val homeScreen = projectFile(
            "app/src/main/java/com/example/mutlabocsnotes/HomeScreen.kt"
        ).readText()
        val categoryTheme = projectFile(
            "app/src/main/java/com/example/mutlabocsnotes/NoteCategoryTheme.kt"
        )

        assertTrue(categoryTheme.isFile)
        assertTrue(homeScreen.contains("noteCategoryColors(note.category)"))
        assertFalse(homeScreen.contains("Color(0xFFD9F0FF)"))
        assertFalse(homeScreen.contains("Color(0xFFFFE2E2)"))
        assertFalse(homeScreen.contains("Color(0xFFE1F5E3)"))
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
