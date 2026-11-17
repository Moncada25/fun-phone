package com.bookverse.contacts.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.bookverse.contacts.R
import com.bookverse.contacts.data.model.Contact
import com.bookverse.contacts.utils.ContactUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class FavoritesWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return FavoritesWidgetViewsFactory(this.applicationContext)
    }
}

class FavoritesWidgetViewsFactory(
    private val context: Context,
) : RemoteViewsService.RemoteViewsFactory {

    @Volatile
    private var favoriteItems: List<FavoriteWidgetItem> = emptyList()
    @Volatile
    private var isLoading = false
    @Volatile
    private var publishLoadedData = false
    private val loadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val avatarSizePx by lazy {
        (context.resources.displayMetrics.density * AVATAR_SIZE_DP).roundToInt()
            .coerceAtLeast(1)
    }

    override fun onCreate() {
        loadFavoriteContacts()
    }

    override fun onDataSetChanged() {
        if (publishLoadedData) {
            publishLoadedData = false
            return
        }
        loadFavoriteContacts()
    }

    override fun onDestroy() {
        loadScope.cancel()
        favoriteItems = emptyList()
    }

    override fun getCount(): Int {
        return if (favoriteItems.isEmpty()) 1 else favoriteItems.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        if (favoriteItems.isEmpty()) {
            val views = RemoteViews(context.packageName, R.layout.widget_empty_state)
            views.setTextViewText(
                R.id.empty_message,
                context.getString(R.string.widget_no_favorites)
            )
            return views
        }

        if (position >= favoriteItems.size) {
            return RemoteViews(context.packageName, R.layout.widget_favorite_item)
        }

        val item = favoriteItems[position]
        val contact = item.contact
        val views = RemoteViews(context.packageName, R.layout.widget_favorite_item)

        views.setTextViewText(R.id.contact_name, contact.name)

        if (item.avatar != null) {
            views.setImageViewBitmap(R.id.contact_photo, item.avatar)
            views.setViewVisibility(R.id.contact_photo, android.view.View.VISIBLE)
            views.setInt(R.id.contact_photo, "setBackgroundResource", 0)
        } else {
            setupInitialView(views, contact)
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
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return views
    }

    private fun setupInitialView(views: RemoteViews, contact: Contact) {
        val color = ContactUtils.getRandomColor(contact.name)
        val androidColor = android.graphics.Color.rgb(
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )

        val circularBitmap = createCircularBackground(
            androidColor,
            contact.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        )
        views.setImageViewBitmap(R.id.contact_photo, circularBitmap)
        views.setViewVisibility(R.id.contact_photo, android.view.View.VISIBLE)
    }

    private fun createCircularBackground(color: Int, initial: String): Bitmap {
        val size = avatarSizePx
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

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
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

        return if (output.width == avatarSizePx) {
            output
        } else {
            output.scale(avatarSizePx, avatarSizePx)
        }
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_loading_item)
    }

    override fun getViewTypeCount(): Int = 2

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    private fun loadFavoriteContacts() {
        if (isLoading) {
            return
        }
        isLoading = true
        loadScope.launch {
            favoriteItems = try {
                FavoritesWidgetDataSource.loadFavorites(context).map { contact ->
                    FavoriteWidgetItem(
                        contact = contact,
                        avatar = contact.photoUri
                            ?.let { uri -> ContactUtils.uriToBitmap(uri, context) }
                            ?.let(::getCircularBitmap),
                    )
                }
            } catch (_: Exception) {
                emptyList()
            } finally {
                isLoading = false
            }
            publishLoadedData = true
            val manager = AppWidgetManager.getInstance(context)
            val widgetIds = manager.getAppWidgetIds(
                ComponentName(context, FavoritesWidgetProvider::class.java),
            )
            manager.notifyAppWidgetViewDataChanged(widgetIds, R.id.widget_grid_view)
        }
    }

    private companion object {
        private const val AVATAR_SIZE_DP = 64f
    }
}

private data class FavoriteWidgetItem(
    val contact: Contact,
    val avatar: Bitmap?,
)
