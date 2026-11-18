package com.bookverse.contacts.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Timer
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.ToolAction
import com.bookverse.contacts.data.model.ToolItem
import com.bookverse.contacts.data.model.ToolsOrderConfig
import com.composables.icons.lucide.Gamepad
import com.composables.icons.lucide.KeySquare
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.PiggyBank
import com.composables.icons.lucide.QrCode
import com.composables.icons.lucide.ScanQrCode

object ToolsRegistry {

    fun getToolItems(
        orderConfig: ToolsOrderConfig,
        passwordManagerEnabled: Boolean,
        includeHidden: Boolean = false
    ): List<ToolItem> {
        val allTools = getAllTools()
        val hiddenItems = orderConfig.hiddenItems

        val ordered = orderConfig.items
            .mapNotNull { itemId ->
                if (itemId == "passwords" && !passwordManagerEnabled) {
                    return@mapNotNull null
                }
                if (!includeHidden && itemId in hiddenItems) {
                    return@mapNotNull null
                }
                allTools[itemId]
            }
        val missing = allTools.values.filter { tool ->
            if (tool.id == "passwords" && !passwordManagerEnabled) return@filter false
            if (!includeHidden && tool.id in hiddenItems) return@filter false
            !orderConfig.items.contains(tool.id)
        }
        return ordered + missing
    }

    private fun getAllTools(): Map<String, ToolItem> {
        return mapOf(
            "notes" to ToolItem(
                id = "notes",
                icon = Icons.AutoMirrored.Filled.StickyNote2,
                titleRes = R.string.notes_title,
                descriptionRes = R.string.notes_description,
                action = ToolAction.NavigateToNotes
            ),
            "expense_manager" to ToolItem(
                id = "expense_manager",
                icon = Lucide.PiggyBank,
                titleRes = R.string.expense_manager_item_title,
                descriptionRes = R.string.expense_manager_item_description,
                action = ToolAction.NavigateToExpenseManager
            ),
            "voice" to ToolItem(
                id = "voice",
                icon = Lucide.Mic,
                titleRes = R.string.voice_utilities_item,
                descriptionRes = R.string.voice_recognition_description,
                action = ToolAction.NavigateToVoiceRecognition
            ),
            "holidays" to ToolItem(
                id = "holidays",
                icon = Icons.Default.CalendarMonth,
                titleRes = R.string.holidays_item,
                descriptionRes = R.string.holidays_description,
                action = ToolAction.NavigateToPublicHolidays
            ),
            "games" to ToolItem(
                id = "games",
                icon = Lucide.Gamepad,
                titleRes = R.string.games_title,
                descriptionRes = R.string.games_description,
                action = ToolAction.NavigateToGames
            ),
            "paint" to ToolItem(
                id = "paint",
                icon = Icons.Default.Palette,
                titleRes = R.string.paint_title,
                descriptionRes = R.string.paint_description,
                action = ToolAction.NavigateToPaint
            ),
            "passwords" to ToolItem(
                id = "passwords",
                icon = Lucide.KeySquare,
                titleRes = R.string.passwords_item,
                descriptionRes = R.string.password_manager_description,
                action = ToolAction.NavigateToPasswords
            ),
            "menstrual_calendar" to ToolItem(
                id = "menstrual_calendar",
                icon = Icons.Default.Favorite,
                titleRes = R.string.menstrual_calendar_item,
                descriptionRes = R.string.menstrual_calendar_description,
                action = ToolAction.NavigateToMenstrualCalendar
            ),
            "qr_scanner" to ToolItem(
                id = "qr_scanner",
                icon = Lucide.ScanQrCode,
                titleRes = R.string.read_qr,
                descriptionRes = R.string.qr_scanner_description,
                action = ToolAction.NavigateToQRScanner
            ),
            "qr_generator" to ToolItem(
                id = "qr_generator",
                icon = Lucide.QrCode,
                titleRes = R.string.generate_qr,
                descriptionRes = R.string.qr_generator_description,
                action = ToolAction.NavigateToQRGenerator
            ),
            "pomodoro" to ToolItem(
                id = "pomodoro",
                icon = Icons.Default.Timer,
                titleRes = R.string.pomodoro_title,
                descriptionRes = R.string.pomodoro_description,
                action = ToolAction.NavigateToPomodoro
            )
        )
    }
}
