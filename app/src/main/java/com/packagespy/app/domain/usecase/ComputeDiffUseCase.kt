package com.packagespy.app.domain.usecase

import com.packagespy.app.domain.model.AppDiff
import com.packagespy.app.domain.model.AppRiskInfo
import com.packagespy.app.domain.model.AppSnapshot
import javax.inject.Inject

/**
 * Compares the persisted [AppSnapshot]s of the previous scan (reason ids as
 * flagged at scan time, never re-scored) with the current scan's results.
 * Diffs with new reasons come first, then by app name (case-insensitive),
 * then by package name.
 */
class ComputeDiffUseCase @Inject constructor() {

    fun diff(
        previous: List<AppSnapshot>,
        current: List<AppRiskInfo>
    ): List<AppDiff> {
        val previousByPkg = previous.associateBy { it.packageName }
        val diffs = current.mapNotNull { now ->
            val before = previousByPkg[now.packageName] ?: return@mapNotNull null
            val addedPerms = now.permissions - before.permissions.toSet()
            val removedPerms = before.permissions - now.permissions.toSet()
            val previousReasonIds = before.reasonIds
            val newReasons = now.reasons.filter { it.id !in previousReasonIds }
            val diff = AppDiff(
                packageName = now.packageName,
                appName = now.appName,
                addedPermissions = addedPerms,
                removedPermissions = removedPerms,
                newReasons = newReasons
            )
            diff.takeIf { it.isNotable }
        }
        return diffs.sortedWith(
            compareByDescending<AppDiff> { it.newReasons.isNotEmpty() }
                .thenBy { it.appName.lowercase() }
                .thenBy { it.packageName }
        )
    }
}
