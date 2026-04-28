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
        val result = ArrayList<AppRiskInfo>(total)
        packages.forEachIndexed { index, info ->
            try {
                result += buildRiskInfo(info)
            } catch (_: Throwable) {
                // Defensive: never let one broken package abort the whole scan.
            }
            onProgress?.invoke(index + 1, total)
        }
        return result
    }

    fun scanSingle(packageName: String): AppRiskInfo? {
        val info = packageInfo(packageName) ?: return null
        return runCatching { buildRiskInfo(info) }.getOrNull()
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

    private fun buildRiskInfo(info: PackageInfo): AppRiskInfo {
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
            ReceiverInfo(
                name = activityInfo.name ?: "",
                actions = receiverActions(info.packageName, activityInfo.name),
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
     * Resolve the intent-filter actions registered for a manifest receiver.
     *
     * PackageManager doesn't return intent-filters via GET_RECEIVERS alone, so
     * we instead query each "package-related" action and check whether this
     * receiver is among the resolvers.
     */
    private fun receiverActions(packageName: String, receiverName: String?): List<String> {
        if (receiverName.isNullOrBlank()) return emptyList()
        val candidates = listOf(
            RiskScorer.ACTION_PACKAGE_ADDED,
            RiskScorer.ACTION_PACKAGE_REMOVED,
            RiskScorer.ACTION_BOOT_COMPLETED,
            "android.intent.action.PACKAGE_REPLACED",
            "android.intent.action.PACKAGE_CHANGED",
            "android.intent.action.PACKAGE_FULLY_REMOVED",
        )
        val result = mutableListOf<String>()
        for (action in candidates) {
            val intent = android.content.Intent(action).apply {
                if (action.startsWith("android.intent.action.PACKAGE_")) {
                    data = android.net.Uri.fromParts("package", "dummy.pkg", null)
                }
            }
            val resolvers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryBroadcastReceivers(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0L),
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryBroadcastReceivers(intent, 0)
            }
            val match = resolvers.any {
                it.activityInfo?.packageName == packageName &&
                    it.activityInfo?.name == receiverName
            }
            if (match) result += action
        }
        return result
    }
}
