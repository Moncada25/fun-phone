package com.bookverse.contacts.ui.screens.contacts

import com.bookverse.contacts.ui.components.ContactAvatarText
import com.bookverse.contacts.utils.ContactDisplay
import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import com.bookverse.contacts.feature.contacts.R
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import com.bookverse.contacts.ui.theme.GradientScrimColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.ButtonSheetScaffold
import com.bookverse.contacts.ui.components.MainTopBar
import com.bookverse.contacts.ui.components.PermissionBanner
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.ui.viewmodel.FavoritesCardShape
import com.bookverse.contacts.ui.viewmodel.FavoritesSortOrder
import com.bookverse.contacts.utils.avatarLabel
import com.bookverse.contacts.utils.initialInfo
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    contactsViewModel: ContactsViewModel,
    onContactClick: (Contact) -> Unit,
    onOpenFavoritesOrder: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    appSettingsViewModel: AppSettingsViewModel,
    profileContact: Contact? = null,
    gridState: LazyGridState = rememberLazyGridState(),
    isLimitedMode: Boolean = false,
    onReviewPhoneStatus: () -> Unit = {},
    onChooseTools: () -> Unit = {},
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onBackground
    val contacts by contactsViewModel.contacts.collectAsStateWithLifecycle()
    val columns by appSettingsViewModel.favoritesColumns.collectAsStateWithLifecycle()
    val cardShapeMode by appSettingsViewModel.favoritesCardShape.collectAsStateWithLifecycle()
    val sortOrder by appSettingsViewModel.favoritesSortOrder.collectAsStateWithLifecycle()
    val customOrder by appSettingsViewModel.favoritesCustomOrder.collectAsStateWithLifecycle()
    val showProfileInFavorites by appSettingsViewModel.showProfileInFavorites.collectAsStateWithLifecycle()
    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior()
    val snackbar = rememberAppSnackbarState()
    val context = LocalContext.current
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showAddFavoritesDialog by remember { mutableStateOf(false) }
    val cardShape = remember(cardShapeMode) { cardShapeMode.toComposeShape() }
    val favorites = remember(contacts) {
        contacts.filter { it.isFavorite }
    }
    val alphabeticalFavorites = remember(favorites) {
        favorites.sortedWith(
            compareBy<Contact> { it.initialInfo().category }
                .thenBy { it.initialInfo().sortKey }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
    }
    val alphabeticalIndex = remember(alphabeticalFavorites) {
        alphabeticalFavorites.withIndex().associate { it.value.id to it.index }
    }
    val favoriteContacts = remember(alphabeticalFavorites, customOrder, sortOrder) {
        if (sortOrder == FavoritesSortOrder.CUSTOM && customOrder.isNotEmpty()) {
            val orderMap = customOrder.withIndex().associate { it.value to it.index }
            alphabeticalFavorites.sortedWith(
                compareBy<Contact> { orderMap[it.id] ?: Int.MAX_VALUE }
                    .thenBy { alphabeticalIndex[it.id] ?: Int.MAX_VALUE }
            )
        } else {
            alphabeticalFavorites
        }
    }
    val favoriteIds = remember(favorites) { favorites.map { it.id } }

    LaunchedEffect(Unit) {
        contactsViewModel.snackbarEvents.collect { ev ->
            snackbar.show(
                message = ev.message,
                type = ev.kind,
                actionLabel = ev.actionLabel
            )
        }
    }

    val hasContacts = contacts.isNotEmpty()

    LaunchedEffect(sortOrder, favoriteIds, customOrder, hasContacts) {
        if (sortOrder == FavoritesSortOrder.CUSTOM && hasContacts) {
            val sanitized = customOrder.filter { it in favoriteIds } +
                    favoriteIds.filter { it !in customOrder }
            if (sanitized != customOrder) {
                appSettingsViewModel.updateFavoritesCustomOrder(context, sanitized)
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                MainTopBar(
                    title = stringResource(R.string.favorites_title),
                    scrollBehavior = scrollBehavior,
                    actions = {
                        IconAction(
                            imageVector = Icons.Default.DisplaySettings,
                            contentDescription = stringResource(R.string.favorites_settings_action),
                            onClick = { showSettingsSheet = true }
                        )
                        if (favoriteContacts.isNotEmpty() || showProfileInFavorites) {
                            IconAction(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = stringResource(R.string.add_favorites_cta),
                                onClick = { showAddFavoritesDialog = true }
                            )
                        }
                    }
                )
                if (isLimitedMode) {
                    PermissionBanner(
                        onReviewPhoneStatus = onReviewPhoneStatus,
                        onChooseTools = onChooseTools,
                    )
                }
            }
        },
        snackbarHost = { AppSnackbarHost(snackbar.hostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            if (favoriteContacts.isEmpty() && !showProfileInFavorites && !isLimitedMode) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = accentColor.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.no_favorites),
                        color = textColor.copy(alpha = 0.8f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.no_favorites_description),
                        color = textColor.copy(alpha = 0.6f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = accentColor.copy(alpha = 0.2f)
                        ),
                        onClick = { showAddFavoritesDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.add_favorites_cta),
                                color = textColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp + com.bookverse.contacts.ui.components.LocalMainContentBottomPadding.current, top = paddingValues.calculateTopPadding() + 12.dp),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    state = gridState
                ) {
                    if (showProfileInFavorites) {
                        item(key = "profile") {
                            ProfileFavoriteItem(
                                profileContact = profileContact,
                                onProfileClick = onNavigateToProfile,
                                textColor = textColor,
                                accentColor = accentColor,
                                contactsViewModel = contactsViewModel,
                                columns = columns,
                                shape = cardShape,
                                cardShapeMode = cardShapeMode,
                            )
                        }
                    }

                    items(favoriteContacts, key = { it.id }) { contact ->
                        FavoriteContactItem(
                            contact = contact,
                            onContactClick = { onContactClick(contact) },
                            textColor = textColor,
                            contactsViewModel = contactsViewModel,
                            columns = columns,
                            shape = cardShape,
                            cardShapeMode = cardShapeMode,
                        )
                    }
                }
            }
        }

        if (showAddFavoritesDialog) {
            val message = stringResource(R.string.multiple_contacts_modified)
            AddFavoritesDialog(
                onDismiss = { showAddFavoritesDialog = false },
                onAddFavorites = { selectedContactIds ->
                    val currentFavorites =
                        contacts.filter { it.isFavorite }.map { it.id }.toSet()
                    val newSelection = selectedContactIds.toSet()

                    val toAdd = newSelection - currentFavorites
                    val toRemove = currentFavorites - newSelection

                    toAdd.forEach { contactId ->
                        val contact = contacts.find { it.id == contactId }
                        if (contact != null) {

                            contactsViewModel.setFavorite(contactId, true, message)
                        }
                    }

                    toRemove.forEach { contactId ->
                        val contact = contacts.find { it.id == contactId }
                        if (contact != null) {
                            contactsViewModel.setFavorite(contactId, false, "")
                        }
                    }
                },
                contactsViewModel = contactsViewModel,
            )
        }

        if (showSettingsSheet) {
            val sheetScrollState = rememberScrollState()
            ButtonSheetScaffold(
                title = stringResource(R.string.favorites_settings_title),
                onDismiss = { showSettingsSheet = false },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(sheetScrollState)
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Text(
                        text = stringResource(R.string.favorites_size_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    ListItem(
                        modifier = Modifier.clickable {
                            val targetColumns = if (columns <= 2) 3 else 2
                            appSettingsViewModel.setFavoritesColumns(context, targetColumns)
                        },
                        headlineContent = {
                            Text(text = stringResource(R.string.favorites_large_cards))
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.favorites_large_cards_description),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = columns <= 2,
                                onCheckedChange = { isLarge ->
                                    val targetColumns = if (isLarge) 2 else 3
                                    appSettingsViewModel.setFavoritesColumns(context, targetColumns)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = accentColor,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                                    checkedTrackColor = accentColor.copy(alpha = 0.5f),
                                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.5f
                                    )
                                )
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    ListItem(
                        modifier = Modifier.clickable {
                            appSettingsViewModel.setShowProfileInFavorites(
                                context,
                                !showProfileInFavorites
                            )
                        },
                        headlineContent = {
                            Text(text = stringResource(R.string.favorites_show_profile))
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.favorites_show_profile_description),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.PersonOutline,
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = showProfileInFavorites,
                                onCheckedChange = { isEnabled ->
                                    appSettingsViewModel.setShowProfileInFavorites(
                                        context,
                                        isEnabled
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = accentColor,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                                    checkedTrackColor = accentColor.copy(alpha = 0.5f),
                                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.5f
                                    )
                                )
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    ListItem(
                        modifier = Modifier.clickable {
                            appSettingsViewModel.setFavoritesCardShape(
                                context,
                                if (cardShapeMode == FavoritesCardShape.ROUND) {
                                    FavoritesCardShape.SQUARE
                                } else {
                                    FavoritesCardShape.ROUND
                                }
                            )
                        },
                        headlineContent = {
                            Text(text = stringResource(R.string.favorites_shape_section_title))
                        },
                        supportingContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(text = stringResource(R.string.favorites_shape_round))
                                Text(
                                    text = stringResource(R.string.favorites_shape_round_description),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Circle,
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = cardShapeMode == FavoritesCardShape.ROUND,
                                onCheckedChange = { isRound ->
                                    appSettingsViewModel.setFavoritesCardShape(
                                        context,
                                        if (isRound) {
                                            FavoritesCardShape.ROUND
                                        } else {
                                            FavoritesCardShape.SQUARE
                                        }
                                    )
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = accentColor,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
                                    checkedTrackColor = accentColor.copy(alpha = 0.5f),
                                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.5f
                                    )
                                )
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                    Text(
                        text = stringResource(R.string.favorites_order_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    val onSelectAlphabetical = {
                        appSettingsViewModel.setFavoritesSortOrder(
                            context,
                            FavoritesSortOrder.ALPHABETICAL
                        )
                    }
                    val onSelectCustom = {
                        if (sortOrder != FavoritesSortOrder.CUSTOM) {
                            appSettingsViewModel.setFavoritesSortOrder(
                                context,
                                FavoritesSortOrder.CUSTOM
                            )
                        }
                        if (favoriteIds.isNotEmpty()) {
                            val sanitized = customOrder.filter { it in favoriteIds } +
                                    favoriteIds.filter { it !in customOrder }
                            appSettingsViewModel.updateFavoritesCustomOrder(context, sanitized)
                        }
                        showSettingsSheet = false
                        onOpenFavoritesOrder()
                    }

                    ListItem(
                        modifier = Modifier.clickable { onSelectAlphabetical() },
                        headlineContent = {
                            Text(text = stringResource(R.string.favorites_order_alphabetical))
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.favorites_order_alphabetical_description),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.SortByAlpha,
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            RadioButton(
                                selected = sortOrder == FavoritesSortOrder.ALPHABETICAL,
                                onClick = { onSelectAlphabetical() },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = accentColor,
                                    unselectedColor = textColor
                                )
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    ListItem(
                        modifier = Modifier.clickable { onSelectCustom() },
                        headlineContent = {
                            Text(text = stringResource(R.string.favorites_order_custom))
                        },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.favorites_order_custom_description),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            RadioButton(
                                selected = sortOrder == FavoritesSortOrder.CUSTOM,
                                onClick = { onSelectCustom() },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = accentColor,
                                    unselectedColor = textColor
                                )
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteContactItem(
    contact: Contact,
    onContactClick: () -> Unit,
    textColor: Color,
    contactsViewModel: ContactsViewModel,
    columns: Int,
    shape: Shape,
    cardShapeMode: FavoritesCardShape,
    modifier: Modifier = Modifier
) {
    val photoUri = contact.photoUri
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = photoUri) {
        value = photoUri?.let { contactsViewModel.loadContactPhoto(it) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .clickable(onClick = onContactClick)
            .background(ContactDisplay.getRandomColor(contact.name)),
        contentAlignment = Alignment.Center
    ) {
        val avatarFontSize = if (columns == 2) 70.sp else 54.sp
        val avatarTextModifier = if (columns == 2) {
            Modifier
        } else {
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        }

        if (photoUri != null) {
            val loadedBitmap = bitmap
            if (loadedBitmap != null) {
                Image(
                    bitmap = loadedBitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.cd_contact_photo),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (cardShapeMode != FavoritesCardShape.ROUND) {
                ContactAvatarText(
                    text = contact.avatarLabel(),
                    color = textColor,
                    fontSize = avatarFontSize,
                    fontWeight = FontWeight.Bold,
                    modifier = avatarTextModifier
                )
            }
        } else if (cardShapeMode != FavoritesCardShape.ROUND) {
            ContactAvatarText(
                text = contact.avatarLabel(),
                color = textColor,
                fontSize = avatarFontSize,
                fontWeight = FontWeight.Bold,
                modifier = avatarTextModifier
            )
        }

        FavoriteNameOverlay(
            name = contact.name,
            cardShapeMode = cardShapeMode,
        )
    }
}

@Composable
fun ProfileFavoriteItem(
    profileContact: Contact?,
    onProfileClick: () -> Unit,
    textColor: Color,
    accentColor: Color,
    contactsViewModel: ContactsViewModel,
    columns: Int,
    shape: Shape,
    cardShapeMode: FavoritesCardShape,
    modifier: Modifier = Modifier
) {
    val photoUri = profileContact?.photoUri
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = photoUri) {
        value = photoUri?.let { contactsViewModel.loadContactPhoto(it) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .clickable(onClick = onProfileClick)
            .background(
                if (profileContact != null) {
                    ContactDisplay.getRandomColor(profileContact.name)
                } else {
                    accentColor.copy(alpha = 0.3f)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        val avatarFontSize = if (columns == 2) 70.sp else 54.sp
        val avatarTextModifier = if (columns == 2) {
            Modifier
        } else {
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        }

        if (profileContact != null) {
            if (photoUri != null) {
                val loadedBitmap = bitmap
                if (loadedBitmap != null) {
                    Image(
                        bitmap = loadedBitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.cd_contact_photo),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    ContactAvatarText(
                        text = profileContact.avatarLabel(),
                        color = textColor,
                        fontSize = avatarFontSize,
                        fontWeight = FontWeight.Bold,
                        modifier = avatarTextModifier
                    )
                }
            } else {
                val iconSize = if (columns == 2) 80.dp else 60.dp
                Icon(
                    imageVector = Icons.Outlined.PersonOutline,
                    contentDescription = stringResource(CoreUiR.string.profile_item),
                    modifier = avatarTextModifier.size(iconSize),
                    tint = accentColor
                )
            }
        } else {
            val iconSize = if (columns == 2) 80.dp else 60.dp
            Icon(
                imageVector = Icons.Outlined.PersonOutline,
                contentDescription = stringResource(CoreUiR.string.profile_item),
                modifier = avatarTextModifier.size(iconSize),
                tint = accentColor
            )
        }

        FavoriteNameOverlay(
            name = stringResource(R.string.my_profile),
            cardShapeMode = cardShapeMode,
        )
    }
}

@Composable
private fun BoxScope.FavoriteNameOverlay(
    name: String,
    cardShapeMode: FavoritesCardShape,
) {
    if (cardShapeMode == FavoritesCardShape.ROUND) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            FavoriteNameText(name = name, centered = true)
        }
    } else {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, GradientScrimColor)
                    )
                )
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 24.dp)
        ) {
            FavoriteNameText(name = name, centered = false)
        }
    }
}

@Composable
private fun FavoriteNameText(
    name: String,
    centered: Boolean,
) {
    Text(
        modifier = if (centered) Modifier.padding(horizontal = 24.dp) else Modifier,
        text = name,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

internal fun FavoritesCardShape.toComposeShape(): Shape = when (this) {
    FavoritesCardShape.SQUARE -> RoundedCornerShape(24.dp)
    FavoritesCardShape.ROUND -> CircleShape
}
