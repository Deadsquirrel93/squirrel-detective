package com.packagespy.app.presentation.wiki

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.packagespy.app.R
import com.packagespy.app.presentation.components.NutSpyBackground
import com.packagespy.app.presentation.components.nutSpyTopAppBarColors

private data class WikiEntry(
    val titleRes: Int,
    val bodyRes: Int,
)

private data class WikiCategory(
    val titleRes: Int,
    val entries: List<WikiEntry>,
)

private val WIKI_CATEGORIES: List<WikiCategory> = listOf(
    WikiCategory(
        titleRes = R.string.wiki_category_visibility,
        entries = listOf(
            WikiEntry(R.string.wiki_query_all_title, R.string.wiki_query_all_body),
            WikiEntry(R.string.wiki_usage_stats_title, R.string.wiki_usage_stats_body),
            WikiEntry(R.string.wiki_read_logs_title, R.string.wiki_read_logs_body),
            WikiEntry(R.string.wiki_get_tasks_title, R.string.wiki_get_tasks_body),
        ),
    ),
    WikiCategory(
        titleRes = R.string.wiki_category_services,
        entries = listOf(
            WikiEntry(R.string.wiki_accessibility_title, R.string.wiki_accessibility_body),
            WikiEntry(R.string.wiki_device_admin_title, R.string.wiki_device_admin_body),
            WikiEntry(R.string.wiki_notif_listener_title, R.string.wiki_notif_listener_body),
            WikiEntry(R.string.wiki_vpn_title, R.string.wiki_vpn_body),
            WikiEntry(R.string.wiki_input_method_title, R.string.wiki_input_method_body),
            WikiEntry(R.string.wiki_overlay_title, R.string.wiki_overlay_body),
        ),
    ),
    WikiCategory(
        titleRes = R.string.wiki_category_sensors,
        entries = listOf(
            WikiEntry(R.string.wiki_camera_title, R.string.wiki_camera_body),
            WikiEntry(R.string.wiki_record_audio_title, R.string.wiki_record_audio_body),
        ),
    ),
    WikiCategory(
        titleRes = R.string.wiki_category_communication,
        entries = listOf(
            WikiEntry(R.string.wiki_sms_title, R.string.wiki_sms_body),
            WikiEntry(R.string.wiki_contacts_title, R.string.wiki_contacts_body),
            WikiEntry(R.string.wiki_call_log_title, R.string.wiki_call_log_body),
            WikiEntry(R.string.wiki_phone_state_title, R.string.wiki_phone_state_body),
            WikiEntry(R.string.wiki_get_accounts_title, R.string.wiki_get_accounts_body),
        ),
    ),
    WikiCategory(
        titleRes = R.string.wiki_category_location,
        entries = listOf(
            WikiEntry(R.string.wiki_location_title, R.string.wiki_location_body),
            WikiEntry(R.string.wiki_bg_location_title, R.string.wiki_bg_location_body),
        ),
    ),
    WikiCategory(
        titleRes = R.string.wiki_category_storage,
        entries = listOf(
            WikiEntry(R.string.wiki_manage_storage_title, R.string.wiki_manage_storage_body),
            WikiEntry(R.string.wiki_storage_title, R.string.wiki_storage_body),
        ),
    ),
    WikiCategory(
        titleRes = R.string.wiki_category_system,
        entries = listOf(
            WikiEntry(R.string.wiki_internet_title, R.string.wiki_internet_body),
            WikiEntry(R.string.wiki_install_packages_title, R.string.wiki_install_packages_body),
            WikiEntry(R.string.wiki_write_settings_title, R.string.wiki_write_settings_body),
            WikiEntry(R.string.wiki_post_notifications_title, R.string.wiki_post_notifications_body),
            WikiEntry(R.string.wiki_boot_title, R.string.wiki_boot_body),
            WikiEntry(R.string.wiki_foreground_service_title, R.string.wiki_foreground_service_body),
            WikiEntry(R.string.wiki_wake_lock_title, R.string.wiki_wake_lock_body),
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WikiScreen(onBack: () -> Unit) {
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
                item {
                    Text(
                        text = stringResource(R.string.wiki_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
                items(WIKI_CATEGORIES) { category ->
                    WikiCategoryCard(category)
                }
            }
        }
    }
}

@Composable
private fun WikiCategoryCard(category: WikiCategory) {
    var expanded by rememberSaveable(category.titleRes) { mutableStateOf(false) }

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
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                )
            }
            AnimatedVisibility(visible = expanded) {
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
            fontFamily = FontFamily.Monospace,
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
