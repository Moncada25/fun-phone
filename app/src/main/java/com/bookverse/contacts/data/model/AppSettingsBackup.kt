package com.bookverse.contacts.data.model

import com.google.gson.Gson
import com.google.gson.JsonParser

data class AppSettingsBackup(
    val accentColorName: String,
    val fontType: String,
    val animationSpeed: String,
    val vibrationDuration: Int,
    val vibrationPattern: String,
    val themeMode: String,
    val transitionType: String,
    val defaultDialerConsent: Boolean,
    val passwordManagerEnabled: Boolean,
    val sensitiveSectionsLockEnabled: Boolean,
    val defaultContactAccount: String,
    val favoritesColumns: Int,
    val favoritesSortOrder: String,
    val favoritesCustomOrder: List<String>,
    val accentBarStyle: String,
    val appLanguage: String,
    val expenseCurrency: String,
    val toolsOrder: ToolsOrderConfig,
    val startupScreen: String,
    val callAnswerStyle: String = "swipe",
    val dtmfTonesEnabled: String = "disabled",
    val immersiveIncomingCalls: Boolean = false,
    val answerWithVolumeUp: Boolean = false,
    val fontScale: Float = 1.0f,
    val listDensity: String = "NORMAL",
    val favoritesCardShape: String = "SQUARE",
    val darkModeStyle: String = "AMOLED",
    val customAccentColor: String? = null,
    val backupVersion: Int = 4,
    val backupDate: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromJson(json: String): AppSettingsBackup? {
            return try {
                val jsonObject = JsonParser.parseString(json).asJsonObject
                val backup = Gson().fromJson(json, AppSettingsBackup::class.java)

                val dtmfValue = if (jsonObject.has("dtmfTonesEnabled")) {
                    val element = jsonObject.get("dtmfTonesEnabled")
                    if (element.isJsonPrimitive && element.asJsonPrimitive.isBoolean) {
                        if (element.asBoolean) "enabled" else "disabled"
                    } else {
                        element.asString
                    }
                } else {
                    "disabled"
                }

                val answerWithVolumeUp = if (jsonObject.has("answerWithVolumeUp")) {
                    runCatching { jsonObject.get("answerWithVolumeUp").asBoolean }.getOrDefault(false)
                } else {
                    false
                }

                val favoritesCardShape = jsonObject.get("favoritesCardShape")
                    ?.takeIf { it.isJsonPrimitive }
                    ?.asString
                    ?: "SQUARE"

                backup.copy(
                    dtmfTonesEnabled = dtmfValue,
                    answerWithVolumeUp = answerWithVolumeUp,
                    favoritesCardShape = favoritesCardShape,
                    darkModeStyle = jsonObject.get("darkModeStyle")?.asString ?: "AMOLED",
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    fun toJson(): String {
        return Gson().toJson(this)
    }
}
