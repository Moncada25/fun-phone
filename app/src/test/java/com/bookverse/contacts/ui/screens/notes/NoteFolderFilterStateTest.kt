package com.bookverse.contacts.ui.screens.notes

import com.bookverse.contacts.data.notes.NOTE_FOLDER_FILTER_ALL
import com.bookverse.contacts.data.notes.NoteFolder
import org.junit.Assert.assertEquals
import org.junit.Test

class NoteFolderFilterStateTest {
    @Test
    fun selectsAllOption_whenAllFilterIsActive() {
        val folders = listOf(
            NoteFolder(id = 11, name = "Personal"),
            NoteFolder(id = 12, name = "Work"),
        )

        val state = buildNoteFolderFilterState(folders, NOTE_FOLDER_FILTER_ALL)

        assertEquals(listOf(11L, 12L, null), state.options.map { it?.id })
        assertEquals(2, state.selectedIndex)
    }

    @Test
    fun fallsBackToFirstFolder_whenSelectionNoLongerExists() {
        val folders = listOf(
            NoteFolder(id = 11, name = "Personal"),
            NoteFolder(id = 12, name = "Work"),
        )

        val state = buildNoteFolderFilterState(folders, selectedFolderId = 99)

        assertEquals(0, state.selectedIndex)
    }
}
