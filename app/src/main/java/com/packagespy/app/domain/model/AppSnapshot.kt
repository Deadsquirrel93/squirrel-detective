package com.packagespy.app.domain.model

/**
 * Persisted snapshot of one app from a previous scan, used only for diffing
 * against the current scan. Unlike [AppRiskInfo], this carries the reason ids
 * that were actually flagged at scan time instead of being re-scored later —
 * re-scoring loses receivers, debuggable status, target SDK and launcher
 * info, which aren't persisted.
 */
data class AppSnapshot(
    val packageName: String,
    val appName: String,
    val permissions: List<String>,
    val reasonIds: Set<ThreatId>,
)
