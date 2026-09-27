package com.packagespy.app.data.local

import com.packagespy.app.domain.model.AppRiskInfo
import com.packagespy.app.domain.model.RiskLevel
import com.packagespy.app.domain.model.ThreatId
import com.packagespy.app.domain.model.ThreatReason
import com.packagespy.app.domain.usecase.ComputeDiffUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaselineTest {

    private fun reason(id: ThreatId, severity: RiskLevel = RiskLevel.RED) = ThreatReason(
        id = id,
        shortTextRes = 0,
        explanationRes = 0,
        severity = severity,
    )

    private fun appInfo(
        packageName: String = "com.example.app",
        appName: String = "Example App",
        permissions: List<String> = emptyList(),
        reasons: List<ThreatReason> = emptyList(),
    ) = AppRiskInfo(
        packageName = packageName,
        appName = appName,
        versionName = "1.0",
        isSystemApp = false,
        installerPackage = "com.android.vending",
        permissions = permissions,
        receivers = emptyList(),
        reasons = reasons,
        riskLevel = RiskLevel.YELLOW,
    )

    /** Applies the same semantics as [AppSnapshotDao.writeBaseline] to a plain map. */
    private fun MutableMap<String, AppSnapshotEntity>.writeBaseline(
        rows: List<AppSnapshotEntity>,
        keep: List<String>,
    ) {
        rows.forEach { put(it.packageName, it) }
        keys.retainAll(keep.toSet())
    }

    @Test
    fun `baselineRowsToWrite excludes pending packages but includes new installs`() {
        val current = listOf(
            appInfo(packageName = "com.pending.app"),
            appInfo(packageName = "com.stable.app"),
            appInfo(packageName = "com.new.app"),
        )

        val rows = baselineRowsToWrite(
            current = current,
            pendingPackages = setOf("com.pending.app"),
            scannedAtMillis = 42L,
        )

        val packageNames = rows.map { it.packageName }
        assertEquals(setOf("com.stable.app", "com.new.app"), packageNames.toSet())
        assertTrue(rows.all { it.scannedAtMillis == 42L })
    }

    @Test
    fun `acknowledgedRows keeps only requested packages that are still installed`() {
        val current = listOf(
            appInfo(packageName = "com.a"),
            appInfo(packageName = "com.b"),
        )

        val rows = acknowledgedRows(
            current = current,
            packages = setOf("com.a", "com.uninstalled"),
            scannedAtMillis = 7L,
        )

        assertEquals(listOf("com.a"), rows.map { it.packageName })
        assertEquals(7L, rows.single().scannedAtMillis)
    }

    @Test
    fun `enabled then not acknowledged then disabled keeps reporting until the baseline moves`() {
        val diffUseCase = ComputeDiffUseCase()
        val base = mutableMapOf(
            "com.a" to appInfo(packageName = "com.a").toEntity(scannedAtMillis = 0L),
        )

        // Scan 1: accessibility gets turned on for com.a.
        val scan1 = listOf(
            appInfo(packageName = "com.a", reasons = listOf(reason(ThreatId.ACCESSIBILITY_ENABLED))),
        )
        val diff1 = diffUseCase.diff(base.values.map { it.toSnapshot() }, scan1)
        val pending1 = diff1.map { it.packageName }.toSet()
        assertEquals(setOf("com.a"), pending1)

        base.writeBaseline(
            rows = baselineRowsToWrite(scan1, pending1, scannedAtMillis = 1L),
            keep = scan1.map { it.packageName },
        )
        // Not acknowledged: the baseline still has the pre-scan1 row.
        assertEquals(0L, base.getValue("com.a").scannedAtMillis)
        assertTrue(base.getValue("com.a").reasonIds.isEmpty())

        // Scan 2: accessibility is turned back off, matching the untouched baseline.
        val scan2 = listOf(appInfo(packageName = "com.a"))
        val diff2 = diffUseCase.diff(base.values.map { it.toSnapshot() }, scan2)
        assertTrue(diff2.isEmpty())
        val pending2 = diff2.map { it.packageName }.toSet()

        base.writeBaseline(
            rows = baselineRowsToWrite(scan2, pending2, scannedAtMillis = 2L),
            keep = scan2.map { it.packageName },
        )
        assertEquals(2L, base.getValue("com.a").scannedAtMillis)
    }

    @Test
    fun `unacknowledged change keeps reappearing on a rescan with no device changes`() {
        val diffUseCase = ComputeDiffUseCase()
        val base = mutableMapOf(
            "com.a" to appInfo(packageName = "com.a").toEntity(scannedAtMillis = 0L),
        )
        val activeScan = listOf(
            appInfo(packageName = "com.a", reasons = listOf(reason(ThreatId.ACCESSIBILITY_ENABLED))),
        )

        val diff1 = diffUseCase.diff(base.values.map { it.toSnapshot() }, activeScan)
        val pending1 = diff1.map { it.packageName }.toSet()
        base.writeBaseline(
            rows = baselineRowsToWrite(activeScan, pending1, scannedAtMillis = 1L),
            keep = activeScan.map { it.packageName },
        )

        // Rescan with the same device state: still not acknowledged.
        val diff2 = diffUseCase.diff(base.values.map { it.toSnapshot() }, activeScan)
        assertEquals(setOf("com.a"), diff2.map { it.packageName }.toSet())
    }

    @Test
    fun `a removed-permission-only change is not notable and still moves the baseline`() {
        val diffUseCase = ComputeDiffUseCase()
        val base = mutableMapOf(
            "com.a" to appInfo(
                packageName = "com.a",
                permissions = listOf("android.permission.INTERNET", "android.permission.CAMERA"),
            ).toEntity(scannedAtMillis = 0L),
        )
        val scan = listOf(
            appInfo(packageName = "com.a", permissions = listOf("android.permission.INTERNET")),
        )

        val diff = diffUseCase.diff(base.values.map { it.toSnapshot() }, scan)
        assertTrue(diff.isEmpty())
        val pending = diff.map { it.packageName }.toSet()

        base.writeBaseline(
            rows = baselineRowsToWrite(scan, pending, scannedAtMillis = 5L),
            keep = scan.map { it.packageName },
        )
        assertEquals(listOf("android.permission.INTERNET"), base.getValue("com.a").toSnapshot().permissions)
        assertEquals(5L, base.getValue("com.a").scannedAtMillis)
    }
}
