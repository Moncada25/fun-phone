package com.bookverse.contacts.ui.screens.contacts.dialer

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.PersonAddAlt1
import androidx.compose.material.icons.outlined.Whatsapp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.ui.components.NumberKeypad
import com.bookverse.contacts.ui.theme.AppChrome
import com.bookverse.contacts.ui.theme.CallAcceptColor
import com.bookverse.contacts.ui.theme.chromePolicy
import com.bookverse.contacts.ui.viewmodel.AccentBarStyle
import com.bookverse.contacts.utils.ContactUtils

@Composable
internal fun DialerScreenLayout(
    modifier: Modifier = Modifier,
    accentColor: Color,
    textColor: Color,
    accentBarStyle: AccentBarStyle,
    fontType: String,
    dtmfTonesEnabled: String,
    vibrationDuration: Int,
    vibrationPattern: String,
    phoneFieldValue: TextFieldValue,
    dialedNumber: String,
    suggestionEntries: List<DialerSuggestionEntry>,
    filteredContacts: List<DialerContactSearchIndex>,
    unknownTitle: String,
    onValueChange: (TextFieldValue) -> Unit,
    registerEditText: (DialerEditText) -> Unit,
    onContactCall: (String) -> Unit,
    onAddContact: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onDelete: () -> Unit,
    onLongDelete: () -> Unit,
    onDigit: (String) -> Unit,
    onOneLongPress: () -> Unit,
    onNumberLongPress: (Int) -> Unit,
    loadContactPhoto: suspend (Uri) -> Bitmap?,
    numberContext: DialerNumberContext = DialerNumberContext(),
    onCountryClick: (() -> Unit)? = null,
    onMatchedContactClick: (() -> Unit)? = null,
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter,
    ) {
        val compactHeight = maxHeight < 680.dp
        val availableHeight = maxHeight
        val fontScale = LocalDensity.current.fontScale
        val keypadSpacing = if (compactHeight) 4.dp else 8.dp
        val compactButtonSize = if (compactHeight && fontScale < 1.5f) 52.dp else null

        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DialerResultsArea(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                dialedNumber = dialedNumber,
                suggestionEntries = suggestionEntries,
                filteredContacts = filteredContacts,
                unknownTitle = unknownTitle,
                accentColor = accentColor,
                textColor = textColor,
                loadContactPhoto = loadContactPhoto,
                onContactCall = onContactCall,
                onAddContact = onAddContact,
            )

            DialerControlPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (compactHeight) {
                            Modifier
                                .heightIn(max = availableHeight)
                                .verticalScroll(rememberScrollState())
                        } else {
                            Modifier
                        }
                    ),
                accentColor = accentColor,
                accentBarStyle = accentBarStyle,
                fontType = fontType,
                dtmfTonesEnabled = dtmfTonesEnabled,
                vibrationDuration = vibrationDuration,
                vibrationPattern = vibrationPattern,
                phoneFieldValue = phoneFieldValue,
                dialedNumber = dialedNumber,
                numberContext = numberContext,
                onCountryClick = onCountryClick,
                onMatchedContactClick = onMatchedContactClick,
                onValueChange = onValueChange,
                registerEditText = registerEditText,
                onAddContact = onAddContact,
                onCall = onCall,
                onWhatsApp = onWhatsApp,
                onDelete = onDelete,
                onLongDelete = onLongDelete,
                onDigit = onDigit,
                onOneLongPress = onOneLongPress,
                onNumberLongPress = onNumberLongPress,
                keypadButtonSize = compactButtonSize,
                keypadSpacing = keypadSpacing,
            )
        }
    }
}

