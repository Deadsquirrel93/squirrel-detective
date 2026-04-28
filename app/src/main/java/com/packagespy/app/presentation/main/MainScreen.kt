package com.packagespy.app.presentation.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.packagespy.app.R
import com.packagespy.app.domain.model.AppRiskInfo
import com.packagespy.app.domain.model.ScanProgress
import com.packagespy.app.presentation.components.AppIcon
import com.packagespy.app.presentation.components.NutSpyBackground
import com.packagespy.app.presentation.components.RiskBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    includeSystem: Boolean,
    onAppClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(includeSystem) {
        viewModel.ensureInitialScan(includeSystem)
    }

    NutSpyBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    stringResource(R.string.results_title),
                                    fontWeight = FontWeight.Bold,
                                )
                                val subtitle = when {
                                    state.isRescanning -> {
                                        val p = state.scanProgress
                                        if (p != null && p.total > 0) {
                                            stringResource(
                                                R.string.results_scanning_progress,
                                                p.current,
                                                p.total,
                                            )
                                        } else {
                                            stringResource(R.string.results_scanning)
                                        }
                                    }
                                    else -> stringResource(
                                        R.string.results_subtitle_count,
                                        state.risky.size,
                                    )
                                }
                                Text(
                                    subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.results_back),
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { viewModel.rescan() },
                                enabled = !state.isRescanning,
                            ) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = stringResource(R.string.results_rescan),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    )
                    ScanProgressBar(
                        isScanning = state.isRescanning,
                        progress = state.scanProgress,
                    )
                }
            },
        ) { padding ->
            if (state.isLoading) {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val p = state.scanProgress
                        if (p != null && p.total > 0) {
                            val animated by animateFloatAsState(
                                targetValue = p.fraction,
                                label = "scan_progress",
                            )
                            CircularProgressIndicator(progress = { animated })
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(
                                    R.string.results_scanning_progress,
                                    p.current,
                                    p.total,
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.results_preparing),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                return@Scaffold
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
            ) {
                item { FilterRow(state.filter, viewModel::setFilter) }

                if (state.recentChanges.isNotEmpty()) {
                    item { ChangesBanner(changesCount = state.recentChanges.size) }
                }

                if (state.risky.isEmpty()) {
                    item { EmptyRisky() }
                } else {
                    items(state.risky, key = { it.packageName }) { app ->
                        AppRow(app = app, onClick = { onAppClick(app.packageName) })
                    }
                }

                item {
                    SafeAppsSection(
                        expanded = state.showSafe,
                        apps = state.safe,
                        onToggle = viewModel::toggleSafe,
                        onClick = onAppClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanProgressBar(isScanning: Boolean, progress: ScanProgress?) {
    if (!isScanning) return
    if (progress != null && progress.total > 0) {
        val animated by animateFloatAsState(
            targetValue = progress.fraction,
            label = "topbar_scan_progress",
        )
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun FilterRow(current: RiskFilter, onSelect: (RiskFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RiskFilter.values().forEach { filter ->
            FilterChip(
                selected = current == filter,
                onClick = { onSelect(filter) },
                label = {
                    val labelRes = when (filter) {
                        RiskFilter.ALL -> R.string.filter_all
                        RiskFilter.RED -> R.string.filter_red
                        RiskFilter.YELLOW -> R.string.filter_yellow
                        RiskFilter.GREEN -> R.string.filter_green
                    }
                    Text(stringResource(labelRes))
                },
            )
        }
    }
}

@Composable
private fun ChangesBanner(changesCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.results_changes_banner, changesCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun AppRow(app: AppRiskInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(packageName = app.packageName)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    RiskBadge(app.riskLevel)
                }
                Spacer(Modifier.height(2.dp))
                val shortRes = app.reasons.firstOrNull()?.shortTextRes
                Text(
                    text = if (shortRes != null && shortRes != 0) {
                        stringResource(shortRes)
                    } else {
                        stringResource(R.string.detail_short_default)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyRisky() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.results_empty_risky),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SafeAppsSection(
    expanded: Boolean,
    apps: List<AppRiskInfo>,
    onToggle: () -> Unit,
    onClick: (String) -> Unit,
) {
    Column {
        TextButton(
            onClick = onToggle,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
            )
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.results_safe_section, apps.size))
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                apps.forEach { app ->
                    AppRow(app = app, onClick = { onClick(app.packageName) })
                }
            }
        }
    }
}
