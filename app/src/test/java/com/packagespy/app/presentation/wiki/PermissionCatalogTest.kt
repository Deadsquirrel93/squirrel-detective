package com.packagespy.app.presentation.wiki

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PermissionCatalogTest {

    @Test
    fun `finds entry by full and short android permission name`() {
        assertNotNull(PermissionCatalog.findEntry("android.permission.CAMERA"))
        assertNotNull(PermissionCatalog.findEntry("CAMERA"))
        assertNotNull(PermissionCatalog.findEntry("android.permission.READ_MEDIA_IMAGES"))
    }

    @Test
    fun `maps typed FOREGROUND_SERVICE variants to base entry`() {
        assertNotNull(PermissionCatalog.findEntry("android.permission.FOREGROUND_SERVICE_LOCATION"))
    }

    @Test
    fun `returns null for unknown vendor permission`() {
        assertNull(PermissionCatalog.findEntry("com.vendor.permission.SECRET_THING"))
    }
}
