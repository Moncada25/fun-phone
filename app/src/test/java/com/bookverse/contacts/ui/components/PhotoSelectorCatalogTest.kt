package com.bookverse.contacts.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PhotoSelectorCatalogTest {
    @Test
    fun catalogContainsNineUniqueAvatarsPerCategory() {
        assertEquals(36, avatarOptions.size)
        assertEquals(36, avatarOptions.map { it.id }.toSet().size)
        assertEquals(36, avatarOptions.map { it.drawableRes }.toSet().size)

        AvatarCategory.entries.forEach { category ->
            assertEquals(
                "Unexpected avatar count for $category",
                9,
                avatarOptions.count { it.category == category },
            )
        }
    }

    @Test
    fun everyCategoryFormsAFixedThreeByThreeGrid() {
        assertEquals(3, AVATAR_GRID_COLUMNS)
        AvatarCategory.entries.forEach { category ->
            assertEquals(
                AVATAR_GRID_COLUMNS * AVATAR_GRID_COLUMNS,
                avatarOptions.count { it.category == category },
            )
        }
    }

    @Test
    fun catalogUsesTheExpectedCategoryOrder() {
        assertEquals(
            listOf(
                AvatarCategory.People,
                AvatarCategory.Professions,
                AvatarCategory.Interests,
                AvatarCategory.Fantasy,
            ),
            AvatarCategory.entries.toList(),
        )
    }

    @Test
    fun generatedFileNameKeepsSafeIdAndUniqueSuffix() {
        val first = avatarFileName("person blue/1", "first")
        val second = avatarFileName("person blue/1", "second")

        assertEquals("avatar_person_blue_1_first.webp", first)
        assertEquals("avatar_person_blue_1_second.webp", second)
        assertNotEquals(first, second)
        assertEquals("avatar_____safe.webp", avatarFileName("***", "safe"))
        assertEquals("avatar_custom_safe.webp", avatarFileName("", "safe"))
    }
}
