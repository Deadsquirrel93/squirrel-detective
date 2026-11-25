package com.packagespy.app.presentation.detail

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.packagespy.app.R
import com.packagespy.app.domain.model.AppRiskInfo
import com.packagespy.app.domain.model.ReceiverInfo
import com.packagespy.app.domain.model.RiskLevel
import com.packagespy.app.domain.model.ThreatCombo
import com.packagespy.app.domain.model.ThreatId
import com.packagespy.app.domain.model.ThreatReason
import com.packagespy.app.domain.model.displayPermissionName
import com.packagespy.app.presentation.components.AppIcon
import com.packagespy.app.presentation.components.NutSpyBackground
import com.packagespy.app.presentation.components.RiskBadge
import com.packagespy.app.presentation.components.nutSpyTopAppBarColors
import com.packagespy.app.presentation.theme.RiskColors
import com.packagespy.app.presentation.wiki.PermissionCatalog
import com.packagespy.app.presentation.wiki.PermissionProtection
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedPermission by remember { mutableStateOf<String?>(null) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshOnResume()
        onPauseOrDispose { viewModel.onPaused() }
    }

    NutSpyBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        val title = (state as? DetailUiState.Loaded)?.app?.appName
                            ?: stringResource(R.string.detail_title_fallback)
                        Text(title)
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
                        val app = (state as? DetailUiState.Loaded)?.app
                        if (app != null) {
                            IconButton(onClick = { shareAppReport(context, app) }) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = stringResource(R.string.detail_share_report),
                                )
                            }
                            if (!app.isArchive) {
                                val errorMessage = stringResource(R.string.detail_open_in_system_settings_failed)
                                IconButton(onClick = {
                                    openAppSystemSettings(context, app.packageName, errorMessage)
                                }) {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = stringResource(R.string.detail_open_in_system_settings),
                                    )
                                }
                            }
                        }
                    },
                    colors = nutSpyTopAppBarColors(),
                )
            },
        ) { padding ->
            when (val current = state) {
                DetailUiState.Loading -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
                is DetailUiState.NotFound -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(
                            if (current.isArchive) R.string.detail_apk_invalid else R.string.detail_not_found,
                        ),
                    )
                }
                is DetailUiState.Loaded -> {
                    DetailContent(
                        app = current.app,
                        padding = padding,
                        onToggleTrusted = viewModel::toggleTrusted,
                        onPermissionClick = { selectedPermission = it },
                    )
                    selectedPermission?.let { permission ->
                        PermissionDetailSheet(
                            permission = permission,
                            isGranted = if (current.app.isArchive) null else permission in current.app.grantedPermissions,
                            onDismiss = { selectedPermission = null },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailContent(
    app: AppRiskInfo,
    padding: PaddingValues,
    onToggleTrusted: () -> Unit,
    onPermissionClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val settingsError = stringResource(R.string.detail_open_in_system_settings_failed)
    val grantedCount = remember(app.permissions, app.grantedPermissions) {
        app.permissions.count { it in app.grantedPermissions }
    }
    val sortedPermissions = remember(app.permissions, app.grantedPermissions) {
        app.permissions.sortedWith(
            compareByDescending<String> { it in app.grantedPermissions }
                .thenBy { displayPermissionName(it).lowercase() },
        )
    }

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
        item { HeaderCard(app = app, onToggleTrusted = onToggleTrusted) }

        if (app.combos.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.detail_section_combos)) }
            items(app.combos, key = { "combo:" + it.id.name }) { combo ->
                ComboCard(combo)
            }
        }

        if (app.reasons.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.detail_section_findings)) }
            items(app.reasons, key = { "reason:" + it.id.name }) { reason ->
                val target = if (app.isArchive) null else settingsTargetFor(reason.id)
                ReasonCard(
                    reason = reason,
                    settingsButtonRes = target?.buttonLabelRes,
                    onOpenSettings = target?.let { t ->
                        { openTargetedSettings(context, app.packageName, t, settingsError) }
                    },
                )
            }
        }

        if (sortedPermissions.isNotEmpty()) {
            item {
                SectionTitle(
                    if (app.isArchive) {
                        stringResource(R.string.detail_section_permissions, sortedPermissions.size)
                    } else {
                        stringResource(
                            R.string.detail_section_permissions_with_granted,
                            sortedPermissions.size,
                            grantedCount,
                        )
                    },
                )
            }
            item {
                PermissionsList(
                    permissions = sortedPermissions,
                    grantedPermissions = app.grantedPermissions,
                    showGrantStatus = !app.isArchive,
                    onPermissionClick = onPermissionClick,
                )
            }
        }

        val packageReceivers = app.receivers.filter { it.actions.isNotEmpty() }
        if (packageReceivers.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.detail_section_receivers)) }
            items(packageReceivers) { ReceiverCard(it) }
        }
    }
}

