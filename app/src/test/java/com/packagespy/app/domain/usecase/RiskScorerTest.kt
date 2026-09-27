package com.packagespy.app.domain.usecase

import android.Manifest
import com.packagespy.app.domain.model.AppRiskInfo
import com.packagespy.app.domain.model.ReceiverInfo
import com.packagespy.app.domain.model.RiskLevel
import com.packagespy.app.domain.model.ThreatId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Characterization tests for [RiskScorer]. These pin down the CURRENT behavior
 * of `score()` before the Stage 01 toolchain upgrade, so any behavioral
 * regression introduced along the way fails a test instead of shipping silently.
 */
class RiskScorerTest {

    private val scorer = RiskScorer()

    private fun inputs(
        packageName: String = "com.example.app",
        appName: String = "Example App",
        versionName: String? = "1.0",
        isSystemApp: Boolean = false,
        installerPackage: String? = "com.android.vending",
        permissions: List<String> = emptyList(),
        receivers: List<ReceiverInfo> = emptyList(),
        isDebuggable: Boolean = false,
        targetSdk: Int = 34,
        hasLauncherIntent: Boolean = true,
    ) = RiskScorer.Inputs(
        packageName = packageName,
        appName = appName,
        versionName = versionName,
        isSystemApp = isSystemApp,
        installerPackage = installerPackage,
        permissions = permissions,
        receivers = receivers,
        isDebuggable = isDebuggable,
        targetSdk = targetSdk,
        hasLauncherIntent = hasLauncherIntent,
    )

    private fun ids(result: AppRiskInfo): List<ThreatId> = result.reasons.map { it.id }

    // ---- 1: clean app ----

    @Test
    fun `no permissions yields no reasons and SAFE`() {
        val result = scorer.score(inputs())
        assertTrue(result.reasons.isEmpty())
        assertEquals(RiskLevel.SAFE, result.riskLevel)
    }

    // ---- 2: QUERY_ALL_PACKAGES ----

    @Test
    fun `QUERY_ALL_PACKAGES plus INTERNET is RED`() {
        val result = scorer.score(
            inputs(permissions = listOf(RiskScorer.QUERY_ALL_PACKAGES, Manifest.permission.INTERNET))
        )
        assertEquals(listOf(ThreatId.QUERY_ALL_PACKAGES_PLUS_INTERNET), ids(result))
        assertEquals(RiskLevel.RED, result.riskLevel)
    }

    @Test
    fun `QUERY_ALL_PACKAGES alone is YELLOW`() {
        val result = scorer.score(inputs(permissions = listOf(RiskScorer.QUERY_ALL_PACKAGES)))
        assertEquals(listOf(ThreatId.QUERY_ALL_PACKAGES), ids(result))
        assertEquals(RiskLevel.YELLOW, result.riskLevel)
    }

    // ---- 3: READ_LOGS ----

    @Test
    fun `READ_LOGS plus INTERNET is RED`() {
        val result = scorer.score(
            inputs(permissions = listOf(RiskScorer.READ_LOGS, Manifest.permission.INTERNET))
        )
        assertEquals(listOf(ThreatId.READ_LOGS_PLUS_INTERNET), ids(result))
        assertEquals(RiskLevel.RED, result.riskLevel)
    }

    @Test
    fun `READ_LOGS alone is YELLOW`() {
        val result = scorer.score(inputs(permissions = listOf(RiskScorer.READ_LOGS)))
        assertEquals(listOf(ThreatId.READ_LOGS), ids(result))
        assertEquals(RiskLevel.YELLOW, result.riskLevel)
    }

    // ---- 4: every remaining single-permission rule ----

    private data class RuleCase(
        val label: String,
        val permissions: List<String>,
        val expectedId: ThreatId,
        val expectedLevel: RiskLevel,
    )

