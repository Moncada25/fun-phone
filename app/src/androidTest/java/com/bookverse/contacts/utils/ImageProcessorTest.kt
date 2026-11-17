package com.bookverse.contacts.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ImageProcessorTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun largeOpaquePhotoProducesHighResolutionLocalCopyAndOrderedProviderCopies() = runBlocking {
        val sourceFile = File.createTempFile("contact-source-", ".png", context.cacheDir)
        try {
            writeDetailedSource(sourceFile)

            val prepared = ImageProcessor.prepareContactPhoto(context, sourceFile.toUri()).getOrThrow()
            val localBitmap = BitmapFactory.decodeByteArray(
                prepared.localVariant.bytes,
                0,
                prepared.localVariant.bytes.size,
            )
            val providerMaximum = ImageProcessor.contactDisplayMaxDimension(context)

            assertEquals(ContactPhotoFormat.JPEG, prepared.localVariant.format)
            assertEquals(2_048, maxOf(prepared.localVariant.width, prepared.localVariant.height))
            assertEquals(2_048, maxOf(localBitmap.width, localBitmap.height))
            assertEquals(
                minOf(providerMaximum, 2_048),
                maxOf(
                    prepared.providerVariants.first().width,
                    prepared.providerVariants.first().height,
                ),
            )
            assertTrue(prepared.providerVariants.zipWithNext().all { (first, second) ->
                first.width > second.width
            })
            localBitmap.recycle()
        } finally {
            sourceFile.delete()
        }
    }

    @Test
    fun transparentPhotoRemainsLosslessAndIsNotUpscaled() = runBlocking {
        val sourceFile = File.createTempFile("contact-transparent-", ".png", context.cacheDir)
        try {
            val bitmap = Bitmap.createBitmap(600, 400, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.TRANSPARENT)
            canvas.drawCircle(300f, 200f, 150f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.MAGENTA
            })
            try {
                FileOutputStream(sourceFile).use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
                }
            } finally {
                bitmap.recycle()
            }

            val prepared = ImageProcessor.prepareContactPhoto(context, sourceFile.toUri()).getOrThrow()

            assertEquals(ContactPhotoFormat.PNG, prepared.localVariant.format)
            assertEquals(600, prepared.localVariant.width)
            assertEquals(400, prepared.localVariant.height)
        } finally {
            sourceFile.delete()
        }
    }

    private fun writeDetailedSource(file: File) {
        val bitmap = Bitmap.createBitmap(3_000, 2_000, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        repeat(30) { index ->
            paint.color = Color.rgb(
                (index * 37) % 255,
                (index * 71) % 255,
                (index * 113) % 255,
            )
            canvas.drawRect(
                index * 100f,
                0f,
                (index + 1) * 100f,
                bitmap.height.toFloat(),
                paint,
            )
        }
        try {
            FileOutputStream(file).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
        } finally {
            bitmap.recycle()
        }
    }
}
