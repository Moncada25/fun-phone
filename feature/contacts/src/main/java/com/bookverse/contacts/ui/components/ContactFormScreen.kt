package com.bookverse.contacts.ui.components

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import com.bookverse.contacts.data.repository.InCallBackgroundChange
import com.bookverse.contacts.data.repository.IncomingCallBackgroundChange
import com.bookverse.contacts.feature.contacts.R
import com.bookverse.contacts.settings.Countries
import com.bookverse.contacts.settings.Country
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.ui.screens.contacts.DetailContentInset
import com.bookverse.contacts.ui.screens.contacts.DetailHorizontalPadding
import com.bookverse.contacts.ui.screens.contacts.DetailIconGap
import com.bookverse.contacts.ui.screens.contacts.DetailIconSize
import com.bookverse.contacts.ui.screens.contacts.DetailRow
import com.bookverse.contacts.ui.screens.contacts.DetailSectionDivider
import com.bookverse.contacts.ui.screens.contacts.DetailSectionHeader
import com.bookverse.contacts.ui.screens.contacts.SocialMediaSearchDialog
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactSaveResult
import com.bookverse.contacts.ui.viewmodel.ContactTagViewModel
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.utils.ContactDisplay
import com.bookverse.contacts.utils.ContactUtils.isValidEmail
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Tags
import contacts.core.entities.PhoneEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactFormScreen(
    title: String,
    onSave: (Contact?) -> Unit,
    onCancel: () -> Unit,
    phoneNumber: String? = null,
    country: Country?,
    contactToEdit: Contact? = null,
    appSettingsViewModel: AppSettingsViewModel,
    contactsViewModel: ContactsViewModel,
    successMessage: String,
    isProfileEdit: Boolean = false,
    contactTagViewModel: ContactTagViewModel? = null
) {
    val backgroundColor = MaterialTheme.colorScheme.background
    val isEditMode = contactToEdit != null
    val accentColor = MaterialTheme.colorScheme.primary
    val callAnswerStyle by appSettingsViewModel.callAnswerStyle.collectAsStateWithLifecycle()
    val themeMode by appSettingsViewModel.themeMode.collectAsStateWithLifecycle()
    val accounts by contactsViewModel.accounts.collectAsStateWithLifecycle()

    val formKey = remember { "form_${contactToEdit?.id ?: "new"}_${System.currentTimeMillis()}" }

    var name by remember(formKey) { mutableStateOf(contactToEdit?.name ?: "") }
    var nameEdited by remember(formKey) { mutableStateOf(isEditMode) }
    var email by remember(formKey) { mutableStateOf(contactToEdit?.email ?: "") }
    var company by remember(formKey) { mutableStateOf(contactToEdit?.company ?: "") }
    var notes by remember(formKey) { mutableStateOf(contactToEdit?.notes ?: "") }
    var selectedCountry by remember(formKey) { mutableStateOf(country) }
    var phones by remember(formKey) {
        mutableStateOf(
            if (contactToEdit?.phones?.isNotEmpty() == true) {
                contactToEdit.phones.map { phone ->
                    val detectedCountry = Countries.detectCountryFromPhoneNumber(phone.number)
                    if (detectedCountry != null) {
                        phone.copy(
                            number = Countries.removeCountryCodeFromPhoneNumber(
                                phone.number,
                                detectedCountry
                            ),
                            countryCode = detectedCountry.code
                        )
                    } else {
                        phone.copy(countryCode = phone.countryCode)
                    }
                }
            } else if (!phoneNumber.isNullOrBlank()) {
                listOf(ContactPhone(phoneNumber, PhoneEntity.Type.MOBILE, country?.code))
            } else {
                emptyList()
            }
        )
    }
    val originalPhoneCountries = remember(formKey) {
        mutableMapOf<Int, Country?>().apply {
            if (contactToEdit?.phones?.isNotEmpty() == true) {
                contactToEdit.phones.forEachIndexed { index, phone ->
                    val detectedCountry = Countries.detectCountryFromPhoneNumber(phone.number)
                    if (detectedCountry != null) {
                        this[index] = detectedCountry
                    } else {
                        this[index] = country
                    }
                }
            }
        }
    }

    val snackbar = rememberAppSnackbarState()

    LaunchedEffect(Unit) {
        contactsViewModel.snackbarEvents.collect { ev ->
            snackbar.show(
                message = ev.message,
                type = ev.kind,
                actionLabel = ev.actionLabel
            )
        }
    }

    LaunchedEffect(contactToEdit?.id) {
        if (contactToEdit?.id?.isNotBlank() == true) {
            contactTagViewModel?.loadTagsForContacts(listOf(contactToEdit.id))
        }
    }

    fun updatePhoneCountries(newPhones: List<ContactPhone>) {
        val currentSize = originalPhoneCountries.size
        val newSize = newPhones.size

        if (newSize > currentSize) {
            for (i in currentSize until newSize) {
                originalPhoneCountries[i] = selectedCountry
            }
        } else if (newSize < currentSize) {
            for (i in newSize until currentSize) {
                originalPhoneCountries.remove(i)
            }
        }
    }

    var photoUri by remember(formKey) { mutableStateOf(contactToEdit?.photoUri) }
    var photoUpdateKey by remember(formKey) { mutableIntStateOf(0) }
    val defaultContactAccount by appSettingsViewModel.defaultContactAccount.collectAsStateWithLifecycle()
    var selectedAccount by remember(formKey) { mutableStateOf(defaultContactAccount) }
    var showSheet by remember(formKey) { mutableStateOf(false) }
    var showIncomingCallBackgroundSheet by remember(formKey) { mutableStateOf(false) }
    var showIncomingCallPreview by remember(formKey) { mutableStateOf(false) }
    var showInCallBackgroundSheet by remember(formKey) { mutableStateOf(false) }
    var showInCallPreview by remember(formKey) { mutableStateOf(false) }
    var birthday by remember(formKey) { mutableStateOf(contactToEdit?.birthday) }
    var showBirthdayPicker by remember(formKey) { mutableStateOf(false) }
    var showAccountSelector by remember(formKey) { mutableStateOf(false) }
    var socialNetworks by remember(formKey) {
        mutableStateOf(
            contactToEdit?.socialMedia ?: emptyList()
        )
    }
    var showSocialNetworkDialog by remember(formKey) { mutableStateOf(false) }
    var ringtoneUri by remember(formKey) { mutableStateOf(contactToEdit?.ringtone) }
    var showTagDialog by remember(formKey) { mutableStateOf(false) }
    var pendingTagIds by remember(formKey) { mutableStateOf<Set<Long>>(emptySet()) }
    val allTags by contactTagViewModel?.allTags?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(emptyList()) }
    val contactTagsMap by contactTagViewModel?.contactTags?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(emptyMap()) }
    val currentContactTags = if (isEditMode && contactToEdit.id.isNotBlank() == true) {
        contactTagsMap[contactToEdit.id] ?: emptyList()
    } else {
        allTags.filter { it.id in pendingTagIds }
    }
    val context = LocalContext.current
    val resources = LocalResources.current
    var isSaving by remember(formKey) { mutableStateOf(false) }
    var existingIncomingCallBackgroundUri by remember(formKey) { mutableStateOf<Uri?>(null) }
    var incomingCallBackgroundChange by remember(formKey) {
        mutableStateOf<IncomingCallBackgroundChange>(IncomingCallBackgroundChange.Unchanged)
    }
    var isIncomingCallBackgroundLoading by remember(formKey) {
        mutableStateOf(isEditMode && !isProfileEdit)
    }
    var existingInCallBackgroundUri by remember(formKey) { mutableStateOf<Uri?>(null) }
    var inCallBackgroundChange by remember(formKey) {
        mutableStateOf<InCallBackgroundChange>(InCallBackgroundChange.Unchanged)
    }
    var isInCallBackgroundLoading by remember(formKey) {
        mutableStateOf(isEditMode && !isProfileEdit)
    }

    val effectiveIncomingCallBackgroundUri = when (val change = incomingCallBackgroundChange) {
        IncomingCallBackgroundChange.Unchanged -> existingIncomingCallBackgroundUri
        IncomingCallBackgroundChange.Remove -> null
        is IncomingCallBackgroundChange.Replace -> change.sourceUri
    }
    val pendingIncomingCallBackgroundUri =
        (incomingCallBackgroundChange as? IncomingCallBackgroundChange.Replace)?.sourceUri
    val effectiveInCallBackgroundUri = when (val change = inCallBackgroundChange) {
        InCallBackgroundChange.Unchanged -> existingInCallBackgroundUri
        InCallBackgroundChange.Remove -> null
        is InCallBackgroundChange.Replace -> change.sourceUri
    }
    val pendingInCallBackgroundUri =
        (inCallBackgroundChange as? InCallBackgroundChange.Replace)?.sourceUri

    fun cleanupBackgroundTemporary(sourceUri: Uri?, filePrefix: String) {
        sourceUri ?: return
        val sourceFile = sourceUri.path?.let(::File) ?: return
        val cacheDirectory = runCatching { context.cacheDir.canonicalFile }.getOrNull() ?: return
        val sourceCanonical = runCatching { sourceFile.canonicalFile }.getOrNull() ?: return
        if (
            sourceCanonical.parentFile == cacheDirectory &&
            sourceCanonical.name.startsWith(filePrefix)
        ) {
            sourceCanonical.delete()
        }
    }

    fun cleanupIncomingCallBackgroundTemporary(sourceUri: Uri?) {
        cleanupBackgroundTemporary(sourceUri, "incoming_call_background_")
    }

    fun cleanupInCallBackgroundTemporary(sourceUri: Uri?) {
        cleanupBackgroundTemporary(sourceUri, "in_call_background_")
    }

    fun cleanupPendingIncomingCallBackground() {
        cleanupIncomingCallBackgroundTemporary(pendingIncomingCallBackgroundUri)
    }

    fun cleanupPendingInCallBackground() {
        cleanupInCallBackgroundTemporary(pendingInCallBackgroundUri)
    }

    fun cancelForm() {
        cleanupPendingIncomingCallBackground()
        cleanupPendingInCallBackground()
        onCancel()
    }

    LaunchedEffect(formKey, contactToEdit?.id, isProfileEdit) {
        existingIncomingCallBackgroundUri = if (
            !isProfileEdit && contactToEdit != null && contactToEdit.id.isNotBlank()
        ) {
            contactsViewModel.getIncomingCallBackgroundUri(contactToEdit.id)
        } else {
            null
        }
        existingInCallBackgroundUri = if (
            !isProfileEdit && contactToEdit != null && contactToEdit.id.isNotBlank()
        ) {
            contactsViewModel.getInCallBackgroundUri(contactToEdit.id)
        } else {
            null
        }
        isIncomingCallBackgroundLoading = false
        isInCallBackgroundLoading = false
    }

    DisposableEffect(formKey, pendingIncomingCallBackgroundUri, pendingInCallBackgroundUri) {
        onDispose {
            cleanupIncomingCallBackgroundTemporary(pendingIncomingCallBackgroundUri)
            cleanupInCallBackgroundTemporary(pendingInCallBackgroundUri)
        }
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            ringtoneUri = result.data?.getRingtoneUriCompat()
        }
    }

    val readyToSave by remember {
        derivedStateOf {
            !isSaving && name.isNotBlank() && phones.isNotEmpty() &&
                    phones.all { it.number.isNotBlank() } &&
                    (email.isEmpty() || email.isValidEmail())
        }
    }

    BackHandler(onBack = ::cancelForm)

    val scrollBehavior = rememberAppBarScrollBehavior(expandable = false)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                title = title,
                onBackClick = ::cancelForm,
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
            val personIcon = rememberVectorPainter(Icons.Outlined.Person)
            val callIcon = rememberVectorPainter(Icons.Outlined.Call)
            val emailIcon = rememberVectorPainter(Icons.Outlined.Email)
            val companyIcon = rememberVectorPainter(Icons.Outlined.WorkOutline)
            val notesIcon = rememberVectorPainter(Icons.AutoMirrored.Outlined.Notes)
            val birthdayIcon = rememberVectorPainter(Icons.Outlined.Cake)
            val ringtoneIcon = rememberVectorPainter(Icons.Outlined.MusicNote)
            val tagsIcon = rememberVectorPainter(Lucide.Tags)

            LazyColumn(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 128.dp),
            ) {
                item(key = "identity_${formKey}_$photoUpdateKey") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ContactFormPhoto(
                            photoUri = photoUri,
                            contentDescription = stringResource(R.string.cd_contact_photo),
                            onClick = { showSheet = true },
                        )
                        TextButton(
                            onClick = { showSheet = true },
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    if (photoUri != null) {
                                        R.string.contact_form_change_photo
                                    } else {
                                        R.string.contact_form_add_photo
                                    },
                                ),
                            )
                        }
                    }
                }

                item(key = "contact_info_$formKey") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ContactFormFieldRow(icon = personIcon) {
                            TextField(
                                value = name,
                                isError = nameEdited && name.isBlank(),
                                onValueChange = {
                                    name = it
                                    nameEdited = true
                                },
                                label = stringResource(R.string.name_placeholder),
                                imeAction = ImeAction.Next,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        ContactFormFieldRow(icon = callIcon) {
                            PhoneNumbersField(
                                phones = phones,
                                onPhonesChange = { newPhones ->
                                    updatePhoneCountries(newPhones)
                                    phones = newPhones
                                },
                                countrySelected = selectedCountry,
                                onCountrySelected = { newCountry ->
                                    selectedCountry = newCountry
                                }
                            )
                        }

                        ContactFormFieldRow(icon = emailIcon) {
                            TextField(
                                value = email,
                                isError = email.isNotBlank() && !email.isValidEmail(),
                                keyboardType = KeyboardType.Email,
                                onValueChange = { email = it },
                                label = stringResource(R.string.email_placeholder),
                                imeAction = ImeAction.Next,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        ContactFormFieldRow(icon = companyIcon) {
                            TextField(
                                value = company,
                                onValueChange = { company = it },
                                label = stringResource(R.string.company_placeholder),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                item(key = "personal_$formKey") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        DetailSectionDivider()
                        DetailSectionHeader(stringResource(R.string.contact_form_section_personal_details))

                        val birthdayCopy = birthday?.let { date ->
                            ContactDisplay.capitalizeFirstLetter(
                                SimpleDateFormat(
                                    "MMMM dd",
                                    LocaleController.effectiveLocale(context)
                                ).format(Date(date))
                            )
                        } ?: stringResource(R.string.add_birthday)

                        DetailRow(
                            icon = birthdayIcon,
                            headline = stringResource(R.string.edit_birthday),
                            supporting = birthdayCopy,
                            onClick = { showBirthdayPicker = true },
                        )

                        SocialMediaField(
                            socialNetworks = socialNetworks,
                            onSocialNetworksChange = { socialNetworks = it },
                            onNavigateToSearch = { showSocialNetworkDialog = true },
                        )

                        if (!isProfileEdit) {
                            ContactFormFieldRow(icon = notesIcon) {
                                TextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = stringResource(R.string.edit_notes),
                                    keyboardType = KeyboardType.Text,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = false
                                )
                            }
                        }
                    }
                }

                if (!isProfileEdit) {
                    item(key = "preferences_$formKey") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            DetailSectionDivider()
                            DetailSectionHeader(stringResource(R.string.contact_form_section_preferences))

                            IncomingCallBackgroundField(
                                backgroundUri = effectiveIncomingCallBackgroundUri,
                                isLoading = isIncomingCallBackgroundLoading,
                                onClick = { showIncomingCallBackgroundSheet = true },
                            )

                            IncomingCallBackgroundField(
                                backgroundUri = effectiveInCallBackgroundUri,
                                isLoading = isInCallBackgroundLoading,
                                onClick = { showInCallBackgroundSheet = true },
                                icon = Icons.Outlined.Wallpaper,
                                titleRes = R.string.in_call_background_title,
                                configuredRes = R.string.in_call_background_configured,
                                notConfiguredRes = R.string.in_call_background_not_configured,
                            )

                            val hasRingtone = ringtoneUri != null && ringtoneUri.toString() != "null"
                            val ringtoneTitle = remember(ringtoneUri) {
                                if (hasRingtone) {
                                    runCatching {
                                        RingtoneManager.getRingtone(context, ringtoneUri)?.getTitle(context)
                                    }.getOrNull()
                                } else {
                                    null
                                }
                            }
                            val ringtoneName = when {
                                !hasRingtone -> stringResource(R.string.select_ringtone)
                                ringtoneTitle != null -> ringtoneTitle
                                else -> stringResource(R.string.ringtone)
                            }

                            DetailRow(
                                icon = ringtoneIcon,
                                headline = stringResource(R.string.ringtone),
                                supporting = ringtoneName,
                                onClick = {
                                    val intent =
                                        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                            putExtra(
                                                RingtoneManager.EXTRA_RINGTONE_TYPE,
                                                RingtoneManager.TYPE_RINGTONE
                                            )
                                            putExtra(
                                                RingtoneManager.EXTRA_RINGTONE_TITLE,
                                                resources.getString(R.string.select_ringtone)
                                            )
                                            putExtra(
                                                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                                ringtoneUri
                                            )
                                            putExtra(
                                                RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT,
                                                true
                                            )
                                            putExtra(
                                                RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT,
                                                true
                                            )
                                        }
                                    ringtoneLauncher.launch(intent)
                                },
                                trailing = if (hasRingtone) {
                                    {
                                        IconButton(onClick = { ringtoneUri = null }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(CoreUiR.string.delete),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                } else {
                                    null
                                },
                            )

                            if (contactTagViewModel != null) {
                                DetailRow(
                                    icon = tagsIcon,
                                    headline = stringResource(R.string.tags_title),
                                    supporting = if (currentContactTags.isEmpty()) {
                                        stringResource(R.string.add_tag)
                                    } else {
                                        null
                                    },
                                    onClick = { showTagDialog = true },
                                )

                                if (currentContactTags.isNotEmpty()) {
                                    FlowRow(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                start = DetailContentInset,
                                                end = DetailHorizontalPadding,
                                                bottom = 8.dp,
                                            ),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        currentContactTags.forEach { tag ->
                                            TagChip(
                                                tag = tag,
                                                onRemove = {
                                                    if (isEditMode && contactToEdit.id.isNotBlank()) {
                                                        contactTagViewModel.removeTagFromContact(contactToEdit.id, tag.id)
                                                    } else {
                                                        pendingTagIds = pendingTagIds - tag.id
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isEditMode && !isProfileEdit) {
                    item(key = "account_$formKey") {
                        val accountLabel =
                            if (selectedAccount.isLocal) {
                                stringResource(CoreUiR.string.local_account)
                            } else {
                                selectedAccount.displayName
                            }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            DetailSectionDivider()
                            DetailSectionHeader(stringResource(R.string.contact_form_section_account))
                            DetailRow(
                                icon = rememberVectorPainter(getAccountIcon(selectedAccount.type)),
                                headline = stringResource(CoreUiR.string.account),
                                supporting = accountLabel,
                                onClick = { showAccountSelector = true },
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(backgroundColor)
                    .windowInsetsPadding(WindowInsets.ime)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = 760.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = ::cancelForm,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                    ) {
                        Text(text = stringResource(CoreUiR.string.cancel))
                    }

                    Button(
                        onClick = {
                            val localUri = photoUri

                            val phonesWithCountryCode = phones
                                .mapNotNull { phone ->
                                    val rawNumber = phone.number.trim()
                                    if (rawNumber.isEmpty()) return@mapNotNull null

                                    val normalizedCode = phone.countryCode
                                        ?.trim()
                                        ?.takeIf { it.isNotEmpty() }
                                        ?.let { code ->
                                            if (code.startsWith("+")) {
                                                code
                                            } else {
                                                "+${code.trimStart('+')}"
                                            }
                                        }

                                    val cleanedNumber = rawNumber.replace("[^+\\d]".toRegex(), "")
                                    if (cleanedNumber.isEmpty()) return@mapNotNull null

                                    val rebuiltNumber = when {
                                        cleanedNumber.startsWith("+") -> cleanedNumber
                                        normalizedCode != null -> {
                                            val digits = cleanedNumber.removePrefix(
                                                normalizedCode.removePrefix("+")
                                            )
                                            normalizedCode + digits
                                        }

                                        else -> cleanedNumber
                                    }

                                    phone.copy(
                                        number = rebuiltNumber,
                                        countryCode = normalizedCode ?: phone.countryCode
                                    )
                                }
                                .distinctBy { it.number }

                            val contactToSave = if (isEditMode) {
                                contactToEdit.copy(
                                    name = name,
                                    phones = phonesWithCountryCode,
                                    photoUri = localUri,
                                    email = email.ifEmpty { null },
                                    company = company.ifEmpty { null },
                                    notes = notes.ifEmpty { null },
                                    birthday = birthday,
                                    socialMedia = socialNetworks,
                                    ringtone = ringtoneUri
                                )
                            } else {
                                Contact(
                                    id = "",
                                    name = name,
                                    phones = phonesWithCountryCode,
                                    photoUri = localUri,
                                    accounts = listOf(selectedAccount),
                                    email = email.ifEmpty { null },
                                    company = company.ifEmpty { null },
                                    notes = notes.ifEmpty { null },
                                    birthday = birthday,
                                    socialMedia = socialNetworks,
                                    ringtone = ringtoneUri
                                )
                            }

                            isSaving = true
                            fun finishSave(savedContact: Contact) {
                                contactsViewModel.updateIncomingCallBackground(
                                    contactId = savedContact.id,
                                    change = incomingCallBackgroundChange,
                                ) {
                                    cleanupPendingIncomingCallBackground()
                                    contactsViewModel.updateInCallBackground(
                                        contactId = savedContact.id,
                                        change = inCallBackgroundChange,
                                    ) {
                                        cleanupPendingInCallBackground()
                                        onSave(savedContact)
                                    }
                                }
                            }

                            val handleResult: (ContactSaveResult) -> Unit = { result ->
                                when (result) {
                                    ContactSaveResult.Failure -> isSaving = false
                                    is ContactSaveResult.Success -> finishSave(result.contact)
                                    is ContactSaveResult.SuccessWithPhotoWarning -> {
                                        finishSave(result.contact)
                                    }
                                }
                            }
                            if (isEditMode) {
                                if (isProfileEdit) {
                                    contactsViewModel.updateProfile(
                                        contact = contactToSave,
                                        successMessage = successMessage,
                                        onResult = handleResult,
                                    )
                                } else {
                                    contactsViewModel.updateContact(
                                        contact = contactToSave,
                                        successMessage = successMessage,
                                        onResult = handleResult,
                                    )
                                }
                            } else {
                                if (isProfileEdit) {
                                    contactsViewModel.createProfile(
                                        contact = contactToSave,
                                        successMessage = successMessage,
                                        onSaveResult = handleResult,
                                    )
                                } else {
                                    contactsViewModel.createContact(
                                        contact = contactToSave,
                                        account = selectedAccount,
                                        successMessage = successMessage,
                                        onSaveResult = { result ->
                                            val created = when (result) {
                                                is ContactSaveResult.Success -> result.contact
                                                is ContactSaveResult.SuccessWithPhotoWarning -> {
                                                    result.contact
                                                }

                                                ContactSaveResult.Failure -> null
                                            }
                                            if (created != null && created.id.isNotBlank()) {
                                                pendingTagIds.forEach { tagId ->
                                                    contactTagViewModel?.assignTagToContact(created.id, tagId)
                                                }
                                            }
                                            handleResult(result)
                                        },
                                    )
                                }
                            }
                        },
                        enabled = readyToSave,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp),
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = LocalContentColor.current,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(text = stringResource(CoreUiR.string.save))
                        }
                    }
                }
            }

            if (showSheet) {
                PhotoSelector(
                    key = formKey,
                    onDismiss = {
                        showSheet = false
                    },
                    onPhotoSelected = { uri ->
                        photoUri = uri
                        photoUpdateKey++
                        showSheet = false
                    },
                    onDeletePhoto = {
                        photoUri = null
                        photoUpdateKey++
                        showSheet = false
                    },
                    hasExistingPhoto = photoUri != null,
                    existingPhotoUri = photoUri,
                )
            }

            if (showIncomingCallBackgroundSheet) {
                IncomingCallBackgroundSelector(
                    onDismiss = { showIncomingCallBackgroundSheet = false },
                    onPreviewBackground = { showIncomingCallPreview = true },
                    onBackgroundSelected = { uri ->
                        cleanupPendingIncomingCallBackground()
                        incomingCallBackgroundChange = IncomingCallBackgroundChange.Replace(uri)
                        showIncomingCallBackgroundSheet = false
                    },
                    onDeleteBackground = {
                        showIncomingCallPreview = false
                        cleanupPendingIncomingCallBackground()
                        incomingCallBackgroundChange = IncomingCallBackgroundChange.Remove
                        showIncomingCallBackgroundSheet = false
                    },
                    hasExistingBackground = effectiveIncomingCallBackgroundUri != null,
                )
            }

            if (showIncomingCallPreview && effectiveIncomingCallBackgroundUri != null) {
                val previewName = name.ifBlank { stringResource(CoreUiR.string.unknown) }
                val previewPhone = phones
                    .firstOrNull { it.number.isNotBlank() }
                    ?.let { phone ->
                        val typeLabel = stringResource(incomingCallPreviewPhoneTypeLabel(phone.type))
                        "$typeLabel ${phone.number}"
                    }

                IncomingCallPreviewDialog(
                    displayName = previewName,
                    phoneLabel = previewPhone,
                    photoUri = photoUri,
                    backgroundUri = effectiveIncomingCallBackgroundUri,
                    callAnswerStyle = callAnswerStyle,
                    themeMode = themeMode,
                    accentColor = accentColor,
                    onDismiss = { showIncomingCallPreview = false },
                )
            }

            if (showInCallBackgroundSheet) {
                IncomingCallBackgroundSelector(
                    onDismiss = { showInCallBackgroundSheet = false },
                    onPreviewBackground = { showInCallPreview = true },
                    onBackgroundSelected = { uri ->
                        cleanupPendingInCallBackground()
                        inCallBackgroundChange = InCallBackgroundChange.Replace(uri)
                        showInCallBackgroundSheet = false
                    },
                    onDeleteBackground = {
                        showInCallPreview = false
                        cleanupPendingInCallBackground()
                        inCallBackgroundChange = InCallBackgroundChange.Remove
                        showInCallBackgroundSheet = false
                    },
                    hasExistingBackground = effectiveInCallBackgroundUri != null,
                    titleRes = R.string.in_call_background_title,
                    subtitleRes = R.string.in_call_background_description,
                    removeLabelRes = R.string.in_call_background_remove,
                    previewLabelRes = R.string.in_call_preview_title,
                    cameraPermissionRes = R.string.in_call_background_camera_permission,
                    temporaryFilePrefix = "in_call_background_",
                )
            }

            if (showInCallPreview && effectiveInCallBackgroundUri != null) {
                val previewName = name.ifBlank { stringResource(CoreUiR.string.unknown) }
                val previewPhone = phones
                    .firstOrNull { it.number.isNotBlank() }
                    ?.let { phone ->
                        val typeLabel = stringResource(incomingCallPreviewPhoneTypeLabel(phone.type))
                        "$typeLabel ${phone.number}"
                    }

                InCallBackgroundPreviewDialog(
                    displayName = previewName,
                    phoneLabel = previewPhone,
                    photoUri = photoUri,
                    backgroundUri = effectiveInCallBackgroundUri,
                    onDismiss = { showInCallPreview = false },
                )
            }

            if (showBirthdayPicker) {
                DatePickerDialog(
                    selectedDate = birthday ?: System.currentTimeMillis(),
                    onDateSelected = { birthday = it },
                    onDismiss = { showBirthdayPicker = false },
                    title = stringResource(R.string.select_birthday)
                )
            }

            if (showSocialNetworkDialog) {
                SocialMediaSearchDialog(
                    onDismiss = { showSocialNetworkDialog = false },
                    onSocialNetworkSelected = { url ->
                        val updatedList = socialNetworks.toMutableList()
                        updatedList.add(url)
                        socialNetworks = updatedList
                        showSocialNetworkDialog = false
                    },
                )
            }

            if (showAccountSelector) {
                OptionsDialog(
                    title = stringResource(CoreUiR.string.account),
                    options = accounts.map { account ->
                        if (account.isLocal) {
                            stringResource(CoreUiR.string.local_account)
                        } else {
                            account.displayName
                        }
                    },
                    onOptionSelected = { index ->
                        selectedAccount = accounts[index]
                        showAccountSelector = false
                    },
                    onDismiss = { showAccountSelector = false },
                )
            }

            if (showTagDialog && contactTagViewModel != null) {
                TagAssignmentDialog(
                    allTags = allTags,
                    assignedTagIds = if (isEditMode && contactToEdit.id.isNotBlank() == true) {
                        currentContactTags.map { it.id }.toSet()
                    } else {
                        pendingTagIds
                    },
                    onApply = { selectedIds ->
                        if (isEditMode && contactToEdit.id.isNotBlank() == true) {
                            val currentIds = currentContactTags.map { it.id }.toSet()
                            (selectedIds - currentIds).forEach { tagId ->
                                contactTagViewModel.assignTagToContact(contactToEdit.id, tagId)
                            }
                            (currentIds - selectedIds).forEach { tagId ->
                                contactTagViewModel.removeTagFromContact(contactToEdit.id, tagId)
                            }
                        } else {
                            pendingTagIds = selectedIds
                        }
                    },
                    onCreateTag = { name, color ->
                        contactTagViewModel.createTag(name, color)
                    },
                    onDismiss = { showTagDialog = false },
                    appSettingsViewModel = appSettingsViewModel,
                )
            }
        }
    }
}

private fun incomingCallPreviewPhoneTypeLabel(type: PhoneEntity.Type?): Int = when (type) {
    PhoneEntity.Type.WORK -> CoreUiR.string.phone_type_work
    PhoneEntity.Type.HOME -> CoreUiR.string.phone_type_home
    PhoneEntity.Type.MAIN -> CoreUiR.string.phone_type_main
    else -> CoreUiR.string.phone_type_mobile
}

@Composable
private fun ContactFormPhoto(
    photoUri: Uri?,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val placeholderColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(
        modifier = Modifier
            .size(112.dp)
            .clip(CircleShape)
            .background(placeholderColor)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = ColorPainter(placeholderColor),
                error = ColorPainter(placeholderColor),
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.AddAPhoto,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ContactFormFieldRow(
    icon: Painter?,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DetailHorizontalPadding, vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 24.dp)
                .size(DetailIconSize),
        ) {
            if (icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.width(DetailIconGap))
        Column(
            modifier = Modifier.weight(1f),
            content = content,
        )
    }
}

@Composable
internal fun IncomingCallBackgroundField(
    backgroundUri: Uri?,
    isLoading: Boolean,
    onClick: () -> Unit,
    icon: ImageVector = Icons.Outlined.Image,
    titleRes: Int = R.string.incoming_call_background_title,
    configuredRes: Int = R.string.incoming_call_background_configured,
    notConfiguredRes: Int = R.string.incoming_call_background_not_configured,
) {
    val title = stringResource(titleRes)
    val status = when {
        isLoading -> stringResource(CoreUiR.string.loading)
        backgroundUri != null -> stringResource(configuredRes)
        else -> stringResource(notConfiguredRes)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = !isLoading,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = "$title. $status" }
            .heightIn(min = 56.dp)
            .padding(
                start = DetailHorizontalPadding,
                end = DetailHorizontalPadding,
                top = 10.dp,
                bottom = 10.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DetailIconSize),
        )
        Spacer(modifier = Modifier.width(DetailIconGap))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            isLoading -> {
                Spacer(modifier = Modifier.width(12.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            }

            backgroundUri != null -> {
                Spacer(modifier = Modifier.width(12.dp))
                AsyncImage(
                    model = backgroundUri,
                    contentDescription = null,
                    modifier = Modifier
                        .width(36.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

private fun Intent.getRingtoneUriCompat(): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
    }
}

private fun getAccountIcon(accountType: String) = when (accountType) {
    "com.google" -> Icons.Default.Android
    "com.android.local" -> Icons.Outlined.PhoneAndroid
    else -> Icons.Outlined.AccountCircle
}
