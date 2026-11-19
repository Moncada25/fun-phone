package com.bookverse.contacts.ui.screens.contacts

import com.bookverse.contacts.utils.ContactDisplay
import com.bookverse.contacts.feature.contacts.R
import com.bookverse.contacts.utils.PhoneActionLauncher
import com.bookverse.contacts.ui.theme.SecondaryTextAlpha
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.RecentType
import com.bookverse.contacts.data.model.SocialMediaType
import com.bookverse.contacts.settings.Countries
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.ConfirmDialog
import com.bookverse.contacts.ui.components.FullScreenImage
import com.bookverse.contacts.ui.components.LoaderDialog
import com.bookverse.contacts.ui.components.OptionsDialog
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.getPhoneTypeLabel
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.utils.ContactUtils
import com.bookverse.contacts.utils.VibrationUtils
import com.bookverse.contacts.utils.avatarLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.outlined.Public

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    phoneActions: PhoneActionLauncher,
    onBackClick: () -> Unit,
    onEditProfile: (Contact) -> Unit,
    onNavigateToCreateProfile: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
    contactsViewModel: ContactsViewModel
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val accentColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = textColor.copy(alpha = SecondaryTextAlpha)
    val vibrationDuration by appSettingsViewModel.vibrationDuration.collectAsStateWithLifecycle()
    val vibrationPattern by appSettingsViewModel.vibrationPattern.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    var showShareOptionsDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPhoneSelectionDialog by remember { mutableStateOf(false) }
    var phoneAction by remember { mutableStateOf<(String) -> Unit>({}) }
    var showFullImage by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showQrLoader by remember { mutableStateOf(false) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showVCardPhotoChoiceDialog by remember { mutableStateOf(false) }
    val profileContact = contactsViewModel.profile.collectAsStateWithLifecycle().value
    val snackbar = rememberAppSnackbarState()
    val pendingSnackbarEvents by contactsViewModel.pendingSnackbarEvents.collectAsStateWithLifecycle()

    LaunchedEffect(profileContact) {
        if (profileContact == null) {
            onNavigateToCreateProfile()
        }
    }

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

    suspend fun shareProfileQR() {
        if (profileContact == null) return
        showQrLoader = true
        try {
            val bitmap = contactsViewModel.shareContactQR(
                contact = profileContact,
            )
            withContext(Dispatchers.Main) {
                showQrLoader = false
                if (bitmap != null) {
                    qrBitmap = bitmap
                    showQrDialog = true
                } else {
                    Toast.makeText(
                        context,
                        resources.getString(CoreUiR.string.qr_error_generating),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                showQrLoader = false
                Toast.makeText(
                    context,
                    resources.getString(CoreUiR.string.qr_error_generating),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun shareQRCode(bitmap: Bitmap) {
        contactsViewModel.shareQRCode(bitmap, "profile_qr")
    }

    BackHandler {
        if (showFullImage) showFullImage = false else onBackClick()
    }

    if (profileContact == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = accentColor)
        }
        return
    }

    val contact = profileContact
    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                expandable = true,
                title = stringResource(R.string.profile_title),
                onBackClick = {
                    if (showFullImage) showFullImage = false else onBackClick()
                },
                actions = {
                    IconAction(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_contact_title),
                        onClick = { onEditProfile(contact) }
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
                contact.photoUri?.let(contactsViewModel::uriToBitmap)?.asImageBitmap()
            }
            val firstName = ContactDisplay.getFirstName(contact.name)
            val phoneCopied = stringResource(R.string.phone_copied)
            val emailCopied = stringResource(R.string.email_copied)
            val companyCopied = stringResource(R.string.company_copied)
            val websiteCopied = stringResource(R.string.website_copied)
            val websiteFallback = stringResource(CoreUiR.string.website)
            val locale = LocaleController.effectiveLocale(context)
            val contactEmail = contact.email?.takeIf { it.isNotBlank() }
            val contactCompany = contact.company?.takeIf { it.isNotBlank() }
            val contactBirthday = contact.birthday

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

            val callIcon = rememberVectorPainter(Icons.Outlined.Call)
            val whatsappIcon = rememberVectorPainter(Icons.Default.Whatsapp)
            val emailIcon = rememberVectorPainter(Icons.Outlined.Email)
            val companyIcon = rememberVectorPainter(Icons.Outlined.WorkOutline)
            val birthdayIcon = rememberVectorPainter(Icons.Outlined.Cake)
            val websiteIcon = rememberVectorPainter(Icons.Outlined.Public)
            val shareIcon = rememberVectorPainter(Icons.Outlined.Share)
            val deleteIcon = rememberVectorPainter(Icons.Outlined.Delete)

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
                                    onLongClick = { copyValue(phone.number, phoneCopied) },
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

                if (contactCompany != null || contactBirthday != null) {
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

                item(key = "actions") {
                    Column {
                        DetailSectionDivider()
                        DetailRow(
                            icon = shareIcon,
                            headline = stringResource(CoreUiR.string.share),
                            onClick = { showShareOptionsDialog = true },
                        )
                        DetailRow(
                            icon = deleteIcon,
                            headline = stringResource(CoreUiR.string.delete),
                            headlineColor = MaterialTheme.colorScheme.error,
                            iconTint = MaterialTheme.colorScheme.error,
                            onClick = { showDeleteDialog = true },
                        )
                    }
                }
            }

            if (showDeleteDialog) {
                ConfirmDialog(
                    onConfirm = {
                        contactsViewModel.deleteProfile(profileContact.id.toLong()) {
                            showDeleteDialog = false
                            onBackClick()
                        }
                    },
                    onDismiss = { showDeleteDialog = false },
                    title = stringResource(CoreUiR.string.delete),
                    message = stringResource(R.string.delete_profile_confirm),
                    toastMessage = stringResource(R.string.profile_deleted),
                )
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
                                text = stringResource(R.string.share_profile),
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
                                        shareQRCode(qrBitmap!!)
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
                    containerColor = backgroundColor,
                    textContentColor = textColor
                )
            }

            if (showShareOptionsDialog) {
                OptionsDialog(
                    title = stringResource(R.string.share_profile),
                    options = listOf(
                        stringResource(R.string.share_qr),
                        stringResource(R.string.share_vcard)
                    ),
                    onOptionSelected = { index ->
                        showShareOptionsDialog = false
                        when (index) {
                            0 -> {
                                coroutineScope.launch {
                                    shareProfileQR()
                                }
                            }

                            1 -> {
                                if (contact.photoUri != null) {
                                    showVCardPhotoChoiceDialog = true
                                } else {
                                    contactsViewModel.shareProfileCard(
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
                        contactsViewModel.shareProfileCard(context, includePhoto = idx == 0)
                    },
                    onDismiss = { showVCardPhotoChoiceDialog = false },
                )
            }

            if (showQrLoader) {
                LoaderDialog(
                    message = stringResource(R.string.creating_qr),
                )
            }
        }
    }
}
