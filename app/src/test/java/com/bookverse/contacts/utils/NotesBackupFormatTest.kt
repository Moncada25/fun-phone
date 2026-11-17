package com.bookverse.contacts.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotesBackupFormatTest {
    private fun document(meta: String): String =
        "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n$meta<title>Note</title>\n" +
            "</head>\n<body>\n<p>Body</p>\n</body>\n</html>"

    @Test
    fun metadataRoundTripsTagIdsAndImageNames() {
        val meta = buildNoteBackupMetaTags(
            tagIds = listOf(4L, 9L),
            imageFileNames = listOf("abc123.jpg", "def456.png"),
        )
        val parsed = parseNoteBackupMetadata(document(meta))
        assertEquals(listOf(4L, 9L), parsed.tagIds)
        assertEquals(listOf("abc123.jpg", "def456.png"), parsed.imageFileNames)
    }

    @Test
    fun backupsWrittenBeforeTagsAndImagesHaveEmptyMetadata() {
        val parsed = parseNoteBackupMetadata(document(""))
        assertTrue(parsed.tagIds.isEmpty())
        assertTrue(parsed.imageFileNames.isEmpty())
        assertEquals("", buildNoteBackupMetaTags(emptyList(), emptyList()))
    }

    @Test
    fun metadataInsideTheNoteBodyIsIgnored() {
        val html = document("").replace(
            "<p>Body</p>",
            "<meta name=\"note-images\" content=\"evil.jpg\"><meta name=\"note-tag-ids\" content=\"1\">",
        )
        val parsed = parseNoteBackupMetadata(html)
        assertTrue(parsed.tagIds.isEmpty())
        assertTrue(parsed.imageFileNames.isEmpty())
    }

    @Test
    fun unsafeImageNamesAndInvalidTagIdsAreDropped() {
        val html = document(
            "<meta name=\"note-tag-ids\" content=\"3, x, 3, 8\">\n" +
                "<meta name=\"note-images\" content=\"../../databases/app.db,ok_1.jpg,a/b.png,c.gif\">\n",
        )
        val parsed = parseNoteBackupMetadata(html)
        assertEquals(listOf(3L, 8L), parsed.tagIds)
        assertEquals(listOf("ok_1.jpg"), parsed.imageFileNames)
        assertFalse(buildNoteBackupMetaTags(emptyList(), listOf("../x.jpg")).contains("note-images"))
    }

    @Test
    fun imageFileNamesAreRestrictedToGeneratedShapes() {
        assertTrue(NotesImageStore.isValidFileName(NotesImageStore.newFileName("jpg")))
        assertTrue(NotesImageStore.isValidFileName(NotesImageStore.newFileName("png")))
        assertFalse(NotesImageStore.isValidFileName(""))
        assertFalse(NotesImageStore.isValidFileName(".jpg"))
        assertFalse(NotesImageStore.isValidFileName("a.jpeg"))
        assertFalse(NotesImageStore.isValidFileName("a\\b.jpg"))
        assertFalse(NotesImageStore.isValidFileName("a.jpg/"))
        assertEquals("jpg", NotesImageStore.extensionOf("photo.JPEG"))
        assertEquals("png", NotesImageStore.extensionOf("x.png"))
        assertEquals(null, NotesImageStore.extensionOf("x.webp"))
    }
}
