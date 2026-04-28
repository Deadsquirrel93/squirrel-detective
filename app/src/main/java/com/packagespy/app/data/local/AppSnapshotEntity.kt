package com.packagespy.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Snapshot of one app from a single scan.
 *
 * Permissions are stored as a newline-joined string and reasons as JSON-ish
 * encoded ids — Room doesn't need the structured form because the diff
 * use-case only needs permission membership and reason ids.
 */
@Entity(tableName = "app_snapshots")
data class AppSnapshotEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val versionName: String?,
    val isSystemApp: Boolean,
    val installerPackage: String?,
    /** Newline-separated permission names. */
    val permissions: String,
    /** Newline-separated [ThreatId.name] values. */
    val reasonIds: String,
    val riskLevelOrdinal: Int,
    val scannedAtMillis: Long
)
