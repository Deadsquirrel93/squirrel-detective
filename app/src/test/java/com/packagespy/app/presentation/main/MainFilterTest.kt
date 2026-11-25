package com.packagespy.app.presentation.main

import android.Manifest
import com.packagespy.app.domain.model.AppRiskInfo
import com.packagespy.app.domain.model.RiskLevel
import com.packagespy.app.domain.model.ThreatId
import com.packagespy.app.domain.model.ThreatReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainFilterTest {

    private fun app(
        pkg: String,
        name: String,
        risk: RiskLevel = RiskLevel.YELLOW,
        permissions: List<String> = emptyList(),
        granted: Set<String> = permissions.toSet(),
        reasons: List<ThreatReason> = emptyList(),
        lastUpdateTime: Long = 0L,
    ) = AppRiskInfo(
        packageName = pkg,
        appName = name,
        versionName = "1.0",
        isSystemApp = false,
        installerPackage = null,
        permissions = permissions,
        receivers = emptyList(),
        reasons = reasons,
        riskLevel = risk,
        grantedPermissions = granted,
        lastUpdateTime = lastUpdateTime,
    )

    @Test
    fun `search query matches permission names and surfaces safe apps`() {
        val offlineCam = app(
            pkg = "com.offline.cam",
            name = "Offline Camera",
            risk = RiskLevel.SAFE,
            permissions = listOf(Manifest.permission.CAMERA),
        )
        val other = app(pkg = "com.other", name = "Notes", risk = RiskLevel.YELLOW)

        val (main, safe) = filterAndPartitionApps(
            apps = listOf(offlineCam, other),
            query = "CAMERA",
            riskFilter = RiskFilter.ALL,
            capability = CapabilityFilter.ANY,
            sortMode = SortMode.BY_RISK,
        )

        assertEquals(listOf("com.offline.cam"), main.map { it.packageName })
        assertTrue(safe.isEmpty())
    }

    @Test
    fun `capability filter requires runtime permission to be granted`() {
        val grantedCam = app(
            pkg = "com.granted",
            name = "Granted Cam",
            risk = RiskLevel.SAFE,
            permissions = listOf(Manifest.permission.CAMERA),
            granted = setOf(Manifest.permission.CAMERA),
        )
        val deniedCam = app(
            pkg = "com.denied",
            name = "Denied Cam",
            risk = RiskLevel.SAFE,
            permissions = listOf(Manifest.permission.CAMERA),
            granted = emptySet(),
        )

        val (main, _) = filterAndPartitionApps(
            apps = listOf(grantedCam, deniedCam),
            query = "",
            riskFilter = RiskFilter.ALL,
            capability = CapabilityFilter.CAMERA,
            sortMode = SortMode.BY_RISK,
        )

        assertEquals(listOf("com.granted"), main.map { it.packageName })
    }

    @Test
    fun `recently updated sort puts newest update first`() {
        val older = app(pkg = "com.old", name = "Alpha", risk = RiskLevel.RED, lastUpdateTime = 100L)
        val newer = app(pkg = "com.new", name = "Zeta", risk = RiskLevel.YELLOW, lastUpdateTime = 500L)

        val (main, _) = filterAndPartitionApps(
            apps = listOf(older, newer),
            query = "",
            riskFilter = RiskFilter.ALL,
            capability = CapabilityFilter.ANY,
            sortMode = SortMode.RECENTLY_UPDATED,
        )

        assertEquals(listOf("com.new", "com.old"), main.map { it.packageName })
    }

    @Test
    fun `accessibility capability filter matches apps with accessibility threat`() {
        val a11y = app(
            pkg = "com.a11y",
            name = "Helper",
            reasons = listOf(ThreatReason(ThreatId.ACCESSIBILITY_ENABLED, 0, 0, RiskLevel.RED)),
            risk = RiskLevel.RED,
        )
        val plain = app(pkg = "com.plain", name = "Plain")

        val (main, _) = filterAndPartitionApps(
            apps = listOf(a11y, plain),
            query = "",
            riskFilter = RiskFilter.ALL,
            capability = CapabilityFilter.ACCESSIBILITY,
            sortMode = SortMode.BY_RISK,
        )

        assertEquals(listOf("com.a11y"), main.map { it.packageName })
    }
}
