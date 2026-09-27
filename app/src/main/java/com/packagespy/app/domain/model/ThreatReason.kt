package com.packagespy.app.domain.model

import androidx.annotation.StringRes

/**
 * A single concrete reason an app is flagged. Carries a stable id (used for
 * persistence and diffing) plus references to localized text resources.
 */
data class ThreatReason(
    val id: ThreatId,
    @StringRes val shortTextRes: Int,
    @StringRes val explanationRes: Int,
    val severity: RiskLevel,
)

enum class ThreatId {
    QUERY_ALL_PACKAGES,
    QUERY_ALL_PACKAGES_PLUS_INTERNET,
    PACKAGE_USAGE_STATS,
    REQUEST_INSTALL_PACKAGES,
    READ_LOGS,
    READ_LOGS_PLUS_INTERNET,
    RECEIVER_PACKAGE_ADDED,
    RECEIVER_PACKAGE_REMOVED,
    RECEIVER_BOOT_WITH_PACKAGE_PERMISSION,
    GET_ACCOUNTS,
    BIND_ACCESSIBILITY,
    OVERLAY_WINDOW,
    RECORD_AUDIO_PLUS_INTERNET,
    CAMERA_PLUS_INTERNET,
    SMS_ACCESS,
    CONTACTS_PLUS_INTERNET,
    CALL_LOG_ACCESS,
    PHONE_STATE_ACCESS,
    LOCATION_PLUS_INTERNET,
    DEVICE_ADMIN,
    NOTIFICATION_LISTENER,
    VPN_SERVICE,
    INPUT_METHOD_SERVICE,
    EXTERNAL_STORAGE_ALL,
    WRITE_SETTINGS_OR_SECURE,
    GET_TASKS_DEPRECATED,
    UNKNOWN_INSTALLER,
    DEBUGGABLE_BUILD,
    OLD_TARGET_SDK,
    HIDDEN_NO_LAUNCHER,
    KNOWN_LEGITIMATE_LAUNCHER,
    KNOWN_LEGITIMATE_STORE,
    KNOWN_LEGITIMATE_FILE_MANAGER,
    KNOWN_LEGITIMATE_ANTIVIRUS,
}
