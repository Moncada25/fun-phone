package com.bookverse.contacts.ui.screens.settings

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.feature.settings.R
import com.bookverse.contacts.ui.components.ContactAvatarText
import com.bookverse.contacts.ui.components.SearchFieldSurface
import com.bookverse.contacts.utils.contactAvatarLabel
import com.bookverse.contacts.core.ui.R as CoreUiR

internal fun filterSpeedDialContacts(
    contacts: List<Contact>,
    query: String,
): List<Contact> {
    val matchingContacts = if (query.isEmpty()) {
        contacts
    } else {
        contacts.filter { contact -> contact.name.contains(query, ignoreCase = true) }
    }
    return matchingContacts.sortedBy { contact -> contact.name }
}

internal fun resolveSpeedDialContact(
    contacts: List<Contact>,
    contactId: String?,
): Contact? = contactId?.let { id -> contacts.firstOrNull { contact -> contact.id == id } }
@Composable
internal fun SpeedDialConfigSheet(
    onDismiss: () -> Unit,
    contacts: List<Contact>,
    speedDialContacts: Map<Int, String>,
    accentColor: Color,
    onAssignmentChanged: (Int, String?) -> Unit,
    loadContactPhoto: (Uri) -> Bitmap?,
) {
    var selectedNumber by remember { mutableStateOf<Int?>(null) }
    val pickerNumber = selectedNumber

    SettingsSheetScaffold(
        title = stringResource(
            if (pickerNumber == null) {
                R.string.speed_dial_configure
            } else {
                R.string.speed_dial_assign_contact
            },
        ),
        onDismiss = onDismiss,
        confirmDismiss = {
            if (selectedNumber == null) {
                true
            } else {
                selectedNumber = null
                false
            }
        },
    ) {
        if (pickerNumber == null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(10) { number ->
                    val isVoicemail = number == 1
                    val assignedContact = resolveSpeedDialContact(
                        contacts = contacts,
                        contactId = speedDialContacts[number],
                    )
                    SpeedDialNumberItem(
                        number = number,
                        assignedContact = assignedContact,
                        isDisabled = isVoicemail,
                        accentColor = accentColor,
                        textColor = MaterialTheme.colorScheme.onSurface,
                        onClick = {
                            if (!isVoicemail) selectedNumber = number
                        },
                        onRemove = {
                            if (!isVoicemail) onAssignmentChanged(number, null)
                        },
                    )
                }
            }
        } else {
            ContactPickerContent(
                contacts = contacts,
                loadContactPhoto = loadContactPhoto,
                accentColor = accentColor,
                textColor = MaterialTheme.colorScheme.onSurface,
                onContactSelected = { contact ->
                    onAssignmentChanged(pickerNumber, contact.id)
                    selectedNumber = null
                },
            )
        }
    }
}
@Composable
private fun SpeedDialNumberItem(
    number: Int,
    assignedContact: Contact?,
    isDisabled: Boolean,
    accentColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isDisabled) { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isDisabled) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        } else if (assignedContact != null) {
            accentColor.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDisabled) textColor.copy(alpha = 0.4f) else accentColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (isDisabled) {
                    Text(
                        text = stringResource(CoreUiR.string.voicemail),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor.copy(alpha = 0.6f)
                    )
                    Text(
                        text = stringResource(R.string.speed_dial_voicemail_reserved),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.4f)
                    )
                } else if (assignedContact != null) {
                    Text(
                        text = assignedContact.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    assignedContact.phones.firstOrNull()?.let { phone ->
                        Text(
                            text = phone.number,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.speed_dial_not_assigned),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.6f)
                    )
                }
            }

            if (!isDisabled && assignedContact != null) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = stringResource(R.string.speed_dial_remove_contact),
                        tint = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ContactPickerContent(
    contacts: List<Contact>,
    loadContactPhoto: (Uri) -> Bitmap?,
    onContactSelected: (Contact) -> Unit,
    accentColor: Color,
    textColor: Color,
) {
    var searchQuery by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    val filteredContacts = remember(contacts, searchQuery) {
        filterSpeedDialContacts(contacts, searchQuery)
    }

    SearchFieldSurface(
        legacyColor = MaterialTheme.colorScheme.surface,
        allowBackdrop = false,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
    ) {
        BasicTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                color = textColor,
                fontSize = 16.sp,
            ),
            cursorBrush = SolidColor(accentColor),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
            ),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonSearch,
                        contentDescription = stringResource(CoreUiR.string.search),
                        tint = textColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(CoreUiR.string.search),
                                color = textColor.copy(alpha = 0.6f),
                                fontSize = 16.sp,
                            )
                        }
                        innerTextField()
                    }
                }
            },
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (filteredContacts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(CoreUiR.string.no_contacts_found),
                color = textColor.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(
                items = filteredContacts,
                key = Contact::id,
            ) { contact ->
                ContactPickerItem(
                    contact = contact,
                    loadContactPhoto = loadContactPhoto,
                    accentColor = accentColor,
                    textColor = textColor,
                    onClick = { onContactSelected(contact) },
                )
            }
        }
    }
}
@Composable
private fun ContactPickerItem(
    contact: Contact,
    loadContactPhoto: (Uri) -> Bitmap?,
    accentColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    val photoBitmap = remember(contact.photoUri) {
        contact.photoUri?.let(loadContactPhoto)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    ContactAvatarText(
                        text = contact.name.contactAvatarLabel(emptyLabel = "?"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                contact.phones.firstOrNull()?.let { phone ->
                    Text(
                        text = phone.number,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
