package com.bookverse.contacts.settings

enum class FontCategory { RECOMMENDED, SANS, SERIF, MONOSPACE, EXPRESSIVE }

object FontCatalog {
    val recommended = listOf("default", "inter", "manrope", "roboto", "noto_sans")

    fun category(key: String): FontCategory = when (key) {
        in recommended -> FontCategory.RECOMMENDED
        "serif", "playfair", "roboto_serif", "noto_serif" -> FontCategory.SERIF
        "monospace", "jetbrains_mono", "doto", "pixelify_sans" -> FontCategory.MONOSPACE
        "lato", "roboto_condensed", "rubik", "urbanist", "nunito" -> FontCategory.SANS
        else -> FontCategory.EXPRESSIVE
    }

    fun orderedKeys(): List<String> = recommended +
        Fonts.getAvailableFonts().filterNot(recommended::contains).sortedBy { category(it).ordinal }
}
