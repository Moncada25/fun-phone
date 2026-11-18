package com.bookverse.contacts.ui.screens.dialer

import com.bookverse.contacts.utils.PhoneActionLauncher
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.data.model.ContactPhone
import com.bookverse.contacts.data.model.RecentType
import com.bookverse.contacts.data.model.historyKey
import com.bookverse.contacts.settings.Countries
import com.bookverse.contacts.settings.Country
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.CountrySelectDialog
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.components.withoutTop
import com.bookverse.contacts.settings.AppDataStore
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.utils.VibrationUtils
import contacts.core.entities.PhoneEntity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialerScreen(
    onBack: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel = viewModel(),
    directory: DialerDirectory,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    onAddContactClick: (String, Country?) -> Unit = { _, _ -> },
    phoneActions: PhoneActionLauncher,
    onCallPhoneNumber: (Context, String) -> Unit = phoneActions::callPhoneNumber,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    var selectedCountryIso by rememberSaveable {
        mutableStateOf(Countries.getDefaultCountry(context)?.countryCode)
    }
    val configuration = LocalConfiguration.current
    val selectedCountry = remember(selectedCountryIso, configuration) {
        selectedCountryIso?.let { iso ->
            Countries.getCountryList(context).firstOrNull { it.countryCode == iso }
        }
    }
    val countryCallingCodeDigits = remember(configuration) {
         Countries.getCountryList(context).mapNotNull { country ->
            country.code.filter(Char::isDigit).takeIf { it.isNotBlank() }
        }
    }
    val accentColor = MaterialTheme.colorScheme.primary
    val fontType by appSettingsViewModel.fontType.collectAsStateWithLifecycle()
    val accentBarStyle by appSettingsViewModel.accentBarStyle.collectAsStateWithLifecycle()
    val dtmfTonesEnabled by appSettingsViewModel.dtmfTonesEnabled.collectAsStateWithLifecycle()
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val dataStore = remember(context) { AppDataStore.getDataStore(context) }
    var lastDialedNumber by remember { mutableStateOf<String?>(null) }
    val unknownTitle = stringResource(R.string.unknown)
    val vibrationDuration by appSettingsViewModel.vibrationDuration.collectAsStateWithLifecycle()
    val vibrationPattern by appSettingsViewModel.vibrationPattern.collectAsStateWithLifecycle()
    val profileContact = directory.profile.collectAsStateWithLifecycle().value
    val localRecents by directory.localOperations.collectAsStateWithLifecycle()
    val callLogRecents by directory.callLogs.collectAsStateWithLifecycle()
    val topUsedContacts by directory.topUsedContacts.collectAsStateWithLifecycle(initialValue = emptyList())
    val speedDialContacts by appSettingsViewModel.speedDialContacts.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    var showCountryModal by remember { mutableStateOf(false) }
    val snackbar = rememberAppSnackbarState()
    var phoneFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = phoneNumber,
                selection = TextRange(phoneNumber.length)
            )
        )
    }
    val dialedNumber = phoneFieldValue.text
    val keyboardController = LocalSoftwareKeyboardController.current
    val dialerEditTextRef = remember { mutableStateOf<DialerEditText?>(null) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        onDispose { dialerEditTextRef.value = null }
    }
    DisposableEffect(lifecycleOwner, keyboardController) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    keyboardController?.hide()
                    dialerEditTextRef.value?.let { editText ->
                        editText.post {
                            val imm = editText.context
                                .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            imm?.hideSoftInputFromWindow(editText.windowToken, 0)
                        }
                    }
                }

                Lifecycle.Event.ON_PAUSE -> {
                    dialerEditTextRef.value?.let { editText ->
                        editText.clearFocus()
                        val imm = editText.context
                            .getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.hideSoftInputFromWindow(editText.windowToken, 0)
                    }
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val allContacts by directory.contacts.collectAsStateWithLifecycle()

    val smartSuggestions = remember(localRecents, callLogRecents, topUsedContacts) {
        buildSmartSuggestions(
            localRecents = localRecents,
            callLogRecents = callLogRecents,
            topUsedContacts = topUsedContacts
        )
    }

    val suggestionEntries = remember(smartSuggestions) {
        smartSuggestions.mapNotNull { operation ->
            val targetNumber = operation.target.ifBlank {
                operation.contact.phones.firstOrNull()?.number.orEmpty()
            }
            targetNumber.takeIf { it.isNotBlank() }?.let {
                DialerSuggestionEntry(
                    key = operation.historyKey,
                    contact = operation.contact,
                    number = it
                )
            }
        }
    }

    val contactSearchIndex = remember(allContacts) {
        buildDialerContactSearchIndex(allContacts)
    }
    val filteredContacts = remember(contactSearchIndex, dialedNumber) {
        filterDialerContacts(contactSearchIndex, dialedNumber)
    }

    fun sanitizeDialerInput(value: String): String {
        return sanitizeDialerText(value)
    }

    fun buildFullNumber(rawNumber: String): String {
        return buildDialerTarget(rawNumber, selectedCountry?.code)
    }

    val fullNumber = buildFullNumber(dialedNumber)
    val countryCallingCodeSet = remember(countryCallingCodeDigits) { countryCallingCodeDigits.toSet() }
    val matchedContactName = remember(fullNumber, allContacts, countryCallingCodeSet) {
        findExactDialerContact(fullNumber, allContacts, countryCallingCodeSet)?.name
    }
    val singleSuggestedContact = filteredContacts.singleOrNull()
        ?.takeIf { matchedContactName == null }
    val chipContactName = matchedContactName ?: singleSuggestedContact?.contact?.name
    val numberContext = remember(selectedCountry, chipContactName) {
        val country = selectedCountry
        DialerNumberContext(
            callingCode = country?.code?.takeIf { it.isNotBlank() },
            countryFlag = country?.flag,
            countryName = country?.name,
            contactName = chipContactName,
        )
    }

    fun persistLastDialedNumber(number: String) {
        val sanitized = sanitizeDialerInput(number)
        if (sanitized.isBlank()) return
        lastDialedNumber = sanitized
        coroutineScope.launch {
            runCatching {
                dataStore.edit { prefs ->
                    prefs[LAST_DIALED_NUMBER_KEY] = sanitized
                }
            }
        }
    }

    fun fallbackUpdateText(newRaw: String, newSelection: Int) {
        val sanitized = sanitizePastedDialerText(newRaw, newSelection)
        phoneFieldValue = TextFieldValue(
            text = sanitized.text,
            selection = TextRange(sanitized.selectionStart),
        )
        onPhoneNumberChange(sanitized.text)
    }

    fun insertSymbol(symbol: String) {
        val sanitizedSymbol = sanitizeDialerInput(symbol)
        if (sanitizedSymbol.isEmpty()) return
        val editText = dialerEditTextRef.value
        if (editText != null) {
            val current = editText.text?.toString().orEmpty()
            val start = min(editText.selectionStart, editText.selectionEnd).coerceAtLeast(0)
            val end = max(editText.selectionStart, editText.selectionEnd).coerceAtLeast(0)
            val prospective = current.take(start) + sanitizedSymbol + current.substring(end)
            val sanitizedResult = sanitizeDialerInput(prospective)
            if (sanitizedResult.length > MAX_DIALED_NUMBERS) return
            editText.text?.replace(start, end, sanitizedSymbol)
            val newSelection = (start + sanitizedSymbol.length).coerceAtMost(
                editText.text?.length ?: sanitizedResult.length
            )
            editText.setSelection(newSelection)
        } else {
            val updated = insertDialerText(
                current = DialerEditValue(
                    text = phoneFieldValue.text,
                    selectionStart = phoneFieldValue.selection.start,
                    selectionEnd = phoneFieldValue.selection.end,
                ),
                insertedText = sanitizedSymbol,
            )
            if (updated.text != phoneFieldValue.text ||
                updated.selectionStart != phoneFieldValue.selection.start
            ) {
                fallbackUpdateText(updated.text, updated.selectionStart)
            }
        }
    }

    fun deleteSymbol(deleteAll: Boolean = false) {
        val editText = dialerEditTextRef.value
        if (editText != null) {
            val text = editText.text ?: return
            if (deleteAll) {
                text.clear()
                return
            }
            val start = editText.selectionStart
            val end = editText.selectionEnd
            if (start != end) {
                val removeStart = min(start, end).coerceAtLeast(0)
                val removeEnd = max(start, end).coerceAtLeast(0)
                text.delete(removeStart, removeEnd)
                editText.setSelection(removeStart)
            } else if (start > 0) {
                text.delete(start - 1, start)
                editText.setSelection(start - 1)
            }
        } else {
            val updated = deleteDialerText(
                current = DialerEditValue(
                    text = phoneFieldValue.text,
                    selectionStart = phoneFieldValue.selection.start,
                    selectionEnd = phoneFieldValue.selection.end,
                ),
                deleteAll = deleteAll,
            )
            if (updated != DialerEditValue(
                    text = phoneFieldValue.text,
                    selectionStart = phoneFieldValue.selection.start,
                    selectionEnd = phoneFieldValue.selection.end,
                )
            ) {
                fallbackUpdateText(updated.text, updated.selectionStart)
            }
        }
    }

    fun normalizePhoneNumber(number: String): String {
        return number.replace(Regex("[^0-9]"), "")
    }

    fun arePhoneNumbersEquivalent(
        candidateNumber: String,
        normalizedTarget: String,
        countryCodesDigits: Collection<String>
    ): Boolean = areDialerNumbersEquivalent(candidateNumber, normalizedTarget, countryCodesDigits)

    fun resolveNumberExistence(targetNumber: String): NumberExistenceResult {
        val normalizedTarget = normalizePhoneNumber(targetNumber)
        if (normalizedTarget.isBlank()) return NumberExistenceResult(
            contactExists = false,
            profileExists = false
        )

        fun matchesCandidate(rawNumber: String?): Boolean {
            if (rawNumber.isNullOrBlank()) return false
            return arePhoneNumbersEquivalent(
                rawNumber,
                normalizedTarget,
                countryCallingCodeSet
            )
        }

        val existsInContacts = allContacts.any { contact ->
            contact.phones.any { matchesCandidate(it.number) }
        }
        val existsInProfile = profileContact?.phones?.any { matchesCandidate(it.number) } ?: false

        return NumberExistenceResult(existsInContacts, existsInProfile)
    }

    fun openWhatsApp(targetNumber: String) {
        coroutineScope.launch {
            val contact = directory.findContactByPhoneNumber(targetNumber)
            directory.addRecentOperation(
                contact ?: Contact(
                    name = unknownTitle,
                    phones = listOf(
                        ContactPhone(
                            number = targetNumber,
                            type = PhoneEntity.Type.OTHER,
                        )
                    ),
                ),
                type = RecentType.WHATSAPP_OPEN,
                target = targetNumber,
            )
        }
        phoneActions.openWhatsApp(context, targetNumber)
    }

    fun placeCall(targetNumber: String) {
        directory.recordCallUsage(targetNumber)
        onCallPhoneNumber(context, targetNumber)
    }

    fun handleDialerAction(onExecute: (String) -> Unit) {
        when (
            val resolution = resolveDialerAction(
                currentNumber = dialedNumber,
                lastDialedNumber = lastDialedNumber,
                countryCode = selectedCountry?.code,
            )
        ) {
            DialerActionResolution.Empty -> snackbar.show(
                message = resources.getString(R.string.enter_a_number),
                type = SnackbarType.Info,
            )
            is DialerActionResolution.RestoreLastDialed -> {
                phoneFieldValue = TextFieldValue(
                    text = resolution.number,
                    selection = TextRange(resolution.number.length),
                )
                if (dialedNumber != resolution.number) {
                    onPhoneNumberChange(resolution.number)
                }
            }
            is DialerActionResolution.Execute -> {
                persistLastDialedNumber(dialedNumber)
                onExecute(resolution.targetNumber)
            }
        }
    }

    LaunchedEffect(Unit) {
        val storedNumber = runCatching {
            dataStore.data.firstOrNull()?.get(LAST_DIALED_NUMBER_KEY)
        }.getOrNull()

        if (!storedNumber.isNullOrBlank()) {
            lastDialedNumber = sanitizeDialerInput(storedNumber)
        }

        directory.fetchAllRecentOperations()
    }

    LaunchedEffect(phoneNumber) {
        if (phoneFieldValue.text != phoneNumber) {
            phoneFieldValue = phoneFieldValue.copy(
                text = phoneNumber,
                selection = TextRange(phoneNumber.length)
            )
        }
    }

    LaunchedEffect(Unit) {
        keyboardController?.hide()
    }

    BackHandler {
        onBack()
    }

    fun requestAddContact() {
        val existence = resolveNumberExistence(fullNumber)
        when {
            existence.contactExists -> {
                coroutineScope.launch {
                    snackbar.show(
                        message = resources.getString(R.string.number_already_exists),
                        type = SnackbarType.Info,
                    )
                }
            }
            existence.profileExists -> {
                coroutineScope.launch {
                    snackbar.show(
                        message = resources.getString(R.string.profile_already_exists),
                        type = SnackbarType.Info,
                    )
                }
            }
            else -> onAddContactClick(dialedNumber, selectedCountry)
        }
    }

    val isModern = accentBarStyle == AccentBarStyle.MODERN
    var keypadSheetHeight by remember { mutableStateOf(0.dp) }
    val onDialerValueChange: (TextFieldValue) -> Unit = { updatedValue ->
        phoneFieldValue = updatedValue
        onPhoneNumberChange(updatedValue.text)
    }
    val onContactCall: (String) -> Unit = { number ->
        directory.recordCallUsage(number)
        onCallPhoneNumber(context, number)
    }
    val onMatchedContactClick: () -> Unit = {
        val suggested = singleSuggestedContact
        if (suggested != null) {
            onContactCall(suggested.primaryPhone)
        } else {
            handleDialerAction(::placeCall)
        }
    }
    val onLongDelete: () -> Unit = {
        VibrationUtils.vibrate(context, vibrationDuration, vibrationPattern)
        deleteSymbol(deleteAll = true)
    }
    val onOneLongPress: () -> Unit = {
        VibrationUtils.vibrate(context, vibrationDuration, vibrationPattern)
        phoneActions.callVoicemail(context)
    }
    val onNumberLongPress: (Int) -> Unit = { number ->
        handleSpeedDialLongPress(
            phoneActions = phoneActions,
            number,
            speedDialContacts,
            directory.contacts.value,
            context,
            resources,
            snackbar,
        )
    }

    Scaffold(
        topBar = {
            SecondaryTopBar(
                title = stringResource(R.string.dialer),
                onBackClick = onBack,
                appSettingsViewModel = appSettingsViewModel,
            )
        },
        snackbarHost = { AppSnackbarHost(snackbar.hostState) },
        containerColor = backgroundColor,
        contentOverlay = { overlayPadding ->
            if (isModern) {
                DialerModernKeypadSheet(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = overlayPadding.calculateTopPadding()),
                    bottomInset = overlayPadding.calculateBottomPadding(),
                    accentColor = accentColor,
                    fontType = fontType,
                    dtmfTonesEnabled = dtmfTonesEnabled,
                    vibrationDuration = vibrationDuration,
                    vibrationPattern = vibrationPattern,
                    phoneFieldValue = phoneFieldValue,
                    dialedNumber = dialedNumber,
                    numberContext = numberContext,
                    onCountryClick = { showCountryModal = true },
                    onMatchedContactClick = onMatchedContactClick,
                    onValueChange = onDialerValueChange,
                    registerEditText = { dialerEditTextRef.value = it },
                    onAddContact = ::requestAddContact,
                    onCall = { handleDialerAction(::placeCall) },
                    onWhatsApp = { handleDialerAction(::openWhatsApp) },
                    onDelete = { deleteSymbol() },
                    onLongDelete = onLongDelete,
                    onDigit = ::insertSymbol,
                    onOneLongPress = onOneLongPress,
                    onNumberLongPress = onNumberLongPress,
                    onHeightChanged = { keypadSheetHeight = it },
                )
            }
        },
    ) { paddingValues ->
        if (isModern) {
            DialerModernResults(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues.withoutTop()),
                topInset = paddingValues.calculateTopPadding(),
                bottomInset = (keypadSheetHeight - paddingValues.calculateBottomPadding()).coerceAtLeast(0.dp),
                dialedNumber = dialedNumber,
                suggestionEntries = suggestionEntries,
                filteredContacts = filteredContacts,
                unknownTitle = unknownTitle,
                accentColor = accentColor,
                textColor = textColor,
                loadContactPhoto = directory::loadContactPhoto,
                onContactCall = onContactCall,
                onAddContact = ::requestAddContact,
            )
        } else {
            DialerScreenLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(paddingValues),
                accentColor = accentColor,
                textColor = textColor,
                accentBarStyle = accentBarStyle,
                fontType = fontType,
                dtmfTonesEnabled = dtmfTonesEnabled,
                vibrationDuration = vibrationDuration,
                vibrationPattern = vibrationPattern,
                phoneFieldValue = phoneFieldValue,
                dialedNumber = dialedNumber,
                numberContext = numberContext,
                onCountryClick = { showCountryModal = true },
                onMatchedContactClick = onMatchedContactClick,
                suggestionEntries = suggestionEntries,
                filteredContacts = filteredContacts,
                unknownTitle = unknownTitle,
                onValueChange = onDialerValueChange,
                registerEditText = { dialerEditTextRef.value = it },
                onContactCall = onContactCall,
                onAddContact = ::requestAddContact,
                onCall = { handleDialerAction(::placeCall) },
                onWhatsApp = { handleDialerAction(::openWhatsApp) },
                onDelete = { deleteSymbol() },
                onLongDelete = onLongDelete,
                onDigit = ::insertSymbol,
                onOneLongPress = onOneLongPress,
                onNumberLongPress = onNumberLongPress,
                loadContactPhoto = directory::loadContactPhoto,
            )
        }
    }

    if (showCountryModal) {
        CountrySelectDialog(
            onDismiss = { showCountryModal = false },
            onCountrySelected = { country ->
                selectedCountryIso = country?.countryCode
            },
            selectedCountry = selectedCountry,
            showCountryCode = true,
        )
    }
}
