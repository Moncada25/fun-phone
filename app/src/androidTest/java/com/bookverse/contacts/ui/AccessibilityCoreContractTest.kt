package com.bookverse.contacts.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.data.model.ToolsOrderConfig
import com.bookverse.contacts.testing.ComposeTestContent
import com.bookverse.contacts.testing.ComposeTestHostActivity
import com.bookverse.contacts.ui.navigation.MainTabNavigationBar
import com.bookverse.contacts.ui.navigation.NavigationItem
import com.bookverse.contacts.ui.screens.tools.ToolsPresetSelector
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Focused guard for the core controls that must remain usable with TalkBack and large text. */
class AccessibilityCoreContractTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestHostActivity>()

    @Test
    fun navigationTargetsMeetMinimumTouchTarget() {
        composeRule.runOnUiThread {
            ComposeTestContent.show {
                MaterialTheme {
                    MainTabNavigationBar(
                        tabs = listOf(
                            NavigationItem.Favorites,
                            NavigationItem.Contacts,
                            NavigationItem.Recents,
                            NavigationItem.Extras,
                        ),
                        indicatorProgress = 0f,
                        backgroundColor = MaterialTheme.colorScheme.background,
                        onTabSelected = {},
                    )
                }
            }
        }

        assertMinimumTouchTargets()
    }

    @Test
    fun toolsPresetTargetsMeetMinimumTouchTarget() {
        composeRule.runOnUiThread {
            ComposeTestContent.show {
                MaterialTheme {
                    ToolsPresetSelector(
                        toolsOrder = ToolsOrderConfig.default(),
                        onPresetSelected = {},
                    )
                }
            }
        }

        assertMinimumTouchTargets()
    }

    private fun assertMinimumTouchTargets() {
        composeRule.waitForIdle()
        val minimumPixels = with(composeRule.density) { 48.dp.toPx() }
        composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
            assertTrue(
                "Clickable node is narrower than 48dp: ${node.boundsInRoot}",
                node.boundsInRoot.width >= minimumPixels,
            )
            assertTrue(
                "Clickable node is shorter than 48dp: ${node.boundsInRoot}",
                node.boundsInRoot.height >= minimumPixels,
            )
        }
    }
}
