package com.bookverse.contacts

import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.security.autofill.VaultAutofillActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppCompatThemeContractTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun appCompatActivitiesResolveAnAppCompatThemeInDayAndNight() {
        val activities = appCompatActivities()
        assertTrue(activities.any { it.name == VaultAutofillActivity::class.java.name })

        listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES).forEach { nightMode ->
            val configuration = Configuration(context.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
            }
            val resources = context.createConfigurationContext(configuration).resources
            activities.forEach { activity ->
                val theme = resources.newTheme().apply { applyStyle(activity.themeResource, true) }
                val attributes = theme.obtainStyledAttributes(androidx.appcompat.R.styleable.AppCompatTheme)
                val isAppCompat = attributes.hasValue(androidx.appcompat.R.styleable.AppCompatTheme_windowActionBar)
                attributes.recycle()
                assertTrue("${activity.name} uiMode night=$nightMode", isAppCompat)
            }
        }
    }

    private fun appCompatActivities(): List<ActivityInfo> {
        @Suppress("DEPRECATION")
        val activities = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_ACTIVITIES)
            .activities
            .orEmpty()
        return activities.filter { activity ->
            runCatching {
                AppCompatActivity::class.java.isAssignableFrom(Class.forName(activity.name, false, context.classLoader))
            }.getOrDefault(false)
        }
    }
}
