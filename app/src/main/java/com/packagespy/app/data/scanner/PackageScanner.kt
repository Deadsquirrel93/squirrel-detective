package com.packagespy.app.data.scanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.net.toUri
import com.packagespy.app.domain.model.AppRiskInfo
import com.packagespy.app.domain.model.DeclaredService
import com.packagespy.app.domain.model.InstalledAppSummary
import com.packagespy.app.domain.model.ReceiverInfo
import com.packagespy.app.domain.usecase.RiskScorer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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
    private val activeComponentsReader: ActiveComponentsReader,
) {

    private val pm: PackageManager = context.packageManager

    fun scanAll(
        includeSystem: Boolean,
        trustedPackages: Set<String> = emptySet(),
        onProgress: ((current: Int, total: Int) -> Unit)? = null,
    ): List<AppRiskInfo> {
        val packages = installedPackages().filter { info ->
            includeSystem || !isSystemPackage(info)
        }
        val total = packages.size
        onProgress?.invoke(0, total)
        val receiverIndex = buildReceiverActionIndex(queryReceiverActionHits(packageName = null))
        val active = activeComponentsReader.read()
        val result = ArrayList<AppRiskInfo>(total)
        packages.forEachIndexed { index, info ->
            try {
                result += buildRiskInfo(info, receiverIndex, active, trustedPackages)
            } catch (_: Throwable) {
                // Defensive: never let one broken package abort the whole scan.
            }
            onProgress?.invoke(index + 1, total)
        }
        return result
    }

    fun scanSingle(
        packageName: String,
        trustedPackages: Set<String> = emptySet(),
    ): AppRiskInfo? {
        val info = packageInfo(packageName) ?: return null
        val receiverIndex = buildReceiverActionIndex(queryReceiverActionHits(packageName))
        val active = activeComponentsReader.read()
        return runCatching { buildRiskInfo(info, receiverIndex, active, trustedPackages) }.getOrNull()
    }

    /**
     * Copies the APK referenced by [uriString] to a temporary cache file,
     * inspects its manifest via [PackageManager.getPackageArchiveInfo], and
     * deletes the temp file immediately. Requires no storage permissions.
     */
    fun scanApk(uriString: String): AppRiskInfo? {
        val uri = runCatching { uriString.toUri() }.getOrNull() ?: return null
        val tempFile = runCatching {
            File.createTempFile("inspect_", ".apk", context.cacheDir)
        }.getOrNull() ?: return null
        return try {
            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            if (!copied) return null

            val info = archiveInfo(tempFile.absolutePath) ?: return null
            val appInfo = info.applicationInfo?.apply {
                sourceDir = tempFile.absolutePath
                publicSourceDir = tempFile.absolutePath
            }
            val appName = appInfo?.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() }
                ?: info.packageName
            val isDebuggable = appInfo != null &&
                (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            val usesCleartextTraffic = appInfo != null &&
                (appInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0
            val targetSdk = appInfo?.targetSdkVersion ?: 0
            val permissions = info.requestedPermissions?.toList().orEmpty()
            val receivers = info.receivers.orEmpty().map { activityInfo ->
                ReceiverInfo(
                    name = activityInfo.name ?: "",
                    actions = emptyList(),
                    permission = activityInfo.permission,
                )
            }
            val services = info.services.orEmpty().map { serviceInfo ->
                DeclaredService(name = serviceInfo.name ?: "", permission = serviceInfo.permission)
            }

            riskScorer.score(
                RiskScorer.Inputs(
                    packageName = info.packageName,
                    appName = appName,
                    versionName = info.versionName,
                    isSystemApp = false,
                    installerPackage = null,
                    permissions = permissions,
                    receivers = receivers,
                    services = services,
                    isDebuggable = isDebuggable,
                    targetSdk = targetSdk,
                    hasLauncherIntent = true,
                    trustSource = null,
                    activeCapabilities = emptySet(),
                    grantedPermissions = null,
                    usesCleartextTraffic = usesCleartextTraffic,
                ),
            ).copy(isArchive = true)
        } catch (_: Throwable) {
            null
        } finally {
            tempFile.delete()
        }
    }

    private fun archiveInfo(apkPath: String): PackageInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(apkPath, PackageManager.PackageInfoFlags.of(SCAN_FLAGS.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(apkPath, SCAN_FLAGS)
        }
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
                appName = appInfo.loadLabel(pm).toString().ifBlank { info.packageName },
                isSystemApp = isSystem,
            )
        }.sortedBy { it.appName.lowercase() }
    }

    private fun installedPackages(): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(SCAN_FLAGS.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(SCAN_FLAGS)
        }
    }

    private fun packageInfo(packageName: String): PackageInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(SCAN_FLAGS.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, SCAN_FLAGS)
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
        active: ActiveComponents,
        trustedPackages: Set<String>,
    ): AppRiskInfo {
        val appInfo = info.applicationInfo
        val appName = appInfo?.loadLabel(pm)?.toString() ?: info.packageName
        val isSystemApp = appInfo != null &&
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isDebuggable = appInfo != null &&
            (appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val usesCleartextTraffic = appInfo != null &&
            (appInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0
        val targetSdk = appInfo?.targetSdkVersion ?: 0
        val installSource = installSourceOf(info.packageName)
        val rawPermissions = info.requestedPermissions
        val rawFlags = info.requestedPermissionsFlags
        val permissions = rawPermissions?.toList().orEmpty()
        val grantedPermissions = extractGrantedPermissions(rawPermissions, rawFlags)
        val receivers = info.receivers.orEmpty().map { activityInfo ->
            val name = activityInfo.name ?: ""
            ReceiverInfo(
                name = name,
                actions = if (name.isBlank()) {
                    emptyList()
                } else {
                    receiverIndex[ReceiverKey(info.packageName, name)].orEmpty()
                },
                permission = activityInfo.permission,
            )
        }
        val services = info.services.orEmpty().map { serviceInfo ->
            DeclaredService(name = serviceInfo.name ?: "", permission = serviceInfo.permission)
        }
        val hasLauncher = pm.getLaunchIntentForPackage(info.packageName) != null
        return riskScorer.score(
            RiskScorer.Inputs(
                packageName = info.packageName,
                appName = appName,
                versionName = info.versionName,
                isSystemApp = isSystemApp,
                installerPackage = installSource.installing,
                permissions = permissions,
                receivers = receivers,
                services = services,
                isDebuggable = isDebuggable,
                targetSdk = targetSdk,
                hasLauncherIntent = hasLauncher,
                trustSource = selectTrustSource(
                    Build.VERSION.SDK_INT,
                    installSource.initiating,
                    installSource.installing,
                ),
                activeCapabilities = activeCapabilitiesOf(info.packageName, active),
                grantedPermissions = grantedPermissions,
                firstInstallTime = info.firstInstallTime,
                lastUpdateTime = info.lastUpdateTime,
                usesCleartextTraffic = usesCleartextTraffic,
                isUserTrusted = info.packageName in trustedPackages,
            )
        )
    }

    private data class InstallSource(val installing: String?, val initiating: String?)

    private fun installSourceOf(packageName: String): InstallSource {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val info = pm.getInstallSourceInfo(packageName)
                InstallSource(installing = info.installingPackageName, initiating = info.initiatingPackageName)
            } else {
                @Suppress("DEPRECATION")
                InstallSource(installing = pm.getInstallerPackageName(packageName), initiating = null)
            }
        } catch (_: Throwable) {
            InstallSource(installing = null, initiating = null)
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

    private companion object {
        private const val SCAN_FLAGS =
            PackageManager.GET_PERMISSIONS or PackageManager.GET_RECEIVERS or PackageManager.GET_SERVICES
    }
}

/**
 * Extracts permissions whose `REQUESTED_PERMISSION_GRANTED` bit is set.
 * If [flags] is null (some OEMs / synthetic packages), falls back to treating
 * all declared [permissions] as granted to avoid hiding findings.
 */
internal fun extractGrantedPermissions(
    permissions: Array<String>?,
    flags: IntArray?,
    grantedFlag: Int = PackageInfo.REQUESTED_PERMISSION_GRANTED,
): Set<String> {
    if (permissions.isNullOrEmpty()) return emptySet()
    if (flags == null) return permissions.toSet()
    val result = HashSet<String>(permissions.size)
    val count = minOf(permissions.size, flags.size)
    for (i in 0 until count) {
        if ((flags[i] and grantedFlag) != 0) {
            result += permissions[i]
        }
    }
    return result
}
