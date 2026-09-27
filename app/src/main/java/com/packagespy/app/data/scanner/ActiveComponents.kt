package com.packagespy.app.data.scanner

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import androidx.core.app.NotificationManagerCompat
import com.packagespy.app.domain.model.ActiveCapability
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Package names the system currently reports as having a component
 * *enabled* (as opposed to merely declared in a manifest): an accessibility
 * service the user turned on, an active device admin, an enabled input
 * method (keyboard), or a package with granted notification listener
 * access. Read once per scan via [ActiveComponentsReader].
 */
data class ActiveComponents(
    val accessibility: Set<String>,
    val deviceAdmins: Set<String>,
    val inputMethods: Set<String>,
    val notificationListeners: Set<String>,
) {
    companion object {
        val NONE = ActiveComponents(
            accessibility = emptySet(),
            deviceAdmins = emptySet(),
            inputMethods = emptySet(),
            notificationListeners = emptySet(),
        )
    }
}

/**
 * Pure lookup from [packageName] to the [ActiveCapability]s the system
 * currently reports as enabled for it, based on [active].
 */
fun activeCapabilitiesOf(packageName: String, active: ActiveComponents): Set<ActiveCapability> {
    val capabilities = mutableSetOf<ActiveCapability>()
    if (packageName in active.accessibility) capabilities += ActiveCapability.ACCESSIBILITY
    if (packageName in active.deviceAdmins) capabilities += ActiveCapability.DEVICE_ADMIN
    if (packageName in active.inputMethods) capabilities += ActiveCapability.INPUT_METHOD
    if (packageName in active.notificationListeners) capabilities += ActiveCapability.NOTIFICATION_LISTENER
    return capabilities
}

/**
 * Reads which packages currently have an enabled accessibility service, an
 * active device admin, an enabled input method, or granted notification
 * listener access. Each of the four sources is read independently: a
 * missing system service or a failure reading one source falls back to an
 * empty set and never affects the others or aborts the scan.
 */
@Singleton
class ActiveComponentsReader @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun read(): ActiveComponents {
        return ActiveComponents(
            accessibility = readAccessibility(),
            deviceAdmins = readDeviceAdmins(),
            inputMethods = readInputMethods(),
            notificationListeners = readNotificationListeners(),
        )
    }

    private fun readAccessibility(): Set<String> = runCatching {
        val manager = context.getSystemService(AccessibilityManager::class.java)
            ?: return@runCatching emptySet()
        manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .mapNotNull { it.resolveInfo?.serviceInfo?.packageName }
            .toSet()
    }.getOrDefault(emptySet())

    private fun readDeviceAdmins(): Set<String> = runCatching {
        val manager = context.getSystemService(DevicePolicyManager::class.java)
            ?: return@runCatching emptySet()
        manager.activeAdmins.orEmpty().map { it.packageName }.toSet()
    }.getOrDefault(emptySet())

    private fun readInputMethods(): Set<String> = runCatching {
        val manager = context.getSystemService(InputMethodManager::class.java)
            ?: return@runCatching emptySet()
        manager.enabledInputMethodList.map { it.packageName }.toSet()
    }.getOrDefault(emptySet())

    private fun readNotificationListeners(): Set<String> = runCatching {
        NotificationManagerCompat.getEnabledListenerPackages(context)
    }.getOrDefault(emptySet())
}
