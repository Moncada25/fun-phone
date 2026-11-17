package com.bookverse.contacts.integration

import com.bookverse.contacts.domain.call.CallPhoneType
import contacts.core.entities.PhoneEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactsCallContactLookupTest {
    @Test
    fun contactPhoneTypesMapToDialerOwnedTypes() {
        assertEquals(CallPhoneType.MOBILE, PhoneEntity.Type.MOBILE.toCallPhoneType())
        assertEquals(CallPhoneType.WORK, PhoneEntity.Type.WORK.toCallPhoneType())
        assertEquals(CallPhoneType.HOME, PhoneEntity.Type.HOME.toCallPhoneType())
        assertEquals(CallPhoneType.MAIN, PhoneEntity.Type.MAIN.toCallPhoneType())
        assertEquals(CallPhoneType.OTHER, null.toCallPhoneType())
    }
}
