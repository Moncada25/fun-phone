package com.bookverse.contacts.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import com.bookverse.contacts.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.utils.ContactUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

object FavoritesWidgetUpdater {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        if (appWidgetIds.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            updateWidgetsModern(context, appWidgetManager, appWidgetIds)
        } else {
            updateWidgetsLegacy(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun updateWidgetsLegacy(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { appWidgetId ->
            val columnCount = determineColumnCount(appWidgetManager, appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_favorites)

            val serviceIntent = Intent(context, FavoritesWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = toUri(Intent.URI_INTENT_SCHEME).toUri()
            }
            @Suppress("DEPRECATION")
            views.setRemoteAdapter(R.id.widget_grid_view, serviceIntent)
            views.setEmptyView(R.id.widget_grid_view, R.id.empty_view)
            views.setInt(R.id.widget_grid_view, "setNumColumns", columnCount)

            bindPendingIntentTemplate(context, views)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun updateWidgetsModern(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val appContext = context.applicationContext
        scope.launch {
            val favorites = loadFavoriteContacts(appContext)
            appWidgetIds.forEach { id ->
                val columnCount = determineColumnCount(appWidgetManager, id)
                val remoteViews = buildModernRemoteViews(appContext, favorites, columnCount)
                appWidgetManager.updateAppWidget(id, remoteViews)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    internal suspend fun buildModernRemoteViews(
        context: Context,
        favorites: List<Contact>,
        columnCount: Int,
    ): RemoteViews = withContext(Dispatchers.Default) {
        val views = RemoteViews(context.packageName, R.layout.widget_favorites)
        views.setEmptyView(R.id.widget_grid_view, R.id.empty_view)
        views.setInt(R.id.widget_grid_view, "setNumColumns", columnCount)

        val itemsBuilder = RemoteViews.RemoteCollectionItems.Builder()
            .setHasStableIds(true)
            .setViewTypeCount(2)

        if (favorites.isEmpty()) {
            val emptyView = RemoteViews(context.packageName, R.layout.widget_empty_state).apply {
                setTextViewText(
                    R.id.empty_message,
                    context.getString(R.string.widget_no_favorites)
                )
            }
            itemsBuilder.addItem(0, emptyView)
            views.setViewVisibility(R.id.empty_view, View.GONE)
            views.setViewVisibility(R.id.widget_grid_view, View.VISIBLE)
            views.setRemoteAdapter(R.id.widget_grid_view, itemsBuilder.build())
            bindPendingIntentTemplate(context, views)
            return@withContext views
        }

        views.setViewVisibility(R.id.empty_view, View.GONE)
        views.setViewVisibility(R.id.widget_grid_view, View.VISIBLE)

        for ((index, contact) in favorites.withIndex()) {
            val itemView = buildContactItemView(context, contact)
            val stableId = contact.id.hashCode().takeIf { it != 0 }?.toLong()
                ?: index.toLong()
            itemsBuilder.addItem(stableId, itemView)
        }

        views.setRemoteAdapter(R.id.widget_grid_view, itemsBuilder.build())
        bindPendingIntentTemplate(context, views)
        views
    }

    private fun bindPendingIntentTemplate(context: Context, views: RemoteViews) {
        val clickTemplateIntent = Intent(context, ContactDetailActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val clickPendingIntent = PendingIntent.getActivity(
            context,
            0,
            clickTemplateIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.widget_grid_view, clickPendingIntent)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun buildContactItemView(
        context: Context,
        contact: Contact,
    ): RemoteViews = withContext(Dispatchers.IO) {
        val itemViews = RemoteViews(context.packageName, R.layout.widget_favorite_item)

        itemViews.setTextViewText(R.id.contact_name, contact.name)

        val photoApplied = contact.photoUri?.let { uri ->
            ContactUtils.uriToBitmap(uri, context)?.let { toCircularBitmap(context, it) }
        }?.let { avatar ->
            itemViews.setImageViewBitmap(R.id.contact_photo, avatar)
            itemViews.setViewVisibility(R.id.contact_photo, View.VISIBLE)
            true
        } ?: false

        if (!photoApplied) {
            val initial = contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            val color = ContactUtils.getRandomColor(contact.name)
            val androidColor = android.graphics.Color.rgb(
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt()
            )
            val placeholder = createCircularBackground(context, androidColor, initial)
            itemViews.setImageViewBitmap(R.id.contact_photo, placeholder)
            itemViews.setViewVisibility(R.id.contact_photo, View.VISIBLE)
        }

        val fillInIntent = Intent().apply {
            putExtra(ContactDetailActivity.EXTRA_CONTACT_ID, contact.id)
            putExtra(ContactDetailActivity.EXTRA_CONTACT_NAME, contact.name)
            putExtra(
                ContactDetailActivity.EXTRA_CONTACT_PHONE,
                contact.phones.firstOrNull()?.number ?: ""
            )
            putExtra(ContactDetailActivity.EXTRA_CONTACT, contact)
        }
        itemViews.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        itemViews
    }

    private suspend fun loadFavoriteContacts(context: Context): List<Contact> =
        FavoritesWidgetDataSource.loadFavorites(context)

    private fun createCircularBackground(context: Context, color: Int, initial: String): Bitmap {
        val size = avatarSizePx(context)
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        val backgroundPaint = Paint().apply {
            isAntiAlias = true
            this.color = color
        }

        val textPaint = Paint().apply {
            isAntiAlias = true
            this.color = android.graphics.Color.WHITE
            textSize = 54f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, backgroundPaint)

        val textBounds = Rect()
        textPaint.getTextBounds(initial, 0, initial.length, textBounds)
        val textY = radius + textBounds.height() / 2f

        canvas.drawText(initial, radius, textY, textPaint)

        return bitmap
    }

    private fun toCircularBitmap(context: Context, bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = createBitmap(size, size)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
        }

        val srcRect = Rect(
            (bitmap.width - size) / 2,
            (bitmap.height - size) / 2,
            (bitmap.width + size) / 2,
            (bitmap.height + size) / 2
        )
        val destRect = Rect(0, 0, size, size)
        val rectF = RectF(destRect)

        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawOval(rectF, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, srcRect, destRect, paint)

        val targetSize = avatarSizePx(context)

        return if (output.width == targetSize) {
            output
        } else {
            output.scale(targetSize, targetSize)
        }
    }

    private fun avatarSizePx(context: Context): Int =
        (context.resources.displayMetrics.density * AVATAR_SIZE_DP).roundToInt().coerceAtLeast(1)

    private fun determineColumnCount(
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ): Int {
        val options: Bundle? = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidthDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            ?: DEFAULT_WIDTH_DP
        return when {
            minWidthDp >= WIDE_WIDTH_DP -> 4
            minWidthDp >= MEDIUM_WIDTH_DP -> 3
            minWidthDp >= COMPACT_WIDTH_DP -> 2
            else -> 1
        }
    }

    private const val AVATAR_SIZE_DP = 64f
    private const val COMPACT_WIDTH_DP = 110
    private const val MEDIUM_WIDTH_DP = 180
    private const val WIDE_WIDTH_DP = 250
    private const val DEFAULT_WIDTH_DP = WIDE_WIDTH_DP
}
