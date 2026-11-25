package com.packagespy.app.data.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GrantedPermissionsTest {

    private val grantedBit = 2

    @Test
    fun `extracts only permissions with granted flag set`() {
        val perms = arrayOf("android.permission.CAMERA", "android.permission.INTERNET")
        val flags = intArrayOf(0, grantedBit)

        assertEquals(
            setOf("android.permission.INTERNET"),
            extractGrantedPermissions(perms, flags, grantedFlag = grantedBit),
        )
    }

    @Test
    fun `null flags falls back to all declared permissions`() {
        val perms = arrayOf("android.permission.CAMERA", "android.permission.INTERNET")

        assertEquals(
            setOf("android.permission.CAMERA", "android.permission.INTERNET"),
            extractGrantedPermissions(perms, null, grantedFlag = grantedBit),
        )
    }

    @Test
    fun `null or empty permissions returns empty set`() {
        assertTrue(extractGrantedPermissions(null, null, grantedFlag = grantedBit).isEmpty())
        assertTrue(extractGrantedPermissions(emptyArray(), intArrayOf(), grantedFlag = grantedBit).isEmpty())
    }

    @Test
    fun `shorter flags array does not throw`() {
        val perms = arrayOf("a", "b")
        val flags = intArrayOf(grantedBit)

        assertEquals(setOf("a"), extractGrantedPermissions(perms, flags, grantedFlag = grantedBit))
    }
}
