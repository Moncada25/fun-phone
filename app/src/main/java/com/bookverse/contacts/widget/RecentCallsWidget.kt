package com.bookverse.contacts.widget

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CallLog
import android.icu.text.DisplayContext
import android.icu.text.RelativeDateTimeFormatter
import android.icu.util.ULocale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.bookverse.contacts.MainActivity
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.settings.LocaleController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

internal enum class RecentCallKind { INCOMING, OUTGOING, MISSED }

@Immutable
internal data class RecentCallEntry(
    val number: String,
    val name: String?,
    val kind: RecentCallKind,
    val date: Long,
)

internal sealed interface RecentCallsState {
    data object NoPermission : RecentCallsState
    data class Calls(val calls: List<RecentCallEntry>) : RecentCallsState
}

internal object RecentCallsDataSource {
    fun load(context: Context, limit: Int): RecentCallsState {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return RecentCallsState.NoPermission
        val projection = arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE)
        val uri = CallLog.Calls.CONTENT_URI.buildUpon()
            .appendQueryParameter(CallLog.Calls.LIMIT_PARAM_KEY, limit.toString())
            .build()
        val calls = runCatching {
            context.contentResolver.query(uri, projection, null, null, "${CallLog.Calls.DATE} DESC")?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val number = cursor.getString(0).orEmpty()
                        if (number.isBlank()) continue
                        add(
                            RecentCallEntry(
                                number = number,
                                name = cursor.getString(1)?.takeIf { it.isNotBlank() },
                                kind = kindOf(cursor.getInt(2)),
                                date = cursor.getLong(3),
                            ),
                        )
                    }
                }
            }
        }.getOrNull() ?: return RecentCallsState.NoPermission
        return RecentCallsState.Calls(calls)
    }

    private fun kindOf(type: Int): RecentCallKind = when (type) {
        CallLog.Calls.OUTGOING_TYPE -> RecentCallKind.OUTGOING
        CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE, CallLog.Calls.BLOCKED_TYPE -> RecentCallKind.MISSED
        else -> RecentCallKind.INCOMING
    }
}

class RecentCallsWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = withContext(Dispatchers.IO) { RecentCallsDataSource.load(context, MAX_CALLS) }
        val canCall = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        val strings = context.widgetStrings()
        provideContent {
            FunPhoneWidgetTheme {
                RecentCallsContent(context, strings, state, canCall)
            }
        }
    }

    companion object {
        private const val MAX_CALLS = 10
    }
}

private fun recentsIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java)
        .putExtra("open_recents", true)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

private fun dialIntent(context: Context, number: String): Intent =
    Intent(Intent.ACTION_DIAL, "tel:${number}".toUri(), context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

private fun callIntent(number: String): Intent =
    Intent(Intent.ACTION_CALL, "tel:${number}".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

@Composable
private fun RecentCallsContent(context: Context, strings: Context, state: RecentCallsState, canCall: Boolean) {
    val openRecents = actionStartActivity(recentsIntent(context))
    WidgetSurface {
        WidgetHeader(title = strings.getString(R.string.recent_calls_widget_title), onTitleClick = openRecents)
        Spacer(GlanceModifier.height(4.dp))
        when (state) {
            RecentCallsState.NoPermission -> WidgetMessage(
                strings.getString(R.string.recent_calls_widget_permission),
                openRecents,
                R.drawable.ic_widget_lock,
            )
            is RecentCallsState.Calls -> if (state.calls.isEmpty()) {
                WidgetMessage(strings.getString(R.string.recent_calls_widget_empty), openRecents)
            } else {
                val now = System.currentTimeMillis()
                LazyColumn {
                    itemsIndexed(state.calls, itemId = { index, call -> call.date * 31 + index }) { _, call ->
                        RecentCallRow(context, strings, call, now, canCall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentCallRow(context: Context, strings: Context, call: RecentCallEntry, now: Long, canCall: Boolean) {
    val label = call.name ?: call.number
    val missed = call.kind == RecentCallKind.MISSED
    val icon = when (call.kind) {
        RecentCallKind.INCOMING -> R.drawable.ic_widget_call_incoming
        RecentCallKind.OUTGOING -> R.drawable.ic_widget_call_outgoing
        RecentCallKind.MISSED -> R.drawable.ic_widget_call_missed
    }
    val time = relativeCallTime(call.date, now, LocaleController.effectiveLocale(context))
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(dialIntent(context, call.number))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(icon),
                contentDescription = null,
                modifier = GlanceModifier.size(16.dp),
                colorFilter = ColorFilter.tint(if (missed) GlanceTheme.colors.error else GlanceTheme.colors.onSurfaceVariant),
            )
            Spacer(GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = label,
                    maxLines = 1,
                    style = TextStyle(
                        color = if (missed) GlanceTheme.colors.error else GlanceTheme.colors.onSurface,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    ),
                )
                Text(
                    text = time,
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                )
            }
        }
        Box(
            modifier = GlanceModifier
                .size(34.dp)
                .cornerRadius(17.dp)
                .background(GlanceTheme.colors.primaryContainer)
                .clickable(actionStartActivity(if (canCall) callIntent(call.number) else dialIntent(context, call.number))),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_call),
                contentDescription = strings.getString(R.string.recent_calls_widget_call_back, label),
                modifier = GlanceModifier.size(18.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer),
            )
        }
    }
}

internal fun relativeCallTime(date: Long, now: Long, locale: Locale): String {
    val formatter = RelativeDateTimeFormatter.getInstance(
        ULocale.forLocale(locale),
        null,
        RelativeDateTimeFormatter.Style.SHORT,
        DisplayContext.CAPITALIZATION_FOR_STANDALONE,
    )
    val elapsed = (now - date).coerceAtLeast(0L)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsed)
    val hours = TimeUnit.MILLISECONDS.toHours(elapsed)
    val days = TimeUnit.MILLISECONDS.toDays(elapsed)
    return when {
        minutes < 1 -> formatter.format(RelativeDateTimeFormatter.Direction.PLAIN, RelativeDateTimeFormatter.AbsoluteUnit.NOW)
        hours < 1 -> formatter.format(minutes.toDouble(), RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.RelativeUnit.MINUTES)
        days < 1 -> formatter.format(hours.toDouble(), RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.RelativeUnit.HOURS)
        days < 2 -> formatter.format(RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.AbsoluteUnit.DAY)
        days < 7 -> formatter.format(days.toDouble(), RelativeDateTimeFormatter.Direction.LAST, RelativeDateTimeFormatter.RelativeUnit.DAYS)
        else -> DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(date))
    }
}
