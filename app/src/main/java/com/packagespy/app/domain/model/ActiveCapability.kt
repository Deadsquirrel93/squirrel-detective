package com.packagespy.app.domain.model

/**
 * A capability an app currently has turned on in system settings (accessibility
 * service enabled, device admin activated, notification listener granted, or
 * keyboard enabled). This reflects live system state at scan time and is not
 * persisted — unlike a declared component, it can flip off between scans
 * without the app itself changing.
 */
enum class ActiveCapability {
    ACCESSIBILITY,
    DEVICE_ADMIN,
    NOTIFICATION_LISTENER,
    INPUT_METHOD,
}
