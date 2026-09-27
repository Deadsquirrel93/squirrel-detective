package com.packagespy.app.data.local

import com.packagespy.app.domain.model.AppRiskInfo

/**
 * Rows to persist as the new baseline after a scan, excluding packages whose
 * change hasn't been acknowledged yet ([pendingPackages]) — their previous
 * row stays untouched so the next diff still surfaces the change.
 */
fun baselineRowsToWrite(
    current: List<AppRiskInfo>,
    pendingPackages: Set<String>,
    scannedAtMillis: Long,
): List<AppSnapshotEntity> =
    current.filter { it.packageName !in pendingPackages }
        .map { it.toEntity(scannedAtMillis) }

/**
 * Rows to persist once the user acknowledges the pending changes for
 * [packages] — moves the baseline for exactly those packages to the current
 * scan. Packages no longer present in [current] are ignored.
 */
fun acknowledgedRows(
    current: List<AppRiskInfo>,
    packages: Set<String>,
    scannedAtMillis: Long,
): List<AppSnapshotEntity> =
    current.filter { it.packageName in packages }
        .map { it.toEntity(scannedAtMillis) }
