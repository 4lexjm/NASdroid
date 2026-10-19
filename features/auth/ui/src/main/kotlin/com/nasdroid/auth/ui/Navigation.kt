package com.nasdroid.auth.ui

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.nasdroid.auth.ui.register.RegisterServerScreen
import com.nasdroid.auth.ui.serverselect.SelectServerScreen

/**
 * Registers a nested navigation graph for the Auth feature.
 */
fun NavGraphBuilder.authNavigation(
    navController: NavHostController,
    route: String,
    windowSizeClass: WindowSizeClass,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    navigation(
        startDestination = PICKER_ROUTE,
        route = route
    ) {
        composable(PICKER_ROUTE) {
            SelectServerScreen(
                onLoginSuccess = onLoginSuccess,
                onAddServer = {
                    navController.navigate(ADD_SERVER_ROUTE)
                },
                modifier = modifier,
                windowSizeClass = windowSizeClass,
            )
        }
        composable(ADD_SERVER_ROUTE) {
            RegisterServerScreen(
                onServerRegistered = {
                    navController.navigate(PICKER_ROUTE) {
                        popUpTo(PICKER_ROUTE)
                    }
                },
                onNavigateBack = { navController.popBackStack() },
                modifier = modifier,
                windowSizeClass = windowSizeClass,
            )
        }
    }
}

private const val PICKER_ROUTE = "picker"
private const val ADD_SERVER_ROUTE = "addServer"

