package com.bookverse.contacts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.R
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Smartphone

@Composable
fun PermissionBanner(
    onReviewPhoneStatus: () -> Unit,
    onChooseTools: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().background(colors.background).padding(horizontal = 24.dp, vertical = 12.dp),
        shape = RoundedCornerShape(22.dp),
        color = colors.surface,
        contentColor = colors.onSurface,
    ) {
        Column(
            modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Lucide.Smartphone, null, tint = colors.onSurface, modifier = Modifier.size(24.dp))
                Text(stringResource(R.string.permission_banner_title), style = MaterialTheme.typography.titleMedium)
            }
            Text(stringResource(R.string.permission_banner_description), style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = onReviewPhoneStatus,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
            ) { Text(stringResource(R.string.permission_banner_cta)) }
            TextButton(
                onClick = onChooseTools,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = colors.onSurface),
            ) { Text(stringResource(R.string.onboarding_choose_tools)) }
        }
    }
}
