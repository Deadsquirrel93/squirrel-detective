package com.packagespy.app.domain.model

data class AppRiskInfo(
    val packageName: String,
    val appName: String,
    val versionName: String?,
    val isSystemApp: Boolean,
    val installerPackage: String?,
    val permissions: List<String>,
    val receivers: List<ReceiverInfo>,
    val reasons: List<ThreatReason>,
    val riskLevel: RiskLevel,
    val grantedPermissions: Set<String> = permissions.toSet(),
    val combos: List<ThreatCombo> = emptyList(),
    val targetSdk: Int = 0,
    val firstInstallTime: Long = 0L,
    val lastUpdateTime: Long = 0L,
    val usesCleartextTraffic: Boolean = false,
    val isUserTrusted: Boolean = false,
    val isArchive: Boolean = false,
)

data class ReceiverInfo(
    val name: String,
    val actions: List<String>,
    val permission: String? = null,
)

data class DeclaredService(
    val name: String,
    val permission: String?,
)

/** Lightweight summary used by the picker — no permission scan, just metadata. */
data class InstalledAppSummary(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
)