    @Test
    fun `each remaining permission rule maps to its ThreatId and level`() {
        val cases = listOf(
            RuleCase(
                "PACKAGE_USAGE_STATS",
                listOf(RiskScorer.PACKAGE_USAGE_STATS),
                ThreatId.PACKAGE_USAGE_STATS,
                RiskLevel.YELLOW,
            ),
            RuleCase(
                "REQUEST_INSTALL_PACKAGES",
                listOf(RiskScorer.REQUEST_INSTALL_PACKAGES),
                ThreatId.REQUEST_INSTALL_PACKAGES,
                RiskLevel.YELLOW,
            ),
            RuleCase(
                "GET_ACCOUNTS+INTERNET",
                listOf(Manifest.permission.GET_ACCOUNTS, Manifest.permission.INTERNET),
                ThreatId.GET_ACCOUNTS,
                RiskLevel.YELLOW,
            ),
            RuleCase(
                "BIND_ACCESSIBILITY_SERVICE",
                listOf(RiskScorer.BIND_ACCESSIBILITY_SERVICE),
                ThreatId.BIND_ACCESSIBILITY,
                RiskLevel.RED,
            ),
            RuleCase(
                "SYSTEM_ALERT_WINDOW",
                listOf(RiskScorer.SYSTEM_ALERT_WINDOW),
                ThreatId.OVERLAY_WINDOW,
                RiskLevel.YELLOW,
            ),
            RuleCase(
                "BIND_DEVICE_ADMIN",
                listOf(RiskScorer.BIND_DEVICE_ADMIN),
                ThreatId.DEVICE_ADMIN,
                RiskLevel.RED,
            ),
            RuleCase(
                "BIND_NOTIFICATION_LISTENER",
                listOf(RiskScorer.BIND_NOTIFICATION_LISTENER),
                ThreatId.NOTIFICATION_LISTENER,
                RiskLevel.YELLOW,
            ),
            RuleCase(
                "BIND_VPN_SERVICE",
                listOf(RiskScorer.BIND_VPN_SERVICE),
                ThreatId.VPN_SERVICE,
                RiskLevel.YELLOW,
            ),
            RuleCase(
                "BIND_INPUT_METHOD",
                listOf(RiskScorer.BIND_INPUT_METHOD),
                ThreatId.INPUT_METHOD_SERVICE,
                RiskLevel.YELLOW,
            ),
            RuleCase(
                "RECORD_AUDIO+INTERNET",
                listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.INTERNET),
                ThreatId.RECORD_AUDIO_PLUS_INTERNET,
                RiskLevel.YELLOW,
            ),
            RuleCase(
                "CAMERA+INTERNET",
                listOf(Manifest.permission.CAMERA, Manifest.permission.INTERNET),
                ThreatId.CAMERA_PLUS_INTERNET,
                RiskLevel.YELLOW,
            ),
            RuleCase(
                "SMS group (READ_SMS)",
                listOf(Manifest.permission.READ_SMS),
                ThreatId.SMS_ACCESS,
                RiskLevel.RED,
            ),
            RuleCase(
                "CONTACTS+INTERNET",
                listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.INTERNET),
                ThreatId.CONTACTS_PLUS_INTERNET,
                RiskLevel.YELLOW,
            ),
            RuleCase(
                "CALL_LOG group (READ_CALL_LOG)",
                listOf(Manifest.permission.READ_CALL_LOG),
                ThreatId.CALL_LOG_ACCESS,
                RiskLevel.YELLOW,
            ),
            RuleCase(
                "PHONE_STATE (READ_PHONE_STATE)",
                listOf(Manifest.permission.READ_PHONE_STATE),
                ThreatId.PHONE_STATE_ACCESS,
                RiskLevel.YELLOW,
            ),
            RuleCase(
                "LOCATION+INTERNET",
                listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET),
                ThreatId.LOCATION_PLUS_INTERNET,
                RiskLevel.YELLOW,
            ),
            RuleCase(
                "MANAGE_EXTERNAL_STORAGE",
                listOf(RiskScorer.MANAGE_EXTERNAL_STORAGE),
                ThreatId.EXTERNAL_STORAGE_ALL,
                RiskLevel.YELLOW,
            ),
            RuleCase(
                "WRITE_SETTINGS",
                listOf(RiskScorer.WRITE_SETTINGS),
                ThreatId.WRITE_SETTINGS_OR_SECURE,
                RiskLevel.YELLOW,
            ),
            RuleCase(
                "WRITE_SECURE_SETTINGS",
                listOf(RiskScorer.WRITE_SECURE_SETTINGS),
                ThreatId.WRITE_SETTINGS_OR_SECURE,
                RiskLevel.RED,
            ),
            RuleCase(
                "GET_TASKS",
                listOf(RiskScorer.GET_TASKS),
                ThreatId.GET_TASKS_DEPRECATED,
                RiskLevel.YELLOW,
            ),
        )

        for (case in cases) {
            val result = scorer.score(inputs(permissions = case.permissions))
            assertEquals("case: ${case.label}", listOf(case.expectedId), ids(result))
            assertEquals("case: ${case.label}", case.expectedLevel, result.riskLevel)
        }
    }

    // ---- 5: "+INTERNET" combos without INTERNET are not flagged ----

    @Test
    fun `sensitive permissions without INTERNET are not flagged`() {
        val soloPermissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.GET_ACCOUNTS,
        )
        for (permission in soloPermissions) {
            val result = scorer.score(inputs(permissions = listOf(permission)))
            assertTrue("permission: $permission", result.reasons.isEmpty())
            assertEquals("permission: $permission", RiskLevel.SAFE, result.riskLevel)
        }
    }

    // ---- 6: receivers ----

    @Test
    fun `PACKAGE_ADDED receiver is flagged`() {
        val result = scorer.score(
            inputs(receivers = listOf(ReceiverInfo("Rcvr", listOf(RiskScorer.ACTION_PACKAGE_ADDED))))
        )
        assertEquals(listOf(ThreatId.RECEIVER_PACKAGE_ADDED), ids(result))
    }

    @Test
    fun `PACKAGE_REMOVED receiver is flagged`() {
        val result = scorer.score(
            inputs(receivers = listOf(ReceiverInfo("Rcvr", listOf(RiskScorer.ACTION_PACKAGE_REMOVED))))
        )
        assertEquals(listOf(ThreatId.RECEIVER_PACKAGE_REMOVED), ids(result))
    }

    @Test
    fun `BOOT_COMPLETED receiver alone is not flagged`() {
        val result = scorer.score(
            inputs(receivers = listOf(ReceiverInfo("Rcvr", listOf(RiskScorer.ACTION_BOOT_COMPLETED))))
        )
        assertTrue(ThreatId.RECEIVER_BOOT_WITH_PACKAGE_PERMISSION !in ids(result))
    }

    @Test
    fun `BOOT_COMPLETED receiver with QUERY_ALL_PACKAGES is flagged`() {
        val result = scorer.score(
            inputs(
                permissions = listOf(RiskScorer.QUERY_ALL_PACKAGES),
                receivers = listOf(ReceiverInfo("Rcvr", listOf(RiskScorer.ACTION_BOOT_COMPLETED))),
            )
        )
        assertTrue(ThreatId.RECEIVER_BOOT_WITH_PACKAGE_PERMISSION in ids(result))
    }

    // ---- 7: debuggable ----

    @Test
    fun `debuggable third-party app is flagged`() {
        val result = scorer.score(inputs(isDebuggable = true))
        assertEquals(listOf(ThreatId.DEBUGGABLE_BUILD), ids(result))
    }

    @Test
    fun `debuggable system app is not flagged`() {
        val result = scorer.score(inputs(isDebuggable = true, isSystemApp = true))
        assertTrue(ThreatId.DEBUGGABLE_BUILD !in ids(result))
    }

    // ---- 8: old target sdk ----

    @Test
    fun `targetSdk 22 is flagged as old`() {
        val result = scorer.score(inputs(targetSdk = 22))
        assertEquals(listOf(ThreatId.OLD_TARGET_SDK), ids(result))
    }

    @Test
    fun `targetSdk 0 and 23 are not flagged as old`() {
        assertTrue(ThreatId.OLD_TARGET_SDK !in ids(scorer.score(inputs(targetSdk = 0))))
        assertTrue(ThreatId.OLD_TARGET_SDK !in ids(scorer.score(inputs(targetSdk = 23))))
    }

    // ---- 9: hidden, no launcher ----

    @Test
    fun `hidden app with a sensitive permission is flagged RED`() {
        val result = scorer.score(
            inputs(hasLauncherIntent = false, permissions = listOf(RiskScorer.QUERY_ALL_PACKAGES))
        )
        assertTrue(ThreatId.HIDDEN_NO_LAUNCHER in ids(result))
        assertEquals(RiskLevel.RED, result.riskLevel)
    }

    @Test
    fun `hidden app without permissions is not flagged`() {
        val result = scorer.score(inputs(hasLauncherIntent = false))
        assertTrue(result.reasons.isEmpty())
        assertEquals(RiskLevel.SAFE, result.riskLevel)
    }

    // ---- 10: unknown installer ----

    @Test
    fun `null installer with two or more reasons is flagged as unknown installer`() {
        val result = scorer.score(
            inputs(
                installerPackage = null,
                permissions = listOf(RiskScorer.QUERY_ALL_PACKAGES, Manifest.permission.READ_SMS),
            )
        )
        assertTrue(ThreatId.UNKNOWN_INSTALLER in ids(result))
    }

    @Test
    fun `null installer with a single reason is not flagged as unknown installer`() {
        val result = scorer.score(
            inputs(installerPackage = null, permissions = listOf(RiskScorer.QUERY_ALL_PACKAGES))
        )
        assertTrue(ThreatId.UNKNOWN_INSTALLER !in ids(result))
    }

    @Test
    fun `known sideload installer with two reasons is flagged as unknown installer`() {
        val result = scorer.score(
            inputs(
                installerPackage = "com.google.android.packageinstaller",
                permissions = listOf(RiskScorer.QUERY_ALL_PACKAGES, Manifest.permission.READ_SMS),
            )
        )
        assertTrue(ThreatId.UNKNOWN_INSTALLER in ids(result))
    }

    // ---- 11: whitelist ----

    @Test
    fun `whitelisted store package with risky permissions is downgraded to GREEN`() {
        val result = scorer.score(
            inputs(
                packageName = "com.android.vending",
                permissions = listOf(RiskScorer.QUERY_ALL_PACKAGES, Manifest.permission.INTERNET),
            )
        )
        assertEquals(ThreatId.KNOWN_LEGITIMATE_STORE, result.reasons.first().id)
        assertEquals(RiskLevel.GREEN, result.riskLevel)
    }

    @Test
    fun `whitelisted package without permissions stays SAFE with no whitelist reason`() {
        val result = scorer.score(inputs(packageName = "com.android.vending"))
        assertTrue(result.reasons.isEmpty())
        assertEquals(RiskLevel.SAFE, result.riskLevel)
    }

    // ---- 12: system app downgrade ----

    @Test
    fun `system app with a RED cause is downgraded to YELLOW`() {
        val result = scorer.score(
            inputs(isSystemApp = true, permissions = listOf(Manifest.permission.READ_SMS))
        )
        assertEquals(RiskLevel.YELLOW, result.riskLevel)
        assertEquals(RiskLevel.RED, result.reasons.single { it.id == ThreatId.SMS_ACCESS }.severity)
    }

    @Test
    fun `GET_TASKS is not flagged for a system app`() {
        val result = scorer.score(inputs(isSystemApp = true, permissions = listOf(RiskScorer.GET_TASKS)))
        assertTrue(result.reasons.isEmpty())
    }

    // ---- 13: raw fields are preserved as-is ----

    @Test
    fun `AppRiskInfo preserves input permissions, receivers and packageName`() {
        val permissions = listOf(RiskScorer.QUERY_ALL_PACKAGES, Manifest.permission.INTERNET)
        val receivers = listOf(ReceiverInfo("Rcvr", listOf(RiskScorer.ACTION_PACKAGE_ADDED)))
        val result = scorer.score(
            inputs(packageName = "com.example.spy", permissions = permissions, receivers = receivers)
        )
        assertEquals("com.example.spy", result.packageName)
        assertEquals(permissions, result.permissions)
        assertEquals(receivers, result.receivers)
    }
}
