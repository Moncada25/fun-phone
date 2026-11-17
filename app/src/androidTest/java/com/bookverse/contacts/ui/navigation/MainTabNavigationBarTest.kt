package com.bookverse.contacts.ui.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bookverse.contacts.R
import com.bookverse.contacts.testing.ComposeTestContent
import com.bookverse.contacts.testing.ComposeTestHostActivity
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainTabNavigationBarTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComposeTestHostActivity>()

    @Test
    fun selectedTabIsAnnouncedAndClickEmitsTheRequestedRoute() {
        val selectedRoute = AtomicReference<String>()
        showTabs(
            tabs = allTabs,
            indicatorProgress = 1f,
            onTabSelected = { selectedRoute.set(it.route) },
        )

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.tab_contacts))
            .assertIsSelected()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.tab_history))
            .performClick()

        assertEquals(NavigationItem.Recents.route, selectedRoute.get())
    }

    @Test
    fun hiddenToolsConfigurationDoesNotExposeTheToolsTab() {
        showTabs(
            tabs = allTabs.dropLast(1),
            indicatorProgress = 0f,
            onTabSelected = {},
        )

        assertEquals(
            0,
            composeRule
                .onAllNodesWithText(composeRule.activity.getString(R.string.tab_extras))
                .fetchSemanticsNodes()
                .size,
        )
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.tab_favorites))
            .assertIsSelected()
    }

    private fun showTabs(
        tabs: List<NavigationItem>,
        indicatorProgress: Float,
        onTabSelected: (NavigationItem) -> Unit,
    ) {
        composeRule.runOnUiThread {
            ComposeTestContent.show {
                MaterialTheme {
                    MainTabNavigationBar(
                        tabs = tabs,
                        indicatorProgress = indicatorProgress,
                        backgroundColor = MaterialTheme.colorScheme.background,
                        onTabSelected = onTabSelected,
                    )
                }
            }
        }
    }

    private companion object {
        val allTabs = listOf(
            NavigationItem.Favorites,
            NavigationItem.Contacts,
            NavigationItem.Recents,
            NavigationItem.Extras,
        )
    }
}
