package com.bookverse.contacts.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel

@Composable
fun OnboardingFlowScreen(
    onFinish: (limited: Boolean, reviewPhoneStatus: Boolean) -> Unit,
    settingsViewModel: AppSettingsViewModel,
) {
    val context = LocalContext.current
    val flowController = rememberOnboardingFlowController()
    val setup = rememberPhoneSetupActions()
    val accentColor = MaterialTheme.colorScheme.primary
    val toolsOrder by settingsViewModel.toolsOrder.collectAsStateWithLifecycle()
    val passwordManagerEnabled by settingsViewModel.passwordManagerEnabled.collectAsStateWithLifecycle()
    val step = flowController.uiState.currentStep

    BackHandler(enabled = step == OnboardingStep.PHONE_SETUP) {
        flowController.moveTo(OnboardingStep.WELCOME)
    }

    OnboardingContent(
        step = step,
        setupState = setup.controller.uiState,
        accentColor = accentColor,
        onNext = { flowController.moveTo(OnboardingStep.PHONE_SETUP) },
        onBack = { flowController.moveTo(OnboardingStep.WELCOME) },
        onRequest = setup.request,
        onFinish = { onFinish(!readPhoneStatus(context).coreRequirementsGranted, false) },
        toolsOrder = toolsOrder,
        passwordManagerEnabled = passwordManagerEnabled,
        onPresetSelected = { preset ->
            settingsViewModel.setToolsOrder(context, toolsOrder.applyPreset(preset))
        },
        onToolHiddenChanged = { toolId, hidden ->
            settingsViewModel.setToolHidden(context, toolId, hidden)
        },
        onToolsMenuHiddenChanged = { hidden ->
            settingsViewModel.setToolsMenuHidden(context, hidden)
        },
    )
}
