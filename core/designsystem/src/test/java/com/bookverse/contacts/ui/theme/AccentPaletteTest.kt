package com.bookverse.contacts.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentPaletteTest {
    private val configuredSeeds = listOf(
        0xFF1E88E5,
        0xFF1565C0,
        0xFF90CAF9,
        0xFF43A047,
        0xFF2E7D32,
        0xFFA5D6A7,
        0xFFD32F2F,
        0xFFB71C1C,
        0xFFEF9A9A,
        0xFFFB8C00,
        0xFFEF6C00,
        0xFFFFCC80,
        0xFFFFA000,
        0xFF7E57C2,
        0xFF6A1B9A,
        0xFFAF89F5,
        0xFFD81B60,
        0xFFC2185B,
        0xFFF48FB1,
        0xFF009688,
        0xFF00695C,
        0xFF80CBC4,
        0xFF00ACC1,
        0xFF80DEEA,
        0xFF3949AB,
        0xFF607D8B,
        0xFFFBC02D,
        0xFF748873,
        0xFF276C76,
        0xFFB76E56,
        0xFF9473A6,
        0xFFAA8C62,
        0xFF62666D,
        0xFF000000,
        0xFFFFFFFF,
    ).map(::Color)

    private val schemes = listOf(
        Scheme(LightBackgroundColor, LightSurfaceColor, LightTextColor, false),
        Scheme(DarkBackgroundColor, DarkSurfaceColor, DarkTextColor, true),
        Scheme(DarkGrayBackgroundColor, DarkGraySurfaceColor, DarkGrayTextColor, true),
        Scheme(DimBackgroundColor, DimSurfaceColor, DimTextColor, true),
    )

    @Test
    fun configuredSeedsProduceOpaqueAccessibleRolesInEveryTheme() {
        configuredSeeds.forEach { seed ->
            schemes.forEach { scheme ->
                val palette = scheme.palette(seed)

                assertEquals(1f, palette.seed.alpha, 0f)
                assertEquals(1f, palette.onSeed.alpha, 0f)
                assertEquals(1f, palette.primary.alpha, 0f)
                assertEquals(1f, palette.onPrimary.alpha, 0f)
                assertEquals(1f, palette.primaryContainer.alpha, 0f)
                assertEquals(1f, palette.onPrimaryContainer.alpha, 0f)
                assertTrue(contrastRatio(palette.primary, scheme.background) >= 4.49f)
                assertTrue(contrastRatio(palette.primary, scheme.surface) >= 4.49f)
                assertTrue(contrastRatio(palette.onPrimary, palette.primary) >= 4.49f)
                assertTrue(
                    contrastRatio(palette.onPrimaryContainer, palette.primaryContainer) >= 4.49f,
                )
                assertTrue(contrastRatio(palette.onSeed, palette.seed) >= 4.49f)
            }
        }
    }

    @Test
    fun paletteGenerationIsDeterministic() {
        configuredSeeds.forEach { seed ->
            schemes.forEach { scheme ->
                assertEquals(scheme.palette(seed), scheme.palette(seed))
            }
        }
    }

    @Test
    fun containersUseTheExpectedThemeSpecificStrength() {
        val seed = Color(0xFF1E88E5)
        val light = schemes.first().palette(seed)
        val dark = schemes[1].palette(seed)

        assertTrue(contrastRatio(light.primaryContainer, LightSurfaceColor) > 1f)
        assertTrue(
            contrastRatio(dark.primaryContainer, DarkSurfaceColor) >
                contrastRatio(light.primaryContainer, LightSurfaceColor),
        )
    }

    @Test
    fun applyingPaletteRemovesMaterialPurpleFallbacks() {
        val palette = schemes.first().palette(DefaultAccentSeed)
        val scheme = lightColorScheme().withAccentPalette(palette)
        val materialPurpleFallbacks = setOf(
            Color(0xFF6750A4),
            Color(0xFF625B71),
            Color(0xFF7D5260),
            Color(0xFFE7E0EC),
            Color(0xFF79747E),
            Color(0xFFCAC4D0),
        )

        val themedRoles = listOf(
            scheme.primary,
            scheme.secondary,
            scheme.tertiary,
            scheme.surfaceVariant,
            scheme.outline,
            scheme.outlineVariant,
        )

        assertTrue(themedRoles.none { it in materialPurpleFallbacks })
        assertEquals(Color.Transparent, scheme.surfaceTint)
    }

    @Test
    fun adjustedPrimaryReasonablyPreservesSeedHue() {
        configuredSeeds.forEach { seed ->
            schemes.forEach { scheme ->
                val primary = scheme.palette(seed).primary
                val seedHue = hue(seed)
                val primaryHue = hue(primary)

                if (seedHue != null && primaryHue != null) {
                    val difference = abs(seedHue - primaryHue)
                    val circularDifference = minOf(difference, 360f - difference)
                    assertTrue(
                        "Seed $seed shifted $circularDifference degrees in ${scheme.background}",
                        circularDifference <= MAX_REASONABLE_HUE_SHIFT,
                    )
                }
            }
        }
    }

    @Test
    fun surfaceContainersStayNeutralAndReadableAcrossEveryTheme() {
        schemes.forEach { base ->
            val scheme = lightColorScheme(background = base.background, surface = base.surface, onSurface = base.onBackground)
                .withAccentPalette(base.palette(DefaultAccentSeed))
            for (surface in listOf(scheme.surfaceContainerLowest, scheme.surfaceContainerLow, scheme.surfaceContainer, scheme.surfaceContainerHigh, scheme.surfaceContainerHighest)) {
                assertTrue(contrastRatio(scheme.onSurface, surface) >= 4.5f)
                assertTrue(abs(surface.red - surface.blue) < 0.04f)
            }
        }
    }

    private fun Scheme.palette(seed: Color): AccentPalette = createAccentPalette(
        seed = seed,
        background = background,
        surface = surface,
        onBackground = onBackground,
        isDark = isDark,
    )

    private fun hue(color: Color): Float? {
        val maximum = maxOf(color.red, color.green, color.blue)
        val minimum = minOf(color.red, color.green, color.blue)
        val chroma = maximum - minimum
        if (chroma < MIN_HUE_CHROMA) return null

        val segment = when (maximum) {
            color.red -> ((color.green - color.blue) / chroma) % 6f
            color.green -> ((color.blue - color.red) / chroma) + 2f
            else -> ((color.red - color.green) / chroma) + 4f
        }
        return (segment * 60f + 360f) % 360f
    }

    private data class Scheme(
        val background: Color,
        val surface: Color,
        val onBackground: Color,
        val isDark: Boolean,
    )

    private companion object {
        const val MIN_HUE_CHROMA = 0.05f
        const val MAX_REASONABLE_HUE_SHIFT = 20f
    }
}
