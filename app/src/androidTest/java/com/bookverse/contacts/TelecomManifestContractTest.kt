package com.bookverse.contacts

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.telecom.TelecomManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.service.FunPhoneInCallService
import com.bookverse.contacts.service.CallNotificationChannels
import com.bookverse.contacts.service.CallNotificationManager
import com.bookverse.contacts.service.IncomingCallService
import com.bookverse.contacts.service.OngoingCallService
import com.bookverse.contacts.service.ongoingCallContentIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TelecomManifestContractTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val packageManager = context.packageManager

    @Test
    fun packagedAppKeepsRequiredTelecomPermissions() {
        @Suppress("DEPRECATION")
        val packageInfo = packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val permissions = packageInfo.requestedPermissions.orEmpty().toSet()

        assertTrue(Manifest.permission.ANSWER_PHONE_CALLS in permissions)
        assertTrue(Manifest.permission.CALL_PHONE in permissions)
        assertTrue(Manifest.permission.MANAGE_OWN_CALLS in permissions)
        assertTrue(Manifest.permission.READ_CALL_LOG in permissions)
        assertTrue(Manifest.permission.READ_PHONE_STATE in permissions)
        assertTrue(Manifest.permission.USE_FULL_SCREEN_INTENT in permissions)
    }

    @Test
    fun packagedInCallServiceKeepsSystemBindingContract() {
        @Suppress("DEPRECATION")
        val serviceInfo = packageManager.getServiceInfo(
            ComponentName(context, FunPhoneInCallService::class.java),
            PackageManager.GET_META_DATA,
        )

        assertTrue(serviceInfo.exported)
        assertEquals(Manifest.permission.BIND_INCALL_SERVICE, serviceInfo.permission)
        assertTrue(serviceInfo.metaData.getBoolean(TelecomManager.METADATA_IN_CALL_SERVICE_UI))
        assertTrue(serviceInfo.metaData.getBoolean(TelecomManager.METADATA_IN_CALL_SERVICE_RINGING))
    }

    @Test
    fun packagedCallForegroundServicesKeepPhoneCallType() {
        listOf(IncomingCallService::class.java, OngoingCallService::class.java).forEach { serviceClass ->
            @Suppress("DEPRECATION")
            val serviceInfo = packageManager.getServiceInfo(
                ComponentName(context, serviceClass),
                0,
            )

            assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL, serviceInfo.foregroundServiceType)
        }
    }

    @Test
    fun callChannelsAreCreatedAndNotBlockedByDefault() {
        CallNotificationManager.ensureCallChannels(context)
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        assertTrue(
            notificationManager.getNotificationChannel(CallNotificationChannels.INCOMING_CHANNEL_ID)
                .importance > NotificationManager.IMPORTANCE_NONE,
        )
        assertTrue(
            notificationManager.getNotificationChannel(CallNotificationChannels.ONGOING_CHANNEL_ID)
                .importance > NotificationManager.IMPORTANCE_NONE,
        )
    }

    @Test
    fun ongoingNotificationReturnsToExactActiveCall() {
        val intent = ongoingCallContentIntent(context, "call-239", "+573001112233")

        assertEquals(ComponentName(context, MainActivity::class.java), intent.component)
        assertTrue(intent.getBooleanExtra("open_incall", false))
        assertEquals("call-239", intent.getStringExtra("incall_call_id"))
        assertEquals("+573001112233", intent.getStringExtra("incall_number"))
    }
}
