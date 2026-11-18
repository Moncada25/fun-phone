package com.bookverse.contacts.widget

import android.content.Context
import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.bookverse.contacts.settings.LocaleController
import com.bookverse.contacts.ui.theme.DefaultAccentSeed

private val FallbackWidgetColors = ColorProviders(
    light = lightColorScheme(primary = DefaultAccentSeed),
    dark = darkColorScheme(primary = DefaultAccentSeed),
)

internal fun Context.widgetStrings(): Context = LocaleController.localizedContext(applicationContext)

@Composable
internal fun FunPhoneWidgetTheme(content: @Composable () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GlanceTheme(content = content)
    } else {
        GlanceTheme(colors = FallbackWidgetColors, content = content)
    }
}

@Composable
internal fun WidgetSurface(
    modifier: GlanceModifier = GlanceModifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val rounded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GlanceModifier.cornerRadius(android.R.dimen.system_app_widget_background_radius)
    } else {
        GlanceModifier
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .appWidgetBackground()
            .then(rounded)
            .background(GlanceTheme.colors.widgetBackground)
            .padding(12.dp),
        content = content,
    )
}

@Composable
internal fun WidgetHeader(
    title: String,
    onTitleClick: Action,
    actionIcon: Int? = null,
    actionLabel: String? = null,
    onAction: Action? = null,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight().clickable(onTitleClick).padding(vertical = 4.dp),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            ),
        )
        if (actionIcon != null && onAction != null) {
            Box(
                modifier = GlanceModifier
                    .size(32.dp)
                    .cornerRadius(16.dp)
                    .background(GlanceTheme.colors.primaryContainer)
                    .clickable(onAction),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(actionIcon),
                    contentDescription = actionLabel,
                    modifier = GlanceModifier.size(18.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer),
                )
            }
        }
    }
}

@Composable
internal fun WidgetMessage(text: String, onClick: Action, icon: Int? = null) {
    Box(
        modifier = GlanceModifier.fillMaxSize().clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                Image(
                    provider = ImageProvider(icon),
                    contentDescription = null,
                    modifier = GlanceModifier.size(24.dp).padding(bottom = 4.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
                )
            }
            Text(
                text = text,
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
            )
        }
    }
}
