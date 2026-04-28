package com.packagespy.app.domain.model

/**
 * Difference between a previous snapshot and the current scan for one app.
 * Used to surface "TikTok added QUERY_ALL_PACKAGES" style notices.
 */
data class AppDiff(
    val packageName: String,
    val appName: String,
    val addedPermissions: List<String>,
    val removedPermissions: List<String>,
    val newReasons: List<ThreatReason>
) {
    val isNotable: Boolean
        get() = addedPermissions.isNotEmpty() || newReasons.isNotEmpty()
}
