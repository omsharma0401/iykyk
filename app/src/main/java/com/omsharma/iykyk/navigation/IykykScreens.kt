package com.omsharma.iykyk.navigation

import android.net.Uri

sealed class IykykScreens(val route: String) {
    object CaptureScreen : IykykScreens("capture_screen")

    object CollageScreen : IykykScreens("collage_screen/{videoUri}") {
        fun routeFor(videoUri: Uri) = "collage_screen/${Uri.encode(videoUri.toString())}"
    }
}
