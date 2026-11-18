package com.bookverse.contacts.ui.screens.contacts

import com.bookverse.contacts.utils.PhoneActions
import com.bookverse.contacts.ui.theme.SecondaryTextAlpha
import android.graphics.Bitmap
import android.media.RingtoneManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Whatsapp
import androidx.compose.material.icons.filled.WorkOutline
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.data.model.RecentType
import com.bookverse.contacts.data.model.SocialMediaType
import com.bookverse.contacts.data.model.historyKey
import com.bookverse.contacts.settings.Countries
import com.bookverse.contacts.ui.components.ActionIcon
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailsScreen(
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
                appSettingsViewModel = appSettingsViewModel,
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
            LazyColumn(
                state = com.bookverse.contacts.ui.components.rememberDestinationListState(),
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(
                    top = paddingValues.calculateTopPadding() + 24.dp,
                    bottom = 24.dp,
                    start = 24.dp,
                    end = 24.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxSize(0.55f)
                            .aspectRatio(1f),
                        shape = CircleShape,
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 8.dp
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val contactPhotoUri = contact.photoUri
                            if (contactPhotoUri != null) {

                                val bmp = remember(contact.photoUri) {
                                    contactsViewModel.uriToBitmap(contactPhotoUri)
                                }

                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { showFullImage = true },
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(ContactUtils.getRandomColor(contact.name)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = contact.avatarLabel(),
                                            color = textColor,
                                            fontSize = 100.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(ContactUtils.getRandomColor(contact.name)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.avatarLabel(),
                                        color = textColor,
                                        fontSize = 100.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center,
                            color = textColor
                        )
                        if (contact.isBlocked) {
                            Spacer(modifier = Modifier.height(8.dp))
                            BlockedBadge()
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (!contact.email.isNullOrBlank()) {
                            ActionIcon(
                                tooltip = stringResource(R.string.send_email),
                                accentColor = accentColor,
                                icon = Icons.Default.Email,
                                contentDescription = stringResource(R.string.send_email),
                                textColor = textColor
                            ) {
                                ContactUtils.openUrl(
                                    context,
                                    "mailto:${contact.email}"
                                )
                            }
                        }

                        ActionIcon(
                            tooltip = stringResource(R.string.whatsapp_action),
                            accentColor = accentColor,
                            icon = Icons.Default.Whatsapp,
                            contentDescription = stringResource(R.string.whatsapp_action),
                            textColor = textColor
                        ) {
                            if (contact.phones.size > 1) {
                                phoneAction = { phoneNumber ->
                                    PhoneActions.openWhatsApp(context, phoneNumber)
                                    contactsViewModel.addRecentOperation(
                                        contact = contact,
                                        type = RecentType.WHATSAPP_OPEN,
                                        target = phoneNumber
                                    )
                                }
                                showPhoneSelectionDialog = true
                            } else {
                                val primaryPhone = contact.phones.firstOrNull()?.number ?: ""
                                PhoneActions.openWhatsApp(context, primaryPhone)
                                contactsViewModel.addRecentOperation(
                                    contact = contact,
                                    type = RecentType.WHATSAPP_OPEN,
                                    target = primaryPhone
                                )
                            }
                        }

                        val firstName = ContactUtils.getFirstName(contact.name)
                        val message = if (isFavorite) {
                            stringResource(
                                R.string.removed_from_favorites, firstName
                            )
                        } else {
                            stringResource(
                                R.string.added_to_favorites, firstName
                            )
                        }
                        val tooltip = if (isFavorite) {
                            stringResource(R.string.remove_from_favorites, firstName)
                        } else {
                            stringResource(R.string.add_to_favorites, firstName)
                        }

                        ActionIcon(
                            tooltip = tooltip,
                            accentColor = accentColor,
                            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(R.string.favorites_title),
                            textColor = textColor
                        ) {
                            contactsViewModel.toggleFavorite(contact.id, message)
                        }

                        ActionIcon(
                            tooltip = stringResource(R.string.call),
                            accentColor = accentColor,
                            icon = Icons.Outlined.Call,
                            contentDescription = stringResource(R.string.call),
                            textColor = textColor
                        ) {
                            if (contact.phones.size > 1) {
                                phoneAction = { phoneNumber ->
                                    PhoneActions.callPhoneNumber(context, phoneNumber)
                                }
                                showPhoneSelectionDialog = true
                            } else {
                                val primaryPhone = contact.phones.firstOrNull()?.number ?: ""
                                PhoneActions.callPhoneNumber(context, primaryPhone)
                            }
                        }

                        ActionIcon(
                            tooltip = stringResource(R.string.send_sms),
                            accentColor = accentColor,
                            icon = Icons.AutoMirrored.Filled.Message,
                            contentDescription = stringResource(R.string.send_sms),
                            textColor = textColor
                        ) {
                            if (contact.phones.size > 1) {
                                phoneAction = { phoneNumber ->
                                    PhoneActions.openSmsApp(context, phoneNumber)
                                }
                                showPhoneSelectionDialog = true
                            } else {
                                val primaryPhone = contact.phones.firstOrNull()?.number ?: ""
                                PhoneActions.openSmsApp(context, primaryPhone)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }

                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.contact_info_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            val phoneCopied = stringResource(R.string.phone_copied)

                            contact.phones.forEachIndexed { index, phone ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onLongPress = {
                                                    ContactUtils.copyToClipboard(
                                                        context,
                                                        phone.number,
                                                        toast = false
                                                    )
                                                    snackbar.show(
                                                        message = phoneCopied,
                                                        type = SnackbarType.Success
                                                    )
                                                    VibrationUtils.vibrate(
                                                        context, vibrationDuration, vibrationPattern
                                                    )
                                                }
                                            )
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (index == 0) {
                                            Icon(
                                                imageVector = Icons.Outlined.Call,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.width(24.dp))
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            val phoneCountry =
                                                Countries.detectCountryFromPhoneNumber(
                                                    phone.number
                                                )?.flag ?: "🌍"

                                            Text(
                                                text = "${phone.number} $phoneCountry",
                                                color = textColor,
                                                fontSize = 16.sp
                                            )
                                            Text(
                                                text = getPhoneTypeLabel(phone.type),
                                                color = secondaryTextColor,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            val contactEmail = contact.email
                            if (!contactEmail.isNullOrBlank()) {
                                val emailCopied = stringResource(R.string.email_copied)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onLongPress = {
                                                    ContactUtils.copyToClipboard(
                                                        context,
                                                        contactEmail,
                                                        toast = false
                                                    )
                                                    snackbar.show(
                                                        message = emailCopied,
                                                        type = SnackbarType.Success
                                                    )
                                                    VibrationUtils.vibrate(
                                                        context,
                                                        vibrationDuration,
                                                        vibrationPattern
                                                    )
                                                }
                                            )
                                        }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Email,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = contactEmail,
                                                color = textColor,
                                                fontSize = 16.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = stringResource(R.string.email_placeholder),
                                                color = secondaryTextColor,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                DiagnosticLog.debug(DiagnosticEvent.CONTACT_DETAILS_LOADED)

                if (!contact.company.isNullOrBlank() || contact.birthday != null || !contact.notes.isNullOrBlank() || contact.ringtone.toString() != "null") {
                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.about_contact_title,
                                        ContactUtils.getFirstName(contact.name)
                                    ),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                val contactCompany = contact.company
                                if (!contactCompany.isNullOrBlank()) {
                                    val companyCopied = stringResource(R.string.company_copied)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onLongPress = {
                                                        ContactUtils.copyToClipboard(
                                                            context,
                                                            contactCompany,
                                                            toast = false
                                                        )
                                                        snackbar.show(
                                                            message = companyCopied,
                                                            type = SnackbarType.Success
                                                        )
                                                        VibrationUtils.vibrate(
                                                            context,
                                                            vibrationDuration,
                                                            vibrationPattern
                                                        )
                                                    }
                                                )
                                            }
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.WorkOutline,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(
                                                    text = contactCompany,
                                                    color = textColor,
                                                    fontSize = 16.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = stringResource(R.string.company_placeholder),
                                                    color = secondaryTextColor,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                val contactBirthday = contact.birthday
                                if (contactBirthday != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Cake,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(
                                                    text = ContactUtils.capitalizeFirstLetter(
                                                        SimpleDateFormat(
                                                            "MMMM dd",
                                                            LocaleController.effectiveLocale(LocalContext.current)
                                                        ).format(
                                                            Date(contactBirthday)
                                                        )
                                                    ),
                                                    color = textColor,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = stringResource(R.string.edit_birthday),
                                                    color = secondaryTextColor,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        BirthdayReminderBadge(
                                            accentColor = accentColor,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }

                                val contactNotes = contact.notes
                                if (!contactNotes.isNullOrBlank()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(
                                                    text = contactNotes,
                                                    color = textColor,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = stringResource(R.string.notes_title),
                                                    color = secondaryTextColor,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                if (contact.ringtone.toString() != "null") {
                                    val ringtoneName = try {
                                        val ringtone =
                                            RingtoneManager.getRingtone(context, contact.ringtone)
                                        ringtone.getTitle(context)
                                    } catch (_: Exception) {
                                        stringResource(R.string.ringtone)
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(
                                                    text = stringResource(R.string.ringtone),
                                                    color = textColor,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = ringtoneName,
                                                    color = secondaryTextColor,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (contact.socialMedia.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.websites),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                val websiteCopied = stringResource(R.string.website_copied)

                                contact.socialMedia.forEach { socialNetworkUrl ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onLongPress = {
                                                        ContactUtils.copyToClipboard(
                                                            context,
                                                            socialNetworkUrl,
                                                            toast = false
                                                        )
                                                        snackbar.show(
                                                            message = websiteCopied,
                                                            type = SnackbarType.Success
                                                        )
                                                        VibrationUtils.vibrate(
                                                            context,
                                                            vibrationDuration,
                                                            vibrationPattern
                                                        )
                                                    },
                                                    onTap = {
                                                        ContactUtils.openUrl(
                                                            context,
                                                            socialNetworkUrl
                                                        )
                                                    }
                                                )
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val iconRes =
                                            SocialMediaType.getSocialMediaIcon(socialNetworkUrl)
                                        if (iconRes == SocialMediaType.WEBSITE_ICON) {
                                            Icon(
                                                imageVector = Icons.Default.Public,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        } else {
                                            Icon(
                                                painter = androidx.compose.ui.res.painterResource(id = iconRes),
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = SocialMediaType.getSocialMediaName(
                                                    socialNetworkUrl,
                                                    stringResource(R.string.website),
                                                ),
                                                color = textColor,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (currentContactTags.isNotEmpty()) {
                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.tags_title),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    currentContactTags.forEach { tag ->
                                        TagChip(
                                            tag = tag,
                                            onRemove = null
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (contactRecentOperations.isNotEmpty()) {
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.recent_activity_title),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                }

                                contactRecentOperations.take(3).forEach { op ->
                                    HistoryItem(
                                        operation = op,
                                        textColor = textColor,
                                        secondaryTextColor = secondaryTextColor
                                    )
                                }

                                if (contactRecentOperations.size > 3) {
                                    Button(
                                        onClick = { showAllRecent = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = accentColor.copy(alpha = 0.5f)
                                        ),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.show_full_history),
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showShareOptionsDialog = true }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = stringResource(R.string.share),
                                    color = textColor,
                                    fontSize = 16.sp
                                )
                            }

                            if (contact.phones.isNotEmpty() && blockedNumbersSupported) {
                                val errorColor = MaterialTheme.colorScheme.error
                                val blockColor =
                                    if (contact.isBlocked) errorColor else textColor
                                val iconColor =
                                    if (contact.isBlocked) errorColor else accentColor
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            triggerBlockToggle()
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = iconColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = if (contact.isBlocked) {
                                            stringResource(R.string.contact_unblock_action)
                                        } else {
                                            stringResource(R.string.contact_block_action)
                                        },
                                        color = blockColor,
                                        fontSize = 16.sp
                                    )
                                }
                            }

                            if (contact.phones.isNotEmpty() && !blockedNumbersSupported) {
                                Text(
                                    text = stringResource(R.string.contact_block_not_supported),
                                    color = secondaryTextColor,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(
                                        start = 40.dp,
                                        top = 4.dp,
                                        bottom = 4.dp
                                    )
                                )
                            } else if (contact.phones.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.contact_block_no_numbers),
                                    color = secondaryTextColor,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(
                                        start = 40.dp,
                                        top = 4.dp,
                                        bottom = 4.dp
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showDeleteDialog = true
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = stringResource(R.string.delete),
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                if (contactAccounts.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.contact_info),
                                color = textColor
                            )

                            DiagnosticLog.debug(DiagnosticEvent.CONTACT_ACCOUNT_SELECTION_RESOLVED)

                            if (contactAccounts.size == 1) {
                                Text(
                                    text = if (contactAccounts.first().isLocal) {
                                        stringResource(R.string.device)
                                    } else {
                                        contactAccounts.first().displayName
                                    },
                                    color = secondaryTextColor,
                                )
                            } else {
                                TextButton(
                                    onClick = { showAccountsDialog = true },
                                    content = {
                                        Text(
                                            text = pluralStringResource(
                                                R.plurals.multiple_account,
                                                contactAccounts.size,
                                                contactAccounts.size,
                                            ),
                                            color = accentColor
                                        )
                                    }
                                )
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
                    appSettingsViewModel = appSettingsViewModel
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
                                            stringResource(R.string.device)
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
                    title = stringResource(R.string.select_phone_number),
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
                                            text = stringResource(R.string.share),
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
                                        ContactUtils.getFirstName(contact.name),
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
                                    text = stringResource(R.string.understood),
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
                                        text = stringResource(R.string.delete),
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
                    appSettingsViewModel = appSettingsViewModel
                )
            }
        }
    }
}
