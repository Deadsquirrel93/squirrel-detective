package com.packagespy.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionNamesTest {

    @Test
    fun `strips the android permission prefix`() {
        assertEquals("CAMERA", displayPermissionName("android.permission.CAMERA"))
    }

    @Test
    fun `leaves third-party permissions unchanged`() {
        assertEquals(
            "com.google.android.c2dm.permission.RECEIVE",
            displayPermissionName("com.google.android.c2dm.permission.RECEIVE"),
        )
    }

    @Test
    fun `leaves a string without the prefix unchanged`() {
        assertEquals("CAMERA", displayPermissionName("CAMERA"))
    }

    @Test
    fun `does not strip the prefix when it is not at the start`() {
        assertEquals(
            "x.android.permission.Y",
            displayPermissionName("x.android.permission.Y"),
        )
    }
}
