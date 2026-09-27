package com.packagespy.app.data.scanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.packagespy.app.domain.model.AppRiskInfo
import com.packagespy.app.domain.model.InstalledAppSummary
import com.packagespy.app.domain.model.ReceiverInfo
import com.packagespy.app.domain.usecase.RiskScorer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps PackageManager to extract every piece of metadata we care about
 * (permissions + receivers) and run it through the [RiskScorer].
 */
@Singleton
class PackageScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val riskScorer: RiskScorer,
) {

    private val pm: PackageManager = context.packageManager

    fun scanAll(
        includeSystem: Boolean,
        onProgress: ((current: Int, total: Int) -> Unit)? = null,
    ): List<AppRiskInfo> {
        val packages = installedPackages().filter { info ->
            includeSystem || !isSystemPackage(info)
        }
        val total = packages.size
        onProgress?.invoke(0, total)
        val receiverIndex = buildReceiverActionIndex(queryReceiverActionHits(packageName = null))
        val result = ArrayList<AppRiskInfo>(total)
        packages.forEachIndexed { index, info ->
            try {
                result += buildRiskInfo(info, receiverIndex)
            } catch (_: Throwable) {
                // Defensive: never let one broken package abort the whole scan.
            }
            onProgress?.invoke(index + 1, total)
        }
        return result
    }

    fun scanSingle(packageName: String): AppRiskInfo? {
        val info = packageInfo(packageName) ?: return null
        val receiverIndex = buildReceiverActionIndex(queryReceiverActionHits(packageName))
        return runCatching { buildRiskInfo(info, receiverIndex) }.getOrNull()
    }

    fun listInstalled(includeSystem: Boolean): List<InstalledAppSummary> {
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(0)
        }
        return packages.mapNotNull { info ->
            val appInfo = info.applicationInfo ?: return@mapNotNull null
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!includeSystem && isSystem) return@mapNotNull null
            InstalledAppSummary(
                packageName = info.packageName,
                appName = appInfo.loadLabel(pm)?.toString() ?: info.packageName,
                isSystemApp = isSystem,
            )
        }.sortedBy { it.appName.lowercase() }
    }

    private fun installedPackages(): List<PackageInfo> {
        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_RECEIVERS
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(flags)
        }
    }

    private fun packageInfo(packageName: String): PackageInfo? {
        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_RECEIVERS
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, flags)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun isSystemPackage(info: PackageInfo): Boolean {
        val appInfo = info.applicationInfo ?: return false
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }

    private fun buildRiskInfo(
        info: PackageInfo,
        receiverIndex: Map<ReceiverKey, List<String>>,
    ): AppRiskInfo {
        val appInfo = info.applicationInfo
        val appName = appInfo?.loadLabel(pm)?.toString() ?: info.packageName
        val isSystemApp = appInfo != null &&
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isDebuggable = appInfo != null &&
            (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val targetSdk = appInfo?.targetSdkVersion ?: 0
        val installer = installerOf(info.packageName)
        val permissions = info.requestedPermissions?.toList().orEmpty()
        val receivers = info.receivers.orEmpty().map { activityInfo ->
            val name = activityInfo.name ?: ""
            ReceiverInfo(
                name = name,
                actions = if (name.isBlank()) {
                    emptyList()
                } else {
                    receiverIndex[ReceiverKey(info.packageName, name)].orEmpty()
                },
            )
        }
        val hasLauncher = pm.getLaunchIntentForPackage(info.packageName) != null
        return riskScorer.score(
            RiskScorer.Inputs(
                packageName = info.packageName,
                appName = appName,
                versionName = info.versionName,
                isSystemApp = isSystemApp,
                installerPackage = installer,
                permissions = permissions,
                receivers = receivers,
                isDebuggable = isDebuggable,
                targetSdk = targetSdk,
                hasLauncherIntent = hasLauncher,
            )
        )
    }

    private fun installerOf(packageName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Resolves, for each of [WATCHED_RECEIVER_ACTIONS], the manifest receivers
     * that would handle it — with exactly one `queryBroadcastReceivers` call
     * per action, regardless of how many packages/receivers are being scanned.
     *
     * PackageManager doesn't return intent-filters via GET_RECEIVERS alone, so
     * we instead query each "package-related" action and check who resolves it.
     *
     * @param packageName restrict the query to a single package, or `null` to
     *   resolve across all installed packages (used by [scanAll]).
     */
    private fun queryReceiverActionHits(packageName: String?): Map<String, List<ReceiverKey>> {
        val hits = mutableMapOf<String, List<ReceiverKey>>()
        for (action in WATCHED_RECEIVER_ACTIONS) {
            val intent = android.content.Intent(action).apply {
                if (action.startsWith("android.intent.action.PACKAGE_")) {
                    data = android.net.Uri.fromParts("package", "dummy.pkg", null)
                }
                if (packageName != null) {
                    setPackage(packageName)
                }
            }
            val resolvers = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.queryBroadcastReceivers(
                        intent,
                        PackageManager.ResolveInfoFlags.of(0L),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.queryBroadcastReceivers(intent, 0)
                }
            } catch (_: Exception) {
                emptyList()
            }
            hits[action] = resolvers.mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo
                val resolvedPackageName = activityInfo?.packageName
                val resolvedName = activityInfo?.name
                if (resolvedPackageName != null && resolvedName != null) {
                    ReceiverKey(resolvedPackageName, resolvedName)
                } else {
                    null
                }
            }
        }
        return hits
    }
}
