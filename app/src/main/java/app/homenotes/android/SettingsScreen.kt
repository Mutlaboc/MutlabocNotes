package app.homenotes.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val SETTINGS_LOGOUT_BUTTON_TEST_TAG = "settings_logout_button"
const val SETTINGS_THEME_SWITCH_TEST_TAG = "settings_theme_switch"

@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    selectedLanguage: AppLanguage,
    availableLanguages: List<AppLanguage>,
    onLanguageChange: (AppLanguage) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
) {
    var isLanguageMenuExpanded by remember { mutableStateOf(false) }
    val selectedLanguageName = stringResource(selectedLanguage.displayNameResId)

    Scaffold(
        backgroundColor = CozyAuth.Cream,
        topBar = {
            CozyTopBar(
                title = stringResource(R.string.settings_title),
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CozyAuth.Cream)
                .pixelScreenFrame()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            SettingsSectionTitle(stringResource(R.string.settings_account))
            Spacer(modifier = Modifier.height(8.dp))
            PixelPrimaryButton(
                text = stringResource(R.string.settings_logout),
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SETTINGS_LOGOUT_BUTTON_TEST_TAG)
            )
            Spacer(modifier = Modifier.height(24.dp))

            SettingsSectionTitle(stringResource(R.string.settings_theme))
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isDarkTheme) {
                        stringResource(R.string.settings_theme_dark)
                    } else {
                        stringResource(R.string.settings_theme_light)
                    },
                    color = CozyAuth.InkSoft,
                    fontFamily = CozyAuth.PixelFont,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = onThemeChange,
                    colors = cozySwitchColors(),
                    modifier = Modifier.testTag(SETTINGS_THEME_SWITCH_TEST_TAG)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            SettingsSectionTitle(stringResource(R.string.settings_language))
            Spacer(modifier = Modifier.height(8.dp))
            PixelOutlineButton(
                text = stringResource(R.string.settings_selected_language, selectedLanguageName),
                onClick = { isLanguageMenuExpanded = true },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(
                expanded = isLanguageMenuExpanded,
                onDismissRequest = { isLanguageMenuExpanded = false },
                modifier = Modifier.background(CozyAuth.CardCream)
            ) {
                availableLanguages.forEach { language ->
                    DropdownMenuItem(onClick = {
                        onLanguageChange(language)
                        isLanguageMenuExpanded = false
                    }) {
                        Text(
                            text = stringResource(language.displayNameResId),
                            color = CozyAuth.Ink,
                            fontFamily = CozyAuth.PixelFont
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        color = CozyAuth.Ink,
        fontFamily = CozyAuth.PixelFont,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
}

@Preview
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(
        isDarkTheme = false,
        onThemeChange = {},
        selectedLanguage = AppLanguage.RU,
        availableLanguages = AppLanguage.entries.toList(),
        onLanguageChange = {},
        onLogout = {},
        onBack = {}
    )
}
