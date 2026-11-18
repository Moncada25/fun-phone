package com.bookverse.contacts.widget

import com.bookverse.contacts.navigation.AppEntryPoints
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.utils.ContactUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Immutable
internal data class FavoriteWidgetEntry(
    val id: String,
    val name: String,
    val phone: String,
    val avatar: Bitmap,
)

class FavoritesWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entries = withContext(Dispatchers.IO) {
            favoriteWidgetEntries(context, FavoritesWidgetDataSource.loadFavorites(context).take(MAX_FAVORITES))
        }
        val strings = context.widgetStrings()
        provideContent {
            FunPhoneWidgetTheme {
                FavoritesContent(context, strings, entries)
            }
        }
    }

    companion object {
        private const val MAX_FAVORITES = 16
    }
}

private const val AVATAR_DP = 52f

internal fun favoriteWidgetEntries(context: Context, contacts: List<Contact>): List<FavoriteWidgetEntry> {
    val size = (context.resources.displayMetrics.density * AVATAR_DP).roundToInt().coerceAtLeast(1)
    return contacts.map { contact ->
        val photo = contact.photoUri?.let { uri -> runCatching { ContactUtils.uriToBitmap(uri, context) }.getOrNull() }
        FavoriteWidgetEntry(
            id = contact.id,
            name = contact.name,
            phone = contact.phones.firstOrNull()?.number.orEmpty(),
            avatar = photo?.let { circularCrop(it, size) } ?: initialAvatar(contact.name, size),
        )
    }
}

private fun favoritesIntent(context: Context): Intent =
    AppEntryPoints.mainActivityIntent(context)
        .putExtra("open_favorites", true)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

private fun contactIntent(context: Context, entry: FavoriteWidgetEntry): Intent =
    AppEntryPoints.contactDetailIntent(context)
        .setAction("com.bookverse.contacts.widget.OPEN_CONTACT_${entry.id}")
        .putExtra(AppEntryPoints.EXTRA_CONTACT_ID, entry.id)
        .putExtra(AppEntryPoints.EXTRA_CONTACT_NAME, entry.name)
        .putExtra(AppEntryPoints.EXTRA_CONTACT_PHONE, entry.phone)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

@Composable
private fun FavoritesContent(context: Context, strings: Context, entries: List<FavoriteWidgetEntry>) {
    val openFavorites = actionStartActivity(favoritesIntent(context))
    WidgetSurface {
        if (entries.isEmpty()) {
            WidgetMessage(strings.getString(R.string.widget_no_favorites), openFavorites)
            return@WidgetSurface
        }
        val columns = ((LocalSize.current.width - 24.dp) / CELL_WIDTH).toInt().coerceIn(1, 5)
        LazyVerticalGrid(gridCells = GridCells.Fixed(columns)) {
            items(entries, itemId = { it.id.hashCode().toLong() }) { entry ->
                FavoriteCell(context, entry)
            }
        }
    }
}

@Composable
private fun FavoriteCell(context: Context, entry: FavoriteWidgetEntry) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(actionStartActivity(contactIntent(context, entry))),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            provider = ImageProvider(entry.avatar),
            contentDescription = entry.name,
            modifier = GlanceModifier.size(48.dp),
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = entry.name,
            maxLines = 1,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

private val CELL_WIDTH = 76.dp

private fun initialAvatar(name: String, size: Int): Bitmap {
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    val radius = size / 2f
    canvas.drawCircle(
        radius,
        radius,
        radius,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContactUtils.getRandomColor(name).toArgb() },
    )
    val initial = name.firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    if (initial.isNotEmpty()) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = size * 0.42f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val bounds = Rect()
        textPaint.getTextBounds(initial, 0, initial.length, bounds)
        canvas.drawText(initial, radius, radius + bounds.height() / 2f, textPaint)
    }
    return bitmap
}

private fun circularCrop(source: Bitmap, size: Int): Bitmap {
    val side = minOf(source.width, source.height)
    val output = createBitmap(size, size)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    canvas.drawOval(RectF(0f, 0f, size.toFloat(), size.toFloat()), paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    val left = (source.width - side) / 2
    val top = (source.height - side) / 2
    canvas.drawBitmap(source, Rect(left, top, left + side, top + side), Rect(0, 0, size, size), paint)
    return output
}
