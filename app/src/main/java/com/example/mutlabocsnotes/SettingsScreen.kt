package com.example.mutlabocsnotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Composable-функция для отображения экрана настроек.
@Composable
fun SettingsScreen (
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onDeleteAccount: () -> Unit,
    onBack: () -> Unit,
) {
    // TODO реализовать бы смену языков...
    val languages = listOf("Русский", "English", "Deutsch")
    var isLanguageMenuExpanded by remember { mutableStateOf(false) }
    var selectedLanguage by rememberSaveable { mutableStateOf(languages.first()) }

    Scaffold (
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) {paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Аккаунт",
                style = MaterialTheme.typography.subtitle1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDeleteAccount,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Удалить аккаунт")
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Тема",
                style = MaterialTheme.typography.subtitle1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isDarkTheme) "Темная" else "Светлая",
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = onThemeChange
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Язык",
                style = MaterialTheme.typography.subtitle1
            )
            OutlinedButton(
                onClick = { isLanguageMenuExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Выбран $selectedLanguage")
            }
            DropdownMenu(
                expanded = isLanguageMenuExpanded,
                onDismissRequest = {isLanguageMenuExpanded = false}
            ) {
                languages.forEach { language ->
                    DropdownMenuItem(onClick = {
                        selectedLanguage = language
                        isLanguageMenuExpanded = false
                    }) {
                        Text(language)
                    }
                }
            }
        }

    }
}

// Preview-composable для предпросмотра в Android Studio.
@Preview
@Composable
fun SettingsScreenPreview () {
    SettingsScreen(
        isDarkTheme = false,
        onThemeChange = {},
        onDeleteAccount = {},
        onBack = {}
    )
}
