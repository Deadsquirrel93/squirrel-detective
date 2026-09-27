package com.packagespy.app.domain.usecase

import android.Manifest
import com.packagespy.app.R
import com.packagespy.app.domain.model.AppRiskInfo
import com.packagespy.app.domain.model.DeclaredService
import com.packagespy.app.domain.model.KnownLegitimateApps
import com.packagespy.app.domain.model.LegitimateCategory
import com.packagespy.app.domain.model.ReceiverInfo
import com.packagespy.app.domain.model.RiskLevel
import com.packagespy.app.domain.model.ThreatId
import com.packagespy.app.domain.model.ThreatReason
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pure-Kotlin risk classifier. Takes raw permission/receiver data already
 * extracted by the scanner and converts it into a list of [ThreatReason]s
 * plus an aggregate [RiskLevel]. Since stage 02, BIND_* capabilities are
 * detected from declared service/receiver components rather than from
 * `uses-permission`, which any app can declare without effect.
 */
@Singleton
class RiskScorer @Inject constructor() {

    data class Inputs(
        val packageName: String,
        val appName: String,
        val versionName: String?,
        val isSystemApp: Boolean,
        val installerPackage: String?,
        val permissions: List<String>,
        val receivers: List<ReceiverInfo>,
        val services: List<DeclaredService> = emptyList(),
        val isDebuggable: Boolean = false,
        val targetSdk: Int = 0,
        val hasLauncherIntent: Boolean = true,
    )

    fun score(inputs: Inputs): AppRiskInfo {
        val permSet = inputs.permissions.toSet()
        val hasInternet = Manifest.permission.INTERNET in permSet
        val reasons = mutableListOf<ThreatReason>()

        // ---- Package-visibility / observation signals ----

        val hasQueryAll = QUERY_ALL_PACKAGES in permSet
        if (hasQueryAll && hasInternet) {
            reasons += reason(
                ThreatId.QUERY_ALL_PACKAGES_PLUS_INTERNET,
                R.string.threat_query_all_internet_short,
                R.string.threat_query_all_internet_full,
                RiskLevel.RED,
            )
        } else if (hasQueryAll) {
            reasons += reason(
                ThreatId.QUERY_ALL_PACKAGES,
                R.string.threat_query_all_short,
                R.string.threat_query_all_full,
                RiskLevel.YELLOW,
            )
        }

        if (READ_LOGS in permSet) {
            val severe = hasInternet
            reasons += reason(
                if (severe) ThreatId.READ_LOGS_PLUS_INTERNET else ThreatId.READ_LOGS,
                if (severe) R.string.threat_read_logs_internet_short else R.string.threat_read_logs_short,
                R.string.threat_read_logs_full,
                if (severe) RiskLevel.RED else RiskLevel.YELLOW,
            )
        }

        if (PACKAGE_USAGE_STATS in permSet) {
            reasons += reason(
                ThreatId.PACKAGE_USAGE_STATS,
                R.string.threat_package_usage_short,
                R.string.threat_package_usage_full,
                RiskLevel.YELLOW,
            )
        }

        if (REQUEST_INSTALL_PACKAGES in permSet) {
            reasons += reason(
                ThreatId.REQUEST_INSTALL_PACKAGES,
                R.string.threat_install_packages_short,
                R.string.threat_install_packages_full,
                RiskLevel.YELLOW,
            )
        }

        if (Manifest.permission.GET_ACCOUNTS in permSet && hasInternet) {
            reasons += reason(
                ThreatId.GET_ACCOUNTS,
                R.string.threat_get_accounts_short,
                R.string.threat_get_accounts_full,
                RiskLevel.YELLOW,
            )
        }

        if (inputs.services.any { it.permission == BIND_ACCESSIBILITY_SERVICE }) {
            reasons += reason(
                ThreatId.BIND_ACCESSIBILITY,
                R.string.threat_accessibility_short,
                R.string.threat_accessibility_full,
                RiskLevel.RED,
            )
        }

        // ---- New: powerful service binds and overlays ----

        if (SYSTEM_ALERT_WINDOW in permSet) {
            reasons += reason(
                ThreatId.OVERLAY_WINDOW,
                R.string.threat_overlay_short,
                R.string.threat_overlay_full,
                RiskLevel.YELLOW,
            )
        }

        if (inputs.receivers.any { it.permission == BIND_DEVICE_ADMIN }) {
            reasons += reason(
                ThreatId.DEVICE_ADMIN,
                R.string.threat_device_admin_short,
                R.string.threat_device_admin_full,
                RiskLevel.RED,
            )
        }

        if (inputs.services.any { it.permission == BIND_NOTIFICATION_LISTENER }) {
            reasons += reason(
                ThreatId.NOTIFICATION_LISTENER,
                R.string.threat_notifications_short,
                R.string.threat_notifications_full,
                RiskLevel.YELLOW,
            )
        }

        if (inputs.services.any { it.permission == BIND_VPN_SERVICE }) {
            reasons += reason(
                ThreatId.VPN_SERVICE,
                R.string.threat_vpn_short,
                R.string.threat_vpn_full,
                RiskLevel.YELLOW,
            )
        }

        if (inputs.services.any { it.permission == BIND_INPUT_METHOD }) {
            reasons += reason(
                ThreatId.INPUT_METHOD_SERVICE,
                R.string.threat_input_method_short,
                R.string.threat_input_method_full,
                RiskLevel.YELLOW,
            )
        }

        // ---- Sensitive media / messaging access ----

        if (Manifest.permission.RECORD_AUDIO in permSet && hasInternet) {
            reasons += reason(
                ThreatId.RECORD_AUDIO_PLUS_INTERNET,
                R.string.threat_record_audio_internet_short,
                R.string.threat_record_audio_internet_full,
                RiskLevel.YELLOW,
            )
        }

        if (Manifest.permission.CAMERA in permSet && hasInternet) {
            reasons += reason(
                ThreatId.CAMERA_PLUS_INTERNET,
                R.string.threat_camera_internet_short,
                R.string.threat_camera_internet_full,
                RiskLevel.YELLOW,
            )
        }

        val smsPerms = setOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_MMS,
            Manifest.permission.RECEIVE_WAP_PUSH,
        )
        if (smsPerms.any { it in permSet }) {
            reasons += reason(
                ThreatId.SMS_ACCESS,
                R.string.threat_sms_short,
                R.string.threat_sms_full,
                RiskLevel.RED,
            )
        }

