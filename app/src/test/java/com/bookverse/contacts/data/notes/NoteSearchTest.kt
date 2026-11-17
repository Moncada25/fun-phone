package com.bookverse.contacts.data.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSearchTest {
    private val richHtml =
        "<div style=\"text-align: left;\"><span style=\"color: rgb(255, 0, 0);\">Compras</span> " +
            "del <b>s&aacute;</b>bado</div><p class=\"note\">Caf&eacute;&nbsp;&amp;&nbsp;t&#233; " +
            "&lt;urgente&gt;</p><ul><li>Pan</li><li>Leche</li></ul>"

    private fun find(
        query: String,
        notes: List<Note>,
        checklists: Map<Long, List<ChecklistItem>> = emptyMap(),
        reminders: Map<Long, ReminderData> = emptyMap(),
        recordings: Map<Long, RecordingData> = emptyMap(),
    ): List<Long> = searchNotes(
        notes = notes,
        query = query,
        searchIndex = buildNoteSearchIndex(notes, checklists, reminders, recordings),
    ).map { it.id }

    @Test
    fun visibleTextDropsMarkupDecodesEntitiesAndCollapsesWhitespace() {
        assertEquals(
            "Compras del sábado Café & té <urgente> Pan Leche",
            htmlToVisibleText(richHtml),
        )
    }

    @Test
    fun inlineTagsDoNotSplitWordsAndBlockTagsSeparateThem() {
        assertEquals("important word", htmlToVisibleText("<b>impor</b>tant<br>word"))
        assertEquals("one two", htmlToVisibleText("<p>one</p><p>two</p>"))
        assertEquals("a < b and c > d", htmlToVisibleText("a < b   and c > d"))
    }

    @Test
    fun hiddenBlocksAndCommentsAreNotVisible() {
        assertEquals(
            "shown",
            htmlToVisibleText("<style>.secret{color:red}</style><!-- hidden -->shown<script>x()</script>"),
        )
    }

    @Test
    fun entitiesDecodeOnceAndUnknownOnesStayLiteral() {
        assertEquals("&lt;", decodeHtmlEntities("&amp;lt;"))
        assertEquals("€ A A &bogus; & alone", decodeHtmlEntities("&euro; &#65; &#x41; &bogus; & alone"))
    }

    @Test
    fun markupWordsNeverMatch() {
        val note = Note(id = 1, title = "Lista", content = richHtml)
        listOf("div", "span", "style", "color", "rgb", "class", "nbsp", "amp", "aacute", "text-align").forEach {
            assertTrue("markup term $it matched", find(it, listOf(note)).isEmpty())
        }
        assertEquals(listOf(1L), find("compras", listOf(note)))
        assertEquals(listOf(1L), find("urgente", listOf(note)))
    }

    @Test
    fun matchingIgnoresCaseDiacriticsAndExtraWhitespace() {
        val note = Note(id = 1, title = "Reunión", content = richHtml)
        assertEquals(listOf(1L), find("SABADO", listOf(note)))
        assertEquals(listOf(1L), find("cafe & te", listOf(note)))
        assertEquals(listOf(1L), find("  reunion   pan ", listOf(note)))
        assertEquals(listOf(1L), find("café", listOf(note)))
        assertTrue(find("reunion domingo", listOf(note)).isEmpty())
        assertTrue(find("   ", listOf(note)).isEmpty())
    }

    @Test
    fun searchCoversEveryFolder() {
        val notes = listOf(
            Note(id = 1, title = "Trabajo", content = "<p>informe anual</p>", folderId = 7),
            Note(id = 2, title = "Casa", content = "<p>informe del gas</p>", folderId = null),
            Note(id = 3, title = "Otro", content = "<p>nada</p>", folderId = 9),
        )
        assertEquals(listOf(1L, 2L), find("informe", notes))
    }

    @Test
    fun checklistItemsReminderMessagesAndTranscriptionsAreSearched() {
        val checklist = Note(id = 1, title = "Mercado", isChecklist = true)
        val reminder = Note(id = 2, title = "Cita", content = "ignored", isReminder = true)
        val recording = Note(id = 3, title = "Audio", isRecording = true)
        val notes = listOf(checklist, reminder, recording)
        val checklists = mapOf(1L to listOf(ChecklistItem(noteId = 1, text = "Plátanos maduros")))
        val reminders = mapOf(
            2L to ReminderData(noteId = 2, date = 0L, interval = 0, message = "Llevar el pasaporte"),
        )
        val recordings = mapOf(3L to RecordingData(noteId = 3, transcription = "Idea para el jardín"))

        assertEquals(listOf(1L), find("platanos", notes, checklists, reminders, recordings))
        assertEquals(listOf(2L), find("pasaporte", notes, checklists, reminders, recordings))
        assertEquals(listOf(3L), find("jardin", notes, checklists, reminders, recordings))
    }

    @Test
    fun lockedNotesMatchOnTitleOnly() {
        val lockedNote = Note(id = 1, title = "Banco", content = "<p>clave secreta 4821</p>", isLocked = true)
        val lockedChecklist = Note(id = 2, title = "Privada", isChecklist = true, isLocked = true)
        val lockedReminder = Note(id = 3, title = "Médico", content = "resultado", isReminder = true, isLocked = true)
        val lockedRecording = Note(id = 4, title = "Voz", isRecording = true, isLocked = true)
        val notes = listOf(lockedNote, lockedChecklist, lockedReminder, lockedRecording)
        val checklists = mapOf(2L to listOf(ChecklistItem(noteId = 2, text = "pin secreto")))
        val reminders = mapOf(
            3L to ReminderData(noteId = 3, date = 0L, interval = 0, message = "resultado secreto"),
        )
        val recordings = mapOf(4L to RecordingData(noteId = 4, transcription = "confesión secreta"))

        assertTrue(find("secret", notes, checklists, reminders, recordings).isEmpty())
        assertTrue(find("4821", notes, checklists, reminders, recordings).isEmpty())
        assertTrue(find("resultado", notes, checklists, reminders, recordings).isEmpty())
        assertEquals(listOf(1L), find("banco", notes, checklists, reminders, recordings))
        assertEquals(listOf(3L), find("medico", notes, checklists, reminders, recordings))
        assertFalse(
            buildNoteSearchText(lockedNote).contains("clave"),
        )
    }

    @Test
    fun notesMissingFromTheIndexFallBackToTheirTitle() {
        val note = Note(id = 5, title = "Recién creada", content = "<p>contenido</p>")
        assertEquals(listOf(note), searchNotes(listOf(note), "recien", emptyMap()))
        assertTrue(searchNotes(listOf(note), "contenido", emptyMap()).isEmpty())
    }
}
