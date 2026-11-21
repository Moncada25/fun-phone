package com.bookverse.contacts.domain.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactsReadPolicyTest {
    @Test
    fun skipsReadAndReturnsEmptyWhenPermissionIsMissing() {
        var readCalled = false

        val result = ContactsReadPolicy.readIfGranted(isGranted = false) {
            readCalled = true
            listOf("Ada")
        }

        assertFalse(readCalled)
        assertTrue(result.isEmpty())
    }

    @Test
    fun returnsReadResultWhenPermissionIsGranted() {
        var readCalled = false

        val result = ContactsReadPolicy.readIfGranted(isGranted = true) {
            readCalled = true
            listOf("Ada", "Grace")
        }

        assertTrue(readCalled)
        assertEquals(listOf("Ada", "Grace"), result)
    }
}
