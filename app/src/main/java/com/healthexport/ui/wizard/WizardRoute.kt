package com.healthexport.ui.wizard

/** Typed navigation routes for the wizard. */
sealed class WizardRoute(val route: String) {
    data object Step1 : WizardRoute("step1_data_selection")
    data object Step2 : WizardRoute("step2_time_range")
    data object Step3 : WizardRoute("step3_destination")
    data object Step4 : WizardRoute("step4_summary")
}

val wizardSteps = listOf(
    WizardRoute.Step1,
    WizardRoute.Step2,
    WizardRoute.Step3,
    WizardRoute.Step4,
)
