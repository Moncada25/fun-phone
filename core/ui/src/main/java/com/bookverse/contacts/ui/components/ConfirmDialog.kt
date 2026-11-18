package com.bookverse.contacts.ui.components

import com.bookverse.contacts.ui.theme.LocalAppChromeStyle
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.ui.theme.accentPalette
import com.bookverse.contacts.ui.theme.AccentBarStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    message: String,
    confirmButtonLabel: String? = null,
    toastMessage: String? = null,
    isDestructive: Boolean = true,
) {
    val context = LocalContext.current
    val accentBarStyle = LocalAppChromeStyle.current
    val colorScheme = MaterialTheme.colorScheme
    val accentPalette = MaterialTheme.accentPalette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val isRetro = accentBarStyle == AccentBarStyle.RETRO
    val headerColor = if (isRetro) {
        accentPalette.seed
    } else {
        colorScheme.onSurface.copy(alpha = 0.06f)
    }
    val headerContentColor = if (isRetro) accentPalette.onSeed else colorScheme.onSurface
    val actionColor = if (isDestructive) colorScheme.error else colorScheme.primary
    val actionContentColor = contentColorFor(actionColor)
    val actionShape = if (isRetro) RoundedCornerShape(999.dp) else RoundedCornerShape(16.dp)
    val resolvedConfirmLabel = confirmButtonLabel ?: stringResource(R.string.delete)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = headerColor,
                contentColor = headerContentColor,
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(
                    text = title,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            if (message.isNotBlank()) {
                Text(
                    text = message,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurface,
                )
            }

            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                    toastMessage?.let { messageToShow ->
                        Toast.makeText(context, messageToShow, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = actionColor,
                    contentColor = actionContentColor,
                ),
                shape = actionShape,
            ) {
                Icon(
                    imageVector = if (isDestructive) Icons.Default.Delete else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(resolvedConfirmLabel)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
