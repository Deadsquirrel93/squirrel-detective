package com.packagespy.app.presentation.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.packagespy.app.R
import com.packagespy.app.core.localization.LocaleManager
import com.packagespy.app.presentation.components.NutSpyBackground
import com.packagespy.app.presentation.components.NutSpyCard
import com.packagespy.app.presentation.components.nutSpyTopAppBarColors

private const val GITHUB_RELEASES_URL =
    "https://github.com/Deadsquirrel93/squirrel-detective/releases/latest"
private const val OBTAINIUM_ADD_URL =
    "obtainium://add/https://github.com/Deadsquirrel93/squirrel-detective"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val languageCode by viewModel.languageCode.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val versionName = remember(context) { currentVersionName(context) }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    NutSpyBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
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
                NutSpyCard {
                    Text(
                        text = stringResource(R.string.language_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.language_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = languageCode == null,
                            onClick = { viewModel.setLanguage(null) },
                            label = { Text(stringResource(R.string.language_system)) },
                        )
                        FilterChip(
                            selected = languageCode == LocaleManager.RUSSIAN,
                            onClick = { viewModel.setLanguage(LocaleManager.RUSSIAN) },
                            label = { Text(stringResource(R.string.language_russian)) },
                        )
                        FilterChip(
                            selected = languageCode == LocaleManager.ENGLISH,
                            onClick = { viewModel.setLanguage(LocaleManager.ENGLISH) },
                            label = { Text(stringResource(R.string.language_english)) },
                        )
                    }
                }

                NutSpyCard {
                    Text(
                        text = stringResource(R.string.updates_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    versionName?.let { version ->
                        Text(
                            text = stringResource(R.string.updates_current_version, version),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = stringResource(R.string.updates_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val openUrlFailed = stringResource(R.string.updates_open_url_failed)
                    val obtainiumMissing = stringResource(R.string.updates_obtainium_missing)
                    OutlinedButton(
                        onClick = { openExternalUrl(context, GITHUB_RELEASES_URL, openUrlFailed) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.updates_open_github))
                    }
                    OutlinedButton(
                        onClick = { openExternalUrl(context, OBTAINIUM_ADD_URL, obtainiumMissing) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.updates_add_obtainium))
                    }
                }

                NutSpyCard {
                    Text(
                        text = stringResource(R.string.about_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.about_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun currentVersionName(context: Context): String? = runCatching {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(0L),
        ).versionName
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }
}.getOrNull()

private fun openExternalUrl(context: Context, url: String, errorMessage: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
    }
}
