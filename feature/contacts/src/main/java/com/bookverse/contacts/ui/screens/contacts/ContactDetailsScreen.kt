package com.bookverse.contacts.ui.screens.contacts

import com.bookverse.contacts.utils.ContactDisplay
import com.bookverse.contacts.feature.contacts.R
import com.bookverse.contacts.utils.PhoneActionLauncher
import com.bookverse.contacts.ui.theme.SecondaryTextAlpha
import android.graphics.Bitmap
import android.media.RingtoneManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Whatsapp
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.data.model.RecentType
import com.bookverse.contacts.data.model.SocialMediaType
import com.bookverse.contacts.data.model.historyKey
import com.bookverse.contacts.settings.Countries
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.ButtonClick
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.components.FullScreenImage
import com.bookverse.contacts.ui.components.HistoryItem
import com.bookverse.contacts.ui.components.LoaderDialog
import com.bookverse.contacts.ui.components.OptionsDialog
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.TagChip
import com.bookverse.contacts.ui.components.getPhoneTypeLabel
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.utils.ContactUtils
import com.bookverse.contacts.utils.VibrationUtils
import com.bookverse.contacts.utils.avatarLabel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailsScreen(
    phoneActions: PhoneActionLauncher,
    contact: Contact,
    onBackClick: () -> Unit,
    onEditContact: (Contact) -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    contactsViewModel: ContactsViewModel,
    contactTagViewModel: com.bookverse.contacts.ui.viewmodel.ContactTagViewModel? = null
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val contactsState = contactsViewModel.contacts.collectAsStateWithLifecycle()
    val blockedNumbers by contactsViewModel.blockedNumbers.collectAsStateWithLifecycle()

    val navigationContact = contact
    val navigationNumbers = remember(navigationContact.phones) {
        navigationContact.phones.mapNotNull {
            normalizeContactDetailsPhone(it.number)
        }.toSet()
    }
    val contactFromState = remember(contactsState.value, navigationContact, navigationNumbers) {
        val byId = contactsState.value.find { existing ->
            navigationContact.id.isNotBlank() && existing.id == navigationContact.id
        }
        byId ?: if (navigationNumbers.isNotEmpty()) {
            contactsState.value.firstOrNull { candidate ->
                candidate.phones.any {
                    normalizeContactDetailsPhone(it.number) in navigationNumbers
                }
            }
        } else {
            null
        }
    } ?: navigationContact
    val normalizedNumbers = remember(contactFromState.phones, navigationNumbers) {
        val merged = mutableSetOf<String>()
        merged.addAll(
            contactFromState.phones.mapNotNull {
                normalizeContactDetailsPhone(it.number)
            },
        )
        merged.addAll(navigationNumbers)
        merged.toSet()
    }
    val isContactBlocked = remember(
        blockedNumbers,
        contactFromState.id,
        navigationContact.id,
        normalizedNumbers,
        contactFromState.isBlocked
    ) {
        val candidateIds = buildSet {
            if (contactFromState.id.isNotBlank()) add(contactFromState.id)
            if (navigationContact.id.isNotBlank()) add(navigationContact.id)
        }
        val blockedById = candidateIds.isNotEmpty() && blockedNumbers.any { info ->
            val infoId = info.contact?.id
            infoId != null && infoId in candidateIds
        }
        val blockedByNumber = normalizedNumbers.isNotEmpty() && blockedNumbers.any { info ->
            val normalized = normalizeContactDetailsPhone(
                info.normalizedNumber ?: info.displayNumber,
            )
            normalized != null && normalizedNumbers.contains(normalized)
        }
        contactFromState.isBlocked || blockedById || blockedByNumber
    }
    val contact = if (contactFromState.isBlocked == isContactBlocked) {
        contactFromState
    } else {
        contactFromState.copy(isBlocked = isContactBlocked)
    }

    val isFavorite = contact.isFavorite
    val contactAccounts = remember(contact.accounts) {
        contact.accounts.orEmpty().filterNot {
            it.name in ContactAccount.ACCOUNTS_BLACKLIST
        }.distinct()
    }

    val accentColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = textColor.copy(alpha = SecondaryTextAlpha)

    val vibrationDuration by appSettingsViewModel.vibrationDuration.collectAsStateWithLifecycle()
    val vibrationPattern by appSettingsViewModel.vibrationPattern.collectAsStateWithLifecycle()

    val contactTagsMap by contactTagViewModel?.contactTags?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(emptyMap()) }
    val currentContactTags = contactTagsMap[contact.id] ?: emptyList()

    LaunchedEffect(contact.id) {
        contactTagViewModel?.loadTagsForContacts(listOf(contact.id))
    }

    val localOperations by contactsViewModel.localOperations.collectAsStateWithLifecycle()
    val callLogs by contactsViewModel.callLogs.collectAsStateWithLifecycle()
    val contactRecentOperations = remember(localOperations, callLogs, contact) {
        (localOperations + callLogs).filter {
            it.contact.id == contact.id && contact.id.isNotEmpty()
        }.sortedByDescending { it.date }
    }
    var showAllRecent by remember { mutableStateOf(false) }
    var showConfirmDeleteHistory by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPhoneSelectionDialog by remember { mutableStateOf(false) }
    var showAccountsDialog by remember(contact.id, contactAccounts) { mutableStateOf(false) }
    var phoneAction by remember { mutableStateOf<(String) -> Unit>({}) }
    var showFullImage by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showQrLoader by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showShareOptionsDialog by remember { mutableStateOf(false) }
    var showVCardPhotoChoiceDialog by remember { mutableStateOf(false) }
    val snackbar = rememberAppSnackbarState()
    val pendingSnackbarEvents by contactsViewModel.pendingSnackbarEvents.collectAsStateWithLifecycle()
    val blockedNumbersSupported by remember { mutableStateOf(true) }

    fun performBlockAction(targetContact: Contact, shouldBlock: Boolean) {
        val resolvedContact = if (targetContact.id.isNotEmpty()) {
            contactsState.value.find { it.id == targetContact.id } ?: targetContact
        } else {
            targetContact
        }
        if (shouldBlock) {
            contactsViewModel.blockContact(resolvedContact)
        } else {
            contactsViewModel.unblockContact(resolvedContact)
        }
    }

    fun triggerBlockToggle() {
        if (!blockedNumbersSupported) {
            snackbar.show(
                message = resources.getString(R.string.contact_block_not_supported),
                type = SnackbarType.Warning
            )
            return
        }

        val shouldBlock = !contact.isBlocked

        performBlockAction(contact, shouldBlock)
    }

    DiagnosticLog.debug(DiagnosticEvent.CONTACT_DETAILS_OPENED)

    LaunchedEffect(Unit) {
        contactsViewModel.snackbarEvents.collect { ev ->
            snackbar.show(
                message = ev.message,
                type = ev.kind,
                actionLabel = ev.actionLabel
            )
        }
    }
    LaunchedEffect(pendingSnackbarEvents) {
        pendingSnackbarEvents.forEach { event ->
            snackbar.show(
                message = event.message,
                type = event.kind,
                actionLabel = event.actionLabel,
            )
        }
        contactsViewModel.consumePendingSnackbarEvents(pendingSnackbarEvents)
    }
    LaunchedEffect(localOperations, callLogs) {
        if (localOperations.isEmpty() && callLogs.isEmpty()) {
            contactsViewModel.fetchAllRecentOperations()
        }
    }

    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                expandable = true,
                title = stringResource(R.string.contact_details_title),
                onBackClick = {
                    if (showFullImage) showFullImage = false; else onBackClick()
                },
                actions = {
                    IconAction(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_contact_title),
                        onClick = { onEditContact(contact) }
                    )
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { AppSnackbarHost(snackbar.hostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            val photoBitmap = remember(contact.photoUri) {
                contact.photoUri?.let { contactsViewModel.uriToBitmap(it) }?.asImageBitmap()
            }
            val firstName = ContactDisplay.getFirstName(contact.name)
            val favoriteMessage = if (isFavorite) {
                stringResource(R.string.removed_from_favorites, firstName)
            } else {
                stringResource(R.string.added_to_favorites, firstName)
            }
            val favoriteDescription = if (isFavorite) {
                stringResource(R.string.remove_from_favorites, firstName)
            } else {
                stringResource(R.string.add_to_favorites, firstName)
            }
            val phoneCopied = stringResource(R.string.phone_copied)
            val emailCopied = stringResource(R.string.email_copied)
            val companyCopied = stringResource(R.string.company_copied)
            val websiteCopied = stringResource(R.string.website_copied)
            val websiteFallback = stringResource(CoreUiR.string.website)
            val ringtoneFallback = stringResource(R.string.ringtone)
            val locale = LocaleController.effectiveLocale(context)

            fun copyValue(value: String, message: String) {
                ContactUtils.copyToClipboard(context, value, toast = false)
                snackbar.show(message = message, type = SnackbarType.Success)
                VibrationUtils.vibrate(context, vibrationDuration, vibrationPattern)
            }

            fun runPhoneAction(action: (String) -> Unit) {
                if (contact.phones.size > 1) {
                    phoneAction = action
                    showPhoneSelectionDialog = true
                } else {
                    action(contact.phones.firstOrNull()?.number ?: "")
                }
            }

            val openWhatsApp: (String) -> Unit = { phoneNumber ->
                phoneActions.openWhatsApp(context, phoneNumber)
                contactsViewModel.addRecentOperation(
                    contact = contact,
                    type = RecentType.WHATSAPP_OPEN,
                    target = phoneNumber,
                )
            }

            val contactEmail = contact.email?.takeIf { it.isNotBlank() }
            val contactCompany = contact.company?.takeIf { it.isNotBlank() }
            val contactNotes = contact.notes?.takeIf { it.isNotBlank() }
            val contactBirthday = contact.birthday
            val hasRingtone = contact.ringtone.toString() != "null"
            val ringtoneName = if (hasRingtone) {
                remember(contact.ringtone) {
                    runCatching {
                        RingtoneManager.getRingtone(context, contact.ringtone)?.getTitle(context)
                    }.getOrNull()
                } ?: ringtoneFallback
            } else {
                null
            }

            val callIcon = rememberVectorPainter(Icons.Outlined.Call)
            val messageIcon = rememberVectorPainter(Icons.AutoMirrored.Outlined.Message)
            val whatsappIcon = rememberVectorPainter(Icons.Default.Whatsapp)
            val emailIcon = rememberVectorPainter(Icons.Outlined.Email)
            val favoriteIcon = rememberVectorPainter(
                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            )
            val companyIcon = rememberVectorPainter(Icons.Outlined.WorkOutline)
            val birthdayIcon = rememberVectorPainter(Icons.Outlined.Cake)
            val notesIcon = rememberVectorPainter(Icons.AutoMirrored.Outlined.StickyNote2)
            val ringtoneIcon = rememberVectorPainter(Icons.Outlined.MusicNote)
            val websiteIcon = rememberVectorPainter(Icons.Outlined.Public)
            val tagsIcon = rememberVectorPainter(Icons.AutoMirrored.Outlined.Label)
            val shareIcon = rememberVectorPainter(Icons.Outlined.Share)
            val blockIcon = rememberVectorPainter(Icons.Outlined.Block)
            val deleteIcon = rememberVectorPainter(Icons.Outlined.Delete)

            DiagnosticLog.debug(DiagnosticEvent.CONTACT_DETAILS_LOADED)

            LazyColumn(
                state = com.bookverse.contacts.ui.components.rememberDestinationListState(),
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = 32.dp,
                ),
            ) {
                item(key = "hero") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DetailHorizontalPadding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ContactDetailsAvatar(
                            photo = photoBitmap,
                            initials = contact.avatarLabel(),
                            fallbackColor = ContactDisplay.getRandomColor(contact.name),
                            initialsColor = textColor,
                            onPhotoClick = { showFullImage = true },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = textColor,
                        )
                        if (contactCompany != null) {
                            Text(
                                text = contactCompany,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = secondaryTextColor,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        if (contact.isBlocked) {
                            Spacer(modifier = Modifier.height(10.dp))
                            BlockedBadge()
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            ContactQuickAction(
                                icon = callIcon,
                                label = stringResource(CoreUiR.string.call),
                                contentDescription = stringResource(CoreUiR.string.call),
                                emphasized = true,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    runPhoneAction { phoneActions.callPhoneNumber(context, it) }
                                },
                            )
                            ContactQuickAction(
                                icon = messageIcon,
                                label = stringResource(R.string.contact_action_message),
                                contentDescription = stringResource(R.string.send_sms),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    runPhoneAction { phoneActions.openSmsApp(context, it) }
                                },
                            )
                            ContactQuickAction(
                                icon = whatsappIcon,
                                label = stringResource(CoreUiR.string.recent_type_whatsapp),
                                contentDescription = stringResource(CoreUiR.string.whatsapp_action),
                                modifier = Modifier.weight(1f),
                                onClick = { runPhoneAction(openWhatsApp) },
                            )
                            if (contactEmail != null) {
                                ContactQuickAction(
                                    icon = emailIcon,
                                    label = stringResource(R.string.email_placeholder),
                                    contentDescription = stringResource(R.string.send_email),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        ContactUtils.openUrl(context, "mailto:$contactEmail")
                                    },
                                )
                            }
                            ContactQuickAction(
                                icon = favoriteIcon,
                                label = stringResource(R.string.contact_action_favorite),
                                contentDescription = favoriteDescription,
                                selected = isFavorite,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    contactsViewModel.toggleFavorite(contact.id, favoriteMessage)
                                },
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (contact.phones.isNotEmpty() || contactEmail != null) {
                    item(key = "contact_info") {
                        Column {
                            DetailSectionDivider()
                            DetailSectionHeader(stringResource(R.string.contact_info_title))
                            contact.phones.forEachIndexed { index, phone ->
                                val phoneCountry =
                                    Countries.detectCountryFromPhoneNumber(phone.number)?.flag ?: "🌍"
                                DetailRow(
                                    icon = if (index == 0) callIcon else null,
                                    headline = "${phone.number} $phoneCountry",
                                    supporting = getPhoneTypeLabel(phone.type),
                                    onClick = { phoneActions.callPhoneNumber(context, phone.number) },
                                    onLongClick = { copyValue(phone.number, phoneCopied) },
                                    trailing = {
                                        IconButton(
                                            onClick = { phoneActions.openSmsApp(context, phone.number) },
                                        ) {
                                            Icon(
                                                painter = messageIcon,
                                                contentDescription = stringResource(R.string.send_sms),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                )
                            }
                            if (contactEmail != null) {
                                DetailRow(
                                    icon = emailIcon,
                                    headline = contactEmail,
                                    supporting = stringResource(R.string.email_placeholder),
                                    headlineMaxLines = 1,
                                    onClick = { ContactUtils.openUrl(context, "mailto:$contactEmail") },
                                    onLongClick = { copyValue(contactEmail, emailCopied) },
                                )
                            }
                        }
                    }
                }

                if (contactCompany != null || contactBirthday != null || contactNotes != null || hasRingtone) {
                    item(key = "about") {
                        Column {
                            DetailSectionDivider()
                            DetailSectionHeader(stringResource(R.string.about_contact_title, firstName))
                            if (contactCompany != null) {
                                DetailRow(
                                    icon = companyIcon,
                                    headline = contactCompany,
                                    supporting = stringResource(R.string.company_placeholder),
                                    headlineMaxLines = 1,
                                    onLongClick = { copyValue(contactCompany, companyCopied) },
                                )
                            }
                            if (contactBirthday != null) {
                                DetailRow(
                                    icon = birthdayIcon,
                                    headline = ContactDisplay.capitalizeFirstLetter(
                                        SimpleDateFormat("MMMM dd", locale).format(Date(contactBirthday)),
                                    ),
                                    supporting = stringResource(R.string.edit_birthday),
                                )
                                BirthdayReminderBadge(
                                    accentColor = accentColor,
                                    modifier = Modifier.padding(
                                        start = DetailContentInset,
                                        end = DetailHorizontalPadding,
                                        bottom = 8.dp,
                                    ),
                                )
                            }
                            if (contactNotes != null) {
                                DetailRow(
                                    icon = notesIcon,
                                    headline = contactNotes,
                                    supporting = stringResource(CoreUiR.string.notes_title),
                                )
                            }
                            if (ringtoneName != null) {
                                DetailRow(
                                    icon = ringtoneIcon,
                                    headline = ringtoneName,
                                    supporting = ringtoneFallback,
                                    headlineMaxLines = 1,
                                )
                            }
                        }
                    }
                }

                if (contact.socialMedia.isNotEmpty()) {
                    item(key = "websites") {
                        Column {
                            DetailSectionDivider()
                            DetailSectionHeader(stringResource(R.string.websites))
                            contact.socialMedia.forEach { socialNetworkUrl ->
                                val iconRes = SocialMediaType.getSocialMediaIcon(socialNetworkUrl)
                                DetailRow(
                                    icon = if (iconRes == SocialMediaType.WEBSITE_ICON) {
                                        websiteIcon
                                    } else {
                                        painterResource(id = iconRes)
                                    },
                                    headline = SocialMediaType.getSocialMediaName(
                                        socialNetworkUrl,
                                        websiteFallback,
                                    ),
                                    supporting = socialNetworkUrl,
                                    onClick = { ContactUtils.openUrl(context, socialNetworkUrl) },
                                    onLongClick = { copyValue(socialNetworkUrl, websiteCopied) },
                                )
                            }
                        }
                    }
                }

                if (currentContactTags.isNotEmpty()) {
                    item(key = "tags") {
                        Column {
                            DetailSectionDivider()
                            DetailSectionHeader(stringResource(R.string.tags_title))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = DetailHorizontalPadding,
                                        end = DetailHorizontalPadding,
                                        top = 8.dp,
                                        bottom = 8.dp,
                                    ),
                            ) {
                                Icon(
                                    painter = tagsIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .size(24.dp),
                                )
                                Spacer(modifier = Modifier.width(20.dp))
                                FlowRow(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    currentContactTags.forEach { tag ->
                                        TagChip(tag = tag, onRemove = null)
                                    }
                                }
                            }
                        }
                    }
                }

                if (contactRecentOperations.isNotEmpty()) {
                    item(key = "recent") {
                        Column {
                            DetailSectionDivider()
                            DetailSectionHeader(stringResource(R.string.recent_activity_title))
                            Column(modifier = Modifier.padding(horizontal = DetailHorizontalPadding)) {
                                contactRecentOperations.take(3).forEach { op ->
                                    HistoryItem(
                                        operation = op,
                                        textColor = textColor,
                                        secondaryTextColor = secondaryTextColor,
                                    )
                                }
                            }
                            if (contactRecentOperations.size > 3) {
                                TextButton(
                                    onClick = { showAllRecent = true },
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                ) {
                                    Text(text = stringResource(R.string.show_full_history))
                                }
                            }
                        }
                    }
                }

                item(key = "actions") {
                    Column {
                        DetailSectionDivider()
                        DetailRow(
                            icon = shareIcon,
                            headline = stringResource(CoreUiR.string.share),
                            onClick = { showShareOptionsDialog = true },
                        )
                        if (contact.phones.isNotEmpty() && blockedNumbersSupported) {
                            val errorColor = MaterialTheme.colorScheme.error
                            DetailRow(
                                icon = blockIcon,
                                headline = if (contact.isBlocked) {
                                    stringResource(CoreUiR.string.contact_unblock_action)
                                } else {
                                    stringResource(CoreUiR.string.contact_block_action)
                                },
                                headlineColor = if (contact.isBlocked) errorColor else textColor,
                                iconTint = if (contact.isBlocked) {
                                    errorColor
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                onClick = { triggerBlockToggle() },
                            )
                        }
                        if (contact.phones.isNotEmpty() && !blockedNumbersSupported) {
                            DetailRow(
                                icon = blockIcon,
                                headline = stringResource(R.string.contact_block_not_supported),
                                headlineColor = secondaryTextColor,
                            )
                        } else if (contact.phones.isEmpty()) {
                            DetailRow(
                                icon = blockIcon,
                                headline = stringResource(CoreUiR.string.contact_block_no_numbers),
                                headlineColor = secondaryTextColor,
                            )
                        }
                        DetailRow(
                            icon = deleteIcon,
                            headline = stringResource(CoreUiR.string.delete),
                            headlineColor = MaterialTheme.colorScheme.error,
                            iconTint = MaterialTheme.colorScheme.error,
                            onClick = { showDeleteDialog = true },
                        )
                    }
                }

                if (contactAccounts.isNotEmpty()) {
                    item(key = "accounts") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, start = DetailHorizontalPadding, end = DetailHorizontalPadding),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.contact_info),
                                style = MaterialTheme.typography.bodySmall,
                                color = secondaryTextColor,
                            )

                            DiagnosticLog.debug(DiagnosticEvent.CONTACT_ACCOUNT_SELECTION_RESOLVED)

                            if (contactAccounts.size == 1) {
                                Text(
                                    text = if (contactAccounts.first().isLocal) {
                                        stringResource(CoreUiR.string.device)
                                    } else {
                                        contactAccounts.first().displayName
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = textColor,
                                )
                            } else {
                                TextButton(onClick = { showAccountsDialog = true }) {
                                    Text(
                                        text = pluralStringResource(
                                            R.plurals.multiple_account,
                                            contactAccounts.size,
                                            contactAccounts.size,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showDeleteDialog) {
                ConfirmDialog(
                    onConfirm = {
                        contactsViewModel.deleteContact(
                            contactId = contact.id,
                            successMessage = resources.getString(
                                R.string.contact_deleted,
                                contact.name
                            ),
                            errorMessage = resources.getString(R.string.delete_contact_error)
                        ) { success ->
                            if (success) {
                                onBackClick()
                            }
                        }
                    },
                    onDismiss = { showDeleteDialog = false },
                    title = stringResource(R.string.delete_contact),
                    message = stringResource(R.string.delete_contact_confirm, contact.name),
                    toastMessage = null,
                )
            }

            if (showAccountsDialog && contactAccounts.size > 1) {
                ModalBottomSheet(
                    onDismissRequest = { showAccountsDialog = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.multiple_account_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        contactAccounts.forEach { account ->
                            TextButton(
                                onClick = { showAccountsDialog = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (account.isLocal) {
                                            Icons.Default.PhoneAndroid
                                        } else if (account.type == ContactAccount.WHATSAPP) {
                                            Icons.Default.Whatsapp
                                        } else {
                                            Icons.Default.Email
                                        },
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = if (account.isLocal) {
                                            stringResource(CoreUiR.string.device)
                                        } else {
                                            account.displayName
                                        },
                                        color = textColor,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            if (showPhoneSelectionDialog) {
                val phoneOptions = contact.phones.mapIndexed { index, phone ->
                    "#${index + 1}: ${phone.number}"
                }

                OptionsDialog(
                    title = stringResource(CoreUiR.string.select_phone_number),
                    options = phoneOptions,
                    onOptionSelected = { index: Int ->
                        phoneAction(contact.phones[index].number)
                        showPhoneSelectionDialog = false
                    },
                    onDismiss = {
                        showPhoneSelectionDialog = false
                    },
                )
            }

            if (showFullImage) {
                contact.photoUri?.let { uri ->
                    val bmpFull = remember(uri) { contactsViewModel.uriToBitmap(uri) }
                    bmpFull?.let { bitmap ->
                        FullScreenImage(
                            bitmap = bitmap.asImageBitmap(),
                            onDismiss = { showFullImage = false }
                        )
                    }
                }
            }

            if (qrBitmap != null && showQrDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showQrDialog = false
                        qrBitmap = null
                    },
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.share_contact),
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrBitmap!!.asImageBitmap(),
                                    contentDescription = stringResource(R.string.cd_qr_code),
                                    modifier = Modifier.size(250.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Button(
                                    onClick = {
                                        contactsViewModel.shareQRCode(qrBitmap!!)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = accentColor,
                                        contentColor = textColor
                                    ),
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(CoreUiR.string.share),
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    containerColor = MaterialTheme.colorScheme.surface,
                    textContentColor = textColor
                )
            }

            if (showShareOptionsDialog) {
                OptionsDialog(
                    title = stringResource(R.string.share_contact),
                    options = listOf(
                        stringResource(R.string.share_qr),
                        stringResource(R.string.share_vcard)
                    ),
                    onOptionSelected = { index ->
                        showShareOptionsDialog = false
                        when (index) {
                            0 -> {
                                coroutineScope.launch {
                                    qrBitmap = contactsViewModel.shareContactQR(contact)
                                    if (qrBitmap != null) {
                                        showQrDialog = true
                                    }
                                }
                            }

                            1 -> {
                                if (contact.photoUri != null) {
                                    showVCardPhotoChoiceDialog = true
                                } else {
                                    contactsViewModel.shareContactCard(
                                        contact,
                                        context,
                                        includePhoto = false
                                    )
                                }
                            }
                        }
                    },
                    onDismiss = {
                        showShareOptionsDialog = false
                    },
                )
            }

            if (showVCardPhotoChoiceDialog) {
                OptionsDialog(
                    title = stringResource(R.string.share_vcard_include_photo_title),
                    options = listOf(
                        stringResource(R.string.share_vcard_with_photo),
                        stringResource(R.string.share_vcard_without_photo)
                    ),
                    onOptionSelected = { idx ->
                        showVCardPhotoChoiceDialog = false
                        contactsViewModel.shareContactCard(
                            contact,
                            context,
                            includePhoto = idx == 0
                        )
                    },
                    onDismiss = { showVCardPhotoChoiceDialog = false },
                )
            }

            if (showQrLoader) {
                LoaderDialog(
                    message = stringResource(R.string.creating_qr),
                )
            }

            if (showAllRecent) {
                Dialog(
                    onDismissRequest = { showAllRecent = false },
                    properties = androidx.compose.ui.window.DialogProperties(
                        usePlatformDefaultWidth = false,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.full_history,
                                        contactRecentOperations.size,
                                        contactRecentOperations.size,
                                        ContactDisplay.getFirstName(contact.name),
                                    ),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(contactRecentOperations, key = { it.historyKey }) { op ->
                                        HistoryItem(
                                            operation = op,
                                            textColor = textColor,
                                            secondaryTextColor = secondaryTextColor
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {

                                ButtonClick(
                                    onClick = { showAllRecent = false },
                                    modifier = Modifier.weight(1f),
                                    text = stringResource(CoreUiR.string.understood),
                                    fontSize = 18
                                )

                                Button(
                                    onClick = { showConfirmDeleteHistory = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Text(
                                        text = stringResource(CoreUiR.string.delete),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showConfirmDeleteHistory) {
                val successMessage = stringResource(R.string.delete_contact_full_history_success)
                ConfirmDialog(
                    onConfirm = {
                        coroutineScope.launch {
                            contactsViewModel.deleteRecentOperations(contactRecentOperations)
                            showConfirmDeleteHistory = false
                            showAllRecent = false
                            snackbar.show(
                                message = successMessage,
                                type = SnackbarType.Success
                            )
                        }
                    },
                    onDismiss = { showConfirmDeleteHistory = false },
                    title = stringResource(R.string.delete_contact_full_history_title),
                    message = stringResource(
                        R.string.delete_contact_full_history_message,
                        contact.name
                    ),
                )
            }
        }
    }
}
