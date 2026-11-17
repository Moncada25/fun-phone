package com.bookverse.contacts.service

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import java.util.Collections

object MissedCallSuppressor {
    private var appContext: Context? = null
    private var registered = false
    private val processedIds = Collections.synchronizedSet(mutableSetOf<Long>())
    private val handler = Handler(Looper.getMainLooper())
    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            suppress()
        }
    }

    fun start(context: Context) {
        if (registered) return
        appContext = context.applicationContext
        try {
            context.contentResolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, observer)
            registered = true
            suppress()
        } catch (_: SecurityException) {
            DiagnosticLog.error(DiagnosticEvent.CALL_LOG_PERMISSION_DENIED)
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.CALL_LOG_OBSERVER_REGISTER_FAILED)
        }
    }

    fun stop() {
        if (!registered) return
        try {
            appContext?.contentResolver?.unregisterContentObserver(observer)
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.CALL_LOG_OBSERVER_UNREGISTER_FAILED)
        }
        registered = false
        appContext = null
    }

    private fun suppress() {
        val ctx = appContext ?: return
        try {
            val projection = arrayOf(CallLog.Calls._ID, CallLog.Calls.TYPE, CallLog.Calls.NEW, CallLog.Calls.DATE)
            val selection = CallLog.Calls.TYPE + "=? AND " + CallLog.Calls.NEW + "=1"
            val args = arrayOf(CallLog.Calls.MISSED_TYPE.toString())
            ctx.contentResolver.query(CallLog.Calls.CONTENT_URI, projection, selection, args, CallLog.Calls.DATE + " DESC")?.use { cursor ->
                var count = 0
                while (cursor.moveToNext() && count < 10) {
                    val id = cursor.getLong(0)
                    if (processedIds.contains(id)) continue
                    val values = ContentValues().apply { put(CallLog.Calls.NEW, 0) }
                    try {
                        ctx.contentResolver.update(ContentUris.withAppendedId(CallLog.Calls.CONTENT_URI, id), values, null, null)
                        processedIds.add(id)
                    } catch (_: SecurityException) {
                        DiagnosticLog.error(DiagnosticEvent.CALL_LOG_UPDATE_PERMISSION_DENIED)
                    }
                    count++
                }
            }
        } catch (_: Exception) {
            DiagnosticLog.error(DiagnosticEvent.CALL_LOG_SUPPRESSION_FAILED)
        }
    }
}
