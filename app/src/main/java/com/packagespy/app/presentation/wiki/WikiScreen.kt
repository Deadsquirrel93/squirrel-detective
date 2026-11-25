package com.packagespy.app.presentation.wiki

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.packagespy.app.R
import com.packagespy.app.presentation.components.NutSpyBackground
import com.packagespy.app.presentation.components.nutSpyTextFieldColors
import com.packagespy.app.presentation.components.nutSpyTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WikiScreen(onBack: () -> Unit) {
    val resources = LocalResources.current
    var query by rememberSaveable { mutableStateOf("") }
    val trimmed = query.trim()

    val filteredCategories = remember(trimmed, resources) {
        if (trimmed.isEmpty()) {
            PermissionCatalog.categories
        } else {
            PermissionCatalog.categories.mapNotNull { category ->
                val catMatches = resources.getString(category.titleRes)
                    .contains(trimmed, ignoreCase = true)
                val matchingEntries = if (catMatches) {
                    category.entries
                } else {
                    category.entries.filter { entry ->
                        resources.getString(entry.titleRes).contains(trimmed, ignoreCase = true) ||
                            resources.getString(entry.bodyRes).contains(trimmed, ignoreCase = true) ||
                            entry.permissions.any { it.contains(trimmed, ignoreCase = true) }
                    }
                }
                if (matchingEntries.isEmpty()) null else category.copy(entries = matchingEntries)
            }
        }
    }

    NutSpyBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.wiki_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.results_back),
                            )
                        }
                    },
                    colors = nutSpyTopAppBarColors(),
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                    start = 12.dp,
                    end = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "wiki_search") {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        },
                        placeholder = { Text(stringResource(R.string.wiki_search_hint)) },
                        colors = nutSpyTextFieldColors(),
                    )
                }
                if (trimmed.isEmpty()) {
                    item(key = "wiki_intro") {
                        Text(
                            text = stringResource(R.string.wiki_intro),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                    }
                }
                if (filteredCategories.isEmpty()) {
                    item(key = "wiki_empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.wiki_no_results),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    items(filteredCategories, key = { it.titleRes }) { category ->
                        WikiCategoryCard(
                            category = category,
                            forceExpanded = trimmed.isNotEmpty(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WikiCategoryCard(
    category: WikiCategory,
    forceExpanded: Boolean,
) {
    var expanded by rememberSaveable(category.titleRes) { mutableStateOf(false) }
    val isExpanded = forceExpanded || expanded

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(category.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SelectionContainer {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            category.entries.forEachIndexed { index, entry ->
                                WikiEntryRow(entry)
                                if (index != category.entries.lastIndex) {
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WikiEntryRow(entry: WikiEntry) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(entry.titleRes),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = if (entry.isMonospaceTitle) FontFamily.Monospace else FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(entry.bodyRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
