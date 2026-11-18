package com.bookverse.contacts.settings

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.testing.ComposeTestHostActivity
import com.bookverse.contacts.ui.viewmodel.AppLanguage
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class LocaleResourceSelectionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestHostActivity>()

    @Test
    fun shouldResolveLocalizedSettingsCopyForEverySupportedLanguage() {
        val targetContext = composeRule.activity.applicationContext

        try {
            composeRule.runOnUiThread {
                LocaleController().apply(AppLanguage.ENGLISH)
            }
            val englishContext = LocaleController.localizedContext(targetContext)
            val englishSection = englishContext.getString(R.string.animations_vibration_section)

            listOf(
                AppLanguage.SPANISH,
                AppLanguage.GERMAN,
                AppLanguage.FRENCH,
                AppLanguage.PORTUGUESE,
            ).forEach { language ->
                composeRule.runOnUiThread {
                    LocaleController().apply(language)
                }
                val localizedContext = LocaleController.localizedContext(targetContext)
                assertNotEquals(
                    "${language.name} must not fall back to English",
                    englishSection,
                    localizedContext.getString(R.string.animations_vibration_section),
                )
            }
        } finally {
            composeRule.runOnUiThread {
                LocaleController().apply(AppLanguage.SYSTEM)
            }
        }
    }
}
