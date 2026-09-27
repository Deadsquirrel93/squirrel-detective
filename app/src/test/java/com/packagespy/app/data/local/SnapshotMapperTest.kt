package com.packagespy.app.data.local

import com.packagespy.app.domain.model.AppRiskInfo
import com.packagespy.app.domain.model.RiskLevel
import com.packagespy.app.domain.model.ThreatId
import com.packagespy.app.domain.model.ThreatReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotMapperTest {

    private fun reason(id: ThreatId, severity: RiskLevel = RiskLevel.YELLOW) = ThreatReason(
        id = id,
        shortTextRes = 0,
        explanationRes = 0,
        severity = severity,
    )

    private fun appInfo(
        permissions: List<String> = emptyList(),
        reasons: List<ThreatReason> = emptyList(),
    ) = AppRiskInfo(
        packageName = "com.example.app",
        appName = "Example App",
        versionName = "1.0",
        isSystemApp = false,
        installerPackage = "com.android.vending",
        permissions = permissions,
        receivers = emptyList(),
        reasons = reasons,
        riskLevel = RiskLevel.YELLOW,
    )

    @Test
    fun `round-trip through entity preserves packageName, appName, permissions and reason ids`() {
        val info = appInfo(
            permissions = listOf("android.permission.INTERNET", "android.permission.SEND_SMS"),
            reasons = listOf(reason(ThreatId.SMS_ACCESS), reason(ThreatId.QUERY_ALL_PACKAGES)),
        )

        val snapshot = info.toEntity(scannedAtMillis = 1_000L).toSnapshot()

        assertEquals("com.example.app", snapshot.packageName)
        assertEquals("Example App", snapshot.appName)
        assertEquals(
            listOf("android.permission.INTERNET", "android.permission.SEND_SMS"),
            snapshot.permissions,
        )
        assertEquals(setOf(ThreatId.SMS_ACCESS, ThreatId.QUERY_ALL_PACKAGES), snapshot.reasonIds)
    }

    @Test
    fun `round-trip preserves the new enabled-capability reason ids`() {
        val info = appInfo(
            reasons = listOf(
                reason(ThreatId.ACCESSIBILITY_ENABLED, RiskLevel.RED),
                reason(ThreatId.DEVICE_ADMIN_ACTIVE, RiskLevel.RED),
                reason(ThreatId.NOTIFICATION_LISTENER_ENABLED, RiskLevel.RED),
                reason(ThreatId.INPUT_METHOD_ENABLED, RiskLevel.RED),
            ),
        )

        val snapshot = info.toEntity(scannedAtMillis = 1_000L).toSnapshot()

        assertEquals(
            setOf(
                ThreatId.ACCESSIBILITY_ENABLED,
                ThreatId.DEVICE_ADMIN_ACTIVE,
                ThreatId.NOTIFICATION_LISTENER_ENABLED,
                ThreatId.INPUT_METHOD_ENABLED,
            ),
            snapshot.reasonIds,
        )
    }

    @Test
    fun `unknown reason id is dropped`() {
        val entity = AppSnapshotEntity(
            packageName = "com.example.app",
            appName = "Example App",
            versionName = "1.0",
            isSystemApp = false,
            installerPackage = "com.android.vending",
            permissions = "",
            reasonIds = "SMS_ACCESS\nNOT_A_REAL_ID",
            riskLevelOrdinal = RiskLevel.YELLOW.ordinal,
            scannedAtMillis = 0L,
        )

        val snapshot = entity.toSnapshot()

        assertEquals(setOf(ThreatId.SMS_ACCESS), snapshot.reasonIds)
    }

    @Test
    fun `empty stored strings decode to empty collections`() {
        val entity = AppSnapshotEntity(
            packageName = "com.example.app",
            appName = "Example App",
            versionName = "1.0",
            isSystemApp = false,
            installerPackage = "com.android.vending",
            permissions = "",
            reasonIds = "",
            riskLevelOrdinal = RiskLevel.SAFE.ordinal,
            scannedAtMillis = 0L,
        )

        val snapshot = entity.toSnapshot()

        assertTrue(snapshot.permissions.isEmpty())
        assertTrue(snapshot.reasonIds.isEmpty())
    }
}
