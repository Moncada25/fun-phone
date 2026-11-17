package com.bookverse.contacts.settings

import android.app.ActivityManager
import android.content.Context
import android.os.Build

enum class GlassLevel {
    NONE,
    BLUR,
    FULL,
}

enum class GlassEffects {
    AUTO,
    ON,
    OFF,
    ;

    fun level(sdkInt: Int, mediaPerformanceClass: Int, totalMemoryBytes: Long): GlassLevel = when {
        this == OFF || sdkInt < Build.VERSION_CODES.S -> GlassLevel.NONE
        this == ON -> GlassLevel.FULL
        mediaPerformanceClass >= Build.VERSION_CODES.S -> GlassLevel.FULL
        totalMemoryBytes >= MID_TIER_MIN_MEMORY_BYTES -> GlassLevel.BLUR
        else -> GlassLevel.NONE
    }

    fun levelOnDevice(context: Context): GlassLevel =
        level(Build.VERSION.SDK_INT, deviceMediaPerformanceClass(), deviceTotalMemory(context))

    companion object {
        const val MID_TIER_MIN_MEMORY_BYTES = 5_000_000_000L

        fun fromName(name: String?): GlassEffects = entries.firstOrNull { it.name == name } ?: AUTO

        private fun deviceMediaPerformanceClass(): Int =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.VERSION.MEDIA_PERFORMANCE_CLASS else 0

        private fun deviceTotalMemory(context: Context): Long {
            val activityManager = context.getSystemService(ActivityManager::class.java) ?: return 0L
            return ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo).totalMem
        }
    }
}
