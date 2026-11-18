package com.bookverse.contacts.widget

import com.bookverse.contacts.utils.PhoneActions
import com.bookverse.contacts.navigation.AppEntryPoints
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.bundleOf
import com.bookverse.contacts.App
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import com.bookverse.contacts.settings.AnimationSpeed.Companion.LocalAnimationSpeed
import com.bookverse.contacts.settings.Fonts
import com.bookverse.contacts.ui.activities.LocalizedAppCompatActivity
import com.bookverse.contacts.ui.screens.contacts.ContactDetailsScreen
import com.bookverse.contacts.ui.screens.contacts.UpdateContactScreen
import com.bookverse.contacts.ui.system.configureTransparentSystemBars
import com.bookverse.contacts.ui.theme.ContactsAppTheme
import com.bookverse.contacts.ui.theme.accentPalette
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel

class ContactDetailActivity : LocalizedAppCompatActivity() {

    private val appContainer by lazy { (application as App).container }
    private val appSettingsViewModel: AppSettingsViewModel by viewModels {
        appContainer.appSettingsViewModelFactory
    }
    private val contactsViewModel: ContactsViewModel by viewModels {
        appContainer.contactsViewModelFactory
    }

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val extras = intent?.extras ?: bundleOf()
        val contactId = extras.getString(EXTRA_CONTACT_ID)
        val fallbackName = extras.getString(EXTRA_CONTACT_NAME).orEmpty()
        val fallbackPhone = extras.getString(EXTRA_CONTACT_PHONE).orEmpty()
        val contactFromIntent: Contact? = extras.getContactCompat(EXTRA_CONTACT)
            ?: contactId?.takeIf { it.isNotBlank() }?.let {
                Contact(
                    id = it,
                    name = fallbackName,
                    phones = listOfNotNull(
                        fallbackPhone.takeIf { number -> number.isNotBlank() }
                            ?.let { number -> ContactPhone(number = number, type = null) }
                    )
                )
            }

        if (contactFromIntent == null && contactId.isNullOrBlank()) {
            finish()
            return
        }

        setContent {
            LaunchedEffect(Unit) {
                appSettingsViewModel.preloadSettings(this@ContactDetailActivity)
            }
            val themeMode by appSettingsViewModel.themeMode.collectAsStateWithLifecycle()
            val darkModeStyle by appSettingsViewModel.darkModeStyle.collectAsStateWithLifecycle()
            val accentSeed by appSettingsViewModel.accentColor.collectAsStateWithLifecycle()
            val accentBarStyle by appSettingsViewModel.accentBarStyle.collectAsStateWithLifecycle()
            val animationSpeed by appSettingsViewModel.animationSpeed.collectAsStateWithLifecycle()
            val fontType by appSettingsViewModel.fontType.collectAsStateWithLifecycle()
            val fontFamily = remember(fontType) { Fonts.getFontFamily(fontType) }
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
                val background = MaterialTheme.colorScheme.background
                CompositionLocalProvider(LocalAnimationSpeed provides animationSpeed) {
                    Surface(Modifier.fillMaxSize(), color = background) {
                        val contacts by contactsViewModel.contacts.collectAsStateWithLifecycle()
                        var displayedContact by remember(contactFromIntent) {
                            mutableStateOf(contactFromIntent)
                        }
                        val resolvedContact = remember(contacts, contactId, displayedContact) {
                            val targetId = contactId ?: displayedContact?.id.orEmpty()
                            contacts.firstOrNull { it.id == targetId && targetId.isNotBlank() }
                                ?: displayedContact
                        }
                        val statusBarBackground = if (resolvedContact != null && accentBarStyle == AccentBarStyle.RETRO) {
                            MaterialTheme.accentPalette.seed
                        } else {
                            background
                        }
                        LaunchedEffect(background, statusBarBackground) {
                            configureTransparentSystemBars(background, statusBarBackground)
                        }
                        var isEditing by rememberSaveable { mutableStateOf(false) }

                        AnimatedContent(
                            targetState = Pair(isEditing, resolvedContact),
                            transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                            label = "contact-detail-content"
                        ) { (editing, contact) ->
                            if (contact == null) {
                                MissingContactState(onClose = { this@ContactDetailActivity.finish() })
                            } else if (editing) {
                                UpdateContactScreen(
                                    onContactUpdated = { updated ->
                                        displayedContact = updated
                                        isEditing = false
                                    },
                                    onBackClick = { isEditing = false },
                                    contactToEdit = contact,
                                    appSettingsViewModel = appSettingsViewModel,
                                    contactsViewModel = contactsViewModel
                                )
                            } else {
                                ContactDetailsScreen(
                                    phoneActions = PhoneActions,
                                    contact = contact,
                                    onBackClick = { this@ContactDetailActivity.finish() },
                                    onEditContact = { latest ->
                                        displayedContact = latest
                                        isEditing = true
                                    },
                                    appSettingsViewModel = appSettingsViewModel,
                                    contactsViewModel = contactsViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MissingContactState(onClose: () -> Unit) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.widget_contact_not_found),
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onClose) {
                        Text(text = stringResource(R.string.close))
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun Bundle.getContactCompat(key: String): Contact? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelable(key, Contact::class.java)
        } else {
            getParcelable(key) as? Contact
        }

    companion object {
        const val EXTRA_CONTACT_ID = AppEntryPoints.EXTRA_CONTACT_ID
        const val EXTRA_CONTACT_NAME = AppEntryPoints.EXTRA_CONTACT_NAME
        const val EXTRA_CONTACT_PHONE = AppEntryPoints.EXTRA_CONTACT_PHONE
        const val EXTRA_CONTACT = "contact"
    }
}
