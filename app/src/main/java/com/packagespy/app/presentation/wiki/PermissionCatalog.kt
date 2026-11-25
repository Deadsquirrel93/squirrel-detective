package com.packagespy.app.presentation.wiki

import android.content.Context
import android.content.pm.PermissionInfo
import androidx.annotation.StringRes
import androidx.core.content.pm.PermissionInfoCompat
import com.packagespy.app.R
import com.packagespy.app.domain.model.displayPermissionName

data class WikiEntry(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val permissions: Set<String> = emptySet(),
    val isMonospaceTitle: Boolean = true,
)

data class WikiCategory(
    @StringRes val titleRes: Int,
    val entries: List<WikiEntry>,
)

enum class PermissionProtection {
    DANGEROUS,
    NORMAL,
    SIGNATURE,
    OTHER,
}

data class ResolvedPermissionDetails(
    val fullName: String,
    val wikiEntry: WikiEntry?,
    val systemLabel: String?,
    val systemDescription: String?,
    val protection: PermissionProtection,
)

object PermissionCatalog {

    val categories: List<WikiCategory> = listOf(
        WikiCategory(
            titleRes = R.string.wiki_category_combos,
            entries = listOf(
                WikiEntry(
                    titleRes = R.string.combo_banking_2fa_title,
                    bodyRes = R.string.combo_banking_2fa_summary,
                    isMonospaceTitle = false,
                ),
                WikiEntry(
                    titleRes = R.string.combo_silent_dropper_title,
                    bodyRes = R.string.combo_silent_dropper_summary,
                    isMonospaceTitle = false,
                ),
                WikiEntry(
                    titleRes = R.string.combo_stalkerware_title,
                    bodyRes = R.string.combo_stalkerware_summary,
                    isMonospaceTitle = false,
                ),
                WikiEntry(
                    titleRes = R.string.combo_uninstall_lock_title,
                    bodyRes = R.string.combo_uninstall_lock_summary,
                    isMonospaceTitle = false,
                ),
                WikiEntry(
                    titleRes = R.string.combo_profiling_title,
                    bodyRes = R.string.combo_profiling_summary,
                    isMonospaceTitle = false,
                ),
            ),
        ),
        WikiCategory(
            titleRes = R.string.wiki_category_visibility,
            entries = listOf(
                WikiEntry(
                    R.string.wiki_query_all_title,
                    R.string.wiki_query_all_body,
                    permissions = setOf("QUERY_ALL_PACKAGES"),
                ),
                WikiEntry(
                    R.string.wiki_usage_stats_title,
                    R.string.wiki_usage_stats_body,
                    permissions = setOf("PACKAGE_USAGE_STATS"),
                ),
                WikiEntry(
                    R.string.wiki_read_logs_title,
                    R.string.wiki_read_logs_body,
                    permissions = setOf("READ_LOGS"),
                ),
                WikiEntry(
                    R.string.wiki_get_tasks_title,
                    R.string.wiki_get_tasks_body,
                    permissions = setOf("GET_TASKS", "REAL_GET_TASKS"),
                ),
            ),
        ),
        WikiCategory(
            titleRes = R.string.wiki_category_services,
            entries = listOf(
                WikiEntry(
                    R.string.wiki_declared_vs_enabled_title,
                    R.string.wiki_declared_vs_enabled_body,
                    isMonospaceTitle = false,
                ),
                WikiEntry(
                    R.string.wiki_accessibility_title,
                    R.string.wiki_accessibility_body,
                    permissions = setOf("BIND_ACCESSIBILITY_SERVICE"),
                ),
                WikiEntry(
                    R.string.wiki_device_admin_title,
                    R.string.wiki_device_admin_body,
                    permissions = setOf("BIND_DEVICE_ADMIN"),
                ),
                WikiEntry(
                    R.string.wiki_notif_listener_title,
                    R.string.wiki_notif_listener_body,
                    permissions = setOf("BIND_NOTIFICATION_LISTENER_SERVICE"),
                ),
                WikiEntry(
                    R.string.wiki_vpn_title,
                    R.string.wiki_vpn_body,
                    permissions = setOf("BIND_VPN_SERVICE"),
                ),
                WikiEntry(
                    R.string.wiki_input_method_title,
                    R.string.wiki_input_method_body,
                    permissions = setOf("BIND_INPUT_METHOD"),
                ),
                WikiEntry(
                    R.string.wiki_overlay_title,
                    R.string.wiki_overlay_body,
                    permissions = setOf("SYSTEM_ALERT_WINDOW"),
                ),
            ),
        ),
        WikiCategory(
            titleRes = R.string.wiki_category_sensors,
            entries = listOf(
                WikiEntry(
                    R.string.wiki_camera_title,
                    R.string.wiki_camera_body,
                    permissions = setOf("CAMERA"),
                ),
                WikiEntry(
                    R.string.wiki_record_audio_title,
                    R.string.wiki_record_audio_body,
                    permissions = setOf("RECORD_AUDIO"),
                ),
                WikiEntry(
                    R.string.wiki_body_sensors_title,
                    R.string.wiki_body_sensors_body,
                    permissions = setOf("BODY_SENSORS", "BODY_SENSORS_BACKGROUND", "ACTIVITY_RECOGNITION"),
                ),
            ),
        ),
        WikiCategory(
            titleRes = R.string.wiki_category_communication,
            entries = listOf(
                WikiEntry(
                    R.string.wiki_sms_title,
                    R.string.wiki_sms_body,
                    permissions = setOf("READ_SMS", "RECEIVE_SMS", "SEND_SMS", "RECEIVE_MMS", "RECEIVE_WAP_PUSH"),
                ),
                WikiEntry(
                    R.string.wiki_contacts_title,
                    R.string.wiki_contacts_body,
                    permissions = setOf("READ_CONTACTS", "WRITE_CONTACTS"),
                ),
                WikiEntry(
                    R.string.wiki_call_log_title,
                    R.string.wiki_call_log_body,
                    permissions = setOf("READ_CALL_LOG", "WRITE_CALL_LOG", "PROCESS_OUTGOING_CALLS"),
                ),
                WikiEntry(
                    R.string.wiki_phone_state_title,
                    R.string.wiki_phone_state_body,
                    permissions = setOf("READ_PHONE_STATE", "READ_PHONE_NUMBERS"),
                ),
                WikiEntry(
                    R.string.wiki_get_accounts_title,
                    R.string.wiki_get_accounts_body,
                    permissions = setOf("GET_ACCOUNTS"),
                ),
                WikiEntry(
                    R.string.wiki_calendar_title,
                    R.string.wiki_calendar_body,
                    permissions = setOf("READ_CALENDAR", "WRITE_CALENDAR"),
                ),
            ),
        ),
        WikiCategory(
            titleRes = R.string.wiki_category_location,
            entries = listOf(
                WikiEntry(
                    R.string.wiki_location_title,
                    R.string.wiki_location_body,
                    permissions = setOf("ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION"),
                ),
                WikiEntry(
                    R.string.wiki_bg_location_title,
                    R.string.wiki_bg_location_body,
                    permissions = setOf("ACCESS_BACKGROUND_LOCATION"),
                ),
                WikiEntry(
                    R.string.wiki_nearby_devices_title,
                    R.string.wiki_nearby_devices_body,
                    permissions = setOf(
                        "BLUETOOTH_SCAN",
                        "BLUETOOTH_CONNECT",
                        "BLUETOOTH_ADVERTISE",
                        "NEARBY_WIFI_DEVICES",
                    ),
                ),
            ),
        ),
        WikiCategory(
            titleRes = R.string.wiki_category_storage,
            entries = listOf(
                WikiEntry(
                    R.string.wiki_manage_storage_title,
                    R.string.wiki_manage_storage_body,
                    permissions = setOf("MANAGE_EXTERNAL_STORAGE"),
                ),
                WikiEntry(
                    R.string.wiki_storage_title,
                    R.string.wiki_storage_body,
                    permissions = setOf("READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE"),
                ),
                WikiEntry(
                    R.string.wiki_read_media_title,
                    R.string.wiki_read_media_body,
                    permissions = setOf(
                        "READ_MEDIA_IMAGES",
                        "READ_MEDIA_VIDEO",
                        "READ_MEDIA_AUDIO",
                        "READ_MEDIA_VISUAL_USER_SELECTED",
                    ),
                ),
            ),
        ),
        WikiCategory(
            titleRes = R.string.wiki_category_system,
            entries = listOf(
                WikiEntry(
                    R.string.wiki_internet_title,
                    R.string.wiki_internet_body,
                    permissions = setOf("INTERNET"),
                ),
                WikiEntry(
                    R.string.wiki_install_packages_title,
                    R.string.wiki_install_packages_body,
                    permissions = setOf("REQUEST_INSTALL_PACKAGES"),
                ),
                WikiEntry(
                    R.string.wiki_write_settings_title,
                    R.string.wiki_write_settings_body,
                    permissions = setOf("WRITE_SETTINGS", "WRITE_SECURE_SETTINGS"),
                ),
                WikiEntry(
                    R.string.wiki_post_notifications_title,
                    R.string.wiki_post_notifications_body,
                    permissions = setOf("POST_NOTIFICATIONS"),
                ),
                WikiEntry(
                    R.string.wiki_boot_title,
                    R.string.wiki_boot_body,
                    permissions = setOf("RECEIVE_BOOT_COMPLETED"),
                ),
                WikiEntry(
                    R.string.wiki_foreground_service_title,
                    R.string.wiki_foreground_service_body,
                    permissions = setOf("FOREGROUND_SERVICE"),
                ),
                WikiEntry(
                    R.string.wiki_wake_lock_title,
                    R.string.wiki_wake_lock_body,
                    permissions = setOf("WAKE_LOCK"),
                ),
                WikiEntry(
                    R.string.wiki_battery_opt_title,
                    R.string.wiki_battery_opt_body,
                    permissions = setOf("REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"),
                ),
                WikiEntry(
                    R.string.wiki_exact_alarm_title,
                    R.string.wiki_exact_alarm_body,
                    permissions = setOf("SCHEDULE_EXACT_ALARM", "USE_EXACT_ALARM"),
                ),
                WikiEntry(
                    R.string.wiki_full_screen_intent_title,
                    R.string.wiki_full_screen_intent_body,
                    permissions = setOf("USE_FULL_SCREEN_INTENT"),
                ),
            ),
        ),
    )

