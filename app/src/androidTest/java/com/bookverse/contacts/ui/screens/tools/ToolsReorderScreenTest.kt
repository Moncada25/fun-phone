package com.bookverse.contacts.ui.screens.tools

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.R
import com.bookverse.contacts.settings.AppSettingsStore
import com.bookverse.contacts.ui.components.AppBarStateRegistry
import com.bookverse.contacts.ui.components.LocalAppBarRoute
import com.bookverse.contacts.ui.components.LocalAppBarStateRegistry
import com.bookverse.contacts.ui.components.LocalBackdropAllowed
import com.bookverse.contacts.ui.components.rememberAppBarStateRegistry
import com.bookverse.contacts.ui.theme.ContactsAppTheme
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class ToolsReorderScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val settings = AppSettingsViewModel(MemorySettingsStore())
    private lateinit var registry: AppBarStateRegistry
    private var style by mutableStateOf(AccentBarStyle.MODERN)
    private val list get() = compose.onNode(hasScrollToIndexAction())
    private val bar get() = registry.stateFor("${style.name}/tools-order")
    private val shortWindow get() = context.resources.configuration.screenHeightDp < 480

    @Test
    fun headerFollowsRealListAndRestoresDestinationOffsets() {
        val restoration = showScreen()
        val initialY = titleY()
        compose.runOnIdle { assertEquals(if (shortWindow) 0f else 1f, bar.collapsedFraction, 0.01f) }
        pullAtStart()
        if (!shortWindow) assertTrue(titleY() > initialY + 50f)
        list.performTouchInput { swipeUp() }
        assertEquals(initialY, titleY(), 1f)
        list.performScrollToIndex(7)
        val (index, offset, height) = compose.runOnIdle {
            val state = registry.listFor("tools-order/content")
            Triple(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset, bar.heightOffset)
        }
        assertTrue(index > 0)
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            assertEquals(index, registry.listFor("tools-order/content").firstVisibleItemIndex)
            assertEquals(offset, registry.listFor("tools-order/content").firstVisibleItemScrollOffset)
            assertEquals(height, bar.heightOffset, 0f)
        }
    }

    @Test
    fun draggingReordersToolsWithoutMovingTheHeader() {
        showScreen()
        list.performScrollToIndex(3)
        val before = settings.toolsOrder.value.items
        val notes = compose.onNodeWithText(text(R.string.notes_title))
        val expense = compose.onNodeWithText(text(R.string.expense_manager_item_title))
        val distance = expense.fetchSemanticsNode().boundsInRoot.center.y - notes.fetchSemanticsNode().boundsInRoot.center.y
        val headerY = titleY()
        notes.performTouchInput {
            down(center)
            advanceEventTime(700)
            moveTo(center + Offset(0f, distance), delayMillis = 150)
        }
        assertEquals(headerY, titleY(), 1f)
        notes.performTouchInput { up() }
        compose.runOnIdle {
            val after = settings.toolsOrder.value.items
            assertEquals(before.toSet(), after.toSet())
            assertTrue(after.indexOf("notes") > after.indexOf("expense_manager"))
        }
    }

    @Test
    fun minimalAndRetroKeepTheirFixedHeaders() {
        showScreen()
        for (variant in listOf(AccentBarStyle.MINIMAL, AccentBarStyle.RETRO)) {
            compose.runOnIdle {
                style = variant
                settings.setAccentBarStyle(context, variant)
            }
            list.performScrollToIndex(0)
            val initialY = titleY()
            pullAtStart()
            assertEquals(initialY, titleY(), 1f)
            list.performTouchInput { swipeUp() }
            assertEquals(initialY, titleY(), 1f)
        }
    }

    private fun showScreen(): StateRestorationTester {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            ContactsAppTheme(themeMode = ThemeMode.SYSTEM, accentBarStyle = style) {
                registry = rememberAppBarStateRegistry()
                CompositionLocalProvider(
                    LocalAppBarStateRegistry provides registry,
                    LocalAppBarRoute provides "tools-order",
                    LocalBackdropAllowed provides false,
                ) {
                    ToolsReorderScreen(settings, {})
                }
            }
        }
        compose.waitForIdle()
        return restoration
    }

    private fun pullAtStart() = list.performTouchInput {
        swipe(Offset(center.x, height * 0.2f), Offset(center.x, height * 0.6f), durationMillis = 600)
    }

    private fun titleY() = compose.onNodeWithText(text(R.string.tools_order_title)).fetchSemanticsNode().boundsInRoot.center.y
    private fun text(resource: Int) = context.getString(resource)

    private class MemorySettingsStore : AppSettingsStore, DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        private val mutex = Mutex()
        override fun dataStore(context: Context): DataStore<Preferences> = this
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = mutex.withLock {
            transform(data.value).also { data.value = it }
        }
    }
}
