package com.packagespy.app.domain.repository

import com.packagespy.app.domain.model.AppDiff
import com.packagespy.app.domain.model.AppRiskInfo
import com.packagespy.app.domain.model.InstalledAppSummary
import com.packagespy.app.domain.model.ScanProgress
import kotlinx.coroutines.flow.Flow

interface AppRiskRepository {
    /** Cached results from the most recent scan, observed reactively. */
    fun observeApps(): Flow<List<AppRiskInfo>>

    /** Live progress of an in-flight scan; null while no scan is running. */
    fun observeScanProgress(): Flow<ScanProgress?>

    /** Force a fresh scan of all installed apps. Persists the snapshot. */
    suspend fun rescan(includeSystem: Boolean): List<AppRiskInfo>

    /** Diff between the previous and the current snapshot. */
    suspend fun diffSinceLastScan(): List<AppDiff>

    /** Lightweight installed-apps list used by the picker. */
    suspend fun listInstalled(includeSystem: Boolean): List<InstalledAppSummary>

    /** Single-app scan that bypasses the cache and persistence. */
    suspend fun scanSingle(packageName: String): AppRiskInfo?
}
