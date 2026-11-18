package com.bookverse.contacts.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.core.ui.R

@Composable
internal fun HistoryNoResults(
    isSearch: Boolean,
    accentColor: Color,
    textColor: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = accentColor.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(
                if (isSearch) {
                    R.string.history_search_empty_title
                } else {
                    R.string.history_filter_empty_title
                }
            ),
            color = textColor.copy(alpha = 0.85f),
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(
                if (isSearch) {
                    R.string.history_search_empty_description
                } else {
                    R.string.history_filter_empty_description
                }
            ),
            color = textColor.copy(alpha = 0.65f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
        )
    }
}
