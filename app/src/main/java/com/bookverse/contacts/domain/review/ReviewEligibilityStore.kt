package com.bookverse.contacts.domain.review

import android.content.Context
import android.telecom.TelecomManager
import androidx.core.content.edit
import com.bookverse.contacts.BuildConfig
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ReviewEligibilityStore {
    private const val PREFS_NAME = "in_app_review"
    private const val COMPLETED_CALL_DAYS_KEY = "completed_call_days"
    private const val LAST_PROMPT_AT_KEY = "last_prompt_at"
    private const val LAST_PROMPT_VERSION_KEY = "last_prompt_version"
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun recordConnectedCall(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val completedDays = prefs.getStringSet(COMPLETED_CALL_DAYS_KEY, emptySet())
            .orEmpty()
            .toMutableSet()
        val localDate = Instant.ofEpochMilli(nowMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(dateFormatter)
        completedDays += localDate
        prefs.edit { putStringSet(COMPLETED_CALL_DAYS_KEY, completedDays) }
    }

    fun isEligible(context: Context, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val snapshot = ReviewEligibilitySnapshot(
            completedCallDays = prefs.getStringSet(COMPLETED_CALL_DAYS_KEY, emptySet()).orEmpty().size,
            lastPromptAtMillis = prefs.getLong(LAST_PROMPT_AT_KEY, 0L),
            nowMillis = nowMillis,
            isDefaultDialer = telecomManager.defaultDialerPackage == context.packageName,
        )
        return ReviewEligibilityPolicy.isEligible(snapshot)
    }

    fun markPromptRequested(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putLong(LAST_PROMPT_AT_KEY, nowMillis)
            putLong(LAST_PROMPT_VERSION_KEY, BuildConfig.VERSION_CODE.toLong())
        }
    }
}
