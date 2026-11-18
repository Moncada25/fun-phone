package com.bookverse.contacts.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

enum class OnboardingStep { WELCOME, PHONE_SETUP }

@Immutable
data class OnboardingUiState(val currentStep: OnboardingStep = OnboardingStep.WELCOME)

@Stable
class OnboardingFlowController internal constructor(initialState: OnboardingUiState = OnboardingUiState()) {
    var uiState by mutableStateOf(initialState)
        private set

    fun moveTo(step: OnboardingStep) {
        uiState = uiState.copy(currentStep = step)
    }

    companion object {
        val StateSaver = Saver<OnboardingFlowController, String>(
            save = { it.uiState.currentStep.name },
            restore = { name ->
                OnboardingFlowController(OnboardingUiState(
                    OnboardingStep.entries.firstOrNull { it.name == name } ?: OnboardingStep.WELCOME,
                ))
            },
        )
    }
}

@Composable
fun rememberOnboardingFlowController(): OnboardingFlowController =
    rememberSaveable(saver = OnboardingFlowController.StateSaver) { OnboardingFlowController() }
