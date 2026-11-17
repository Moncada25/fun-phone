package com.bookverse.contacts

import android.app.Application
import android.os.Build
import android.os.StrictMode
import androidx.core.content.edit
import com.bookverse.contacts.data.service.ReminderBootWorker
import com.bookverse.contacts.integration.AppContainer
import com.bookverse.contacts.integration.createAppContainer
import com.bookverse.contacts.service.MissedCallSuppressor
import com.bookverse.contacts.settings.LocaleController
import java.io.File

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        LocaleController().initializeAtStartup(this)
        container = createAppContainer(this)
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }
        cleanupAppCheckStateOnUpdate()
        MissedCallSuppressor.start(this)
        ReminderBootWorker.enqueueReschedule(this)
    }

    private fun cleanupAppCheckStateOnUpdate() {
        val currentVersionCode = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, 0).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionCode.toLong()
            }
        } catch (e: Exception) {
            return
        }

        val tracker = getSharedPreferences("funphone_version_tracker", MODE_PRIVATE)
        val lastVersionCode = tracker.getLong("last_known_version_code", -1L)
        if (lastVersionCode != currentVersionCode) {
            val sharedPrefsDir = File(applicationInfo.dataDir, "shared_prefs")
            if (sharedPrefsDir.exists()) {
                sharedPrefsDir.listFiles()?.forEach { file ->
                    val shouldDelete = file.name.startsWith("com.google.firebase.appcheck.store.") ||
                        file.name == "com.google.mlkit.internal.xml" ||
                        file.name.startsWith("com.google.mlkit.") ||
                        file.name.startsWith("com.google.android.gms.internal.mlkit.")
                    if (shouldDelete) {
                        runCatching { file.delete() }
                        runCatching { File(sharedPrefsDir, "${file.name}.bak").delete() }
                    }
                }
            }
        }
        tracker.edit {
            putLong("last_known_version_code", currentVersionCode)
        }
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
