package com.bookverse.contacts.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ButtonSheetScaffold(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    confirmDismiss: () -> Boolean = { true },
    content: @Composable ColumnScope.() -> Unit,
) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val currentConfirmDismiss by rememberUpdatedState(confirmDismiss)
    val requestDismiss = remember {
        {
            if (currentConfirmDismiss()) {
                currentOnDismiss()
            }
        }
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            targetValue != SheetValue.Hidden || currentConfirmDismiss()
        },
    )
    val maxSheetHeight = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.height.toDp() * 0.9f
    }
    val contentScrollBarrier = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset = if (source == NestedScrollSource.UserInput) {
                Offset(x = 0f, y = available.y)
            } else {
                Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity,
            ): Velocity = Velocity(x = 0f, y = available.y)
        }
    }
    val headerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val headerContentColor = MaterialTheme.colorScheme.onSurface

    ModalBottomSheet(
        onDismissRequest = requestDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = true,
        tonalElevation = 0.dp,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = modifier
                // Keep boundary flings inside scrollable content while preserving header drags.
                .nestedScroll(contentScrollBarrier)
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp),
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = headerColor,
                contentColor = headerContentColor,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (navigationIcon != null) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            navigationIcon()
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        subtitle?.takeIf(String::isNotBlank)?.let { supportingText ->
                            Text(
                                text = supportingText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = headerContentColor.copy(alpha = 0.78f),
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
