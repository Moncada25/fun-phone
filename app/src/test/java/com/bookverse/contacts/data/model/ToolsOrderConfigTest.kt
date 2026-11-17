package com.bookverse.contacts.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolsOrderConfigTest {
    @Test
    fun shouldHideToolsMenuForNewInstallDefault() {
        val config = ToolsOrderConfig.default()

        assertTrue(config.toolsMenuHidden)
        assertEquals(config.items.toSet(), config.hiddenItems.toSet())
        assertEquals(ToolsPreset.ESSENTIAL_PHONE, config.matchingPreset())
    }

    @Test
    fun shouldKeepCustomOrderWhenApplyingPhoneAndOrganizePreset() {
        val config = ToolsOrderConfig.default().copy(items = listOf("voice", "notes", "games"))

        val presetConfig = config.applyPreset(ToolsPreset.PHONE_AND_ORGANIZE)

        assertEquals(listOf("voice", "notes", "games"), presetConfig.items)
        assertEquals(listOf("games"), presetConfig.hiddenItems)
        assertTrue(!presetConfig.toolsMenuHidden)
    }

    @Test
    fun shouldShowEveryToolWhenApplyingEverythingPreset() {
        val config = ToolsOrderConfig.default()

        val presetConfig = config.applyPreset(ToolsPreset.EVERYTHING)

        assertTrue(!presetConfig.toolsMenuHidden)
        assertTrue(presetConfig.hiddenItems.isEmpty())
        assertEquals(ToolsPreset.EVERYTHING, presetConfig.matchingPreset())
    }

    @Test
    fun shouldNotMatchAPresetAfterChoosingToolsIndividually() {
        val customConfig = ToolsOrderConfig.default()
            .applyPreset(ToolsPreset.EVERYTHING)
            .withToolHidden("notes", true)

        assertNull(customConfig.matchingPreset())
    }

    @Test
    fun shouldEnableFirstToolWhenShowingAnEmptyToolsMenu() {
        val config = ToolsOrderConfig.default()

        val visibleConfig = config.withMenuHidden(false)

        assertTrue(!visibleConfig.toolsMenuHidden)
        assertEquals("notes", visibleConfig.items.first { it !in visibleConfig.hiddenItems })
    }

    @Test
    fun shouldHideToolsMenuWhenLastVisibleToolIsDisabled() {
        val config = ToolsOrderConfig.default()
            .withMenuHidden(false)

        val hiddenConfig = config.withToolHidden("notes", true)

        assertTrue(hiddenConfig.toolsMenuHidden)
        assertEquals(hiddenConfig.items.toSet(), hiddenConfig.hiddenItems.toSet())
    }

    @Test
    fun shouldHideToolsMenuWhenOnlyVisibleToolIsUnavailable() {
        val config = ToolsOrderConfig.default().copy(
            hiddenItems = ToolsOrderConfig.default().items - "passwords",
            toolsMenuHidden = false
        )

        val normalizedConfig = config.normalized(setOf("passwords"))

        assertTrue(normalizedConfig.toolsMenuHidden)
    }

    @Test
    fun shouldRemoveRetiredAcademicToolFromStoredConfiguration() {
        val config = ToolsOrderConfig.fromJson(
            """
            {
              "items": ["academic_manager", "notes"],
              "hiddenItems": ["academic_manager"],
              "toolsMenuHidden": false
            }
            """.trimIndent()
        )

        requireNotNull(config)
        assertEquals(listOf("notes"), config.items)
        assertTrue("academic_manager" !in config.hiddenItems)
    }
}
