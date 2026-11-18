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
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.feature.contacts.R
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import coil.compose.AsyncImage
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import com.bookverse.contacts.data.repository.InCallBackgroundChange
import com.bookverse.contacts.data.repository.IncomingCallBackgroundChange
import com.bookverse.contacts.settings.Countries
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.settings.Country
import com.bookverse.contacts.ui.screens.contacts.SocialMediaSearchDialog
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactTagViewModel
import com.bookverse.contacts.ui.viewmodel.ContactSaveResult
import com.bookverse.contacts.utils.ContactUtils
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Tags
import com.bookverse.contacts.utils.ContactUtils.isValidEmail
import contacts.core.entities.PhoneEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

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
    val textColor = MaterialTheme.colorScheme.onBackground
    val isEditMode = contactToEdit != null
    val accentColor = MaterialTheme.colorScheme.primary
    val callAnswerStyle by appSettingsViewModel.callAnswerStyle.collectAsStateWithLifecycle()
    val themeMode by appSettingsViewModel.themeMode.collectAsStateWithLifecycle()
    val accounts by contactsViewModel.accounts.collectAsStateWithLifecycle()

    val formKey = remember { "form_${contactToEdit?.id ?: "new"}_${System.currentTimeMillis()}" }

    var name by remember(formKey) { mutableStateOf(contactToEdit?.name ?: "") }
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
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
                    .padding(paddingValues)
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 20.dp,
                    bottom = 128.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "identity_${formKey}_$photoUpdateKey") {
                    val photoDescription = stringResource(R.string.cd_contact_photo)

                    ContactFormSection(title = stringResource(R.string.contact_info_title)) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(128.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                                .clickable(role = Role.Button) { showSheet = true }
                                .semantics {
                                    contentDescription = photoDescription
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoUri != null) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    placeholder = ColorPainter(accentColor),
                                    error = ColorPainter(accentColor)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = contentColorFor(accentColor)
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(36.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 4.dp
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.padding(9.dp)
                                )
                            }
                        }

                        TextField(
                            value = name,
                            isError = name.isBlank(),
                            onValueChange = { name = it },
                            label = stringResource(R.string.name_placeholder),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        TextField(
                            value = email,
                            isError = email.isNotBlank() && !email.isValidEmail(),
                            keyboardType = KeyboardType.Email,
                            onValueChange = { email = it },
                            label = stringResource(R.string.email_placeholder),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        TextField(
                            value = company,
                            onValueChange = { company = it },
                            label = stringResource(R.string.company_placeholder),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                item(key = "phones_$formKey") {
                    ContactFormSection(title = stringResource(R.string.contact_form_section_phone_numbers)) {
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
                }

                item(key = "personal_$formKey") {
                    ContactFormSection(title = stringResource(R.string.contact_form_section_personal_details)) {
                        val birthdayCopy = if (birthday != null) {
                            ContactUtils.capitalizeFirstLetter(
                                SimpleDateFormat(
                                    "MMMM dd",
                                    LocaleController.effectiveLocale(context)
                                ).format(Date(birthday!!))
                            )
                        } else {
                            stringResource(R.string.add_birthday)
                        }

                        ContactFormActionRow(
                            icon = Icons.Default.Cake,
                            title = stringResource(R.string.edit_birthday),
                            value = birthdayCopy,
                            accentColor = accentColor,
                            onClick = { showBirthdayPicker = true }
                        )

                        SocialMediaField(
                            socialNetworks = socialNetworks,
                            onSocialNetworksChange = { socialNetworks = it },
                            onNavigateToSearch = { showSocialNetworkDialog = true },
                            appSettingsViewModel = appSettingsViewModel
                        )

                        if (!isProfileEdit) {
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

                if (!isProfileEdit) {
                    item(key = "preferences_$formKey") {
                        ContactFormSection(title = stringResource(R.string.contact_form_section_preferences)) {
                            IncomingCallBackgroundField(
                                backgroundUri = effectiveIncomingCallBackgroundUri,
                                isLoading = isIncomingCallBackgroundLoading,
                                accentColor = accentColor,
                                onClick = { showIncomingCallBackgroundSheet = true },
                            )

                            IncomingCallBackgroundField(
                                backgroundUri = effectiveInCallBackgroundUri,
                                isLoading = isInCallBackgroundLoading,
                                accentColor = accentColor,
                                onClick = { showInCallBackgroundSheet = true },
                                titleRes = R.string.in_call_background_title,
                                descriptionRes = R.string.in_call_background_description,
                                configuredRes = R.string.in_call_background_configured,
                                notConfiguredRes = R.string.in_call_background_not_configured,
                            )

                            val ringtoneName =
                                if (ringtoneUri != null && ringtoneUri.toString() != "null") {
                                    try {
                                        val ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
                                        ringtone.getTitle(context)
                                    } catch (_: Exception) {
                                        stringResource(R.string.ringtone)
                                    }
                                } else {
                                    stringResource(R.string.select_ringtone)
                                }

                            ContactFormActionRow(
                                icon = Icons.Default.MusicNote,
                                title = stringResource(R.string.ringtone),
                                value = ringtoneName,
                                accentColor = accentColor,
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
                                trailingContent = if (ringtoneUri != null && ringtoneUri.toString() != "null") {
                                    {
                                        IconButton(onClick = { ringtoneUri = null }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(CoreUiR.string.delete),
                                                tint = accentColor
                                            )
                                        }
                                    }
                                } else {
                                    null
                                }
                            )

                            if (contactTagViewModel != null) {
                                ContactFormActionRow(
                                    icon = Lucide.Tags,
                                    title = stringResource(R.string.tags_title),
                                    value = if (currentContactTags.isEmpty()) {
                                        stringResource(R.string.add_tag)
                                    } else {
                                        currentContactTags.joinToString { it.name }
                                    },
                                    accentColor = accentColor,
                                    onClick = { showTagDialog = true }
                                )

                                if (currentContactTags.isNotEmpty()) {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        currentContactTags.forEach { tag ->
                                            TagChip(
                                                tag = tag,
                                                onRemove = {
                                                    if (isEditMode && contactToEdit.id.isNotBlank() == true) {
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

                        ContactFormSection(title = stringResource(R.string.contact_form_section_account)) {
                            ContactFormActionRow(
                                icon = getAccountIcon(selectedAccount.type),
                                title = stringResource(CoreUiR.string.account),
                                value = accountLabel,
                                accentColor = accentColor,
                                onClick = { showAccountSelector = true }
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = backgroundColor,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.ime)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ButtonClick(
                            onClick = ::cancelForm,
                            modifier = Modifier.weight(1f),
                            text = stringResource(CoreUiR.string.cancel),
                            fontSize = 18
                        )

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
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(999.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = textColor,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(
                                    text = stringResource(CoreUiR.string.save),
                                    color = textColor,
                                    fontSize = 18.sp,
                                )
                            }
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
                    appSettingsViewModel = appSettingsViewModel
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
internal fun IncomingCallBackgroundField(
    backgroundUri: Uri?,
    isLoading: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    titleRes: Int = R.string.incoming_call_background_title,
    descriptionRes: Int = R.string.incoming_call_background_description,
    configuredRes: Int = R.string.incoming_call_background_configured,
    notConfiguredRes: Int = R.string.incoming_call_background_not_configured,
) {
    val title = stringResource(titleRes)
    val status = when {
        isLoading -> stringResource(CoreUiR.string.loading)
        backgroundUri != null -> stringResource(configuredRes)
        else -> stringResource(notConfiguredRes)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = !isLoading,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = "$title. $status" },
        shape = RoundedCornerShape(16.dp),
        color = accentColor.copy(alpha = 0.12f),
        contentColor = accentColor,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(88.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isLoading -> CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = accentColor,
                        strokeWidth = 2.dp,
                    )

                    backgroundUri != null -> AsyncImage(
                        model = backgroundUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )

                    else -> Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = accentColor,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                )
                Text(
                    text = stringResource(descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = accentColor,
            )
        }
    }
}

@Composable
private fun ContactFormSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
internal fun ContactFormActionRow(
    icon: ImageVector,
    title: String,
    value: String,
    accentColor: Color,
    onClick: () -> Unit,
    trailingContent: (@Composable (() -> Unit))? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = accentColor.copy(alpha = 0.12f),
        contentColor = accentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accentColor
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
    "com.android.local" -> Icons.Default.PhoneAndroid
    else -> Icons.Default.AccountCircle
}
