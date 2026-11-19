package com.bookverse.contacts.ui.activities

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import androidx.activity.enableEdgeToEdge
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bookverse.contacts.App
import com.bookverse.contacts.MainActivity
import com.bookverse.contacts.domain.call.CallState
import com.bookverse.contacts.domain.call.IncomingCallKeyAction
import com.bookverse.contacts.domain.call.IncomingCallKeyPolicy
import com.bookverse.contacts.service.CallController
import com.bookverse.contacts.service.DefaultCallRepository
import com.bookverse.contacts.settings.Fonts
import com.bookverse.contacts.ui.screens.calls.IncomingCallScreen
import com.bookverse.contacts.ui.theme.ContactsAppTheme
import com.bookverse.contacts.ui.system.configureTransparentSystemBars
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.theme.ThemeMode
import kotlinx.coroutines.launch

class IncomingCallActivity : LocalizedAppCompatActivity() {

    companion object {
        const val EXTRA_CALL_ID = "call_id"
        const val EXTRA_PHONE_NUMBER = "phone_number"
    }

    private val appContainer by lazy { (application as App).container }
    private val appSettingsViewModel: AppSettingsViewModel by viewModels {
        appContainer.appSettingsViewModelFactory
    }
    private var answerWithVolumeUpEnabled = false
    private var declineWithVolumeDownEnabled = false
    private var currentCallId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setupWindowFlags()

        val callId = intent.getStringExtra(EXTRA_CALL_ID)
        val number = intent.getStringExtra(EXTRA_PHONE_NUMBER)
        currentCallId = callId

        setContent {
            val appSettingsViewModel = this@IncomingCallActivity.appSettingsViewModel

            androidx.compose.runtime.LaunchedEffect(Unit) {
                appSettingsViewModel.preloadSettings(this@IncomingCallActivity)
            }

            val themeMode by appSettingsViewModel.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val darkModeStyle by appSettingsViewModel.darkModeStyle.collectAsStateWithLifecycle()
            val accentBarStyle by appSettingsViewModel.accentBarStyle.collectAsStateWithLifecycle()
            val accentSeed by appSettingsViewModel.accentColor.collectAsStateWithLifecycle()
            val fontType by appSettingsViewModel.fontType.collectAsStateWithLifecycle()
            val fontFamily = androidx.compose.runtime.remember(fontType) { Fonts.getFontFamily(fontType) }
            val fontScale by appSettingsViewModel.fontScale.collectAsStateWithLifecycle()
            val glassEffects by appSettingsViewModel.glassEffects.collectAsStateWithLifecycle()

            ContactsAppTheme(
                accentSeed = accentSeed,
                fontFamily = fontFamily,
                fontScale = fontScale,
                themeMode = themeMode,
                darkModeStyle = darkModeStyle,
                accentBarStyle = accentBarStyle,
                glassEffects = glassEffects,
            ) {
                val systemBarBackground = MaterialTheme.colorScheme.background
                androidx.compose.runtime.LaunchedEffect(systemBarBackground) {
                    configureTransparentSystemBars(systemBarBackground)
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val contactsViewModel: ContactsViewModel = viewModel(
                        factory = appContainer.contactsViewModelFactory,
                    )
                    val callContactLookup = remember(contactsViewModel) {
                        appContainer.callContactLookupFactory.create(contactsViewModel)
                    }
                    contactsViewModel.loadContacts()

                    IncomingCallScreen(
                        callId = callId,
                        onAnswer = {
                            val i = Intent(this, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                putExtra("open_incall", true)
                                putExtra("incall_call_id", callId)
                                putExtra("incall_number", number)
                            }
                            startActivity(i)
                            finishAndRemoveTask()
                        },
                        onDismiss = {
                            finishAndRemoveTask()
                        },
                        onAutoMissed = {
                            finishAndRemoveTask()
                        },
                        callContactLookup = callContactLookup,
                        themeMode = themeMode,
                        appSettingsViewModel = appSettingsViewModel
                    )
                }
            }
        }

        lifecycleScope.launch {
            DefaultCallRepository.calls.collect { calls ->
                val trackedCallId = currentCallId ?: return@collect
                if (calls.none { it.id == trackedCallId && it.state != CallState.DISCONNECTED }) {
                    finishAndRemoveTask()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    appSettingsViewModel.answerWithVolumeUpEnabled.collect { enabled ->
                        answerWithVolumeUpEnabled = enabled
                    }
                }
                launch {
                    appSettingsViewModel.declineWithVolumeDownEnabled.collect { enabled ->
                        declineWithVolumeDownEnabled = enabled
                    }
                }
            }
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (handleIncomingCallKey(keyCode)) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun setupWindowFlags() {
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.requestDismissKeyguard(this, null)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun handleIncomingCallKey(keyCode: Int): Boolean {
        val action = IncomingCallKeyPolicy.actionFor(
            keyCode = keyCode,
            answerWithVolumeUp = answerWithVolumeUpEnabled,
            declineWithVolumeDown = declineWithVolumeDownEnabled,
        ) ?: return false
        val call = DefaultCallRepository.activeCall.value
        val trackedCallId = currentCallId
        val callId = when {
            call != null && call.state == CallState.RINGING -> call.id
            call == null && trackedCallId != null -> trackedCallId
            else -> return false
        }
        when (action) {
            IncomingCallKeyAction.ANSWER -> CallController.answer(callId)
            IncomingCallKeyAction.DECLINE -> CallController.reject(callId)
        }
        return true
    }
}
