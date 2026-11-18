package com.bookverse.contacts.ui.screens.contacts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.bookverse.contacts.feature.contacts.R
import com.bookverse.contacts.ui.components.MessageDialog
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.viewmodel.ContactVCardResult

const val VCARD_MIME_TYPE = "text/x-vcard"
val VCARD_OPEN_MIME_TYPES = arrayOf(VCARD_MIME_TYPE, "text/vcard", "text/directory")

@Composable
fun ContactVCardResultHost(
    result: ContactVCardResult?,
    isImporting: Boolean,
    onConsumed: () -> Unit,
    onMessage: (String, SnackbarType) -> Unit,
) {
    val resources = LocalResources.current
    LaunchedEffect(result) {
        when (result) {
            is ContactVCardResult.Exported -> {
                onMessage(
                    resources.getQuantityString(
                        R.plurals.vcard_export_success,
                        result.count,
                        result.count,
                    ),
                    SnackbarType.Success,
                )
                onConsumed()
            }

            ContactVCardResult.ExportFailed -> {
                onMessage(resources.getString(R.string.vcard_export_error), SnackbarType.Error)
                onConsumed()
            }

            ContactVCardResult.ImportFailed -> {
                onMessage(resources.getString(R.string.vcard_import_error), SnackbarType.Error)
                onConsumed()
            }

            is ContactVCardResult.Imported, null -> Unit
        }
    }

    if (result is ContactVCardResult.Imported && !isImporting) {
        val summary = result.summary
        MessageDialog(
            title = stringResource(R.string.vcard_import_result_title),
            message1 = listOf(
                pluralStringResource(
                    R.plurals.vcard_import_imported,
                    summary.imported,
                    summary.imported,
                ),
                pluralStringResource(
                    R.plurals.vcard_import_skipped,
                    summary.skippedDuplicates,
                    summary.skippedDuplicates,
                ),
                pluralStringResource(
                    R.plurals.vcard_import_failed,
                    summary.failed,
                    summary.failed,
                ),
            ).joinToString("\n"),
            onDismiss = onConsumed,
            onButtonClick = onConsumed,
        )
    }
}
