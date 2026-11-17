package com.bookverse.contacts.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.R
import com.bookverse.contacts.ui.components.AppScreenScaffold
import com.bookverse.contacts.ui.theme.contrastRatio
import com.bookverse.contacts.ui.theme.ContactsAppTheme
import com.bookverse.contacts.ui.viewmodel.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.NavigationViewModel
import com.bookverse.contacts.ui.viewmodel.ThemeMode
import com.bookverse.contacts.ui.viewmodel.DarkModeStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ModernPagerTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var pagerState: PagerState

    @Test
    fun selectedLabelKeepsContrastOnUniformGlass() {
        compose.setContent {
            ContactsAppTheme(themeMode = ThemeMode.LIGHT, accentBarStyle = AccentBarStyle.MODERN) {
                AppScreenScaffold(bottomBar = {
                    Box(Modifier.testTag("glass-dock")) {
                        MainTabNavigationBar(
                            listOf(NavigationItem.Favorites, NavigationItem.Contacts, NavigationItem.Recents, NavigationItem.Extras),
                            1f, MaterialTheme.colorScheme.background, {},
                        )
                    }
                }) { Box(Modifier.fillMaxSize()) }
            }
        }
        val dock = compose.onNodeWithTag("glass-dock")
        val origin = dock.fetchSemanticsNode().boundsInRoot.topLeft
        val selection = compose.onNodeWithText(text(R.string.tab_contacts)).fetchSemanticsNode().boundsInRoot
        val image = dock.captureToImage().toPixelMap()
        val pixels = buildList {
            for (y in (selection.top - origin.y).toInt() until (selection.bottom - origin.y).toInt()) {
                for (x in (selection.left - origin.x).toInt() until (selection.right - origin.x).toInt()) add(image[x, y])
            }
        }
        val foreground = pixels.maxBy { it.blue - it.red }
        val background = pixels.filter { maxOf(it.red, it.green, it.blue) - minOf(it.red, it.green, it.blue) <= 4f / 255f }
            .minBy { it.luminance() }
        assertTrue("Selected text must keep 4.5:1 contrast on the shaded glass", contrastRatio(foreground, background) >= 4.5f)
    }

    @Test
    fun glassSelectionUsesAccentOnlyForSelectedContent() {
        var accent by mutableStateOf(Color(0xFF2196F3))
        compose.setContent {
            ContactsAppTheme(themeMode = ThemeMode.LIGHT, accentSeed = accent, accentBarStyle = AccentBarStyle.MODERN) {
                Column(Modifier.width(360.dp).testTag("glass-dock")) {
                    MainTabNavigationBar(
                        listOf(NavigationItem.Favorites, NavigationItem.Contacts, NavigationItem.Recents, NavigationItem.Extras),
                        1f, MaterialTheme.colorScheme.background, {},
                    )
                }
            }
        }
        val dockBounds = compose.onNodeWithTag("glass-dock").fetchSemanticsNode().boundsInRoot
        val selectedBounds = compose.onNodeWithText(text(R.string.tab_contacts)).fetchSemanticsNode().boundsInRoot
        val before = compose.onNodeWithTag("glass-dock").captureToImage().toPixelMap()
        compose.runOnIdle { accent = Color(0xFFB76E56) }
        val after = compose.onNodeWithTag("glass-dock").captureToImage().toPixelMap()
        var changedPixels = 0
        for (y in 0 until before.height step 2) {
            for (x in 0 until before.width step 2) {
                if (before[x, y] != after[x, y]) {
                    assertTrue("Only selected content should use the accent", selectedBounds.contains(androidx.compose.ui.geometry.Offset(x + dockBounds.left, y + dockBounds.top)))
                    changedPixels++
                }
            }
        }
        assertTrue("The selected icon and label should respond to the accent", changedPixels > 10)
        compose.onNodeWithText(text(R.string.tab_contacts)).assertIsSelected()
    }

    @Test
    fun pressedTabFeedbackStaysInsideItsRoundedShape() {
        compose.setContent {
            ContactsAppTheme(themeMode = ThemeMode.LIGHT, accentBarStyle = AccentBarStyle.MODERN) {
                Column(Modifier.width(360.dp)) {
                    MainTabNavigationBar(
                        listOf(NavigationItem.Favorites, NavigationItem.Contacts, NavigationItem.Recents),
                        0f, MaterialTheme.colorScheme.background, {},
                    )
                }
            }
        }
        val tab = compose.onNodeWithText(text(R.string.tab_contacts))
        val before = tab.captureToImage().toPixelMap()
        compose.mainClock.autoAdvance = false
        try {
            tab.performTouchInput { down(center) }
            compose.mainClock.advanceTimeBy(250)
            val pressed = tab.captureToImage().toPixelMap()
            var changedPixels = 0
            for (y in 0 until before.height step 2) {
                for (x in 0 until before.width step 2) {
                    val cornerX = x < before.width / 10 || x >= before.width * 9 / 10
                    val cornerY = y < before.height / 10 || y >= before.height * 9 / 10
                    if (cornerX && cornerY) assertEquals("Press feedback leaked into a square corner", before[x, y], pressed[x, y])
                    if (before[x, y] != pressed[x, y]) changedPixels++
                }
            }
            assertTrue("Press feedback must remain visible inside the capsule", changedPixels > 10)
        } finally {
            tab.performTouchInput { up() }
            compose.mainClock.autoAdvance = true
        }
    }

    @Test
    fun allStylesAndThemesKeepTheSelectedTabAccessible() {
        var style by mutableStateOf(AccentBarStyle.MODERN)
        var theme by mutableStateOf(ThemeMode.LIGHT)
        var dark by mutableStateOf(DarkModeStyle.AMOLED)
        val tabs = listOf(NavigationItem.Favorites, NavigationItem.Contacts, NavigationItem.Recents, NavigationItem.Extras)
        compose.setContent {
            ContactsAppTheme(themeMode = theme, darkModeStyle = dark, accentBarStyle = style) {
                MainTabNavigationBar(tabs, 1f, MaterialTheme.colorScheme.background, {})
            }
        }
        for (variant in AccentBarStyle.entries) {
            for ((mode, surface) in listOf(ThemeMode.LIGHT to DarkModeStyle.AMOLED) + DarkModeStyle.entries.map { ThemeMode.DARK to it }) {
                compose.runOnIdle { style = variant; theme = mode; dark = surface }
                compose.onNodeWithText(text(R.string.tab_contacts)).assertIsSelected()
                for (label in tabs.map { it.titleResId }) {
                    val node = compose.onNodeWithText(text(label)).fetchSemanticsNode()
                    org.junit.Assert.assertTrue(node.boundsInRoot.width > 0)
                }
            }
        }
    }

    @Test
    fun tapThenSwipeKeepsRouteAndIndicatorTogether() {
        val navigation = NavigationViewModel(SavedStateHandle())
        showPager(navigation)
        compose.onNodeWithText(text(R.string.tab_extras)).performClick()
        compose.onNodeWithText(text(R.string.tab_extras)).assertIsSelected()
        compose.onNodeWithText(text(R.string.tab_contacts)).performClick()
        compose.onNodeWithTag("pages").performTouchInput { swipeLeft() }
        compose.onNodeWithText(text(R.string.tab_history)).assertIsSelected()
        compose.runOnIdle { assertEquals(NavigationItem.Recents.route, navigation.uiState.value.currentScreen) }
    }

    @Test
    fun tappingOriginalTabDuringAnimationSettlesWithoutAnOffset() {
        val navigation = NavigationViewModel(SavedStateHandle())
        showPager(navigation)
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText(text(R.string.tab_extras)).performClick()
        compose.mainClock.advanceTimeBy(80)
        compose.onNodeWithText(text(R.string.tab_contacts)).performClick()
        compose.mainClock.advanceTimeBy(800)
        compose.mainClock.autoAdvance = true
        compose.runOnIdle {
            assertEquals(1, pagerState.currentPage)
            assertEquals(0f, pagerState.currentPageOffsetFraction, 0.001f)
            assertEquals(NavigationItem.Contacts.route, navigation.uiState.value.currentScreen)
        }
    }

    private fun showPager(navigation: NavigationViewModel) {
        val tabs = listOf(NavigationItem.Favorites, NavigationItem.Contacts, NavigationItem.Recents, NavigationItem.Extras)
        compose.setContent {
            val route by navigation.uiState.collectAsState()
            val pager = rememberPagerState(initialPage = 1) { tabs.size }
            pagerState = pager
            val progress by remember { derivedStateOf { pager.currentPage + pager.currentPageOffsetFraction } }
            MainTabPagerSync(route.currentScreen, pager, tabs.map { it.route }, navigation)
            ContactsAppTheme(themeMode = ThemeMode.LIGHT, accentBarStyle = AccentBarStyle.MODERN) {
                Column(Modifier.fillMaxSize()) {
                    HorizontalPager(pager, Modifier.weight(1f).testTag("pages")) { Text("Page $it") }
                    MainTabNavigationBar(
                        tabs, progress,
                        MaterialTheme.colorScheme.background, { navigation.navigateTo(it.route) },
                    )
                }
            }
        }
    }

    @Test
    fun dockSupportsLargeTextAndHiddenTools() {
        compose.setContent {
            ContactsAppTheme(themeMode = ThemeMode.LIGHT, accentBarStyle = AccentBarStyle.MODERN, fontScale = 2f) {
                Column(Modifier.width(320.dp)) {
                    MainTabNavigationBar(
                        listOf(NavigationItem.Favorites, NavigationItem.Contacts, NavigationItem.Recents),
                        1f, MaterialTheme.colorScheme.background, {},
                    )
                }
            }
        }
        compose.onNodeWithText(text(R.string.tab_contacts)).assertIsSelected()
        compose.onNodeWithText(text(R.string.tab_extras)).assertDoesNotExist()
        for (label in listOf(R.string.tab_favorites, R.string.tab_contacts, R.string.tab_history)) {
            val bounds = compose.onNodeWithText(text(label)).fetchSemanticsNode().boundsInRoot
            val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
            org.junit.Assert.assertTrue(bounds.height / density >= 48f)
            org.junit.Assert.assertTrue(bounds.width / density >= 48f)
        }
    }

    private fun text(resource: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(resource)
}
