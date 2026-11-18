package com.bookverse.contacts.ui.screens.notes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import com.bookverse.contacts.utils.NotesImageStore

@Stable
class NoteImagePicker internal constructor(
    val pendingRequestKey: String?,
    val pendingUris: List<Uri>,
    val launch: (requestKey: String) -> Unit,
    val consume: () -> Unit,
)

val LocalNoteImagePicker = compositionLocalOf<NoteImagePicker?> { null }

@Composable
fun rememberNoteImagePicker(): NoteImagePicker {
    var requestKey by rememberSaveable { mutableStateOf<String?>(null) }
    var pending by rememberSaveable { mutableStateOf(listOf<String>()) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(NotesImageStore.MAX_IMAGES_PER_PICK),
    ) { uris ->
        pending = uris.map { it.toString() }
        if (uris.isEmpty()) requestKey = null
    }
    return remember(requestKey, pending, launcher) {
        NoteImagePicker(
            pendingRequestKey = requestKey.takeIf { pending.isNotEmpty() },
            pendingUris = pending.map { it.toUri() },
            launch = { key ->
                pending = emptyList()
                requestKey = key
                launcher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            consume = {
                pending = emptyList()
                requestKey = null
            },
        )
    }
}
