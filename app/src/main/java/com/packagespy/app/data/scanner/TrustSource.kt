package com.packagespy.app.data.scanner

import android.os.Build

/**
 * Picks the package name that should be trusted as the "install source" for
 * whitelist decisions. On API 30+, `initiatingPackageName` cannot be spoofed
 * by the app being installed (unlike `installingPackageName`, which can be
 * overridden via `adb install -i`), so it is used with no fallback. Below
 * API 30, only the (spoofable) installing package name is available.
 */
fun selectTrustSource(sdkInt: Int, initiating: String?, installing: String?): String? {
    return if (sdkInt >= Build.VERSION_CODES.R) initiating else installing
}
