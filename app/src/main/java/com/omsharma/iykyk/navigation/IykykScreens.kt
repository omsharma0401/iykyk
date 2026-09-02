package com.omsharma.iykyk.navigation

sealed class IykykScreens(val route: String) {
    object CaptureScreen : IykykScreens("capture_screen")
    object ProcessingScreen : IykykScreens("processing_screen")
    object CollageScreen : IykykScreens("collage_screen")
}