@Composable
private fun HeaderCard(
    app: AppRiskInfo,
    onToggleTrusted: () -> Unit,
) {
    val context = LocalContext.current
    val dateFormat = remember(context) { DateFormat.getMediumDateFormat(context) }
    val isBuiltInWhitelisted = remember(app.reasons) {
        app.reasons.any { it.id.name.startsWith("KNOWN_LEGITIMATE_") }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(packageName = app.packageName)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    app.versionName?.let {
                        Text(
                            stringResource(R.string.detail_version_label, it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    app.installerPackage?.let {
                        Text(
                            stringResource(R.string.detail_installer_label, it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (app.targetSdk > 0) {
                        Text(
                            stringResource(R.string.detail_target_sdk_label, app.targetSdk),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (app.firstInstallTime > 0L) {
                        Text(
                            stringResource(
                                R.string.detail_installed_label,
                                dateFormat.format(Date(app.firstInstallTime)),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (app.lastUpdateTime > 0L && app.lastUpdateTime != app.firstInstallTime) {
                        Text(
                            stringResource(
                                R.string.detail_updated_label,
                                dateFormat.format(Date(app.lastUpdateTime)),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (app.isArchive) {
                        Text(
                            stringResource(R.string.detail_apk_archive),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    if (app.isSystemApp) {
                        Text(
                            stringResource(R.string.detail_system_app),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (app.usesCleartextTraffic) {
                        Text(
                            stringResource(R.string.detail_cleartext_traffic),
                            style = MaterialTheme.typography.labelSmall,
                            color = RiskColors.textFor(RiskLevel.YELLOW),
                        )
                    }
                }
                RiskBadge(app.riskLevel)
            }
            if (!isBuiltInWhitelisted && !app.isArchive) {
                Spacer(Modifier.height(10.dp))
                FilterChip(
                    selected = app.isUserTrusted,
                    onClick = onToggleTrusted,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    label = {
                        Text(
                            stringResource(
                                if (app.isUserTrusted) {
                                    R.string.detail_trusted_active
                                } else {
                                    R.string.detail_trust_app
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun ComboCard(combo: ThreatCombo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RiskColors.bgFor(combo.severity)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = RiskColors.textFor(combo.severity),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(combo.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = RiskColors.textFor(combo.severity),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                RiskBadge(combo.severity)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(combo.summaryRes),
                style = MaterialTheme.typography.bodySmall,
                color = RiskColors.textFor(combo.severity),
            )
        }
    }
}

@Composable
private fun ReasonCard(
    reason: ThreatReason,
    @StringRes settingsButtonRes: Int? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = RiskColors.bgFor(reason.severity)),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (reason.shortTextRes != 0) {
                        stringResource(reason.shortTextRes)
                    } else {
                        reason.id.name
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = RiskColors.textFor(reason.severity),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                RiskBadge(reason.severity)
            }
            if (reason.explanationRes != 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(reason.explanationRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = RiskColors.textFor(reason.severity),
                )
            }
            if (settingsButtonRes != null && onOpenSettings != null) {
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = onOpenSettings,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = stringResource(settingsButtonRes),
                        color = RiskColors.textFor(reason.severity),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = RiskColors.textFor(reason.severity),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionsList(
    permissions: List<String>,
    grantedPermissions: Set<String>,
    showGrantStatus: Boolean,
    onPermissionClick: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            permissions.forEachIndexed { index, permission ->
                val isGranted = !showGrantStatus || permission in grantedPermissions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPermissionClick(permission) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (showGrantStatus) {
                        Icon(
                            imageVector = if (isGranted) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.RadioButtonUnchecked
                            },
                            contentDescription = stringResource(
                                if (isGranted) R.string.permission_granted else R.string.permission_not_granted,
                            ),
                            tint = if (isGranted) {
                                RiskColors.textFor(RiskLevel.GREEN)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            },
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = displayPermissionName(permission),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isGranted) FontWeight.Medium else FontWeight.Normal,
                            color = if (isGranted) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        if (showGrantStatus && !isGranted) {
                            Text(
                                text = stringResource(R.string.permission_not_granted),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp),
                    )
                }
                if (index != permissions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PermissionDetailSheet(
    permission: String,
    isGranted: Boolean?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val details = remember(permission, context) {
        PermissionCatalog.resolveDetails(context, permission)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SelectionContainer {
                Text(
                    text = details.fullName,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (isGranted != null) {
                    StatusPill(
                        text = stringResource(
                            if (isGranted) R.string.permission_granted else R.string.permission_not_granted,
                        ),
                        containerColor = if (isGranted) {
                            RiskColors.bgFor(RiskLevel.GREEN)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (isGranted) {
                            RiskColors.textFor(RiskLevel.GREEN)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                val protRes = when (details.protection) {
                    PermissionProtection.DANGEROUS -> R.string.permission_level_dangerous
                    PermissionProtection.NORMAL -> R.string.permission_level_normal
                    PermissionProtection.SIGNATURE -> R.string.permission_level_signature
                    PermissionProtection.OTHER -> R.string.permission_level_other
                }
                val protBg = if (details.protection == PermissionProtection.DANGEROUS) {
                    RiskColors.bgFor(RiskLevel.YELLOW)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                val protFg = if (details.protection == PermissionProtection.DANGEROUS) {
                    RiskColors.textFor(RiskLevel.YELLOW)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                StatusPill(
                    text = stringResource(protRes),
                    containerColor = protBg,
                    contentColor = protFg,
                )
            }
            details.systemLabel?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            val bodyText = when {
                details.wikiEntry != null -> stringResource(details.wikiEntry.bodyRes)
                details.systemDescription != null -> details.systemDescription
                else -> stringResource(R.string.permission_no_description)
            }
            SelectionContainer {
                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

private enum class SettingsTarget(@StringRes val buttonLabelRes: Int) {
    ACCESSIBILITY(R.string.detail_open_accessibility_settings),
    NOTIFICATION_LISTENER(R.string.detail_open_notification_settings),
    DEVICE_ADMIN(R.string.detail_open_device_admin_settings),
    INPUT_METHOD(R.string.detail_open_keyboard_settings),
    OVERLAY(R.string.detail_open_overlay_settings),
    USAGE_ACCESS(R.string.detail_open_usage_settings),
    UNKNOWN_SOURCES(R.string.detail_open_install_settings),
    ALL_FILES(R.string.detail_open_all_files_settings),
    WRITE_SETTINGS(R.string.detail_open_write_settings),
}

private fun settingsTargetFor(id: ThreatId): SettingsTarget? = when (id) {
    ThreatId.BIND_ACCESSIBILITY, ThreatId.ACCESSIBILITY_ENABLED -> SettingsTarget.ACCESSIBILITY
    ThreatId.NOTIFICATION_LISTENER, ThreatId.NOTIFICATION_LISTENER_ENABLED -> SettingsTarget.NOTIFICATION_LISTENER
    ThreatId.DEVICE_ADMIN, ThreatId.DEVICE_ADMIN_ACTIVE -> SettingsTarget.DEVICE_ADMIN
    ThreatId.INPUT_METHOD_SERVICE, ThreatId.INPUT_METHOD_ENABLED -> SettingsTarget.INPUT_METHOD
    ThreatId.OVERLAY_WINDOW -> SettingsTarget.OVERLAY
    ThreatId.PACKAGE_USAGE_STATS -> SettingsTarget.USAGE_ACCESS
    ThreatId.REQUEST_INSTALL_PACKAGES -> SettingsTarget.UNKNOWN_SOURCES
    ThreatId.EXTERNAL_STORAGE_ALL -> SettingsTarget.ALL_FILES
    ThreatId.WRITE_SETTINGS_OR_SECURE -> SettingsTarget.WRITE_SETTINGS
    else -> null
}

private fun openTargetedSettings(
    context: Context,
    packageName: String,
    target: SettingsTarget,
    errorMessage: String,
) {
    val pkgUri = Uri.fromParts("package", packageName, null)
    val candidates: List<Intent> = when (target) {
        SettingsTarget.ACCESSIBILITY -> listOf(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        SettingsTarget.NOTIFICATION_LISTENER -> listOf(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        SettingsTarget.DEVICE_ADMIN -> listOf(
            Intent().setComponent(
                ComponentName("com.android.settings", "com.android.settings.DeviceAdminSettings"),
            ),
            Intent(Settings.ACTION_SECURITY_SETTINGS),
        )
        SettingsTarget.INPUT_METHOD -> listOf(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        SettingsTarget.OVERLAY -> listOf(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri),
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
        )
        SettingsTarget.USAGE_ACCESS -> listOf(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, pkgUri),
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
        )
        SettingsTarget.UNKNOWN_SOURCES -> listOf(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, pkgUri),
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES),
        )
        SettingsTarget.ALL_FILES -> buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, pkgUri))
                add(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
            add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri))
        }
        SettingsTarget.WRITE_SETTINGS -> listOf(
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, pkgUri),
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS),
        )
    }

    for (intent in candidates) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: ActivityNotFoundException) {
            // Try fallback intent
        } catch (_: SecurityException) {
            // Some OEMs restrict direct internal components like DeviceAdminSettings
        }
    }
    openAppSystemSettings(context, packageName, errorMessage)
}

private fun openAppSystemSettings(context: Context, packageName: String, errorMessage: String) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    }
}

private fun shareAppReport(context: Context, app: AppRiskInfo) {
    val riskLabel = context.getString(
        when (app.riskLevel) {
            RiskLevel.RED -> R.string.risk_red
            RiskLevel.YELLOW -> R.string.risk_yellow
            RiskLevel.GREEN -> R.string.risk_green
            RiskLevel.SAFE -> R.string.risk_safe
        },
    )
    val text = buildString {
        appendLine("${app.appName} (${app.packageName})")
        app.versionName?.let { appendLine(context.getString(R.string.detail_version_label, it)) }
        app.installerPackage?.let { appendLine(context.getString(R.string.detail_installer_label, it)) }
        if (app.targetSdk > 0) appendLine(context.getString(R.string.detail_target_sdk_label, app.targetSdk))
        appendLine("${context.getString(R.string.share_risk_label)}: $riskLabel")

        if (app.combos.isNotEmpty()) {
            appendLine()
            appendLine(context.getString(R.string.detail_section_combos))
            app.combos.forEach { combo ->
                appendLine("- ${context.getString(combo.titleRes)}: ${context.getString(combo.summaryRes)}")
            }
        }

        if (app.reasons.isNotEmpty()) {
            appendLine()
            appendLine(context.getString(R.string.detail_section_findings))
            app.reasons.forEach { reason ->
                val title = if (reason.shortTextRes != 0) context.getString(reason.shortTextRes) else reason.id.name
                appendLine("- $title")
            }
        }

        if (app.permissions.isNotEmpty()) {
            val grantedMark = context.getString(R.string.permission_granted)
            val deniedMark = context.getString(R.string.permission_not_granted)
            appendLine()
            appendLine(context.getString(R.string.detail_section_permissions, app.permissions.size))
            app.permissions.sortedBy { displayPermissionName(it).lowercase() }.forEach { perm ->
                if (app.isArchive) {
                    appendLine("  ${displayPermissionName(perm)}")
                } else {
                    val status = if (perm in app.grantedPermissions) grantedMark else deniedMark
                    appendLine("  ${displayPermissionName(perm)} [$status]")
                }
            }
        }
    }

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "${app.appName} — Squirrel Detective")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(sendIntent, context.getString(R.string.detail_share_report))
    try {
        context.startActivity(chooser)
    } catch (_: ActivityNotFoundException) {
        // Ignore on devices with no share targets
    }
}

@Composable
private fun ReceiverCard(receiver: ReceiverInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                receiver.name,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            receiver.actions.forEach {
                Text(
                    "  $it",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
