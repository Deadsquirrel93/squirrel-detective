package com.packagespy.app.data.scanner

import com.packagespy.app.domain.model.ActiveCapability
import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveComponentsTest {

    private val active = ActiveComponents(
        accessibility = setOf("com.example.accessibility"),
        deviceAdmins = setOf("com.example.admin"),
        inputMethods = setOf("com.example.ime"),
        notificationListeners = setOf("com.example.listener"),
    )

    @Test
    fun `package in no set yields empty capabilities`() {
        assertEquals(emptySet<ActiveCapability>(), activeCapabilitiesOf("com.example.other", active))
    }

    @Test
    fun `package in accessibility set yields ACCESSIBILITY`() {
        assertEquals(
            setOf(ActiveCapability.ACCESSIBILITY),
            activeCapabilitiesOf("com.example.accessibility", active),
        )
    }

    @Test
    fun `package in device admins set yields DEVICE_ADMIN`() {
        assertEquals(
            setOf(ActiveCapability.DEVICE_ADMIN),
            activeCapabilitiesOf("com.example.admin", active),
        )
    }

    @Test
    fun `package in input methods set yields INPUT_METHOD`() {
        assertEquals(
            setOf(ActiveCapability.INPUT_METHOD),
            activeCapabilitiesOf("com.example.ime", active),
        )
    }

    @Test
    fun `package in notification listeners set yields NOTIFICATION_LISTENER`() {
        assertEquals(
            setOf(ActiveCapability.NOTIFICATION_LISTENER),
            activeCapabilitiesOf("com.example.listener", active),
        )
    }

    @Test
    fun `package in all sets yields all four capabilities`() {
        val everywhere = "com.example.everywhere"
        val allActive = ActiveComponents(
            accessibility = setOf(everywhere),
            deviceAdmins = setOf(everywhere),
            inputMethods = setOf(everywhere),
            notificationListeners = setOf(everywhere),
        )
        assertEquals(
            setOf(
                ActiveCapability.ACCESSIBILITY,
                ActiveCapability.DEVICE_ADMIN,
                ActiveCapability.INPUT_METHOD,
                ActiveCapability.NOTIFICATION_LISTENER,
            ),
            activeCapabilitiesOf(everywhere, allActive),
        )
    }

    @Test
    fun `ActiveComponents NONE yields empty capabilities for any package`() {
        assertEquals(
            emptySet<ActiveCapability>(),
            activeCapabilitiesOf("com.example.anything", ActiveComponents.NONE),
        )
    }

    @Test
    fun `other package in the sets does not affect an unrelated package`() {
        assertEquals(emptySet<ActiveCapability>(), activeCapabilitiesOf("com.example.unrelated", active))
    }
}
