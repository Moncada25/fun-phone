package com.bookverse.contacts.ui.screens.settings

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingFlowControllerTest {
    @Test
    fun welcomeAndSetupAreTheOnlySteps() {
        assertEquals(listOf(OnboardingStep.WELCOME, OnboardingStep.PHONE_SETUP), OnboardingStep.entries)
        val controller = OnboardingFlowController()
        assertEquals(OnboardingStep.WELCOME, controller.uiState.currentStep)
        controller.moveTo(OnboardingStep.PHONE_SETUP)
        controller.moveTo(OnboardingStep.WELCOME)
        assertEquals(OnboardingStep.WELCOME, controller.uiState.currentStep)
    }

    @Test
    fun recreationRestoresSetupWithoutRequestingPermissions() {
        val controller = OnboardingFlowController()
        controller.moveTo(OnboardingStep.PHONE_SETUP)
        val scope = object : SaverScope {
            override fun canBeSaved(value: Any) = true
        }
        val saved = with(OnboardingFlowController.StateSaver) { scope.save(controller) }
        val restored = OnboardingFlowController.StateSaver.restore(requireNotNull(saved))
        assertEquals(OnboardingStep.PHONE_SETUP, restored?.uiState?.currentStep)
    }

    @Test
    fun unknownSavedStepFallsBackToWelcome() {
        val restored = OnboardingFlowController.StateSaver.restore("TOOLS_SETUP")
        assertEquals(OnboardingStep.WELCOME, restored?.uiState?.currentStep)
    }
}
