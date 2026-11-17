package com.bookverse.contacts.security

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.bookverse.contacts.settings.AppDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

object AppLockSettings {
    private val enabledKey = booleanPreferencesKey("global_app_lock")

    fun enabled(context: Context): Flow<Boolean> =
        AppDataStore.getDataStore(context.applicationContext).data
            .map { it[enabledKey] ?: false }
            .distinctUntilChanged()

    suspend fun setEnabled(context: Context, enabled: Boolean) {
        AppDataStore.getDataStore(context.applicationContext).edit { it[enabledKey] = enabled }
    }
}

object AppLockSession {
    private val _locked = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()
    private val ownResult = AppLockResultTracker()

    fun lock() {
        _locked.value = true
    }

    fun unlock() {
        _locked.value = false
    }

    fun onOwnActivityLaunchedForResult() {
        ownResult.onLaunched()
    }

    fun onStopped(nowMillis: Long) {
        if (ownResult.shouldLockOnStop(nowMillis)) lock()
    }

    fun onOwnActivityResult(nowMillis: Long) {
        if (ownResult.shouldLockOnResult(nowMillis)) lock()
    }
}

class AppLockResultTracker(private val graceMillis: Long = DEFAULT_GRACE_MILLIS) {
    private var launchPending = false
    private var awaitingSince: Long? = null

    fun onLaunched() {
        launchPending = true
    }

    fun shouldLockOnStop(nowMillis: Long): Boolean {
        if (launchPending) {
            launchPending = false
            awaitingSince = nowMillis
            return false
        }
        awaitingSince = null
        return true
    }

    fun shouldLockOnResult(nowMillis: Long): Boolean {
        val since = awaitingSince
        launchPending = false
        awaitingSince = null
        return since != null && nowMillis - since > graceMillis
    }

    companion object {
        const val DEFAULT_GRACE_MILLIS = 3 * 60 * 1000L
    }
}

enum class AppLockSurface { NONE, COVER, LOCK }

object AppLockPolicy {
    fun surface(
        enabled: Boolean?,
        locked: Boolean,
        onCallScreen: Boolean,
        callActive: Boolean,
        callGraceElapsed: Boolean,
    ): AppLockSurface = when {
        onCallScreen -> AppLockSurface.NONE
        callActive && !callGraceElapsed -> AppLockSurface.NONE
        enabled == null -> AppLockSurface.COVER
        enabled && locked -> AppLockSurface.LOCK
        else -> AppLockSurface.NONE
    }
}
