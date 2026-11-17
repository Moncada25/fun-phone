package com.bookverse.contacts.ui.screens.contacts

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bookverse.contacts.R
import com.bookverse.contacts.domain.contacts.ContactSortOrder
import com.bookverse.contacts.ui.components.SelectBottomSheet

@StringRes
internal fun ContactSortOrder.labelRes(): Int = when (this) {
    ContactSortOrder.FIRST_NAME -> R.string.contact_sort_first_name
    ContactSortOrder.LAST_NAME -> R.string.contact_sort_last_name
    ContactSortOrder.RECENTLY_ADDED -> R.string.contact_sort_recently_added
}

@Composable
internal fun ContactSortSheet(
    selected: ContactSortOrder,
    onSelect: (ContactSortOrder) -> Unit,
    onDismiss: () -> Unit,
) {
    SelectBottomSheet(
        onDismiss = onDismiss,
        title = stringResource(R.string.contact_sort_title),
        options = ContactSortOrder.entries,
        selectedOption = selected,
        onSelect = onSelect,
        optionKey = ContactSortOrder::name,
        optionLabel = { order -> stringResource(order.labelRes()) },
    )
}
