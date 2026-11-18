package com.bookverse.contacts.ui.screens.contacts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.ui.components.ButtonClick
import com.bookverse.contacts.ui.components.FastScroller
import com.bookverse.contacts.ui.components.SearchFieldSurface
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.utils.ContactUtils
import com.bookverse.contacts.utils.avatarLabel
import com.bookverse.contacts.utils.groupContactsAlphabetically
import com.bookverse.contacts.utils.initialInfo
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFavoritesDialog(
    onDismiss: () -> Unit,
    onAddFavorites: (List<String>) -> Unit,
    contactsViewModel: ContactsViewModel,
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onBackground

    val contacts by contactsViewModel.contacts.collectAsStateWithLifecycle()

    val currentFavoriteIds =
        contacts.filter { it.isFavorite }.map { it.id }.toSet()
    var selectedContacts by remember { mutableStateOf(currentFavoriteIds) }

    val alphabeticalComparator = compareBy<Contact> { it.initialInfo().category }
        .thenBy { it.initialInfo().sortKey }
        .thenBy { it.name.lowercase(Locale.ROOT) }

    val allContacts = contacts.sortedWith(alphabeticalComparator)

    var searchQuery by remember { mutableStateOf("") }

    val filteredContacts by remember {
        derivedStateOf {
            if (searchQuery.isEmpty()) {
                allContacts
            } else {
                allContacts.filter { contact ->
                    contact.name.contains(searchQuery, ignoreCase = true) ||
                            contact.phones.any {
                                it.number.contains(
                                    searchQuery,
                                    ignoreCase = true
                                )
                            }
                }.sortedWith(alphabeticalComparator)
            }
        }
    }

    val groupedContacts = remember(filteredContacts, searchQuery) {
        if (searchQuery.isEmpty()) groupContactsAlphabetically(filteredContacts) else emptyList()
    }

    val alphabetSections = remember(groupedContacts) {
        if (groupedContacts.isNotEmpty()) {
            val sections = mutableListOf<Pair<String, Int>>()
            var position = 0
            groupedContacts.forEach { group ->
                sections.add(group.label to position)
                position += group.contacts.size
            }
            sections
        } else {
            emptyList()
        }
    }

    val listState = rememberLazyListState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
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
                            R.plurals.add_favorites_count,
                            selectedContacts.size,
                            selectedContacts.size,
                        ),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = stringResource(R.string.cancel),
                            tint = textColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                SearchFieldSurface(
                    legacyColor = textColor.copy(alpha = 0.2f),
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
                            fontSize = 16.sp
                        ),
                        cursorBrush = SolidColor(accentColor),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { innerTextField ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonSearch,
                                    contentDescription = stringResource(R.string.search_contacts),
                                    tint = textColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            stringResource(R.string.search_contacts),
                                            color = textColor.copy(alpha = 0.6f),
                                            fontSize = 16.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (contacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.contacts_empty),
                            color = textColor.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (filteredContacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_contacts_found),
                            color = textColor.copy(alpha = 0.6f),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            state = listState
                        ) {
                            itemsIndexed(filteredContacts, key = { _, contact -> contact.id }) { _, contact ->
                                AddFavoriteContactItem(
                                    contact = contact,
                                    isSelected = selectedContacts.contains(contact.id),
                                    onToggleSelection = {
                                        selectedContacts =
                                            if (selectedContacts.contains(contact.id)) {
                                                selectedContacts - contact.id
                                            } else {
                                                selectedContacts + contact.id
                                            }
                                    },
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    contactsViewModel = contactsViewModel
                                )
                            }
                        }

                        if (searchQuery.isEmpty() && filteredContacts.isNotEmpty() && contacts.size > 10) {
                            FastScroller(
                                listState = listState,
                                sections = alphabetSections,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(start = 8.dp, end = 4.dp),
                                accentColor = accentColor,
                                isDate = false
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
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.cancel),
                        fontSize = 18
                    )

                    Button(
                        onClick = {
                            onAddFavorites(selectedContacts.toList())
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = textColor
                        ),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.apply),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddFavoriteContactItem(
    contact: Contact,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    accentColor: Color,
    textColor: Color,
    contactsViewModel: ContactsViewModel
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggleSelection),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
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
                            modifier = Modifier.fillMaxSize(),
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
                                color = Color.White,
                                fontSize = 18.sp,
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
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp
                )
                if (contact.phones.isNotEmpty()) {
                    Text(
                        text = contact.phones.first().number,
                        color = textColor.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) accentColor else textColor.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
