package com.bookverse.contacts.ui.components

import android.graphics.Bitmap
import android.net.Uri
import com.bookverse.contacts.domain.call.AudioRoute
import com.bookverse.contacts.domain.call.CallContactLookup
import com.bookverse.contacts.domain.call.CallContactPresentation
import com.bookverse.contacts.domain.call.CallState
import com.bookverse.contacts.domain.call.UiCall
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CallStateBannerTest {
    @Test
    fun shouldUseKnownContactNameForCallBanner() = runTest {
        val call = call(displayName = "Incoming number")
        val lookup = FakeCallContactLookup(
            CallContactPresentation(
                name = "Ada Lovelace",
                photoUri = null,
                phones = emptyList(),
            ),
        )

        assertEquals("Ada Lovelace", resolveCallDisplayName(call, lookup))
    }

    @Test
    fun shouldUseCallFallbackForUnknownCallBannerContact() = runTest {
        val call = call(displayName = "Incoming number")

        assertEquals(
            "Incoming number",
            resolveCallDisplayName(call, FakeCallContactLookup(null)),
        )
    }

    private fun call(displayName: String?) = UiCall(
        id = "call-1",
        number = "+573001112233",
        displayName = displayName,
        state = CallState.ACTIVE,
        audioRoute = AudioRoute.EARPIECE,
    )

    private class FakeCallContactLookup(
        private val contact: CallContactPresentation?,
    ) : CallContactLookup {
        override suspend fun findByPhoneNumber(phoneNumber: String): CallContactPresentation? = contact

        override suspend fun loadPhoto(uri: Uri): Bitmap? = null
    }
}
