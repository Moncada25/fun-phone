package com.bookverse.contacts.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.bookverse.contacts.MainActivity
import com.bookverse.contacts.R
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.data.notes.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val NOTE_ID_KEY = ActionParameters.Key<Long>("note_id")
val OPEN_NOTES_LIST_KEY = ActionParameters.Key<Boolean>("open_notes_list")
val IS_NOTE_LOCKED_KEY = ActionParameters.Key<Boolean>("is_note_locked")

class NotesWidgetActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val noteId = parameters[NOTE_ID_KEY]
        val openList = parameters[OPEN_NOTES_LIST_KEY] ?: false
        if (noteId != null && noteId > 0L) {
            intent.putExtra("open_note", true)
            intent.putExtra("note_id", noteId)
            intent.putExtra("is_note_locked", parameters[IS_NOTE_LOCKED_KEY] ?: false)
        } else if (openList) {
            intent.putExtra("open_notes_list", true)
        }
        context.startActivity(intent)
    }
}

class NotesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val notes = withContext(Dispatchers.IO) {
            AppDatabase.getInstance(context).noteDao().getAllNotesSync()
                .take(5)
        }
        provideContent {
            GlanceTheme {
                WidgetContent(notes = notes)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetContent(notes: List<Note>) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.background)
            .padding(12.dp),
    ) {
        WidgetHeader(context = context)
        Spacer(modifier = GlanceModifier.height(8.dp))
        if (notes.isEmpty()) {
            EmptyState(context = context)
        } else {
            NotesList(notes = notes, context = context)
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetHeader(context: Context) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.notes_widget_recent_title),
            style = TextStyle(
                color = GlanceTheme.colors.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            ),
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Box(modifier = GlanceModifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = "+",
                modifier = GlanceModifier
                    .clickable(
                        actionRunCallback<NotesWidgetActionCallback>(
                            actionParametersOf(OPEN_NOTES_LIST_KEY to true)
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = TextStyle(
                    color = GlanceTheme.colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                ),
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun EmptyState(context: Context) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = context.getString(R.string.notes_widget_empty),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 13.sp,
            ),
        )
    }
}

@androidx.compose.runtime.Composable
private fun NotesList(notes: List<Note>, context: Context) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        notes.forEachIndexed { index, note ->
            NoteItem(note = note, context = context)
            if (index < notes.lastIndex) {
                Spacer(
                    modifier = GlanceModifier
                        .height(1.dp)
                        .fillMaxWidth()
                        .background(GlanceTheme.colors.outline),
                )
                Spacer(modifier = GlanceModifier.height(3.dp))
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun NoteItem(note: Note, context: Context) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(
                actionRunCallback<NotesWidgetActionCallback>(
                    actionParametersOf(
                        NOTE_ID_KEY to note.id,
                        IS_NOTE_LOCKED_KEY to note.isLocked,
                    )
                )
            )
            .padding(vertical = 4.dp),
    ) {
        if (note.isLocked) {
            Text(
                text = context.getString(R.string.notes_widget_locked_title),
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                ),
            )
            Text(
                text = context.getString(R.string.notes_widget_locked_content),
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 12.sp,
                ),
            )
        } else {
            val title = note.title.ifBlank { context.getString(R.string.notes_widget_no_content) }
            val contentPreview = note.content.stripHtml()
            Text(
                text = title,
                maxLines = 1,
                style = TextStyle(
                    color = GlanceTheme.colors.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                ),
            )
            if (contentPreview.isNotEmpty()) {
                Text(
                    text = contentPreview,
                    maxLines = 2,
                    style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                        fontSize = 12.sp,
                    ),
                )
            }
        }
    }
}

private fun String.stripHtml(): String =
    replace(Regex("<[^>]+>"), " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")
        .trim()
        .replace(Regex("\\s+"), " ")
