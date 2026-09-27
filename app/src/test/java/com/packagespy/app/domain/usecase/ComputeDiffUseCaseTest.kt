package com.packagespy.app.domain.usecase

import com.packagespy.app.domain.model.AppRiskInfo
import com.packagespy.app.domain.model.AppSnapshot
import com.packagespy.app.domain.model.RiskLevel
import com.packagespy.app.domain.model.ThreatId
import com.packagespy.app.domain.model.ThreatReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Characterization tests for [ComputeDiffUseCase]. `previous` is a persisted
 * [AppSnapshot] (stored reason ids, no re-scoring) rather than a re-scored
 * [AppRiskInfo].
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
        appName: String = "Example App",
        permissions: List<String> = emptyList(),
        reasons: List<ThreatReason> = emptyList(),
        riskLevel: RiskLevel = RiskLevel.SAFE,
    ) = AppRiskInfo(
        packageName = packageName,
        appName = appName,
        versionName = "1.0",
        isSystemApp = false,
        installerPackage = "com.android.vending",
        permissions = permissions,
        receivers = emptyList(),
        reasons = reasons,
        riskLevel = riskLevel,
    )

    private fun snapshot(
        packageName: String = "com.example.app",
        permissions: List<String> = emptyList(),
        reasonIds: Set<ThreatId> = emptySet(),
    ) = AppSnapshot(
        packageName = packageName,
        appName = "Example App",
        permissions = permissions,
        reasonIds = reasonIds,
    )

    @Test
    fun `added permission produces a notable diff`() {
        val previous = listOf(snapshot(permissions = emptyList()))
        val current = listOf(appInfo(permissions = listOf("android.permission.INTERNET")))

        val result = useCase.diff(previous, current)

        assertEquals(1, result.size)
        assertEquals(listOf("android.permission.INTERNET"), result.single().addedPermissions)
        assertTrue(result.single().removedPermissions.isEmpty())
    }

    @Test
    fun `removed-only permission is not notable and produces no diff`() {
        val previous = listOf(snapshot(permissions = listOf("android.permission.INTERNET")))
        val current = listOf(appInfo(permissions = emptyList()))

        val result = useCase.diff(previous, current)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `new ThreatId produces a notable diff with newReasons`() {
        val previous = listOf(snapshot(reasonIds = emptySet()))
        val current = listOf(appInfo(reasons = listOf(reason(ThreatId.QUERY_ALL_PACKAGES))))

        val result = useCase.diff(previous, current)

        assertEquals(1, result.size)
        assertEquals(listOf(ThreatId.QUERY_ALL_PACKAGES), result.single().newReasons.map { it.id })
    }

    @Test
    fun `no changes produces no diff`() {
        val previous = listOf(
            snapshot(
                permissions = listOf("android.permission.INTERNET"),
                reasonIds = setOf(ThreatId.QUERY_ALL_PACKAGES),
            )
        )
        val current = listOf(
            appInfo(
                permissions = listOf("android.permission.INTERNET"),
                reasons = listOf(reason(ThreatId.QUERY_ALL_PACKAGES)),
            )
        )

        val result = useCase.diff(previous, current)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `package only present in current is not diffed`() {
        val previous = emptyList<AppSnapshot>()
        val current = listOf(appInfo(packageName = "com.example.newapp"))

        val result = useCase.diff(previous, current)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `package only present in previous is not diffed`() {
        val previous = listOf(snapshot(packageName = "com.example.oldapp"))
        val current = emptyList<AppRiskInfo>()

        val result = useCase.diff(previous, current)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `unpersisted reasons do not reappear as new on next scan`() {
        val reasonIds = setOf(
            ThreatId.RECEIVER_PACKAGE_ADDED,
            ThreatId.DEBUGGABLE_BUILD,
            ThreatId.OLD_TARGET_SDK,
            ThreatId.HIDDEN_NO_LAUNCHER,
        )
        val previous = listOf(snapshot(reasonIds = reasonIds))
        val current = listOf(
            appInfo(reasons = reasonIds.map { reason(it) })
        )

        val result = useCase.diff(previous, current)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `diffs without new reasons are sorted by app name case-insensitively`() {
        val previous = listOf(
            snapshot(packageName = "com.example.beta"),
            snapshot(packageName = "com.example.alpha"),
            snapshot(packageName = "com.example.gamma"),
        )
        val current = listOf(
            appInfo(packageName = "com.example.beta", appName = "beta", permissions = listOf("android.permission.INTERNET")),
            appInfo(packageName = "com.example.alpha", appName = "Alpha", permissions = listOf("android.permission.INTERNET")),
            appInfo(packageName = "com.example.gamma", appName = "gamma", permissions = listOf("android.permission.INTERNET")),
        )

        val result = useCase.diff(previous, current)

        assertEquals(listOf("Alpha", "beta", "gamma"), result.map { it.appName })
    }

    @Test
    fun `diffs with new reasons are sorted before diffs without new reasons`() {
        val previous = listOf(
            snapshot(packageName = "com.example.alpha"),
            snapshot(packageName = "com.example.zeta"),
        )
        val current = listOf(
            appInfo(packageName = "com.example.alpha", appName = "Alpha", permissions = listOf("android.permission.INTERNET")),
            appInfo(
                packageName = "com.example.zeta",
                appName = "Zeta",
                reasons = listOf(reason(ThreatId.QUERY_ALL_PACKAGES)),
            ),
        )

        val result = useCase.diff(previous, current)

        assertEquals(listOf("Zeta", "Alpha"), result.map { it.appName })
    }

    @Test
    fun `diffs with equal app names are sorted by package name`() {
        val previous = listOf(
            snapshot(packageName = "b.pkg"),
            snapshot(packageName = "a.pkg"),
        )
        val current = listOf(
            appInfo(packageName = "b.pkg", appName = "Same", permissions = listOf("android.permission.INTERNET")),
            appInfo(packageName = "a.pkg", appName = "Same", permissions = listOf("android.permission.INTERNET")),
        )

        val result = useCase.diff(previous, current)

        assertEquals(listOf("a.pkg", "b.pkg"), result.map { it.packageName })
    }
}
