package io.github.immaghzbad.aetherst.shared

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.immaghzbad.aetherst.platform.PlatformContext
import io.github.immaghzbad.aetherst.shared.ui.AetherViewModel
import io.github.immaghzbad.aetherst.shared.ui.OnboardingViewModel
import io.github.immaghzbad.aetherst.shared.ui.screens.MainScreen
import io.github.immaghzbad.aetherst.shared.ui.theme.MyApplicationTheme

@Composable
fun App(context: PlatformContext) {
    val viewModel: AetherViewModel = viewModel(key = "aether_main_vm") { AetherViewModel(context) }
    val onboardingViewModel: OnboardingViewModel = viewModel(key = "onboarding_vm") { OnboardingViewModel(context) }
    val config by viewModel.config.collectAsState()

    MyApplicationTheme(
        themeMode = config.themeMode,
        appLanguage = config.appLanguage
    ) {
        MainScreen(viewModel, onboardingViewModel, context)
    }
}
