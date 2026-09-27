package com.packagespy.app.domain.usecase

import com.packagespy.app.domain.model.AppRiskInfo
import com.packagespy.app.domain.model.RiskLevel
import com.packagespy.app.domain.model.ThreatId
import com.packagespy.app.domain.model.ThreatReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Characterization tests for [ComputeDiffUseCase]. Pins down the CURRENT
 * diffing behavior before the Stage 01 toolchain upgrade. Built by hand —
 * no [com.packagespy.app.data.local] mapper or Room involved.
 */
class ComputeDiffUseCaseTest {

    private val useCase = ComputeDiffUseCase()

    private fun reason(id: ThreatId, severity: RiskLevel = RiskLevel.YELLOW) = ThreatReason(
        id = id,
        shortTextRes = 0,
        explanationRes = 0,
        severity = severity,
    )

    private fun appInfo(
        packageName: String = "com.example.app",
        permissions: List<String> = emptyList(),
        reasons: List<ThreatReason> = emptyList(),
        riskLevel: RiskLevel = RiskLevel.SAFE,
    ) = AppRiskInfo(
        packageName = packageName,
        appName = "Example App",
        versionName = "1.0",
        isSystemApp = false,
        installerPackage = "com.android.vending",
        permissions = permissions,
        receivers = emptyList(),
        reasons = reasons,
        riskLevel = riskLevel,
    )

    @Test
    fun `added permission produces a notable diff`() {
        val previous = listOf(appInfo(permissions = emptyList()))
        val current = listOf(appInfo(permissions = listOf("android.permission.INTERNET")))

        val result = useCase.diff(previous, current)

        assertEquals(1, result.size)
        assertEquals(listOf("android.permission.INTERNET"), result.single().addedPermissions)
        assertTrue(result.single().removedPermissions.isEmpty())
    }

    @Test
    fun `removed-only permission is not notable and produces no diff`() {
        val previous = listOf(appInfo(permissions = listOf("android.permission.INTERNET")))
        val current = listOf(appInfo(permissions = emptyList()))

        val result = useCase.diff(previous, current)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `new ThreatId produces a notable diff with newReasons`() {
        val previous = listOf(appInfo(reasons = emptyList()))
        val current = listOf(appInfo(reasons = listOf(reason(ThreatId.QUERY_ALL_PACKAGES))))

        val result = useCase.diff(previous, current)

        assertEquals(1, result.size)
        assertEquals(listOf(ThreatId.QUERY_ALL_PACKAGES), result.single().newReasons.map { it.id })
    }

    @Test
    fun `no changes produces no diff`() {
        val snapshot = listOf(
            appInfo(
                permissions = listOf("android.permission.INTERNET"),
                reasons = listOf(reason(ThreatId.QUERY_ALL_PACKAGES)),
            )
        )

        val result = useCase.diff(snapshot, snapshot)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `package only present in current is not diffed`() {
        val previous = emptyList<AppRiskInfo>()
        val current = listOf(appInfo(packageName = "com.example.newapp"))

        val result = useCase.diff(previous, current)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `package only present in previous is not diffed`() {
        val previous = listOf(appInfo(packageName = "com.example.oldapp"))
        val current = emptyList<AppRiskInfo>()

        val result = useCase.diff(previous, current)

        assertTrue(result.isEmpty())
    }
}
