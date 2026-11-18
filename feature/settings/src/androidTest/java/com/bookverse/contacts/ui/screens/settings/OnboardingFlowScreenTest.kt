package com.bookverse.contacts.ui.screens.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.data.model.ToolsOrderConfig
import com.bookverse.contacts.data.model.ToolsPreset
import com.bookverse.contacts.feature.settings.R
import com.bookverse.contacts.ui.components.PermissionBanner
import com.bookverse.contacts.ui.components.setupActionContainerColor
import com.bookverse.contacts.ui.components.setupActionContentColor
import com.bookverse.contacts.ui.components.setupContrastRatio
import com.bookverse.contacts.ui.theme.ContactsAppTheme
import com.bookverse.contacts.ui.theme.DarkModeStyle
import com.bookverse.contacts.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.feature.tools.R as ToolsR

class OnboardingFlowScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val testContent = SwappableComposeContent { composeRule.setContent(it) }

    @Test
    fun twoStepFlowCanFinishWithoutAnySystemRequestOrExtraConfirmation() {
        val requests = AtomicInteger()
        val finishes = AtomicInteger()
        show {
            val flow = rememberOnboardingFlowController()
            OnboardingContent(
                step = flow.uiState.currentStep,
                setupState = PhoneSetupUiState(missingRole()),
                accentColor = accent,
                onNext = { flow.moveTo(OnboardingStep.PHONE_SETUP) },
                onBack = { flow.moveTo(OnboardingStep.WELCOME) },
                onRequest = { requests.incrementAndGet() },
                onFinish = { finishes.incrementAndGet() },
            )
        }
        click(R.string.onboarding_get_started)
        click(R.string.onboarding_continue_later)
        assertEquals(0, requests.get())
        assertEquals(1, finishes.get())
    }

    @Test
    fun grantingOneRequirementUpdatesNextActionWithoutRequestingIt() {
        val requests = AtomicInteger()
        show {
            var state by remember { mutableStateOf(PhoneSetupUiState(missingRole().copy(hasCallLogPermission = false))) }
            Content(state, onRequest = {
                requests.incrementAndGet()
                state = state.copy(phoneStatus = state.phoneStatus.copy(isDefaultDialer = true))
            })
        }
        click(R.string.onboarding_allow_default)
        node(R.string.onboarding_allow_history).assertIsDisplayed()
        assertEquals(1, requests.get())
    }

    @Test
    fun deniedAndBlockedAccessStillAllowLeaving() {
        val finishes = AtomicInteger()
        show {
            var state by remember { mutableStateOf(PhoneSetupUiState(missingRole())) }
            Content(state, onRequest = {
                state = state.copy(outcomes = mapOf(PhoneStatusRequirement.DEFAULT_DIALER to SetupOutcome.DENIED))
            }, onFinish = { finishes.incrementAndGet() })
        }
        click(R.string.onboarding_allow_default)
        node(R.string.onboarding_retry).assertIsDisplayed()
        click(R.string.onboarding_continue_later)
        assertEquals(1, finishes.get())
        show {
            Content(PhoneSetupUiState(
                missingRole().copy(isDefaultDialer = true, hasCallLogPermission = false),
                outcomes = mapOf(PhoneStatusRequirement.CALL_LOG to SetupOutcome.BLOCKED),
            ), onFinish = { finishes.incrementAndGet() })
        }
        node(CoreUiR.string.open_settings).assertIsDisplayed()
        click(R.string.onboarding_continue_later)
        assertEquals(2, finishes.get())
    }

    @Test
    fun readyPhoneOffersEntryWithoutRequestingPermissions() {
        val finished = AtomicInteger()
        show { Content(PhoneSetupUiState(missingRole().copy(isDefaultDialer = true)), onFinish = { finished.incrementAndGet() }) }
        click(R.string.onboarding_enter)
        assertEquals(1, finished.get())
    }

    @Test
    fun exactAlarmRequirementStaysInTheSetupFlow() {
        val requests = AtomicInteger()
        show {
            Content(
                PhoneSetupUiState(
                    PhoneStatusUiState(
                        isDefaultDialer = true,
                        hasCallNotifications = true,
                        hasContactsPermission = true,
                        hasCallLogPermission = true,
                        hasPhoneStatePermission = true,
                        hasCallPhonePermission = true,
                        hasExactAlarmsPermission = false,
                    ),
                ),
                onRequest = { requests.incrementAndGet() },
            )
        }
        node(R.string.onboarding_allow_exact_alarms).performScrollTo().assertIsDisplayed()
        click(R.string.onboarding_allow_exact_alarms)
        assertEquals(1, requests.get())
    }

    @Test
    fun limitedModeCardRoutesToRecoveryAndToolsWithoutRequestingPermissions() {
        val destination = AtomicReference<String>()
        show {
            PermissionBanner(
                onReviewPhoneStatus = { destination.set("phone_status") },
                onChooseTools = { destination.set("tools_order") },
            )
        }
        click(CoreUiR.string.permission_banner_cta)
        assertEquals("phone_status", destination.get())
        click(CoreUiR.string.onboarding_choose_tools)
        assertEquals("tools_order", destination.get())
    }

    @Test
    fun compactLandscapeAtLargeFontKeepsActionsReachable() {
        val finished = AtomicInteger()
        show {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                Box(Modifier.size(width = 320.dp, height = 360.dp)) {
                    Content(PhoneSetupUiState(missingRole()), onFinish = { finished.incrementAndGet() })
                }
            }
        }
        node(R.string.onboarding_allow_default).performScrollTo().assertIsDisplayed()
        node(R.string.onboarding_continue_later).performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, finished.get())
    }

    @Test
    fun themesKeepActionsReadableAndProduceSyntheticPreviews() {
        for ((name, mode, style) in listOf(
            Triple("light", ThemeMode.LIGHT, DarkModeStyle.AMOLED),
            Triple("amoled", ThemeMode.DARK, DarkModeStyle.AMOLED),
            Triple("gray", ThemeMode.DARK, DarkModeStyle.DARK_GRAY),
            Triple("dim", ThemeMode.DARK, DarkModeStyle.DIM),
        )) {
            val contrast = AtomicReference<Float>()
            testContent.show {
                ContactsAppTheme(themeMode = mode, darkModeStyle = style) {
                    contrast.set(setupContrastRatio(setupActionContentColor(accent), setupActionContainerColor(accent)))
                    Content(PhoneSetupUiState(missingRole()))
                }
            }
            node(R.string.onboarding_allow_default).assertIsDisplayed()
            assertTrue("Insufficient contrast in $name", contrast.get() >= 4.5f)
            val minimum = with(composeRule.density) { 48.dp.toPx() }
            composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach {
                assertTrue(it.boundsInRoot.width >= minimum)
                assertTrue(it.boundsInRoot.height >= minimum)
            }
            val screenshot = composeRule.onRoot().captureToImage().asAndroidBitmap()
            File(composeRule.activity.getExternalFilesDir(null), "onboarding-$name.png").outputStream().use {
                screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test
    fun welcomeShowsBenefitsAndProducesSyntheticPreview() {
        show { OnboardingContent(OnboardingStep.WELCOME, PhoneSetupUiState(missingRole()), accent, {}, {}, {}, {}) }
        node(R.string.onboarding_welcome_title).assertIsDisplayed()
        node(R.string.onboarding_get_started).assertIsDisplayed()
        val screenshot = composeRule.onRoot().captureToImage().asAndroidBitmap()
        File(composeRule.activity.getExternalFilesDir(null), "onboarding-welcome.png").outputStream().use {
            screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun welcomeOffersOptionalToolPresetsAndCustomVisibility() {
        val selectedPreset = AtomicReference<ToolsPreset>()
        show {
            OnboardingContent(
                step = OnboardingStep.WELCOME,
                setupState = PhoneSetupUiState(missingRole()),
                accentColor = accent,
                onNext = {},
                onBack = {},
                onRequest = {},
                onFinish = {},
                toolsOrder = ToolsOrderConfig.default(),
                onPresetSelected = { selectedPreset.set(it) },
            )
        }
        composeRule.onNodeWithTag("onboarding_body").performScrollToIndex(1)
        node(R.string.onboarding_optional_tools_title).assertIsDisplayed()
        click(ToolsR.string.tools_preset_everything)
        assertEquals(ToolsPreset.EVERYTHING, selectedPreset.get())
        click(R.string.onboarding_customize_tools)
        node(CoreUiR.string.tools_menu_visible_title).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun lightAndDarkAccentsKeepTextContrast() {
        for (color in listOf(Color(0xFFFFEB3B), Color(0xFF121212), Color(0xFF2196F3))) {
            for (style in DarkModeStyle.entries) {
                val contrast = AtomicReference<Float>()
                show {
                    ContactsAppTheme(themeMode = ThemeMode.DARK, darkModeStyle = style) {
                        contrast.set(setupContrastRatio(setupActionContentColor(color), setupActionContainerColor(color)))
                        Content(PhoneSetupUiState(missingRole()))
                    }
                }
                composeRule.waitForIdle()
                assertFalse(contrast.get() < 4.5f)
            }
        }
    }

    private fun missingRole() = PhoneStatusUiState(false, true, true, true, true, true)
    private val accent = Color(0xFF2196F3)

    @Composable
    private fun Content(
        state: PhoneSetupUiState,
        onRequest: (PhoneStatusRequirement) -> Unit = {},
        onFinish: () -> Unit = {},
    ) {
        OnboardingContent(OnboardingStep.PHONE_SETUP, state, accent, {}, {}, onRequest, onFinish)
    }

    @Composable
    private fun Theme(content: @Composable () -> Unit) {
        ContactsAppTheme(themeMode = ThemeMode.DARK, darkModeStyle = DarkModeStyle.AMOLED, content = content)
    }

    private fun show(content: @Composable () -> Unit) {
        testContent.show { Theme(content) }
    }

    private fun node(res: Int) = composeRule.onNodeWithText(composeRule.activity.getString(res))
    private fun click(res: Int) { node(res).performScrollTo().performClick() }
}
