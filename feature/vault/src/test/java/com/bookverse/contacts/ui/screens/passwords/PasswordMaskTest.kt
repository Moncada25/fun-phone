package com.bookverse.contacts.ui.screens.passwords

import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordMaskTest {
    @Test
    fun shouldUseLargeDot_whenMaskingEachCharacter() {
        assertEquals("●●●●●●", maskPasswordValue("secret"))
    }

    @Test
    fun shouldPreserveCodeUnitLength_whenMaskingUnicodeValue() {
        val value = "a🔐ñ"

        assertEquals(value.length, maskPasswordValue(value).length)
    }

    @Test
    fun shouldProduceEmptyMask_whenValueIsEmpty() {
        assertEquals("", maskPasswordValue(""))
    }
}
