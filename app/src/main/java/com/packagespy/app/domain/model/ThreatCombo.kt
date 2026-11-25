package com.packagespy.app.domain.model

import androidx.annotation.StringRes

enum class ComboId {
    BANKING_TROJAN_2FA,
    SILENT_DROPPER,
    STALKERWARE_SURVEILLANCE,
    UNINSTALL_LOCK,
    DEVICE_PROFILING,
}

/**
 * High-level attack/privacy scenario formed when multiple findings combine
 * on the same app (e.g. overlay + SMS/accessibility + internet = 2FA hijack).
 */
data class ThreatCombo(
    val id: ComboId,
    @StringRes val titleRes: Int,
    @StringRes val summaryRes: Int,
    val severity: RiskLevel,
)
