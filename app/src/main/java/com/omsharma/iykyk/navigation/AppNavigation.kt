package com.omsharma.iykyk.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.omsharma.iykyk.ui.screens.CaptureScreen
import com.omsharma.iykyk.ui.screens.CollageScreen
import com.omsharma.iykyk.ui.screens.ProcessingScreen
import com.omsharma.iykyk.vm.FaceCollageViewModel

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    // Obtained once here - called directly from MainActivity.setContent with no
    // intervening NavBackStackEntry owner, so this resolves against the Activity's
    // ViewModelStoreOwner and is shared for the app's lifetime. Passed down explicitly
    // rather than each screen calling hiltViewModel() itself, which would scope to the
    // individual destination and give each screen its own isolated instance.
    val viewModel: FaceCollageViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = IykykScreens.CaptureScreen.route
    ) {
        composable(route = IykykScreens.CaptureScreen.route) {
            CaptureScreen(
                viewModel = viewModel,
                onRecordingFinished = {
                    navController.navigate(IykykScreens.ProcessingScreen.route)
                }
            )
        }

        composable(route = IykykScreens.ProcessingScreen.route) {
            ProcessingScreen(
                viewModel = viewModel,
                onCollageReady = {
                    navController.navigate(IykykScreens.CollageScreen.route) {
                        // Remove the (now-succeeded) Processing entry so back-navigation
                        // from Collage doesn't return to a stale completed screen.
                        popUpTo(IykykScreens.ProcessingScreen.route) { inclusive = true }
                    }
                },
                onRetry = {
                    // Capture's existing back-stack entry is still there underneath -
                    // just drop back to it rather than pushing a new one.
                    navController.popBackStack()
                }
            )
        }

        composable(route = IykykScreens.CollageScreen.route) {
            CollageScreen(
                viewModel = viewModel,
                onRecordAgain = {
                    // Restart the whole flow with a clean back stack - Processing and
                    // Collage's completed entries shouldn't linger for this new attempt.
                    navController.navigate(IykykScreens.CaptureScreen.route) {
                        popUpTo(IykykScreens.CaptureScreen.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
