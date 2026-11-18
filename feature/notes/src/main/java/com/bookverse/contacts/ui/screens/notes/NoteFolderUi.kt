package com.bookverse.contacts.ui.screens.notes

import androidx.compose.ui.graphics.Color
import com.bookverse.contacts.ui.components.folders.parseFolderColor
import com.bookverse.contacts.ui.components.folders.toFolderColorHex

fun parseFolderColorOrDefault(colorHex: String?, fallback: Color): Color {
    return parseFolderColor(colorHex, fallback)
}

fun Color.toFolderHex(): String {
    return toFolderColorHex()
}

data class FolderSelectionOption(
    val id: Long?,
    val label: String
)
