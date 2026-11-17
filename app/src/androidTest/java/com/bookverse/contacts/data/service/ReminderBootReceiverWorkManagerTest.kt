package com.bookverse.contacts.data.service

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderBootReceiverWorkManagerTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val workManager = WorkManager.getInstance(context)

    @Before
    fun setUp() {
        workManager.cancelUniqueWork(ReminderBootWorker.UNIQUE_WORK)
            .result
            .get(10, TimeUnit.SECONDS)
    }

    @After
    fun cleanUp() {
        workManager.cancelUniqueWork(ReminderBootWorker.UNIQUE_WORK)
            .result
            .get(10, TimeUnit.SECONDS)
    }

    @Test
    fun shouldEnqueueReminderRecoveryAfterBoot() {
        ReminderBootReceiver().onReceive(
            context,
            Intent(Intent.ACTION_BOOT_COMPLETED),
        )

        val workInfos = workManager
            .getWorkInfosForUniqueWork(ReminderBootWorker.UNIQUE_WORK)
            .get(10, TimeUnit.SECONDS)
        assertTrue(workInfos.any { it.state != WorkInfo.State.CANCELLED })
    }
}
