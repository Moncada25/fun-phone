package com.bookverse.contacts.ui.screens.contacts

import com.bookverse.contacts.utils.PhoneActions
import com.bookverse.contacts.ui.theme.SecondaryTextAlpha
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.RecentType
import com.bookverse.contacts.data.model.SocialMediaType
import com.bookverse.contacts.settings.Countries
import com.bookverse.contacts.ui.components.ActionIcon
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
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
                        resources.getString(R.string.qr_error_generating),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                showQrLoader = false
                Toast.makeText(
                    context,
                    resources.getString(R.string.qr_error_generating),
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
                    androidx.compose.material3.Card(
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
                            val photoBitmap = remember(contact.photoUri) {
                                contact.photoUri?.let(contactsViewModel::uriToBitmap)
                            }
                            if (photoBitmap != null) {
                                Image(
                                    bitmap = photoBitmap.asImageBitmap(),
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
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    Text(
                        text = contact.name,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = textColor
                    )
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

                if (!contact.company.isNullOrBlank() || contact.birthday != null) {
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
                    title = stringResource(R.string.delete),
                    message = stringResource(R.string.delete_profile_confirm),
                    toastMessage = stringResource(R.string.profile_deleted),
                    appSettingsViewModel = appSettingsViewModel
                )
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
                                            text = stringResource(R.string.share),
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
