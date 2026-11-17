package com.bookverse.contacts

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.os.StrictMode
import android.os.strictmode.Violation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.service.CallNotificationManager
import com.bookverse.contacts.widget.WidgetSync
import com.bookverse.contacts.widget.WidgetUpdateHelper
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RuntimeIoStrictModeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun missedCallEntryDoesNotPerformDiskIoOnCallerThread() {
        val number = "+15550000000"
        val notificationManager =
            context.getSystemService(NotificationManager::class.java)
        instrumentation.uiAutomation.adoptShellPermissionIdentity(
            Manifest.permission.POST_NOTIFICATIONS,
        )
        try {
            assertNoCallerThreadDiskIo {
                CallNotificationManager.showMissed(context, number)
            }
            assertTrue(
                "Immediate missed-call fallback notification was not posted",
                waitForActiveNotificationText(notificationManager, number),
            )
        } finally {
            notificationManager.cancelAll()
            instrumentation.uiAutomation.dropShellPermissionIdentity()
        }
    }

    @Test
    fun widgetRefreshDoesNotPerformDiskIoOnCallerThread() {
        assertNoCallerThreadDiskIo {
            WidgetSync.start(context)
            WidgetUpdateHelper.updateAllWidgets(context)
        }
    }

    private fun assertNoCallerThreadDiskIo(block: () -> Unit) {
        val violations = CopyOnWriteArrayList<Violation>()
        val originalPolicy = StrictMode.getThreadPolicy()
        val directExecutor = Executor { command -> command.run() }
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder(originalPolicy)
                .detectDiskReads()
                .detectDiskWrites()
                .penaltyListener(directExecutor) { violation -> violations += violation }
                .build()
        )
        try {
            block()
        } finally {
            StrictMode.setThreadPolicy(originalPolicy)
        }
        assertTrue("Unexpected caller-thread disk I/O: $violations", violations.isEmpty())
    }

    private fun waitForActiveNotificationText(
        notificationManager: NotificationManager,
        expectedText: String,
    ): Boolean {
        repeat(20) {
            val posted = notificationManager.activeNotifications.any { notification ->
                notification.notification.extras
                    .getCharSequence(Notification.EXTRA_TEXT)
                    ?.toString() == expectedText
            }
            if (posted) return true
            Thread.sleep(50)
        }
        return false
    }
}
