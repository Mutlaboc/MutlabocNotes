package com.example.mutlabocsnotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

internal fun filterHomeInfoCards(
    cards: List<HomeInfoCard>,
    query: String,
    selectedSection: HomeSection?,
    sectionLabel: (HomeSection) -> String
): List<HomeInfoCard> {
    val queryText = query.trim()
    return cards.filter { card ->
        val sectionText = sectionLabel(card.section)
        val matchesSection = selectedSection == null || card.section == selectedSection
        val matchesQuery = queryText.isBlank() ||
            card.title.contains(queryText, ignoreCase = true) ||
            card.note.contains(queryText, ignoreCase = true) ||
            card.fields.any { field ->
                field.key.contains(queryText, ignoreCase = true) ||
                    field.value.contains(queryText, ignoreCase = true)
            } ||
            card.links.any { it.contains(queryText, ignoreCase = true) } ||
            sectionText.contains(queryText, ignoreCase = true)
        matchesSection && matchesQuery
    }
}

@Composable
internal fun HomeInfoFilterRow(
    selectedSection: HomeSection?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSectionSelected: (HomeSection?) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.home_info_section))
        Box {
            OutlinedButton(onClick = { onExpandedChange(true) }) {
                Text(
                    selectedSection?.let { sectionLabel(it) }
                        ?: stringResource(R.string.home_info_all_sections)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                DropdownMenuItem(onClick = {
                    onSectionSelected(null)
                    onExpandedChange(false)
                }) {
                    Text(stringResource(R.string.home_info_all_sections))
                }
                HomeSection.values().forEach { section ->
                    DropdownMenuItem(onClick = {
                        onSectionSelected(section)
                        onExpandedChange(false)
                    }) {
                        Text(sectionLabel(section))
                    }
                }
            }
        }
    }
}
