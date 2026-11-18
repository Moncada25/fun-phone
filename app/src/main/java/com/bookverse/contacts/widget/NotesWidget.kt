package com.bookverse.contacts.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.bookverse.contacts.MainActivity
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.db.AppDatabase
import com.bookverse.contacts.data.notes.Note
import com.bookverse.contacts.ui.navigation.DynamicShortcuts
import com.bookverse.contacts.ui.navigation.HomeShortcutDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotesWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val notes = withContext(Dispatchers.IO) {
            AppDatabase.getInstance(context).noteDao().getAllNotesSync().take(MAX_NOTES)
        }
        val strings = context.widgetStrings()
        provideContent {
            FunPhoneWidgetTheme {
                NotesContent(context = context, strings = strings, notes = notes)
            }
        }
    }

    companion object {
        private const val MAX_NOTES = 12
    }
}

internal fun notesListIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java)
        .putExtra("open_notes_list", true)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

private fun noteIntent(context: Context, note: Note): Intent =
    Intent(context, MainActivity::class.java)
        .setAction("com.bookverse.contacts.widget.OPEN_NOTE_${note.id}")
        .putExtra("open_note", true)
        .putExtra("note_id", note.id)
        .putExtra("is_note_locked", note.isLocked)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

@Composable
private fun NotesContent(context: Context, strings: Context, notes: List<Note>) {
    val openList = actionStartActivity(notesListIntent(context))
    WidgetSurface {
        WidgetHeader(
            title = strings.getString(R.string.notes_widget_recent_title),
            onTitleClick = openList,
            actionIcon = R.drawable.ic_widget_add,
            actionLabel = strings.getString(R.string.notes_widget_new_note),
            onAction = actionStartActivity(DynamicShortcuts.launchIntent(context, HomeShortcutDestination.NEW_NOTE)),
        )
        Spacer(GlanceModifier.height(6.dp))
        if (notes.isEmpty()) {
            WidgetMessage(strings.getString(R.string.notes_widget_empty), openList)
        } else {
            val compact = LocalSize.current.height < 140.dp
            LazyColumn {
                items(notes, itemId = { it.id }) { note ->
                    NoteRow(context, strings, note, compact)
                }
            }
        }
    }
}

@Composable
private fun NoteRow(context: Context, strings: Context, note: Note, compact: Boolean) {
    val title: String
    val preview: String
    if (note.isLocked) {
        title = strings.getString(R.string.notes_widget_locked_title)
        preview = strings.getString(R.string.notes_widget_locked_content)
    } else {
        title = note.title.ifBlank { strings.getString(R.string.notes_widget_no_content) }
        preview = note.content.stripHtml()
    }
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity(noteIntent(context, note)))
            .padding(vertical = 5.dp),
    ) {
        Text(
            text = title,
            maxLines = 1,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Medium, fontSize = 13.sp),
        )
        if (preview.isNotEmpty()) {
            Text(
                text = preview,
                maxLines = if (compact) 1 else 2,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            )
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
