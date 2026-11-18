package com.bookverse.contacts.settings

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.annotation.FontRes
import com.bookverse.contacts.core.ui.R

enum class Fonts(val font: String, val fontFamily: FontFamily) {
    DEFAULT("default", FontFamily.Default),
    SERIF("serif", FontFamily.Serif),
    MONOSPACE("monospace", FontFamily.Monospace),
    CURSIVE("cursive", FontFamily.Cursive),
    LATO("lato", FontFamily(Font(R.font.lato_regular))),
    PLAYFAIR("playfair", FontFamily(Font(R.font.playfair_italic))),
    PLAYPEN("playpen", FontFamily(Font(R.font.playpen_sans_thai))),
    PLAYWRITE("playwrite", FontFamily(Font(R.font.playwrite_hu))),
    YELLOWTAIL("yellowtail", FontFamily(Font(R.font.yellowtail_regular))),
    DOTO("doto", FontFamily(Font(R.font.doto))),
    HENNY_PENNY("henny_penny", FontFamily(Font(R.font.henny_penny))),
    INTER("inter", variableFontFamily(R.font.inter_variable)),
    ROBOTO("roboto", FontFamily(Font(R.font.roboto_regular))),
    ROBOTO_CONDENSED("roboto_condensed", FontFamily(Font(R.font.roboto_condensed_regular))),
    ROBOTO_SERIF("roboto_serif", FontFamily(Font(R.font.roboto_serif_regular))),
    NOTO_SANS("noto_sans", FontFamily(Font(R.font.noto_sans_regular))),
    NOTO_SERIF("noto_serif", FontFamily(Font(R.font.noto_serif_regular))),
    MANROPE("manrope", variableFontFamily(R.font.manrope_variable)),
    RUBIK("rubik", FontFamily(Font(R.font.rubik_regular))),
    URBANIST("urbanist", FontFamily(Font(R.font.urbanist_regular))),
    NUNITO("nunito", FontFamily(Font(R.font.nunito_regular))),
    JETBRAINS_MONO("jetbrains_mono", FontFamily(Font(R.font.jetbrains_mono_regular))),
    CAVEAT("caveat", FontFamily(Font(R.font.caveat_regular))),
    LOBSTER("lobster", FontFamily(Font(R.font.lobster_regular))),
    PACIFICO("pacifico", FontFamily(Font(R.font.pacifico_regular))),
    GREAT_VIBES("great_vibes", FontFamily(Font(R.font.great_vibes_regular))),
    PIXELIFY_SANS("pixelify_sans", FontFamily(Font(R.font.pixelify_sans_regular)));

    companion object {
        fun getAvailableFonts() = entries.map { it.font }
        fun getFontFamily(fontName: String) = entries.find {
            it.font == fontName
        }?.fontFamily ?: FontFamily.Default

        private val largeButtonFonts = setOf(
            PLAYWRITE.font, HENNY_PENNY.font, PLAYPEN.font, YELLOWTAIL.font,
            CAVEAT.font, LOBSTER.font, PACIFICO.font, GREAT_VIBES.font, PIXELIFY_SANS.font
        )

        fun needsLargeButton(fontName: String): Boolean = largeButtonFonts.any {
            fontName.equals(it, ignoreCase = true)
        }
    }
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun variableFontFamily(@FontRes resource: Int) = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(resource, weight = weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    },
)
