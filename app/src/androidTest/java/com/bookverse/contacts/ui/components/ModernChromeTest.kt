package com.bookverse.contacts.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.ui.theme.ContactsAppTheme
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class ModernChromeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun destinationHeaderOffsetsSurviveRecreation() {
        val restoration = StateRestorationTester(compose)
        lateinit var state: TopAppBarState
        restoration.setContent {
            state = rememberAppBarStateRegistry().stateFor("contacts")
            Text("Restorable header")
        }
        compose.runOnIdle {
            state.heightOffsetLimit = -100f
            state.heightOffset = -80f
            state.contentOffset = -240f
        }
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            assertEquals(-80f, state.heightOffset, 0f)
            assertEquals(-240f, state.contentOffset, 0f)
        }
    }

    @Test
    fun titleStartsCompactAndExpandsOnlyWhenPullingAtTheStart() {
        lateinit var bar: TopAppBarState
        showList(expandable = true, onState = { bar = it })
        val shortWindow = InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.screenHeightDp < 480
        compose.runOnIdle { assertEquals(if (shortWindow) 0f else 1f, bar.collapsedFraction, 0.01f) }
        compose.onNodeWithTag("synthetic-list").performTouchInput {
            swipe(center, Offset(center.x, height * 0.9f), durationMillis = 600)
        }
        compose.runOnIdle { assertEquals(0f, bar.collapsedFraction, 0.01f) }
        compose.onNodeWithTag("synthetic-list").performTouchInput { swipeUp(durationMillis = 80) }
        compose.runOnIdle {
            if (shortWindow) {
                assertEquals(0f, bar.heightOffset, 0f)
            } else {
                assertTrue(bar.collapsedFraction > 0.95f)
            }
        }
        compose.onNodeWithTag("synthetic-list").performScrollToIndex(40)
        compose.onNodeWithTag("synthetic-list").performTouchInput {
            swipe(center, center + Offset(0f, height * 0.1f), durationMillis = 500)
        }
        compose.runOnIdle { assertEquals(if (shortWindow) 0f else 1f, bar.collapsedFraction, 0.01f) }
        compose.onNodeWithTag("synthetic-list").performScrollToIndex(0)
        compose.onNodeWithTag("synthetic-list").performTouchInput {
            swipe(center, Offset(center.x, height * 0.9f), durationMillis = 600)
        }
        compose.runOnIdle { assertEquals(0f, bar.collapsedFraction, 0.01f) }
    }

    @Test
    fun compactEditorBarDoesNotConsumeTheFormsScroll() {
        var firstVisible = { 0 }
        compose.setContent {
            ContactsAppTheme(themeMode = ThemeMode.LIGHT, accentBarStyle = AccentBarStyle.MODERN) {
                val state = rememberLazyListState()
                firstVisible = { state.firstVisibleItemIndex }
                val bar = rememberAppBarScrollBehavior(expandable = false)
                AppScreenScaffold(
                    modifier = Modifier.nestedScroll(bar.nestedScrollConnection),
                    topBar = { ModernTopBar("Editor", bar, expandable = false) },
                ) { padding ->
                    LazyColumn(Modifier.testTag("form").fillMaxSize(), state = state, contentPadding = padding) {
                        items(100) { Text("Field $it", Modifier.heightIn(min = 64.dp).padding(16.dp)) }
                    }
                }
            }
        }
        compose.onNodeWithTag("form").performTouchInput { swipeUp() }
        compose.runOnIdle { assertTrue(firstVisible() > 0) }
    }

    @Test
    fun searchBarStaysPinnedDuringScroll() {
        lateinit var bar: TopAppBarState
        showList(expandable = true, search = true, onState = { bar = it })
        var initialOffset = 0f
        compose.runOnIdle { initialOffset = bar.heightOffset }
        compose.onNodeWithTag("synthetic-list").performTouchInput { swipeUp() }
        compose.runOnIdle { assertEquals(initialOffset, bar.heightOffset, 0f) }
    }

    @Test
    fun titleActionsAndSearchMoveTogetherWhenExpanded() {
        showList(expandable = true, searchEntry = true, onState = {})
        fun titleY() = compose.onNodeWithText("Contacts").fetchSemanticsNode().boundsInRoot.center.y
        fun actionY() = compose.onNodeWithContentDescription("Filter").fetchSemanticsNode().boundsInRoot.center.y
        fun searchY() = compose.onNodeWithText("Search contacts").fetchSemanticsNode().boundsInRoot.center.y
        val titleStart = titleY()
        val actionStart = actionY()
        val searchStart = searchY()
        compose.onNodeWithTag("synthetic-list").performTouchInput {
            swipe(center, Offset(center.x, height * 0.9f), durationMillis = 600)
        }
        val titleTravel = titleY() - titleStart
        val actionTravel = actionY() - actionStart
        assertEquals("Actions must follow the title", titleTravel, actionTravel, 2f)
        assertTrue("The search bar must descend with the title", searchY() - searchStart >= titleTravel - 2f)
        val shortWindow = InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.screenHeightDp < 480
        if (!shortWindow) assertTrue("Expanded controls must be easier to reach", actionTravel > 80f)
    }

    @Test
    fun searchFocusAndClosePreserveTheExpandedHeader() {
        var active by mutableStateOf(false)
        var query by mutableStateOf("")
        compose.setContent {
            ContactsAppTheme(themeMode = ThemeMode.LIGHT, accentBarStyle = AccentBarStyle.MODERN) {
                val bar = rememberAppBarScrollBehavior { !active }
                val focus = remember { FocusRequester() }
                LaunchedEffect(active) { if (active) focus.requestFocus() }
                AppScreenScaffold(
                    modifier = Modifier.nestedScroll(bar.nestedScrollConnection),
                    topBar = {
                        ModernTopBar(
                            "Contacts", bar, expandable = true,
                            searchAction = AppBarSearchAction("Search contacts", { active = true }),
                            searchContent = if (active) ({
                                TopBarSearchField(
                                    value = query, onValueChange = { query = it }, placeholder = "Search contacts",
                                    focusRequester = focus, onClose = { active = false }, clearContentDescription = "Clear search",
                                )
                            }) else null,
                        )
                    },
                ) { padding ->
                    LazyColumn(Modifier.testTag("search-results").fillMaxSize(), contentPadding = padding) {
                        items(100) { Text("Result $it", Modifier.heightIn(min = 64.dp).padding(16.dp)) }
                    }
                }
            }
        }
        compose.onNodeWithTag("search-results").performTouchInput {
            swipe(center, Offset(center.x, height * 0.9f), durationMillis = 600)
        }
        val expandedY = compose.onNodeWithText("Contacts").fetchSemanticsNode().boundsInRoot.center.y
        val idleFieldHeight = compose.onNodeWithText("Search contacts").fetchSemanticsNode().boundsInRoot.height
        compose.onNodeWithText("Search contacts").performClick()
        compose.onNode(hasSetTextAction()).assertIsFocused().performTextInput("9")
        val activeField = compose.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInRoot
        assertEquals("Opening search must preserve the field height", idleFieldHeight, activeField.height, 2f)
        compose.onNodeWithTag("search-results").performTouchInput {
            swipe(Offset(center.x, height * 0.55f), Offset(center.x, height * 0.4f), durationMillis = 300)
        }
        assertEquals(activeField.top, compose.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInRoot.top, 1f)
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.runOnIdle { assertTrue(active); assertEquals("", query) }
        compose.onNodeWithContentDescription("Clear search").performClick()
        compose.runOnIdle { assertTrue(!active) }
        assertEquals(expandedY, compose.onNodeWithText("Contacts").fetchSemanticsNode().boundsInRoot.center.y, 1f)
    }

    private fun showList(expandable: Boolean, search: Boolean = false, searchEntry: Boolean = false, onState: (TopAppBarState) -> Unit) {
        compose.setContent {
            ContactsAppTheme(themeMode = ThemeMode.LIGHT, accentBarStyle = AccentBarStyle.MODERN) {
                CompositionLocalProvider(LocalBackdropAllowed provides false) {
                    val bar = rememberAppBarScrollBehavior(expandable) { !search }
                    onState(bar.state)
                    AppScreenScaffold(
                        modifier = Modifier.nestedScroll(bar.nestedScrollConnection),
                        topBar = {
                            ModernTopBar(
                                "Contacts", bar, expandable,
                                searchContent = if (search) ({ Text("Search field") }) else null,
                                searchAction = if (searchEntry) AppBarSearchAction("Search contacts", {}) else null,
                                actions = if (searchEntry) ({ IconAction(Icons.Default.FilterList, "Filter", {}) }) else null,
                            )
                        },
                    ) { padding ->
                        LazyColumn(Modifier.testTag("synthetic-list").fillMaxSize(), contentPadding = padding) {
                            items(2_000, key = { it }) { Text("Sample contact $it", Modifier.heightIn(min = 64.dp).padding(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}
