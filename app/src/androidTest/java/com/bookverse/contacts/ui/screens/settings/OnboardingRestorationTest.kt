package com.bookverse.contacts.ui.screens.settings

import android.accessibilityservice.AccessibilityService
import android.app.role.RoleManager
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import java.util.concurrent.atomic.AtomicReference
import com.bookverse.contacts.utils.PermissionUtils
import androidx.activity.ComponentActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.bookverse.contacts.R
import com.bookverse.contacts.ui.theme.ContactsAppTheme
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.theme.DarkModeStyle
import com.bookverse.contacts.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test

class OnboardingRestorationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun savedStateRestoresSetupAndBackReturnsToWelcome() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            ContactsAppTheme(themeMode = ThemeMode.DARK, darkModeStyle = DarkModeStyle.AMOLED) {
                OnboardingFlowScreen(onFinish = { _, _ -> }, settingsViewModel = remember { AppSettingsViewModel() })
            }
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.onboarding_get_started))
            .performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.onboarding_setup_title)).assertIsDisplayed()
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.onboarding_welcome_title)).assertIsDisplayed()
    }
    @Test
    fun cancelingAndroidRoleDialogReturnsToSetupAndCanFinishInLimitedMode() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        assumeTrue(!PermissionUtils.isDefaultDialer(composeRule.activity))
        assumeTrue(composeRule.activity.getSystemService(RoleManager::class.java).isRoleAvailable(RoleManager.ROLE_DIALER))
        val finished = AtomicReference<Pair<Boolean, Boolean>>()
        composeRule.setContent {
            ContactsAppTheme(themeMode = ThemeMode.DARK, darkModeStyle = DarkModeStyle.AMOLED) {
                OnboardingFlowScreen(onFinish = { limited, review -> finished.set(limited to review) }, settingsViewModel = remember { AppSettingsViewModel() })
            }
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.onboarding_get_started))
            .performScrollTo().performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.onboarding_allow_default))
            .performScrollTo().performClick()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        composeRule.waitUntil(timeoutMillis = 5_000) {
            automation.rootInActiveWindow?.packageName?.toString()?.contains("permissioncontroller") == true
        }
        automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText(composeRule.activity.getString(R.string.onboarding_retry)))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.onboarding_continue_later))
            .performScrollTo().performClick()
        assertEquals(true to false, finished.get())
    }

}
