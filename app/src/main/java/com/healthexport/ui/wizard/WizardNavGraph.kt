package com.healthexport.ui.wizard

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Wizard navigation graph.
 *
 * Slide transitions give a linear, left-to-right feel that matches the
 * step-by-step nature of the wizard without being distracting.
 */
@Composable
fun WizardNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    val slideSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)

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
                onNext = { navController.navigate(WizardRoute.Step2.route) },
            )
        }
        composable(WizardRoute.Step2.route) {
            Step2TimeRangeScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(WizardRoute.Step3.route) },
            )
        }
        composable(WizardRoute.Step3.route) {
            Step3DestinationScreen(
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(WizardRoute.Step4.route) },
            )
        }
        composable(WizardRoute.Step4.route) {
            Step4SummaryScreen(
                onBack  = { navController.popBackStack() },
                onExport = { /* module 3 */ },
            )
        }
    }
}
