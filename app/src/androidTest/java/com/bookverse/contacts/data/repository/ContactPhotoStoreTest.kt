package com.bookverse.contacts.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.utils.ContactPhotoFormat
import com.bookverse.contacts.utils.ContactUtils
import com.bookverse.contacts.utils.EncodedPhoto
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class ContactPhotoStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = ContactPhotoStore(context)
    private val lookupKeys = mutableSetOf<String>()

    @After
    fun tearDown() {
        lookupKeys.forEach(store::removeContactPhoto)
    }

    @Test
    fun savedPhotoOverridesProviderAndRemainsDecodableWithVersionedUri() {
        val lookupKey = uniqueLookupKey("save")
        val providerUri = Uri.parse("content://contacts/provider-photo")

        assertTrue(store.saveContactPhoto(lookupKey, jpegPhoto(Color.RED)))
        val resolved = requireNotNull(store.resolveContactPhoto(lookupKey, providerUri))
        val decoded = requireNotNull(ContactUtils.uriToBitmap(resolved, context))

        assertEquals("file", resolved.scheme)
        assertEquals(1_200, decoded.width)
        assertEquals(800, decoded.height)
        decoded.recycle()
    }

    @Test
    fun replacementChangesVersionAndUsesNewPixels() {
        val lookupKey = uniqueLookupKey("replace")
        assertTrue(store.saveContactPhoto(lookupKey, jpegPhoto(Color.RED)))
        val firstUri = requireNotNull(store.resolveContactPhoto(lookupKey, null))

        assertTrue(store.saveContactPhoto(lookupKey, jpegPhoto(Color.BLUE)))
        val secondUri = requireNotNull(store.resolveContactPhoto(lookupKey, null))
        val bitmap = requireNotNull(BitmapFactory.decodeFile(secondUri.path))

        assertNotEquals(firstUri, secondUri)
        assertTrue(Color.blue(bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)) > 240)
        bitmap.recycle()
    }

    @Test
    fun removalFallsBackToProviderPhoto() {
        val lookupKey = uniqueLookupKey("remove")
        val providerUri = Uri.parse("content://contacts/provider-photo")
        assertTrue(store.saveContactPhoto(lookupKey, jpegPhoto(Color.RED)))

        assertTrue(store.removeContactPhoto(lookupKey))

        assertEquals(providerUri, store.resolveContactPhoto(lookupKey, providerUri))
    }

    @Test
    fun damagedLocalPhotoIsDiscardedAndFallsBackToProvider() {
        val lookupKey = uniqueLookupKey("damaged")
        val providerUri = Uri.parse("content://contacts/provider-photo")
        assertTrue(store.saveContactPhoto(lookupKey, jpegPhoto(Color.RED)))
        val localUri = requireNotNull(store.resolveContactPhoto(lookupKey, providerUri))
        File(requireNotNull(localUri.path)).writeBytes(byteArrayOf(1, 2, 3, 4))

        assertEquals(providerUri, store.resolveContactPhoto(lookupKey, providerUri))
    }

    @Test
    fun reconciliationRemovesOnlyOrphanedContactPhotos() {
        val activeLookupKey = uniqueLookupKey("active")
        val orphanLookupKey = uniqueLookupKey("orphan")
        assertTrue(store.saveContactPhoto(activeLookupKey, jpegPhoto(Color.GREEN)))
        assertTrue(store.saveContactPhoto(orphanLookupKey, jpegPhoto(Color.YELLOW)))

        store.reconcileContacts(setOf(activeLookupKey))

        assertTrue(store.resolveContactPhoto(activeLookupKey, null) != null)
        assertNull(store.resolveContactPhoto(orphanLookupKey, null))
    }

    private fun uniqueLookupKey(label: String): String =
        "photo-store-$label-${System.nanoTime()}".also(lookupKeys::add)

    private fun jpegPhoto(color: Int): EncodedPhoto {
        val bitmap = Bitmap.createBitmap(1_200, 800, Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
        }
        return try {
            val output = ByteArrayOutputStream()
            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 97, output))
            EncodedPhoto(
                bytes = output.toByteArray(),
                width = bitmap.width,
                height = bitmap.height,
                format = ContactPhotoFormat.JPEG,
            )
        } finally {
            bitmap.recycle()
        }
    }
}
