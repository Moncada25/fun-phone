package com.bookverse.contacts.ui.screens.settings

import android.content.ClipboardManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import com.bookverse.contacts.BuildConfig
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.testing.ComposeTestContent
import com.bookverse.contacts.testing.ComposeTestHostActivity
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import com.bookverse.contacts.feature.settings.R as SettingsR

class AboutScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestHostActivity>()

    @Test
    fun rendersVisibleAboutFeatures() {
        showAboutScreen()

        composeRule.onNodeWithText(string(R.string.app_name)).assertIsDisplayed()
        scrollTo(SettingsR.string.about_features_title)
        composeRule.onNodeWithText(string(SettingsR.string.about_features_title)).assertIsDisplayed()

        listOf(
            SettingsR.string.about_feature_dialer,
            SettingsR.string.about_feature_t9_search,
            SettingsR.string.about_feature_call_controls,
            SettingsR.string.about_feature_call_history,
            SettingsR.string.about_feature_contacts,
            SettingsR.string.about_feature_favorites,
            SettingsR.string.about_feature_dual_sim,
            SettingsR.string.about_feature_default_phone,
            SettingsR.string.about_feature_local_privacy,
            SettingsR.string.about_feature_import_export,
            SettingsR.string.about_feature_duplicates,
            SettingsR.string.about_feature_accessibility,
            SettingsR.string.about_feature_customization,
            SettingsR.string.about_feature_shortcuts,
            SettingsR.string.about_feature_widgets,
            SettingsR.string.about_feature_optional_tools,
        ).forEach { featureRes ->
            composeRule
                .onNode(hasScrollAction())
                .performScrollToNode(hasContentDescription(string(featureRes)))
            composeRule.onNodeWithContentDescription(string(featureRes)).assertIsDisplayed()
        }

        scrollTo(SettingsR.string.about_support_title)
        composeRule.onNodeWithText(string(SettingsR.string.about_support_title)).assertIsDisplayed()
        scrollTo(SettingsR.string.about_footer_message)
        composeRule.onNodeWithText(string(SettingsR.string.about_footer_message)).assertIsDisplayed()
    }

    @Test
    fun exposesBackAndActionRowsAsButtons() {
        showAboutScreen()

        composeRule
            .onNodeWithContentDescription(string(R.string.back))
            .assertHasClickAction()

        listOf(
            SettingsR.string.about_source_code,
            SettingsR.string.about_report_issue,
            SettingsR.string.about_releases,
            SettingsR.string.about_contribute,
            SettingsR.string.about_license,
            SettingsR.string.about_star_repository,
            R.string.share,
            R.string.website,
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
    fun supportButtonSitsBelowGooglePlayRating() {
        showAboutScreen()
        scrollTo(SettingsR.string.about_support_project)
        val rating = composeRule.onNodeWithText(string(SettingsR.string.about_rate_play))
        val support = composeRule.onNodeWithText(string(SettingsR.string.about_support_project))
        rating.assertIsDisplayed()
        support.assertIsDisplayed()
        assertTrue(support.fetchSemanticsNode().boundsInRoot.top >= rating.fetchSemanticsNode().boundsInRoot.bottom)
    }

    @Test
    fun showsSponsorsAvailabilityForDistributionAndBreBKeyAction() {
        showAboutScreen()
        scrollTo(SettingsR.string.about_support_project)
        composeRule.onNodeWithText(string(SettingsR.string.about_support_project)).performClick()

        composeRule.onNodeWithText(string(SettingsR.string.about_international_contribution)).assertIsDisplayed()
        if (BuildConfig.DISTRIBUTION == "community") {
            composeRule.onNodeWithText(string(SettingsR.string.about_github_sponsors)).assertIsEnabled()
        } else {
            composeRule.onNodeWithText(string(SettingsR.string.about_sponsors_unavailable)).assertIsNotEnabled()
        }
        composeRule.onNodeWithText(string(SettingsR.string.about_colombia_only)).assertIsDisplayed()
        composeRule.onNodeWithText(string(SettingsR.string.about_copy_breb_key)).assertIsDisplayed()
    }

    @Test
    fun copiesDisplayedBreBKey() {
        showAboutScreen()
        scrollTo(SettingsR.string.about_support_project)
        composeRule.onNodeWithText(string(SettingsR.string.about_support_project)).performClick()
        composeRule.onNodeWithContentDescription(
            string(SettingsR.string.about_donation_qr, string(SettingsR.string.about_breb_label)),
        ).performScrollTo().assertIsDisplayed()
        val key = string(SettingsR.string.about_breb_key)
        composeRule.onNodeWithText(key).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(SettingsR.string.about_copy_breb_key)).performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            val clipboard = composeRule.activity.getSystemService(ClipboardManager::class.java)
            assertEquals(key, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        }
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
            .onNodeWithText(string(SettingsR.string.about_version_short, version))
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText(string(SettingsR.string.about_version_copied))
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
                .onAllNodesWithText(string(SettingsR.string.easter_egg_toast))
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
                            )
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()

        listOf(
            SettingsR.string.about_star_repository,
            SettingsR.string.about_support_title,
            SettingsR.string.about_footer_message,
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
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.app_name)).assertIsDisplayed()
        scrollTo(SettingsR.string.about_support_title)
        composeRule.onNodeWithText(string(SettingsR.string.about_support_title)).assertIsDisplayed()
        scrollTo(SettingsR.string.about_footer_message)
        composeRule.onNodeWithText(string(SettingsR.string.about_footer_message)).assertIsDisplayed()
    }

    private fun showAboutScreen(onBack: () -> Unit = {}) {
        val settingsViewModel = AppSettingsViewModel()
        composeRule.runOnUiThread {
            ComposeTestContent.show {
                MaterialTheme {
                    AboutScreen(
                        onBack = onBack,
                        gitHubSponsorsEnabled = BuildConfig.GITHUB_SPONSORS_ENABLED,
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
