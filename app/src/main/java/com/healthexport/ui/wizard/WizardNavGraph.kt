package com.healthexport.ui.wizard

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Wizard navigation graph.
 *
 * [viewModel] is created here (above the NavHost) so it is scoped to the
 * Activity lifecycle and shared across all 4 destinations without re-creation.
 */
@Composable
fun WizardNavGraph(
    navController: NavHostController = rememberNavController(),
    viewModel: WizardViewModel       = hiltViewModel(),
) {
    NavHost(
        navController    = navController,
        startDestination = WizardRoute.Step1.route,
        enterTransition  = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            )
        },
        exitTransition   = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            )
        },
        popExitTransition  = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            )
        },
    ) {
        composable(WizardRoute.Step1.route) {
            Step1DataSelectionScreen(
                viewModel = viewModel,
                onNext    = { navController.navigate(WizardRoute.Step2.route) },
            )
        }
        composable(WizardRoute.Step2.route) {
            Step2TimeRangeScreen(
                viewModel = viewModel,
                onBack    = { navController.popBackStack() },
                onNext    = { navController.navigate(WizardRoute.Step3.route) },
            )
        }
        composable(WizardRoute.Step3.route) {
            Step3DestinationScreen(
                viewModel = viewModel,
                onBack    = { navController.popBackStack() },
                onNext    = { navController.navigate(WizardRoute.Step4.route) },
            )
        }
        composable(WizardRoute.Step4.route) {
            Step4SummaryScreen(
                viewModel = viewModel,
                onBack    = { navController.popBackStack() },
                onExport  = {
                    viewModel.resetExportState()
                    navController.popBackStack(WizardRoute.Step1.route, inclusive = false)
                },
            )
        }
    }
}
