package com.bookverse.contacts.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.bookverse.contacts.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class PhotoSelectorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pickerStartsWithPeopleAndSwitchesCategories() {
        showPicker()

        composeRule.onNodeWithText(text(R.string.avatar_category_people)).assertIsSelected()
        composeRule.onNodeWithText(text(R.string.avatar_label_person_blue)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.avatar_label_doctor)).assertDoesNotExist()

        composeRule.onNodeWithText(text(R.string.avatar_category_professions)).performClick()

        composeRule.onNodeWithText(text(R.string.avatar_category_professions)).assertIsSelected()
        composeRule.onNodeWithText(text(R.string.avatar_label_doctor)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.avatar_label_person_blue)).assertDoesNotExist()
    }

    @Test
    fun swipingPagerSelectsTheNextCategory() {
        showPicker()

        composeRule.onNodeWithTag(AVATAR_CATEGORY_PAGER_TAG)
            .performTouchInput { swipeLeft() }

        composeRule.onNodeWithText(text(R.string.avatar_category_professions)).assertIsSelected()
        composeRule.onNodeWithText(text(R.string.avatar_label_doctor)).assertIsDisplayed()
    }

    @Test
    fun avatarIsOneAccessibleActionAndInvokesSelectionOnce() {
        val selections = AtomicInteger(0)
        val selectedId = AtomicReference<String>()
        showPicker(
            onAvatarSelected = { option ->
                selections.incrementAndGet()
                selectedId.set(option.id)
            },
        )

        composeRule.onNodeWithText(text(R.string.avatar_label_person_blue))
            .assertIsDisplayed()
            .assertHasClickAction()
            .performClick()

        assertEquals(1, selections.get())
        assertEquals("person_blue", selectedId.get())
    }

    @Test
    fun backIconInvokesNestedBackAction() {
        val backActions = AtomicInteger(0)
        showPicker(onBack = { backActions.incrementAndGet() })

        composeRule.onNodeWithContentDescription(text(R.string.back))
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, backActions.get())
    }

    @Test
    fun compactPickerRemainsUsableAtTwoHundredPercentFontScale() {
        showPicker(width = 320.dp, fontScale = 2f)

        composeRule.onNodeWithText(text(R.string.avatar_category_people)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.avatar_label_person_blue))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun thirdAvatarRowCanBeScrolledIntoViewOnRegularPhones() {
        showPicker(width = 360.dp)

        composeRule.onNodeWithText(text(R.string.avatar_label_person_red))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun narrowLargeTextLayoutStillPlacesThreeAvatarsInEveryRow() {
        showPicker(width = 320.dp, fontScale = 2f)

        val firstTop = composeRule.onNodeWithText(text(R.string.avatar_label_person_blue))
            .fetchSemanticsNode().boundsInRoot.top
        val thirdTop = composeRule.onNodeWithText(text(R.string.avatar_label_person_orange))
            .fetchSemanticsNode().boundsInRoot.top
        val fourthTop = composeRule.onNodeWithText(text(R.string.avatar_label_person_violet))
            .fetchSemanticsNode().boundsInRoot.top

        assertEquals(firstTop, thirdTop, 1f)
        assertTrue(fourthTop > firstTop)
    }

    private fun showPicker(
        onAvatarSelected: (AvatarOption) -> Unit = {},
        onBack: () -> Unit = {},
        width: Dp = 360.dp,
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale),
            ) {
                MaterialTheme {
                    Box(
                        modifier = Modifier
                            .width(width)
                            .height(700.dp),
                    ) {
                        var selectedCategory by remember { mutableStateOf(AvatarCategory.People) }
                        AvatarPickerContent(
                            categories = AvatarCategory.entries,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it },
                            onAvatarSelected = onAvatarSelected,
                            onBack = onBack,
                        )
                    }
                }
            }
        }
    }

    private fun text(resourceId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resourceId)
}
