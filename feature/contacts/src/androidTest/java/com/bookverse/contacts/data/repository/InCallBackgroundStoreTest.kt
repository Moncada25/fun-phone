package com.bookverse.contacts.data.repository

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class InCallBackgroundStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = InCallBackgroundStore(context)
    private val lookupKeys = mutableSetOf<String>()
    private val sourceFiles = mutableSetOf<File>()

    @After
    fun tearDown() {
        lookupKeys.forEach(store::remove)
        sourceFiles.forEach(File::delete)
    }

    @Test
    fun replacementPreservesOriginalBytesAndReturnsVersionedUri() = runBlocking {
        val lookupKey = uniqueLookupKey("original")
        val bytes = jpegBytes(Color.CYAN)
        val source = sourceFile("original", bytes)

        val result = store.replace(lookupKey, source.toUri())
        val resolved = store.resolve(lookupKey)

        assertEquals(InCallBackgroundUpdateResult.Success, result)
        assertNotNull(resolved)
        assertEquals("file", resolved?.scheme)
        assertNotNull(resolved?.getQueryParameter("v"))
        assertArrayEquals(bytes, File(requireNotNull(resolved?.path)).readBytes())
    }

    @Test
    fun successfulReplacementChangesVersionAndContent() = runBlocking {
        val lookupKey = uniqueLookupKey("replacement")
        assertEquals(
            InCallBackgroundUpdateResult.Success,
            store.replace(lookupKey, sourceFile("first", jpegBytes(Color.RED)).toUri()),
        )
        val firstUri = requireNotNull(store.resolve(lookupKey))

        assertEquals(
            InCallBackgroundUpdateResult.Success,
            store.replace(lookupKey, sourceFile("second", jpegBytes(Color.BLUE)).toUri()),
        )
        val secondUri = requireNotNull(store.resolve(lookupKey))

        assertNotEquals(firstUri, secondUri)
        assertTrue(File(requireNotNull(secondUri.path)).readBytes().contentEquals(jpegBytes(Color.BLUE)))
    }

    @Test
    fun invalidReplacementKeepsPreviousBackground() = runBlocking {
        val lookupKey = uniqueLookupKey("invalid")
        val originalBytes = jpegBytes(Color.GREEN)
        assertEquals(
            InCallBackgroundUpdateResult.Success,
            store.replace(lookupKey, sourceFile("valid", originalBytes).toUri()),
        )
        val invalid = sourceFile("invalid", byteArrayOf(1, 2, 3, 4))

        assertEquals(
            InCallBackgroundUpdateResult.InvalidImage,
            store.replace(lookupKey, invalid.toUri()),
        )

        val resolved = requireNotNull(store.resolve(lookupKey))
        assertArrayEquals(originalBytes, File(requireNotNull(resolved.path)).readBytes())
    }

    @Test
    fun reconciliationRemovesOnlyOrphanedBackgrounds() = runBlocking {
        val active = uniqueLookupKey("active")
        val orphan = uniqueLookupKey("orphan")
        assertEquals(
            InCallBackgroundUpdateResult.Success,
            store.replace(active, sourceFile("active", jpegBytes(Color.YELLOW)).toUri()),
        )
        assertEquals(
            InCallBackgroundUpdateResult.Success,
            store.replace(orphan, sourceFile("orphan", jpegBytes(Color.MAGENTA)).toUri()),
        )

        store.reconcile(setOf(active))

        assertNotNull(store.resolve(active))
        assertNull(store.resolve(orphan))
    }

    @Test
    fun reconciliationPreservesActiveTemporaryCopyAndRemovesStaleTemporaryFile() {
        val directory = File(context.filesDir, "in_call_backgrounds").apply { mkdirs() }
        val activeTemporary = File(directory, ".active-${System.nanoTime()}.tmp").apply {
            writeBytes(byteArrayOf(1, 2, 3))
        }.also(sourceFiles::add)
        val staleTemporary = File(directory, ".stale-${System.nanoTime()}.tmp").apply {
            writeBytes(byteArrayOf(4, 5, 6))
            assertTrue(
                setLastModified(
                    System.currentTimeMillis() -
                        InCallBackgroundStore.STALE_TEMPORARY_FILE_AGE_MILLIS - 1L,
                ),
            )
        }.also(sourceFiles::add)

        store.reconcile(emptySet())

        assertTrue(activeTemporary.exists())
        assertTrue(!staleTemporary.exists())
    }

    private fun uniqueLookupKey(label: String): String =
        "incall-background-$label-${System.nanoTime()}".also(lookupKeys::add)

    private fun sourceFile(label: String, bytes: ByteArray): File =
        File.createTempFile("incall-background-$label-", ".jpg", context.cacheDir).also {
            it.writeBytes(bytes)
            sourceFiles.add(it)
        }

    private fun jpegBytes(color: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(320, 640, Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
        }
        return try {
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output))
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }
}
