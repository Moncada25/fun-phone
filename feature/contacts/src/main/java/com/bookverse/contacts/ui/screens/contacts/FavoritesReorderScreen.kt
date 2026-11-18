package com.bookverse.contacts.ui.screens.contacts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.feature.contacts.R
import com.bookverse.contacts.core.ui.R as CoreUiR
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.ui.viewmodel.ContactsViewModel
import com.bookverse.contacts.ui.viewmodel.FavoritesSortOrder
import com.bookverse.contacts.utils.initialInfo
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FavoritesReorderScreen(
    appSettingsViewModel: AppSettingsViewModel,
    contactsViewModel: ContactsViewModel,
    onBackClick: () -> Unit
) {
    val contacts by contactsViewModel.contacts.collectAsStateWithLifecycle()
    val customOrder by appSettingsViewModel.favoritesCustomOrder.collectAsStateWithLifecycle()
    val columns by appSettingsViewModel.favoritesColumns.collectAsStateWithLifecycle()
    val cardShapeMode by appSettingsViewModel.favoritesCardShape.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
    val orderedFavorites = remember(alphabeticalFavorites, customOrder) {
        if (customOrder.isEmpty()) {
            alphabeticalFavorites
        } else {
            val orderMap = customOrder.withIndex().associate { it.value to it.index }
            alphabeticalFavorites.sortedWith(
                compareBy<Contact> { orderMap[it.id] ?: Int.MAX_VALUE }
                    .thenBy { alphabeticalIndex[it.id] ?: Int.MAX_VALUE }
            )
        }
    }

    val reorderItems = remember { mutableStateListOf<Contact>() }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(orderedFavorites) {
        reorderItems.clear()
        reorderItems.addAll(orderedFavorites)
        initialized = true
    }

    val currentOrderIds by remember {
        derivedStateOf { reorderItems.map { it.id } }
    }

    LaunchedEffect(initialized, currentOrderIds) {
        if (initialized) {
            appSettingsViewModel.updateFavoritesCustomOrder(context, currentOrderIds)
        }
    }

    LaunchedEffect(Unit) {
        appSettingsViewModel.setFavoritesSortOrder(context, FavoritesSortOrder.CUSTOM)
    }

    val gridState = rememberLazyGridState()
    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    Scaffold(
        topBar = {
            SecondaryTopBar(
                title = stringResource(R.string.favorites_order_title),
                onBackClick = onBackClick,
                appSettingsViewModel = appSettingsViewModel
            )
        }
    ) { paddingValues ->
        if (reorderItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.favorites_order_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.favorites_order_empty_description),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(32.dp))

                FilledTonalButton(onClick = onBackClick) {
                    Text(text = stringResource(CoreUiR.string.back))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Text(
                    text = stringResource(R.string.favorites_order_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                val density = LocalDensity.current

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(reorderItems, key = { _, item -> item.id }) { _, contact ->
                        val isDragging = draggingItemId == contact.id
                        val shadow = if (isDragging) with(density) { 16.dp.toPx() } else 0f

                        FavoriteContactItem(
                            contact = contact,
                            onContactClick = {},
                            textColor = MaterialTheme.colorScheme.onBackground,
                            contactsViewModel = contactsViewModel,
                            columns = columns,
                            shape = cardShape,
                            cardShapeMode = cardShapeMode,
                            modifier = Modifier
                                .graphicsLayer {
                                    if (isDragging) {
                                        translationX = dragOffset.x
                                        translationY = dragOffset.y
                                        scaleX = 1.04f
                                        scaleY = 1.04f
                                        shadowElevation = shadow
                                    } else {
                                        translationX = 0f
                                        translationY = 0f
                                        scaleX = 1f
                                        scaleY = 1f
                                        shadowElevation = 0f
                                    }
                                }
                                .pointerInput(contact.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingItemId = contact.id
                                            dragOffset = Offset.Zero
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            if (draggingItemId != contact.id) return@detectDragGesturesAfterLongPress
                                            dragOffset += dragAmount

                                            val draggedIndex =
                                                reorderItems.indexOfFirst { it.id == contact.id }
                                            if (draggedIndex == -1) return@detectDragGesturesAfterLongPress

                                            val layoutInfo = gridState.layoutInfo
                                            val draggedItemInfo =
                                                layoutInfo.visibleItemsInfo.firstOrNull { it.key == contact.id }
                                                    ?: return@detectDragGesturesAfterLongPress

                                            val draggedCenter = Offset(
                                                x = draggedItemInfo.offset.x + draggedItemInfo.size.width / 2f + dragOffset.x,
                                                y = draggedItemInfo.offset.y + draggedItemInfo.size.height / 2f + dragOffset.y
                                            )

                                            val targetInfo = layoutInfo.visibleItemsInfo
                                                .filter { it.key != contact.id }
                                                .firstOrNull { info ->
                                                    val left = info.offset.x.toFloat()
                                                    val top = info.offset.y.toFloat()
                                                    val right = left + info.size.width
                                                    val bottom = top + info.size.height
                                                    draggedCenter.x in left..right && draggedCenter.y in top..bottom
                                                }

                                            val targetIndex = targetInfo?.let { info ->
                                                reorderItems.indexOfFirst { it.id == info.key }
                                            }

                                            if (targetIndex != null && targetIndex != -1 && targetIndex != draggedIndex) {
                                                reorderItems.move(draggedIndex, targetIndex)
                                                dragOffset = Offset.Zero
                                            }
                                        },
                                        onDragCancel = {
                                            if (draggingItemId == contact.id) {
                                                dragOffset = Offset.Zero
                                                draggingItemId = null
                                            }
                                        },
                                        onDragEnd = {
                                            if (draggingItemId == contact.id) {
                                                dragOffset = Offset.Zero
                                                draggingItemId = null
                                            }
                                        }
                                    )
                                }
                        )
                    }
                }
            }
        }
    }
}

private fun <T> MutableList<T>.move(fromIndex: Int, toIndex: Int) {
    if (fromIndex == toIndex) return
    val element = removeAt(fromIndex)
    val target = if (toIndex > fromIndex) toIndex - 1 else toIndex
    add(target.coerceIn(0, size), element)
}