    private val byShortName: Map<String, WikiEntry> by lazy {
        buildMap {
            for (category in categories) {
                for (entry in category.entries) {
                    for (perm in entry.permissions) {
                        put(perm, entry)
                    }
                }
            }
        }
    }

    fun findEntry(permission: String): WikiEntry? {
        val shortName = displayPermissionName(permission)
        byShortName[shortName]?.let { return it }
        if (shortName.startsWith("FOREGROUND_SERVICE_")) {
            return byShortName["FOREGROUND_SERVICE"]
        }
        return null
    }

    fun resolveDetails(context: Context, permission: String): ResolvedPermissionDetails {
        val pm = context.packageManager
        val info = runCatching { pm.getPermissionInfo(permission, 0) }.getOrNull()
        val label = info?.let { runCatching { it.loadLabel(pm).toString() }.getOrNull() }
            ?.takeIf { it.isNotBlank() && it != permission }
        val description = info?.let { runCatching { it.loadDescription(pm)?.toString() }.getOrNull() }
            ?.takeIf { it.isNotBlank() }
        val protection = info?.let { mapProtection(PermissionInfoCompat.getProtection(it)) }
            ?: PermissionProtection.OTHER

        return ResolvedPermissionDetails(
            fullName = permission,
            wikiEntry = findEntry(permission),
            systemLabel = label,
            systemDescription = description,
            protection = protection,
        )
    }

    private fun mapProtection(baseProtection: Int): PermissionProtection = when (baseProtection) {
        PermissionInfo.PROTECTION_DANGEROUS -> PermissionProtection.DANGEROUS
        PermissionInfo.PROTECTION_NORMAL -> PermissionProtection.NORMAL
        PermissionInfo.PROTECTION_SIGNATURE -> PermissionProtection.SIGNATURE
        else -> PermissionProtection.OTHER
    }
}
