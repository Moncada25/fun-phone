package com.bookverse.contacts.security

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.File
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinAuthenticatorTest {
    @Test
    fun serializesAttemptsAcrossConcurrentAuthenticatorInstances() = runTest {
        val file = File.createTempFile("pin-concurrent", ".preferences_pb").apply { delete(); deleteOnExit() }
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })
        store.edit { it[stringPreferencesKey("password_pin_hash")] = "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4" }
        val attempts = (1..5).map { async { PinAuthenticator(store) { 100_000L }.verify("0000") } }.awaitAll()
        assertEquals(4, attempts.count { it == PinResult.Invalid })
        assertEquals(1, attempts.count { it == PinResult.Locked(30) })
        assertEquals(PinResult.Locked(30), PinAuthenticator(store) { 100_000L }.verify("1234"))
    }

    @Test
    fun migratesOnlyAfterSuccessfulAuthenticationAndPersistsLockout() = runTest {
        val file = File.createTempFile("pin-auth", ".preferences_pb").apply { delete(); deleteOnExit() }
        val store = PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })
        val hashKey = stringPreferencesKey("password_pin_hash")
        val legacy = "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4"
        store.edit { it[hashKey] = legacy }
        var time = 100_000L
        val authenticator = PinAuthenticator(store) { time }
        repeat(4) { assertEquals(PinResult.Invalid, authenticator.verify("0000")) }
        assertEquals(legacy, store.data.first()[hashKey])
        assertEquals(PinResult.Locked(30), authenticator.verify("0000"))
        assertEquals(PinResult.Locked(30), PinAuthenticator(store) { time }.verify("1234"))
        time = 0L
        assertEquals(PinResult.Locked(30), PinAuthenticator(store) { time }.verify("1234"))
        time += 30_001
        assertEquals(PinResult.Valid, authenticator.verify("1234"))
        assertTrue(store.data.first()[hashKey]!!.startsWith("pbkdf2-sha256:600000:"))
        authenticator.configureBiometric()
        assertEquals("BIOMETRIC", store.data.first()[stringPreferencesKey("password_auth_method")])
        assertEquals(null, store.data.first()[hashKey])
        authenticator.configure("1234")
        assertEquals("PIN", store.data.first()[stringPreferencesKey("password_auth_method")])
        assertTrue(store.data.first()[hashKey]!!.startsWith("pbkdf2-sha256:600000:"))
        authenticator.clear()
        assertEquals(null, store.data.first()[stringPreferencesKey("password_auth_method")])
        assertEquals(null, store.data.first()[hashKey])
    }
}
