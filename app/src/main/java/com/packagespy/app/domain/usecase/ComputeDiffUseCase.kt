package com.packagespy.app.domain.usecase

import com.packagespy.app.domain.model.AppDiff
import com.packagespy.app.domain.model.AppRiskInfo
import javax.inject.Inject

class ComputeDiffUseCase @Inject constructor() {

    fun diff(
        previous: List<AppRiskInfo>,
        current: List<AppRiskInfo>
    ): List<AppDiff> {
        val previousByPkg = previous.associateBy { it.packageName }
        return current.mapNotNull { now ->
            val before = previousByPkg[now.packageName] ?: return@mapNotNull null
            val addedPerms = now.permissions - before.permissions.toSet()
            val removedPerms = before.permissions - now.permissions.toSet()
            val previousReasonIds = before.reasons.map { it.id }.toSet()
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
    }
}
