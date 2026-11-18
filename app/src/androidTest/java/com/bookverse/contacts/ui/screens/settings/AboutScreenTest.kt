package com.bookverse.contacts.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.testing.ComposeTestContent
import com.bookverse.contacts.testing.ComposeTestHostActivity
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class AboutScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestHostActivity>()

    @Test
    fun rendersVisibleAboutFeatures() {
        showAboutScreen()

        composeRule.onNodeWithText(string(R.string.app_name)).assertIsDisplayed()
        scrollTo(R.string.about_features_title)
        composeRule.onNodeWithText(string(R.string.about_features_title)).assertIsDisplayed()

        listOf(
            R.string.about_feature_dialer,
            R.string.about_feature_t9_search,
            R.string.about_feature_call_controls,
            R.string.about_feature_call_history,
            R.string.about_feature_contacts,
            R.string.about_feature_favorites,
            R.string.about_feature_dual_sim,
            R.string.about_feature_default_phone,
            R.string.about_feature_local_privacy,
            R.string.about_feature_import_export,
            R.string.about_feature_duplicates,
            R.string.about_feature_accessibility,
            R.string.about_feature_customization,
            R.string.about_feature_shortcuts,
            R.string.about_feature_widgets,
            R.string.about_feature_optional_tools,
        ).forEach { featureRes ->
            composeRule
                .onNode(hasScrollAction())
                .performScrollToNode(hasContentDescription(string(featureRes)))
            composeRule.onNodeWithContentDescription(string(featureRes)).assertIsDisplayed()
        }

        scrollTo(R.string.about_actions_title)
        composeRule.onNodeWithText(string(R.string.about_actions_title)).assertIsDisplayed()
        scrollTo(R.string.about_footer_message)
        composeRule.onNodeWithText(string(R.string.about_footer_message)).assertIsDisplayed()
    }

    @Test
    fun exposesBackAndActionRowsAsButtons() {
        showAboutScreen()

        composeRule
            .onNodeWithContentDescription(string(R.string.back))
            .assertHasClickAction()

        scrollTo(R.string.about_actions_title)
        listOf(
            R.string.share,
            R.string.website,
            R.string.about_feedback,
        ).forEach { actionRes ->
            scrollTo(actionRes)
            composeRule
                .onNode(hasText(string(actionRes)) and hasClickAction())
                .assertIsDisplayed()
        }
    }

    @Test
    fun backActionInvokesCallback() {
        val wasBackPressed = AtomicBoolean(false)
        showAboutScreen { wasBackPressed.set(true) }

        composeRule
            .onNodeWithContentDescription(string(R.string.back))
            .performClick()

        assertTrue(wasBackPressed.get())
    }

    @Test
    fun copyingVersionShowsConfirmationSnackbar() {
        showAboutScreen()

        val packageInfo = composeRule.activity.packageManager.getPackageInfo(
            composeRule.activity.packageName,
            0
        )
        val version = packageInfo.versionName ?: "N/A"
        composeRule
            .onNodeWithText(string(R.string.about_version, version))
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText(string(R.string.about_version_copied))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun fiveTapsOnAppNameShowEasterEggConfirmation() {
        showAboutScreen()

        repeat(5) {
            composeRule.onNodeWithText(string(R.string.app_name)).performClick()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText(string(R.string.easter_egg_toast))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun largeTextKeepsActionsReachable() {
        val settingsViewModel = AppSettingsViewModel()
        composeRule.runOnUiThread {
            ComposeTestContent.show {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, fontScale = 2f),
                ) {
                    MaterialTheme {
                        Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                            AboutScreen(
                                onBack = {},
                                appSettingsViewModel = settingsViewModel
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()

        listOf(
            R.string.about_actions_title,
            R.string.about_feedback,
            R.string.about_footer_message,
        ).forEach { targetRes ->
            scrollTo(targetRes)
            composeRule.onNodeWithText(string(targetRes)).assertIsDisplayed()
        }
    }

    @Test
    fun rendersDarkRetroAndRtlVariant() {
        val settingsViewModel = AppSettingsViewModel()
        composeRule.runOnUiThread {
            settingsViewModel.setAccentBarStyle(
                composeRule.activity,
                AccentBarStyle.RETRO
            )
            ComposeTestContent.show {
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                ) {
                    MaterialTheme(colorScheme = darkColorScheme()) {
                        AboutScreen(
                            onBack = {},
                            appSettingsViewModel = settingsViewModel
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.app_name)).assertIsDisplayed()
        scrollTo(R.string.about_actions_title)
        composeRule.onNodeWithText(string(R.string.about_actions_title)).assertIsDisplayed()
        scrollTo(R.string.about_footer_message)
        composeRule.onNodeWithText(string(R.string.about_footer_message)).assertIsDisplayed()
    }

    private fun showAboutScreen(onBack: () -> Unit = {}) {
        val settingsViewModel = AppSettingsViewModel()
        composeRule.runOnUiThread {
            ComposeTestContent.show {
                MaterialTheme {
                    AboutScreen(
                        onBack = onBack,
                        appSettingsViewModel = settingsViewModel
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun scrollTo(stringRes: Int) {
        composeRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText(string(stringRes)))
    }

    private fun string(stringRes: Int, vararg formatArgs: Any): String {
        return composeRule.activity.getString(stringRes, *formatArgs)
    }
}
