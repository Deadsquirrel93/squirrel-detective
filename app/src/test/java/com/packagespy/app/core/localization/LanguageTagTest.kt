package com.packagespy.app.core.localization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguageTagTest {

    @Test
    fun `empty string returns null`() {
        assertNull(normalizeLanguageTag(""))
    }

    @Test
    fun `ru returns ru`() {
        assertEquals("ru", normalizeLanguageTag("ru"))
    }

    @Test
    fun `ru-RU returns ru`() {
        assertEquals("ru", normalizeLanguageTag("ru-RU"))
    }

    @Test
    fun `en_US returns en`() {
        assertEquals("en", normalizeLanguageTag("en_US"))
    }

    @Test
    fun `en,ru returns en, only the first tag is considered`() {
        assertEquals("en", normalizeLanguageTag("en,ru"))
    }

    @Test
    fun `RU is normalized to lowercase ru`() {
        assertEquals("ru", normalizeLanguageTag("RU"))
    }

    @Test
    fun `de returns null, unsupported language`() {
        assertNull(normalizeLanguageTag("de"))
    }

    @Test
    fun `de-DE,ru returns null, only the first tag is considered`() {
        assertNull(normalizeLanguageTag("de-DE,ru"))
    }
}
