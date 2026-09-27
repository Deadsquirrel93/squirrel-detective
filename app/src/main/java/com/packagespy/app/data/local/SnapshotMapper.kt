package com.packagespy.app.data.local

import com.packagespy.app.domain.model.AppRiskInfo
import com.packagespy.app.domain.model.AppSnapshot
import com.packagespy.app.domain.model.ThreatId

private const val SEPARATOR = "\n"

fun AppRiskInfo.toEntity(scannedAtMillis: Long): AppSnapshotEntity {
    return AppSnapshotEntity(
        packageName = packageName,
        appName = appName,
        versionName = versionName,
        isSystemApp = isSystemApp,
        installerPackage = installerPackage,
        permissions = permissions.joinToString(SEPARATOR),
        reasonIds = reasons.joinToString(SEPARATOR) { it.id.name },
        riskLevelOrdinal = riskLevel.ordinal,
        scannedAtMillis = scannedAtMillis,
    )
}

/**
 * Rebuilds an [AppSnapshot] straight from the stored strings, with no
 * re-scoring involved: the diff use-case only needs permission membership
 * and the reason ids that were flagged at scan time.
 */
fun AppSnapshotEntity.toSnapshot(): AppSnapshot {
    val perms = permissions.split(SEPARATOR).filter { it.isNotBlank() }
    val ids = reasonIds.split(SEPARATOR).filter { it.isNotBlank() }
        .mapNotNull { runCatching { ThreatId.valueOf(it) }.getOrNull() }
        .toSet()
    return AppSnapshot(
        packageName = packageName,
        appName = appName,
        permissions = perms,
        reasonIds = ids,
    )
}
