package com.bookverse.contacts.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountriesTest {
    private fun detectedIso(number: String): String? =
        Countries.detectCountryFromPhoneNumber(number)?.countryCode

    @Test
    fun northAmericanNumbersResolveTheirCountryFromTheAreaCode() {
        assertEquals("US", detectedIso("+12125550100"))
        assertEquals("US", detectedIso("+1 (415) 555-0100"))
        assertEquals("CA", detectedIso("+14165550100"))
        assertEquals("CA", detectedIso("+1 604 555 0100"))
        assertEquals("CA", detectedIso("+1 (514) 555-0100"))
        assertEquals("DO", detectedIso("+18095550100"))
        assertEquals("PR", detectedIso("+17875550100"))
        assertEquals("JM", detectedIso("+16585550100"))
    }

    @Test
    fun caribbeanDialCodesStillWinOverTheSharedNorthAmericanCode() {
        assertEquals("JM", detectedIso("+18765550100"))
        assertEquals("BS", detectedIso("+12425550100"))
        assertEquals("TT", detectedIso("+18685550100"))
    }

    @Test
    fun ambiguousNorthAmericanNumbersDefaultToTheUnitedStates() {
        assertEquals("US", detectedIso("+1"))
        assertEquals("US", detectedIso("+14"))
        assertEquals("US", detectedIso("+18005550100"))
        assertEquals("US", detectedIso("+15005550100"))
    }

    @Test
    fun otherSharedDialCodesResolveToTheirMainCountry() {
        assertEquals("GB", detectedIso("+442071234567"))
        assertEquals("RU", detectedIso("+74951234567"))
        assertEquals("KZ", detectedIso("+77011234567"))
        assertEquals("FI", detectedIso("+358401234567"))
        assertEquals("AX", detectedIso("+358181234567"))
        assertEquals("NO", detectedIso("+4722123456"))
        assertEquals("AU", detectedIso("+61212345678"))
        assertEquals("RE", detectedIso("+262262123456"))
        assertEquals("CW", detectedIso("+5999123456"))
        assertEquals("BQ", detectedIso("+5997123456"))
        assertEquals("NF", detectedIso("+672321234"))
    }

    @Test
    fun uniqueDialCodesKeepTheirCountry() {
        assertEquals("CO", detectedIso("+573001234567"))
        assertEquals("MX", detectedIso("+525512345678"))
        assertEquals("ES", detectedIso("+34612345678"))
    }

    @Test
    fun numbersWithoutInternationalPrefixAreNotDetected() {
        assertNull(Countries.detectCountryFromPhoneNumber("4165550100"))
        assertNull(Countries.detectCountryFromPhoneNumber(""))
    }

    @Test
    fun aDialCodeAndNationalNumberResolveTheSameCountryAsTheFullNumber() {
        assertEquals("CA", Countries.findCountryForDialCode("+1", "(416) 555-0100")?.countryCode)
        assertEquals("US", Countries.findCountryForDialCode("+1", "212 555 0100")?.countryCode)
        assertEquals("US", Countries.findCountryForDialCode("+1")?.countryCode)
        assertEquals("GB", Countries.findCountryForDialCode("+44")?.countryCode)
        assertEquals("CO", Countries.findCountryForDialCode("+57", "3001234567")?.countryCode)
        assertNull(Countries.findCountryForDialCode("+999"))
    }
}
