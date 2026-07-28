package app.homenotes.android

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal data class NoteCategoryColors(
    val container: Color,
    val content: Color
)

@Composable
internal fun noteCategoryColors(category: NoteCategory): NoteCategoryColors {
    val isLight = MaterialTheme.colors.isLight
    return when (category) {
        NoteCategory.SHOPPING -> if (isLight) {
            NoteCategoryColors(
                container = Color(0xFFD9F0FF),
                content = Color(0xFF12384A)
            )
        } else {
            NoteCategoryColors(
                container = Color(0xFF14384A),
                content = Color(0xFFD7F0FF)
            )
        }

        NoteCategory.TASKS -> if (isLight) {
            NoteCategoryColors(
                container = Color(0xFFFFE2E2),
                content = Color(0xFF5C2020)
            )
        } else {
            NoteCategoryColors(
                container = Color(0xFF4D2323),
                content = Color(0xFFFFE0E0)
            )
        }

        NoteCategory.RECURRING_TASKS -> if (isLight) {
            NoteCategoryColors(
                container = Color(0xFFE1F5E3),
                content = Color(0xFF1D4022)
            )
        } else {
            NoteCategoryColors(
                container = Color(0xFF1E3D25),
                content = Color(0xFFE2F7E5)
            )
        }
    }
}
