package com.bookverse.contacts.ui.components

import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.testing.ComposeTestContent
import com.bookverse.contacts.testing.ComposeTestHostActivity
import com.bookverse.contacts.ui.auth.ProtectedScreenHost
import com.bookverse.contacts.ui.auth.rememberPasswordAuthState
import com.bookverse.contacts.ui.navigation.allowsBackdrop
import com.bookverse.contacts.ui.theme.ContactsAppTheme
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.theme.ThemeMode
import com.bookverse.contacts.utils.SecondaryScreen
import dev.chrisbanes.haze.HazeState
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BackdropLifecycleTest {
    @get:Rule val compose = createAndroidComposeRule<ComposeTestHostActivity>()
    private val models = ViewModelStore()
    @After fun clearModels() { compose.runOnIdle { models.clear() } }

    @Test
    fun backgroundingDropsTheBackdropAndReturningCreatesANewOne() {
        lateinit var owner: TestOwner
        var backdrop: HazeState? = null
        compose.runOnUiThread {
            owner = TestOwner()
            ComposeTestContent.show {
                ContactsAppTheme(themeMode = ThemeMode.SYSTEM, accentBarStyle = AccentBarStyle.MODERN) {
                    CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                        AppScreenScaffold {
                            val current = LocalAppBackdrop.current
                            SideEffect { backdrop = current }
                            Box(Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
        val first = compose.runOnIdle { assertNotNull(backdrop); backdrop }
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        compose.runOnIdle { assertNull(backdrop) }
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.runOnIdle { assertNotNull(backdrop); assertNotSame(first, backdrop) }
    }

    @Test
    fun authenticatedGlassIsRemovedOnLockAndSecureWindowsRemainProtected() {
        val settings = AppSettingsViewModel(GlassSettingsStore())
        lateinit var owner: TestOwner
        var route by mutableStateOf(SecondaryScreen.PASSWORD_LIST)
        var backdrop: HazeState? = null
        var lockEvents = 0
        compose.runOnUiThread {
            models.put("settings", settings)
            settings.setPasswordManagerEnabled(compose.activity, true)
            settings.setSensitiveSectionsLock(compose.activity, true)
            owner = TestOwner()
            ComposeTestContent.show {
                ContactsAppTheme(themeMode = ThemeMode.SYSTEM, accentBarStyle = AccentBarStyle.MODERN) {
                    CompositionLocalProvider(LocalLifecycleOwner provides owner, LocalBackdropAllowed provides route.allowsBackdrop()) {
                        ProtectedScreenHost(route, false, settings, rememberPasswordAuthState(settings), {}, { lockEvents++ }) {
                            AppScreenScaffold {
                                val current = LocalAppBackdrop.current
                                SideEffect { backdrop = current }
                                DisposableEffect(Unit) { onDispose { backdrop = null } }
                                Text("Synthetic protected content")
                            }
                        }
                    }
                }
            }
        }
        for (screen in listOf(SecondaryScreen.PASSWORD_LIST, SecondaryScreen.EXPENSES)) {
            compose.runOnIdle { route = screen }
            compose.onNodeWithText("Synthetic protected content").assertDoesNotExist()
            compose.runOnIdle { assertNull(backdrop); assertTrue(isSecure()) }
            compose.runOnIdle { settings.unlockProtectedSession() }
            compose.onNodeWithText("Synthetic protected content").assertIsDisplayed()
            compose.runOnIdle { assertNotNull(backdrop); assertTrue(isSecure()) }
            compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
            compose.runOnIdle { assertFalse(settings.protectedSessionUnlocked.value); assertNull(backdrop); assertTrue(isSecure()) }
            compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
            compose.onNodeWithText(compose.activity.getString(R.string.auth_required)).assertIsDisplayed()
        }
        compose.runOnIdle { assertTrue(lockEvents >= 2); route = SecondaryScreen.DETAIL }
        compose.runOnIdle { assertFalse(isSecure()) }
    }

    private fun isSecure() = compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
    private class TestOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
    }
}
