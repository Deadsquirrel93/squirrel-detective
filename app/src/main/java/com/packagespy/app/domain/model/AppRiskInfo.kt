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
)

data class ReceiverInfo(
    val name: String,
    val actions: List<String>,
)

/** Lightweight summary used by the picker — no permission scan, just metadata. */
data class InstalledAppSummary(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
)
