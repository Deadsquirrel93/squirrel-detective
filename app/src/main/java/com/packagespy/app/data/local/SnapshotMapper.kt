package com.packagespy.app.data.local

import com.packagespy.app.domain.model.AppRiskInfo
import com.packagespy.app.domain.model.RiskLevel
import com.packagespy.app.domain.model.ThreatId
import com.packagespy.app.domain.model.ThreatReason
import com.packagespy.app.domain.usecase.RiskScorer

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
 * Re-scores an entity from its stored permissions so that risk-level changes
 * (e.g. updated rules) are reflected on next read. Receivers, debuggable
 * status and target SDK aren't persisted, so the diff use-case only
 * meaningfully compares permissions and reason ids.
 */
fun AppSnapshotEntity.toDomain(scorer: RiskScorer): AppRiskInfo {
    val perms = permissions.split(SEPARATOR).filter { it.isNotBlank() }
    val storedReasonIds = reasonIds.split(SEPARATOR).filter { it.isNotBlank() }
        .mapNotNull { runCatching { ThreatId.valueOf(it) }.getOrNull() }
    val rescored = scorer.score(
        RiskScorer.Inputs(
            packageName = packageName,
            appName = appName,
            versionName = versionName,
            isSystemApp = isSystemApp,
            installerPackage = installerPackage,
            permissions = perms,
            receivers = emptyList(),
        )
    )
    val storedSeverity = RiskLevel.values().getOrElse(riskLevelOrdinal) { RiskLevel.SAFE }
    return rescored.copy(
        // Preserve stored reasons so diff has a stable history of what was flagged.
        reasons = rescored.reasons.ifEmpty {
            storedReasonIds.map {
                ThreatReason(
                    id = it,
                    shortTextRes = 0,
                    explanationRes = 0,
                    severity = storedSeverity,
                )
            }
        },
    )
}
