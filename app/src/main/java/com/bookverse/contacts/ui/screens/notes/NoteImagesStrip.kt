package com.bookverse.contacts.ui.screens.notes

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.ui.components.FullScreenImage
import com.bookverse.contacts.utils.NotesImageStore

internal const val NOTE_IMAGE_THUMBNAIL_EDGE = 320
internal const val NOTE_IMAGE_FULL_EDGE = 2048

internal sealed interface NoteImageLoad {
    data object Loading : NoteImageLoad
    data object Failed : NoteImageLoad
    data class Ready(val bitmap: ImageBitmap) : NoteImageLoad
}

@Composable
internal fun rememberNoteImage(fileName: String, maxLongEdge: Int): NoteImageLoad {
    val context = LocalContext.current.applicationContext
    val state by produceState<NoteImageLoad>(NoteImageLoad.Loading, fileName, maxLongEdge) {
        value = NotesImageStore.loadBitmap(context, fileName, maxLongEdge)
            ?.let { NoteImageLoad.Ready(it.asImageBitmap()) }
            ?: NoteImageLoad.Failed
    }
    return state
}

@Composable
internal fun NoteImagesStrip(
    fileNames: List<String>,
    editable: Boolean,
    importing: Boolean,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (fileNames.isEmpty() && !importing) return
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
    ) {
        items(fileNames, key = { it }) { fileName ->
            val load = rememberNoteImage(fileName, NOTE_IMAGE_THUMBNAIL_EDGE)
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(
                        enabled = load is NoteImageLoad.Ready,
                        role = Role.Image,
                        onClickLabel = stringResource(R.string.note_image_open),
                    ) { onOpen(fileName) },
                contentAlignment = Alignment.Center,
            ) {
                when (load) {
                    is NoteImageLoad.Ready -> Image(
                        bitmap = load.bitmap,
                        contentDescription = stringResource(R.string.note_image_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )

                    NoteImageLoad.Loading -> Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    NoteImageLoad.Failed -> Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = stringResource(R.string.note_image_unavailable),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (editable) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                            .clickable(
                                role = Role.Button,
                                onClickLabel = stringResource(R.string.note_image_remove),
                            ) { onRemove(fileName) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.note_image_remove),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        if (importing) {
            item(key = "note-image-importing") {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
internal fun NoteImageViewer(fileName: String, onDismiss: () -> Unit) {
    val load = rememberNoteImage(fileName, NOTE_IMAGE_FULL_EDGE)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(load) {
        if (load == NoteImageLoad.Failed) currentOnDismiss()
    }
    if (load is NoteImageLoad.Ready) {
        FullScreenImage(bitmap = load.bitmap, onDismiss = onDismiss)
    }
}
