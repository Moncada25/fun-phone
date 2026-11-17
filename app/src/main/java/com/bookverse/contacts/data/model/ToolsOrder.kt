package com.bookverse.contacts.data.model

import androidx.compose.ui.graphics.vector.ImageVector

data class ToolItem(
    val id: String,
    val icon: ImageVector,
    val titleRes: Int,
    val descriptionRes: Int,
    val action: ToolAction,
    val order: Int = 0
)

sealed class ToolAction {
    data object NavigateToNotes : ToolAction()
    data object NavigateToExpenseManager : ToolAction()
    data object NavigateToVoiceRecognition : ToolAction()
    data object NavigateToPublicHolidays : ToolAction()
    data object NavigateToGames : ToolAction()
    data object NavigateToPaint : ToolAction()
    data object NavigateToPasswords : ToolAction()
    data object NavigateToMenstrualCalendar : ToolAction()
    data object NavigateToQRScanner : ToolAction()
    data object NavigateToQRGenerator : ToolAction()
    data object NavigateToPomodoro : ToolAction()
}

data class ToolsOrderConfig(
    val items: List<String>,
    val hiddenItems: List<String> = emptyList(),
    val toolsMenuHidden: Boolean = false
) {

    fun withToolHidden(id: String, hidden: Boolean): ToolsOrderConfig {
        val current = hiddenItems
        val updated = if (hidden) {
            (current + id).distinct()
        } else {
            current - id
        }
        val hasVisibleTool = allToolIds().any { it !in updated }
        return copy(
            hiddenItems = updated,
            toolsMenuHidden = toolsMenuHidden || !hasVisibleTool
        )
    }

    fun withMenuHidden(hidden: Boolean): ToolsOrderConfig {
        if (hidden) return copy(toolsMenuHidden = true)

        val availableItems = allToolIds()
        val updatedHiddenItems = if (availableItems.all { it in hiddenItems }) {
            hiddenItems - availableItems.first()
        } else {
            hiddenItems
        }
        return copy(
            hiddenItems = updatedHiddenItems,
            toolsMenuHidden = false
        )
    }

    fun normalized(unavailableToolIds: Set<String> = emptySet()): ToolsOrderConfig {
        val availableItems = allToolIds().filterNot { it in unavailableToolIds }
        val hasVisibleTool = availableItems.any { it !in hiddenItems }
        return copy(toolsMenuHidden = toolsMenuHidden || !hasVisibleTool)
    }

    fun applyPreset(preset: ToolsPreset): ToolsOrderConfig {
        val orderedItems = items.ifEmpty { defaultToolIds }
        return when (preset) {
            ToolsPreset.ESSENTIAL_PHONE -> copy(
                items = orderedItems,
                hiddenItems = orderedItems.distinct(),
                toolsMenuHidden = true
            )

            ToolsPreset.PHONE_AND_ORGANIZE -> copy(
                items = orderedItems,
                hiddenItems = orderedItems.filter { it in phoneAndOrganizeHiddenToolIds },
                toolsMenuHidden = false
            )

            ToolsPreset.EVERYTHING -> copy(
                items = orderedItems,
                hiddenItems = emptyList(),
                toolsMenuHidden = false
            )
        }
    }

    fun matchingPreset(): ToolsPreset? = ToolsPreset.entries.firstOrNull { preset ->
        val presetConfig = applyPreset(preset)
        toolsMenuHidden == presetConfig.toolsMenuHidden &&
            hiddenItems.toSet() == presetConfig.hiddenItems.toSet()
    }

    private fun allToolIds(): List<String> = (items + defaultToolIds).distinct()

    companion object {
        private val removedToolIds = setOf("academic_manager")

        private val defaultToolIds = listOf(
            "notes",
            "expense_manager",
            "voice",
            "holidays",
            "games",
            "paint",
            "passwords",
            "menstrual_calendar",
            "qr_scanner",
            "qr_generator",
            "pomodoro"
        )

        private val phoneAndOrganizeHiddenToolIds = setOf(
            "expense_manager",
            "games",
            "paint",
            "passwords",
            "menstrual_calendar"
        )

        fun default() = ToolsOrderConfig(
            items = defaultToolIds,
            hiddenItems = defaultToolIds,
            toolsMenuHidden = true
        )

        fun fromJson(json: String): ToolsOrderConfig? {
            return try {
                val parsed = com.google.gson.Gson().fromJson(json, ToolsOrderConfig::class.java)
                    ?: return null
                parsed.copy(
                    items = parsed.items - removedToolIds,
                    hiddenItems = parsed.hiddenItems - removedToolIds
                ).normalized()
            } catch (_: Exception) {
                null
            }
        }
    }

    fun toJson(): String {
        return com.google.gson.Gson().toJson(this)
    }
}
