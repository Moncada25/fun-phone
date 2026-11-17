package com.bookverse.contacts.ui.viewmodel

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

internal class ContactsProviderObserver(
    private val contentResolver: ContentResolver,
    private val debounceMs: Long = DEFAULT_DEBOUNCE_MS,
) {
    private val changes = Channel<Unit>(Channel.CONFLATED)
    private val registered = AtomicBoolean(false)
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            changes.trySend(Unit)
        }
    }

    fun start(scope: CoroutineScope, onChange: suspend () -> Unit) {
        scope.launch(Dispatchers.IO) {
            for (signal in changes) {
                delay(debounceMs.milliseconds)
                changes.tryReceive()
                runCatching { onChange() }
            }
        }
        register()
    }

    fun register() {
        if (!registered.compareAndSet(false, true)) return
        runCatching {
            contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                observer,
            )
        }.onFailure { registered.set(false) }
    }

    fun close() {
        if (registered.getAndSet(false)) {
            runCatching { contentResolver.unregisterContentObserver(observer) }
        }
        changes.close()
    }

    private companion object {
        const val DEFAULT_DEBOUNCE_MS = 1500L
    }
}
