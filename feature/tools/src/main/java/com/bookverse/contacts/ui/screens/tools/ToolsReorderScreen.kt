package com.bookverse.contacts.ui.screens.tools

import com.bookverse.contacts.data.model.ToolsPreset
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.ToolItem
import com.bookverse.contacts.ui.components.GroupItem
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SettingSwitch
import com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior
import com.bookverse.contacts.ui.components.rememberDestinationListState
import com.bookverse.contacts.ui.theme.LocalAppChromeStyle
import com.bookverse.contacts.ui.theme.AccentBarStyle
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.utils.ToolsRegistry

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ToolsReorderScreen(
    appSettingsViewModel: AppSettingsViewModel,
    onBackClick: () -> Unit,
    onPresetSelected: (ToolsPreset) -> Unit,
) {
    val toolsOrder by appSettingsViewModel.toolsOrder.collectAsStateWithLifecycle()
    val passwordManagerEnabled by appSettingsViewModel.passwordManagerEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    val toolItems = remember(toolsOrder, passwordManagerEnabled) {
        ToolsRegistry.getToolItems(toolsOrder, passwordManagerEnabled, includeHidden = true)
    }

    // Populate the first layout so a restored list offset is not clamped to the preset rows.
    val reorderItems = remember { mutableStateListOf<ToolItem>().apply { addAll(toolItems) } }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(toolItems) {
        reorderItems.clear()
        reorderItems.addAll(toolItems)
        initialized = true
    }

    val currentOrderIds by remember {
        derivedStateOf { reorderItems.map { it.id } }
    }

    LaunchedEffect(initialized, currentOrderIds) {
        if (initialized) {
            val newConfig = toolsOrder.copy(items = currentOrderIds)
            appSettingsViewModel.setToolsOrder(context, newConfig)
        }
    }

    val listState = rememberDestinationListState()
    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val modern = LocalAppChromeStyle.current == AccentBarStyle.MODERN
    // Browsing may expand the header; reordering must keep the drop targets stationary.
    val scrollBehavior = rememberAppBarScrollBehavior { modern && draggingItemId == null }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SecondaryTopBar(
                title = stringResource(R.string.tools_order_title),
                onBackClick = onBackClick,
                appSettingsViewModel = appSettingsViewModel,
                expandable = true,
                scrollBehavior = scrollBehavior,
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.tools_menu_visible_title),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.tools_menu_visible_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        SettingSwitch(
                            checked = !toolsOrder.toolsMenuHidden,
                            onCheckedChange = { checked ->
                                appSettingsViewModel.setToolsMenuHidden(context, !checked)
                            },
                        )
                    }
                }
            }

            item {
                ToolsPresetSelector(
                    toolsOrder = toolsOrder,
                    onPresetSelected = { preset ->
                        onPresetSelected(preset)
                        appSettingsViewModel.setToolsOrder(context, toolsOrder.applyPreset(preset))
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Text(
                    text = stringResource(R.string.tools_order_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            itemsIndexed(reorderItems, key = { _, item -> item.id }) { _, toolItem ->
                val isDragging = draggingItemId == toolItem.id
                val shadow = if (isDragging) with(density) { 16.dp.toPx() } else 0f
                val isHidden = toolItem.id in toolsOrder.hiddenItems

                GroupItem(
                    icon = toolItem.icon,
                    title = stringResource(toolItem.titleRes),
                    description = stringResource(toolItem.descriptionRes),
                    onClick = {
                        appSettingsViewModel.setToolHidden(context, toolItem.id, !isHidden)
                    },
                    trailing = {
                        SettingSwitch(
                            checked = !isHidden,
                            onCheckedChange = { checked ->
                                appSettingsViewModel.setToolHidden(context, toolItem.id, !checked)
                            },
                        )
                    },
                    modifier = Modifier
                        .alpha(if (isHidden) 0.45f else 1f)
                        .graphicsLayer {
                            if (isDragging) {
                                translationX = dragOffset.x
                                translationY = dragOffset.y
                                scaleX = 1.02f
                                scaleY = 1.02f
                                shadowElevation = shadow
                            } else {
                                translationX = 0f
                                translationY = 0f
                                scaleX = 1f
                                scaleY = 1f
                                shadowElevation = 0f
                            }
                        }
                        .pointerInput(toolItem.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    draggingItemId = toolItem.id
                                    dragOffset = Offset.Zero
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (draggingItemId != toolItem.id) return@detectDragGesturesAfterLongPress
                                    dragOffset += dragAmount

                                    val draggedIndex =
                                        reorderItems.indexOfFirst { it.id == toolItem.id }
                                    if (draggedIndex == -1) return@detectDragGesturesAfterLongPress

                                    val layoutInfo = listState.layoutInfo
                                    val draggedItemInfo =
                                        layoutInfo.visibleItemsInfo.firstOrNull { it.key == toolItem.id }
                                            ?: return@detectDragGesturesAfterLongPress

                                    val draggedCenter = draggedItemInfo.offset +
                                            draggedItemInfo.size / 2 +
                                            dragOffset.y.toInt()

                                    val targetItem =
                                        layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                            if (item.key == toolItem.id) return@firstOrNull false
                                            val itemTop = item.offset
                                            val itemBottom = item.offset + item.size
                                            draggedCenter in itemTop..itemBottom
                                        } ?: return@detectDragGesturesAfterLongPress

                                    val targetIndex =
                                        reorderItems.indexOfFirst { it.id == targetItem.key }
                                    if (targetIndex == -1 || targetIndex == draggedIndex) return@detectDragGesturesAfterLongPress

                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val itemToMove = reorderItems.removeAt(draggedIndex)
                                    reorderItems.add(targetIndex, itemToMove)
                                    dragOffset = Offset.Zero
                                },
                                onDragEnd = {
                                    draggingItemId = null
                                    dragOffset = Offset.Zero
                                },
                                onDragCancel = {
                                    draggingItemId = null
                                    dragOffset = Offset.Zero
                                },
                            )
                        },
                )
            }
        }
    }
}