@Composable
internal fun DialerResultsArea(
    modifier: Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
    modern: Boolean = false,
    dialedNumber: String,
    suggestionEntries: List<DialerSuggestionEntry>,
    filteredContacts: List<DialerContactSearchIndex>,
    unknownTitle: String,
    accentColor: Color,
    textColor: Color,
    loadContactPhoto: suspend (Uri) -> Bitmap?,
    onContactCall: (String) -> Unit,
    onAddContact: () -> Unit,
) {
    val showAddContactCta = dialedNumber.isNotBlank() && dialedNumber.none { it == '*' || it == '#' }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(if (modern) 2.dp else 8.dp),
    ) {
        if (dialedNumber.isEmpty()) {
            item(key = "dialer_suggestions_title") {
                DialerSectionHeading(
                    text = stringResource(R.string.dialer_suggestions_title),
                    modern = modern,
                )
            }

            if (suggestionEntries.isEmpty()) {
                item(key = "dialer_no_recent_operations") {
                    DialerEmptySuggestions(modern = modern)
                }
            } else {
                itemsIndexed(
                    items = suggestionEntries,
                    key = { _, entry -> entry.key },
                ) { index, entry ->
                    DialerGroupedSurfaceItem(
                        index = index,
                        total = suggestionEntries.size,
                        modern = modern,
                    ) {
                        ContactGroupedItem(
                            contact = entry.contact,
                            subtitle = entry.number,
                            highlightQuery = null,
                            unknownTitle = unknownTitle,
                            accentColor = accentColor,
                            textColor = textColor,
                            loadContactPhoto = loadContactPhoto,
                            modern = modern,
                            onClick = { onContactCall(entry.number) },
                        )
                    }
                }
            }
        } else if (filteredContacts.isEmpty()) {
            if (showAddContactCta) {
                item(key = "dialer_add_contact") {
                    DialerAddContactCard(
                        number = dialedNumber,
                        accentColor = accentColor,
                        textColor = textColor,
                        modern = modern,
                        onClick = onAddContact,
                    )
                }
            }
        } else {
            item(key = "dialer_contacts_title") {
                DialerSectionHeading(
                    text = stringResource(R.string.dialer_contacts_title),
                    modern = modern,
                )
            }

            itemsIndexed(
                items = filteredContacts,
                key = { _, entry -> entry.contact.id },
            ) { index, entry ->
                DialerGroupedSurfaceItem(
                    index = index,
                    total = filteredContacts.size,
                    modern = modern,
                ) {
                    ContactGroupedItem(
                        contact = entry.contact,
                        subtitle = entry.primaryPhone,
                        highlightQuery = dialedNumber,
                        unknownTitle = unknownTitle,
                        accentColor = accentColor,
                        textColor = textColor,
                        loadContactPhoto = loadContactPhoto,
                        modern = modern,
                        onClick = {
                            if (entry.primaryPhone.isNotEmpty()) {
                                onContactCall(entry.primaryPhone)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DialerControlPanel(
    modifier: Modifier,
    accentColor: Color,
    accentBarStyle: AccentBarStyle,
    fontType: String,
    dtmfTonesEnabled: String,
    vibrationDuration: Int,
    vibrationPattern: String,
    phoneFieldValue: TextFieldValue,
    dialedNumber: String,
    numberContext: DialerNumberContext,
    onCountryClick: (() -> Unit)?,
    onMatchedContactClick: (() -> Unit)?,
    onValueChange: (TextFieldValue) -> Unit,
    registerEditText: (DialerEditText) -> Unit,
    onAddContact: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onDelete: () -> Unit,
    onLongDelete: () -> Unit,
    onDigit: (String) -> Unit,
    onOneLongPress: () -> Unit,
    onNumberLongPress: (Int) -> Unit,
    keypadButtonSize: androidx.compose.ui.unit.Dp?,
    keypadSpacing: androidx.compose.ui.unit.Dp,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isMinimal = accentBarStyle.chromePolicy.neutralControls
    val density = LocalDensity.current
    val dialerTextSize = if (keypadButtonSize != null) {
        MaterialTheme.typography.headlineLarge.fontSize
    } else {
        MaterialTheme.typography.displaySmall.fontSize * 0.8f
    }
    val panelShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    val panelBorder = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.18f))

    Surface(
        modifier = modifier,
        shape = panelShape,
        color = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        tonalElevation = if (isMinimal) 0.dp else 2.dp,
        border = panelBorder,
    ) {
        Column(
            modifier = Modifier.padding(
                start = if (keypadButtonSize != null) 12.dp else 20.dp,
                end = if (keypadButtonSize != null) 12.dp else 20.dp,
                top = if (keypadButtonSize != null) 12.dp else 18.dp,
                bottom = if (keypadButtonSize != null) 16.dp else 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (keypadButtonSize != null) 10.dp else 16.dp),
        ) {
            if (!numberContext.isEmpty || onCountryClick != null) {
                DialerNumberContextRow(
                    context = numberContext,
                    modifier = Modifier.fillMaxWidth(),
                    onCountryClick = onCountryClick,
                    onContactClick = onMatchedContactClick,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val canAddContact = dialedNumber.isNotEmpty()
                IconButton(
                    onClick = onAddContact,
                    enabled = canAddContact,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonAddAlt1,
                        contentDescription = stringResource(R.string.add_contact),
                        tint = if (canAddContact) {
                            accentColor
                        } else {
                            colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                        modifier = Modifier.size(26.dp),
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = colorScheme.onSurface.copy(alpha = 0.05f),
                    contentColor = colorScheme.onSurface,
                    tonalElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        DialerNumberEditor(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .focusable(true),
                            value = phoneFieldValue,
                            onValueChange = onValueChange,
                            registerEditText = registerEditText,
                            textColor = colorScheme.onSurface.toArgb(),
                            textSizePx = with(density) { dialerTextSize.toPx() },
                            cursorColor = accentColor.toArgb(),
                            selectionBackgroundColor = accentColor.copy(alpha = 0.4f).toArgb(),
                        )
                    }
                }

                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    DialerDeleteButton(
                        enabled = canAddContact,
                        tint = accentColor,
                        onClick = onDelete,
                        onLongClick = onLongDelete,
                    )
                }
            }

            NumberKeypad(
                modifier = Modifier.fillMaxWidth(),
                onKeyPressed = onDigit,
                onOneLongPress = onOneLongPress,
                onNumberLongPress = onNumberLongPress,
                vibrationDuration = vibrationDuration,
                vibrationPattern = vibrationPattern,
                accentColor = accentColor,
                fontType = fontType,
                accentBarStyle = accentBarStyle,
                dtmfTonesEnabled = dtmfTonesEnabled,
                buttonSize = keypadButtonSize,
                keySpacing = keypadSpacing,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DialerActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Whatsapp,
                    label = stringResource(R.string.recent_type_whatsapp),
                    iconContentDescription = stringResource(R.string.whatsapp_action),
                    containerColor = if (isMinimal) {
                        colorScheme.surface
                    } else {
                        accentColor.copy(alpha = 0.14f)
                    },
                    contentColor = colorScheme.onSurface,
                    onClick = onWhatsApp,
                )
                DialerActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Call,
                    label = stringResource(R.string.call),
                    containerColor = CallAcceptColor,
                    contentColor = contentColorFor(CallAcceptColor),
                    onClick = onCall,
                )
            }
        }
    }
}

@Composable
private fun DialerActionButton(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconContentDescription: String = label,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 52.dp)
            .semantics { role = Role.Button },
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconContentDescription,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DialerSectionHeading(text: String, modern: Boolean) {
    Text(
        text = text,
        style = if (modern) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (modern) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onBackground
        },
        modifier = if (modern) {
            Modifier.padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp)
        } else {
            Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        },
    )
}

@Composable
private fun DialerEmptySuggestions(modern: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp),
        shape = RoundedCornerShape(if (modern) AppChrome.groupCorner else 22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = if (modern) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.no_recent_operations),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun DialerAddContactCard(
    number: String,
    accentColor: Color,
    textColor: Color,
    modern: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .semantics { role = Role.Button },
        shape = RoundedCornerShape(if (modern) AppChrome.groupCorner else 22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = if (modern) null else BorderStroke(1.dp, accentColor.copy(alpha = 0.24f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = if (modern) {
                    Modifier
                        .size(48.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape)
                } else {
                    Modifier
                },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonAddAlt1,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(if (modern) 24.dp else 28.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.add_contact),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                )
                Text(
                    text = number,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun DialerGroupedSurfaceItem(
    index: Int,
    total: Int,
    modern: Boolean,
    content: @Composable () -> Unit,
) {
    if (total <= 0) return

    val isFirst = index == 0
    val isLast = index == total - 1
    val outerCorner = if (modern) AppChrome.groupCorner else 22.dp
    val innerCorner = if (modern) 6.dp else 8.dp
    val shape = RoundedCornerShape(
        topStart = if (isFirst) outerCorner else innerCorner,
        topEnd = if (isFirst) outerCorner else innerCorner,
        bottomStart = if (isLast) outerCorner else innerCorner,
        bottomEnd = if (isLast) outerCorner else innerCorner,
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = if (modern) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        },
    ) {
        content()
    }
}

@Composable
private fun ContactGroupedItem(
    contact: Contact,
    subtitle: String,
    highlightQuery: String?,
    unknownTitle: String,
    accentColor: Color,
    textColor: Color,
    loadContactPhoto: suspend (Uri) -> Bitmap?,
    modern: Boolean,
    onClick: () -> Unit,
) {
    val contactName = contact.name.ifBlank { unknownTitle }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContactAvatar(
            contact = contact,
            loadContactPhoto = loadContactPhoto,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = contactName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 2,
            )
            if (subtitle.isNotBlank()) {
                if (highlightQuery.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.7f),
                    )
                } else {
                    Text(
                        text = ContactUtils.highlightText(subtitle, highlightQuery, accentColor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.7f),
                    )
                }
            }
        }
        if (subtitle.isNotBlank()) {
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(48.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (modern) accentColor.copy(alpha = 0.12f) else Color.Transparent,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = stringResource(R.string.call),
                        tint = accentColor,
                        modifier = Modifier.size(if (modern) 22.dp else 24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactAvatar(
    contact: Contact,
    loadContactPhoto: suspend (Uri) -> Bitmap?,
    modifier: Modifier = Modifier,
) {
    val placeholderColor = remember(contact.id, contact.name) {
        ContactUtils.getRandomColor(contact.name)
    }
    val photoUri = contact.photoUri
    if (photoUri != null) {
        val photoState by produceState<Bitmap?>(initialValue = null, key1 = photoUri) {
            value = loadContactPhoto(photoUri)
        }
        if (photoState != null) {
            Image(
                bitmap = photoState!!.asImageBitmap(),
                contentDescription = stringResource(
                    R.string.contact_photo_description,
                    contact.name,
                ),
                modifier = modifier.clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            return
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(placeholderColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = stringResource(
                R.string.contact_avatar_placeholder,
                contact.name,
            ),
            modifier = Modifier.size(22.dp),
            tint = contentColorFor(placeholderColor),
        )
    }
}
