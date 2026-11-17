package com.bookverse.contacts.ui.screens.contacts.dialer

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatEditText
import kotlin.math.roundToInt

internal class DialerEditText(context: Context) : AppCompatEditText(context) {
    var suppressTextWatcher: Boolean = false
    var suppressSelectionCallback: Boolean = false
    var onSelectionChangedListener: ((Int) -> Unit)? = null

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (!suppressSelectionCallback && !suppressTextWatcher && selStart == selEnd) {
            onSelectionChangedListener?.invoke(selStart)
        }
    }
}

internal fun AppCompatEditText.applySelectionVisuals(
    @ColorInt cursorColor: Int,
    @ColorInt selectionBackgroundColor: Int
) {
    highlightColor = selectionBackgroundColor
    tintCursorDrawable(cursorColor)
    tintSelectionHandles(cursorColor)
}

@SuppressLint("DiscouragedPrivateApi")
private fun AppCompatEditText.tintCursorDrawable(@ColorInt color: Int) {
    val cursorDrawable = resolveCursorDrawable()?.mutate()?.apply {
        colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    } ?: createCursorDrawable(color)
    textCursorDrawable = cursorDrawable
}

private fun AppCompatEditText.tintSelectionHandles(@ColorInt color: Int) {
    val attrs = intArrayOf(
        android.R.attr.textSelectHandle,
        android.R.attr.textSelectHandleLeft,
        android.R.attr.textSelectHandleRight
    )
    val typedArray = context.obtainStyledAttributes(attrs)
    val handles = try {
        List(attrs.size) { index ->
            typedArray.getDrawable(index)?.mutate()?.apply { setTint(color) }
        }
    } finally {
        typedArray.recycle()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        handles.getOrNull(0)?.let { setTextSelectHandle(it) }
        handles.getOrNull(1)?.let { setTextSelectHandleLeft(it) }
        handles.getOrNull(2)?.let { setTextSelectHandleRight(it) }
    } else {
        setLegacyHandleDrawable("mSelectHandleCenter", handles.getOrNull(0))
        setLegacyHandleDrawable("mSelectHandleLeft", handles.getOrNull(1))
        setLegacyHandleDrawable("mSelectHandleRight", handles.getOrNull(2))
    }
}

@SuppressLint("DiscouragedPrivateApi")
private fun AppCompatEditText.setLegacyHandleDrawable(
    fieldName: String,
    drawable: Drawable?
) {
    if (drawable == null) return
    try {
        val editorField = TextView::class.java.getDeclaredField("mEditor")
        editorField.isAccessible = true
        val editor = editorField.get(this) ?: return
        val handleField = editor.javaClass.getDeclaredField(fieldName)
        handleField.isAccessible = true
        handleField.set(editor, drawable)
    } catch (_: Throwable) {
    }
}

@SuppressLint("SoonBlockedPrivateApi")
private fun AppCompatEditText.resolveCursorDrawable(): Drawable? {
    return try {
        val cursorDrawableResField = TextView::class.java.getDeclaredField("mCursorDrawableRes")
        cursorDrawableResField.isAccessible = true
        val drawableResId = cursorDrawableResField.getInt(this)
        if (drawableResId == 0) {
            null
        } else {
            AppCompatResources.getDrawable(context, drawableResId)
        }
    } catch (_: Throwable) {
        null
    }
}

private fun AppCompatEditText.createCursorDrawable(@ColorInt color: Int): Drawable {
    val widthPx = (resources.displayMetrics.density * 2f).roundToInt().coerceAtLeast(1)
    return GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
        setSize(widthPx, 0)
    }
}
