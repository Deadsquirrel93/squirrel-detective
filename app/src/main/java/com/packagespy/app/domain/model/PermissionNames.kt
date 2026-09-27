package com.packagespy.app.domain.model

private const val ANDROID_PERMISSION_PREFIX = "android.permission."

/**
 * Strips the noisy `android.permission.` prefix from a permission name for
 * display purposes. Third-party permissions (which don't share that prefix)
 * are returned unchanged.
 */
fun displayPermissionName(permission: String): String =
    permission.removePrefix(ANDROID_PERMISSION_PREFIX)
