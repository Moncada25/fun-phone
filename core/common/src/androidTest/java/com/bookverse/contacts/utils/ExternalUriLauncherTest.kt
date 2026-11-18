package com.bookverse.contacts.utils

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExternalUriLauncherTest {
    @Test
    fun shouldBuildViewIntentForAppDeepLinkIntentUri() {
        val prepared = ExternalUriLauncher.prepare(
            "intent://auth/login?code=abc#Intent;scheme=crunchyroll;package=com.crunchyroll.crunchyroid;end",
        )

        assertNotNull(prepared)
        assertEquals(Intent.ACTION_VIEW, prepared?.intent?.action)
        assertEquals("crunchyroll", prepared?.intent?.data?.scheme)
        assertEquals("auth", prepared?.intent?.data?.host)
        assertEquals("/login", prepared?.intent?.data?.path)
        assertEquals("abc", prepared?.intent?.data?.getQueryParameter("code"))
        assertEquals("com.crunchyroll.crunchyroid", prepared?.intent?.`package`)
    }

    @Test
    fun shouldRejectNonViewIntentUri() {
        val prepared = ExternalUriLauncher.prepare(
            "intent://share#Intent;scheme=crunchyroll;action=android.intent.action.SEND;end",
        )

        assertNull(prepared)
    }
}
