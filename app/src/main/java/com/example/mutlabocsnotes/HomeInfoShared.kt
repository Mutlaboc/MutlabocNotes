package com.example.mutlabocsnotes

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

const val HOME_INFO_SEARCH_FIELD_TEST_TAG = "home_info_search_field"
const val HOME_INFO_LINK_WARNING_TEST_TAG = "home_info_link_warning"
const val EDIT_HOME_INFO_SAVE_BUTTON_TEST_TAG = "edit_home_info_save_button"

fun homeInfoCardTestTag(cardId: String): String = "home_info_card_$cardId"

fun homeInfoLinkTestTag(cardId: String, index: Int): String = "home_info_link_${cardId}_$index"

fun editHomeInfoLinkFieldTestTag(index: Int): String = "edit_home_info_link_field_$index"

@Composable
fun SectionPicker(
    selectedSection: HomeSection,
    onSectionSelected: (HomeSection) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.home_info_section))
        Spacer(modifier = Modifier.width(12.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(sectionLabel(selectedSection))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                HomeSection.values().forEach { section ->
                    DropdownMenuItem(onClick = {
                        onSectionSelected(section)
                        expanded = false
                    }) {
                        Text(sectionLabel(section))
                    }
                }
            }
        }
    }
}

@Composable
fun sectionLabel(section: HomeSection): String {
    return stringResource(section.labelResId())
}

@StringRes
fun HomeSection.labelResId(): Int = when (this) {
    HomeSection.METERS -> R.string.home_section_meters
    HomeSection.APPLIANCES -> R.string.home_section_appliances
    HomeSection.LIGHTING -> R.string.home_section_lighting
    HomeSection.DOCUMENTS -> R.string.home_section_documents
    HomeSection.CONTACTS -> R.string.home_section_contacts
    HomeSection.OTHER -> R.string.home_section_other
}

@Composable
fun HomeField.displayText(): String {
    val trimmedKey = key.trim()
    val trimmedValue = value.trim()
    return when {
        trimmedKey.isNotBlank() && trimmedValue.isNotBlank() ->
            stringResource(R.string.home_info_field_value, trimmedKey, trimmedValue)
        trimmedKey.isNotBlank() -> trimmedKey
        else -> trimmedValue
    }
}

fun normalizeHomeInfoLinks(links: List<String>): List<String> {
    return links.mapNotNull { rawLink ->
        val trimmed = rawLink.trim()
        when {
            trimmed.isBlank() -> null
            shouldPrefixHttps(trimmed) -> "https://$trimmed"
            else -> trimmed
        }
    }
}

fun hasSuspiciousHomeInfoLinks(links: List<String>): Boolean {
    return links.any(::isSuspiciousHomeInfoLink)
}

private fun shouldPrefixHttps(link: String): Boolean {
    if (Uri.parse(link).scheme != null) return false
    val authority = link.substringBefore('/')
    val host = authority.substringBefore(':')
    val port = authority.substringAfter(':', missingDelimiterValue = "")
    return !link.hasAnyWhitespace() &&
        host.contains('.') &&
        (port.isBlank() || port.all { it.isDigit() })
}

private fun isSuspiciousHomeInfoLink(link: String): Boolean {
    if (link.hasAnyWhitespace()) return true
    val uri = runCatching { Uri.parse(link) }.getOrNull() ?: return true
    val scheme = uri.scheme
    if (scheme == null) return false
    return when (scheme.lowercase()) {
        "http", "https" -> uri.host.isNullOrBlank()
        else -> false
    }
}

private fun String.hasAnyWhitespace(): Boolean = any { it.isWhitespace() }
