package com.omsharma.iykyk.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.omsharma.iykyk.ui.screens.CaptureScreen
import com.omsharma.iykyk.ui.screens.CollageScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    debugVideoUri: Uri? = null,
    contentModifier: Modifier = Modifier
) {
    // Debug hook: jump straight to processing
    LaunchedEffect(debugVideoUri) {
        if (debugVideoUri != null) {
            navController.navigate(IykykScreens.CollageScreen.routeFor(debugVideoUri))
        }
    }

    NavHost(
        navController = navController,
        startDestination = IykykScreens.CaptureScreen.route
    ) {
        composable(route = IykykScreens.CaptureScreen.route) {
            CaptureScreen(
                onVideoReady = { uri ->
                    navController.navigate(IykykScreens.CollageScreen.routeFor(uri))
                }
            )
        }

        composable(
            route = IykykScreens.CollageScreen.route,
            arguments = listOf(navArgument("videoUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("videoUri").orEmpty()
            CollageScreen(
                videoUri = Uri.parse(Uri.decode(encodedUri)),
                modifier = contentModifier,
                onNewVideo = {
                    navController.navigate(IykykScreens.CaptureScreen.route) {
                        popUpTo(IykykScreens.CaptureScreen.route) {
                            inclusive = true
                        } // fresh start
                    }
                }
            )
        }
    }
}
