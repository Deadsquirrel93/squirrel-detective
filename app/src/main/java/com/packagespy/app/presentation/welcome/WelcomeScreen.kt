package com.packagespy.app.presentation.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.packagespy.app.R
import com.packagespy.app.presentation.components.NutSpyBackground
import com.packagespy.app.presentation.components.NutSpyCard
import com.packagespy.app.presentation.components.nutSpyTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onStartFullScan: (includeSystem: Boolean) -> Unit,
    onChooseApp: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWiki: () -> Unit,
) {
    var includeSystem by rememberSaveable { mutableStateOf(false) }

    NutSpyBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = stringResource(R.string.open_settings),
                            )
                        }
                    },
                    colors = nutSpyTopAppBarColors(),
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = padding.calculateTopPadding() + 4.dp,
                        bottom = padding.calculateBottomPadding() + 16.dp,
                    )
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.welcome_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                )

                FullScanCard(
                    includeSystem = includeSystem,
                    onIncludeSystemChange = { includeSystem = it },
                    onStart = { onStartFullScan(includeSystem) },
                )

                SingleAppCard(onChooseApp = onChooseApp)

                WikiCard(onOpenWiki = onOpenWiki)
            }
        }
    }
}

@Composable
private fun FullScanCard(
    includeSystem: Boolean,
    onIncludeSystemChange: (Boolean) -> Unit,
    onStart: () -> Unit,
) {
    NutSpyCard {
        Text(
            text = stringResource(R.string.mode_full_scan_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.mode_full_scan_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.include_system_apps),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.include_system_apps_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = includeSystem, onCheckedChange = onIncludeSystemChange)
        }
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.start_scan))
        }
    }
}

@Composable
private fun SingleAppCard(onChooseApp: () -> Unit) {
    NutSpyCard {
        Text(
            text = stringResource(R.string.mode_single_app_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.mode_single_app_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onChooseApp, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.choose_app))
        }
    }
}

@Composable
private fun WikiCard(onOpenWiki: () -> Unit) {
    NutSpyCard {
        Text(
            text = stringResource(R.string.wiki_card_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.wiki_card_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onOpenWiki, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.wiki_open))
        }
    }
}
