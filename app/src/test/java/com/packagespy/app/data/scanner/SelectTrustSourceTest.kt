package com.packagespy.app.data.scanner

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SelectTrustSourceTest {

    @Test
    fun `API 30 returns initiating package`() {
        assertEquals(
            "com.android.vending",
            selectTrustSource(Build.VERSION_CODES.R, "com.android.vending", "com.android.vending"),
        )
    }

    @Test
    fun `API 36 returns initiating package`() {
        assertEquals(
            "com.android.vending",
            selectTrustSource(36, "com.android.vending", "com.android.vending"),
        )
    }

    @Test
    fun `API 30 with null initiating does not fall back to installing`() {
        assertNull(
            selectTrustSource(Build.VERSION_CODES.R, null, "com.android.vending"),
        )
    }

    @Test
    fun `API 29 returns installing package`() {
        assertEquals(
            "com.android.vending",
            selectTrustSource(29, null, "com.android.vending"),
        )
    }

    @Test
    fun `API 26 with both null returns null`() {
        assertNull(selectTrustSource(26, null, null))
    }
}
