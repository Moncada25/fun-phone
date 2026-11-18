package com.bookverse.contacts.security

import android.os.SystemClock
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface PinResult {
    data object Valid : PinResult
    data object Invalid : PinResult
    data class Locked(val seconds: Int) : PinResult
}

/** Serializes setup, verification and removal against durable attempt counters. */
class PinAuthenticator(
    private val store: DataStore<Preferences>,
    private val now: () -> Long = SystemClock::elapsedRealtime,
) {
    private companion object {
        // All activity/view-model instances must share the same durable attempt limit.
        val mutex = Mutex()
    }
    private val methodKey = stringPreferencesKey("password_auth_method")
    private val biometricKey = booleanPreferencesKey("password_biometric")
    private val sectionsKey = booleanPreferencesKey("sensitive_sections_lock")
    private val hashKey = stringPreferencesKey("password_pin_hash")
    private val failuresKey = intPreferencesKey("password_pin_failures")
    private val lastElapsedKey = longPreferencesKey("password_pin_last_elapsed")
    private val untilKey = longPreferencesKey("password_pin_blocked_until_elapsed")

    suspend fun configure(pin: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val encoded = PinVerifier.create(pin)
            store.edit { prefs ->
                prefs[hashKey] = encoded
                prefs[methodKey] = "PIN"
                prefs[biometricKey] = false
                prefs.remove(failuresKey)
                prefs.remove(untilKey)
                prefs.remove(lastElapsedKey)
            }
        }
    }

    suspend fun configureBiometric() = withContext(Dispatchers.IO) {
        mutex.withLock {
            store.edit { prefs ->
                prefs[methodKey] = "BIOMETRIC"
                prefs[biometricKey] = true
                prefs.remove(hashKey)
                prefs.remove(failuresKey)
                prefs.remove(untilKey)
                prefs.remove(lastElapsedKey)
            }
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            store.edit { prefs ->
                prefs.remove(hashKey)
                prefs.remove(methodKey)
                prefs[biometricKey] = false
                prefs[sectionsKey] = false
                prefs.remove(failuresKey)
                prefs.remove(untilKey)
                prefs.remove(lastElapsedKey)
            }
        }
    }

    suspend fun verify(pin: String): PinResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            val prefs = store.data.first()
            val current = now()
            var deadline = prefs[untilKey] ?: 0L
            if (deadline > 0 && current < (prefs[lastElapsedKey] ?: 0L)) {
                // elapsedRealtime resets on boot. Require a fresh short wait instead of bypassing it.
                deadline = current + 30_000L
                store.edit { it[untilKey] = deadline; it[lastElapsedKey] = current }
            }
            val remaining = deadline - current
            if (remaining > 0) {
                // Corrupt deadlines must not turn a short wait into a permanent lockout.
                if (remaining > 30_000L) store.edit { it[untilKey] = current + 30_000L }
                return@withLock PinResult.Locked(((remaining.coerceAtMost(30_000L) + 999) / 1000).toInt())
            }
            val encoded = prefs[hashKey]
            if (encoded != null && PinVerifier.verify(pin, encoded)) {
                val upgraded = if (PinVerifier.isLegacy(encoded)) PinVerifier.create(pin) else encoded
                store.edit { updated ->
                    updated[hashKey] = upgraded
                    updated.remove(failuresKey)
                    updated.remove(untilKey)
                    updated.remove(lastElapsedKey)
                }
                PinResult.Valid
            } else {
                val failures = (prefs[failuresKey] ?: 0).coerceIn(0, 4) + 1
                store.edit { updated ->
                    updated[lastElapsedKey] = current
                    updated[failuresKey] = if (failures >= 5) 0 else failures
                    if (failures >= 5) updated[untilKey] = current + 30_000L
                }
                if (failures >= 5) PinResult.Locked(30) else PinResult.Invalid
            }
        }
    }
}