        if (Manifest.permission.READ_CONTACTS in permSet && hasInternet) {
            reasons += reason(
                ThreatId.CONTACTS_PLUS_INTERNET,
                R.string.threat_contacts_internet_short,
                R.string.threat_contacts_internet_full,
                RiskLevel.YELLOW,
            )
        }

        val callLogPerms = setOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
        )
        if (callLogPerms.any { it in permSet }) {
            reasons += reason(
                ThreatId.CALL_LOG_ACCESS,
                R.string.threat_call_log_short,
                R.string.threat_call_log_full,
                RiskLevel.YELLOW,
            )
        }

        val phoneStatePerms = setOf(
            Manifest.permission.READ_PHONE_STATE,
            READ_PHONE_NUMBERS,
        )
        if (phoneStatePerms.any { it in permSet }) {
            reasons += reason(
                ThreatId.PHONE_STATE_ACCESS,
                R.string.threat_phone_state_short,
                R.string.threat_phone_state_full,
                RiskLevel.YELLOW,
            )
        }

        val locationPerms = setOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            ACCESS_BACKGROUND_LOCATION,
        )
        if (locationPerms.any { it in permSet } && hasInternet) {
            reasons += reason(
                ThreatId.LOCATION_PLUS_INTERNET,
                R.string.threat_location_internet_short,
                R.string.threat_location_internet_full,
                RiskLevel.YELLOW,
            )
        }

        if (MANAGE_EXTERNAL_STORAGE in permSet) {
            reasons += reason(
                ThreatId.EXTERNAL_STORAGE_ALL,
                R.string.threat_external_storage_short,
                R.string.threat_external_storage_full,
                RiskLevel.YELLOW,
            )
        }

        if (WRITE_SETTINGS in permSet || WRITE_SECURE_SETTINGS in permSet) {
            val severe = WRITE_SECURE_SETTINGS in permSet
            reasons += reason(
                ThreatId.WRITE_SETTINGS_OR_SECURE,
                R.string.threat_write_settings_short,
                R.string.threat_write_settings_full,
                if (severe) RiskLevel.RED else RiskLevel.YELLOW,
            )
        }

        if (!inputs.isSystemApp && (GET_TASKS in permSet || REAL_GET_TASKS in permSet)) {
            reasons += reason(
                ThreatId.GET_TASKS_DEPRECATED,
                R.string.threat_get_tasks_short,
                R.string.threat_get_tasks_full,
                RiskLevel.YELLOW,
            )
        }

        // ---- Receiver-based threats ----

        val receiverActions = inputs.receivers.flatMap { it.actions }.toSet()
        val listensPackageAdded = ACTION_PACKAGE_ADDED in receiverActions
        val listensPackageRemoved = ACTION_PACKAGE_REMOVED in receiverActions
        val listensBoot = ACTION_BOOT_COMPLETED in receiverActions

        if (listensPackageAdded) {
            reasons += reason(
                ThreatId.RECEIVER_PACKAGE_ADDED,
                R.string.threat_receiver_added_short,
                R.string.threat_receiver_added_full,
                RiskLevel.YELLOW,
            )
        }
        if (listensPackageRemoved) {
            reasons += reason(
                ThreatId.RECEIVER_PACKAGE_REMOVED,
                R.string.threat_receiver_removed_short,
                R.string.threat_receiver_removed_full,
                RiskLevel.YELLOW,
            )
        }
        if (listensBoot && (hasQueryAll || PACKAGE_USAGE_STATS in permSet)) {
            reasons += reason(
                ThreatId.RECEIVER_BOOT_WITH_PACKAGE_PERMISSION,
                R.string.threat_receiver_boot_short,
                R.string.threat_receiver_boot_full,
                RiskLevel.YELLOW,
            )
        }

        // ---- Build/install metadata signals ----

        if (inputs.isDebuggable && !inputs.isSystemApp) {
            reasons += reason(
                ThreatId.DEBUGGABLE_BUILD,
                R.string.threat_debuggable_short,
                R.string.threat_debuggable_full,
                RiskLevel.YELLOW,
            )
        }

        if (inputs.targetSdk in 1..22 && !inputs.isSystemApp) {
            reasons += reason(
                ThreatId.OLD_TARGET_SDK,
                R.string.threat_old_target_sdk_short,
                R.string.threat_old_target_sdk_full,
                RiskLevel.YELLOW,
            )
        }

        // Hidden-app signal: no launcher activity AND has any sensitive permission.
        val sensitiveSignal = reasons.any { it.severity == RiskLevel.RED || it.severity == RiskLevel.YELLOW }
        if (!inputs.hasLauncherIntent && !inputs.isSystemApp && sensitiveSignal) {
            reasons += reason(
                ThreatId.HIDDEN_NO_LAUNCHER,
                R.string.threat_no_launcher_short,
                R.string.threat_no_launcher_full,
                RiskLevel.RED,
            )
        }

        // Sideloaded app with broad access — only flag if multiple risky reasons exist.
        if (!inputs.isSystemApp && reasons.size >= 2) {
            val installer = inputs.installerPackage
            val sideloaded = installer.isNullOrBlank() || installer in UNKNOWN_INSTALLERS
            if (sideloaded) {
                reasons += reason(
                    ThreatId.UNKNOWN_INSTALLER,
                    R.string.threat_unknown_installer_short,
                    R.string.threat_unknown_installer_full,
                    RiskLevel.YELLOW,
                )
            }
        }

        // ---- Whitelist downgrade ----

        val legitimateCategory = KnownLegitimateApps.categorize(inputs.packageName)
        val finalReasons = if (legitimateCategory != null && reasons.isNotEmpty()) {
            listOf(legitimateReason(legitimateCategory)) + reasons
        } else {
            reasons
        }

        val aggregate = aggregateLevel(
            reasons = finalReasons,
            legitimateCategory = legitimateCategory,
            isSystemApp = inputs.isSystemApp,
        )

        return AppRiskInfo(
            packageName = inputs.packageName,
            appName = inputs.appName,
            versionName = inputs.versionName,
            isSystemApp = inputs.isSystemApp,
            installerPackage = inputs.installerPackage,
            permissions = inputs.permissions,
            receivers = inputs.receivers,
            reasons = finalReasons,
            riskLevel = aggregate,
        )
    }

    private fun reason(
        id: ThreatId,
        shortRes: Int,
        explanationRes: Int,
        severity: RiskLevel,
    ) = ThreatReason(
        id = id,
        shortTextRes = shortRes,
        explanationRes = explanationRes,
        severity = severity,
    )

    private fun legitimateReason(category: LegitimateCategory): ThreatReason = when (category) {
        LegitimateCategory.LAUNCHER -> reason(
            ThreatId.KNOWN_LEGITIMATE_LAUNCHER,
            R.string.whitelist_launcher_short,
            R.string.whitelist_launcher_full,
            RiskLevel.GREEN,
        )
        LegitimateCategory.APP_STORE -> reason(
            ThreatId.KNOWN_LEGITIMATE_STORE,
            R.string.whitelist_store_short,
            R.string.whitelist_store_full,
            RiskLevel.GREEN,
        )
        LegitimateCategory.FILE_MANAGER -> reason(
            ThreatId.KNOWN_LEGITIMATE_FILE_MANAGER,
            R.string.whitelist_file_manager_short,
            R.string.whitelist_file_manager_full,
            RiskLevel.GREEN,
        )
        LegitimateCategory.ANTIVIRUS -> reason(
            ThreatId.KNOWN_LEGITIMATE_ANTIVIRUS,
            R.string.whitelist_antivirus_short,
            R.string.whitelist_antivirus_full,
            RiskLevel.GREEN,
        )
        LegitimateCategory.SYSTEM_TOOL -> reason(
            ThreatId.KNOWN_LEGITIMATE_SYSTEM,
            R.string.whitelist_system_short,
            R.string.whitelist_system_full,
            RiskLevel.GREEN,
        )
    }

    private fun aggregateLevel(
        reasons: List<ThreatReason>,
        legitimateCategory: LegitimateCategory?,
        isSystemApp: Boolean,
    ): RiskLevel {
        if (reasons.isEmpty()) return RiskLevel.SAFE
        if (legitimateCategory != null) return RiskLevel.GREEN
        val maxSeverity = reasons.maxBy { -it.severity.sortOrder }.severity
        return if (isSystemApp && maxSeverity == RiskLevel.RED) RiskLevel.YELLOW
        else maxSeverity
    }

    companion object {
        const val QUERY_ALL_PACKAGES = "android.permission.QUERY_ALL_PACKAGES"
        const val PACKAGE_USAGE_STATS = "android.permission.PACKAGE_USAGE_STATS"
        const val REQUEST_INSTALL_PACKAGES = "android.permission.REQUEST_INSTALL_PACKAGES"
        const val READ_LOGS = "android.permission.READ_LOGS"
        const val BIND_ACCESSIBILITY_SERVICE = "android.permission.BIND_ACCESSIBILITY_SERVICE"
        const val SYSTEM_ALERT_WINDOW = "android.permission.SYSTEM_ALERT_WINDOW"
        const val BIND_DEVICE_ADMIN = "android.permission.BIND_DEVICE_ADMIN"
        const val BIND_NOTIFICATION_LISTENER = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
        const val BIND_VPN_SERVICE = "android.permission.BIND_VPN_SERVICE"
        const val BIND_INPUT_METHOD = "android.permission.BIND_INPUT_METHOD"
        const val MANAGE_EXTERNAL_STORAGE = "android.permission.MANAGE_EXTERNAL_STORAGE"
        const val WRITE_SETTINGS = "android.permission.WRITE_SETTINGS"
        const val WRITE_SECURE_SETTINGS = "android.permission.WRITE_SECURE_SETTINGS"
        const val GET_TASKS = "android.permission.GET_TASKS"
        const val REAL_GET_TASKS = "android.permission.REAL_GET_TASKS"
        const val READ_PHONE_NUMBERS = "android.permission.READ_PHONE_NUMBERS"
        const val ACCESS_BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION"

        const val ACTION_PACKAGE_ADDED = "android.intent.action.PACKAGE_ADDED"
        const val ACTION_PACKAGE_REMOVED = "android.intent.action.PACKAGE_REMOVED"
        const val ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"

        // Installers that indicate a sideload — adb or the system package
        // installer (used when the user opens an APK file directly).
        private val UNKNOWN_INSTALLERS = setOf(
            "com.android.shell",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
        )
    }
}
