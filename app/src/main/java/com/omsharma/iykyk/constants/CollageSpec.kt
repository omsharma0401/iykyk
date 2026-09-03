package com.omsharma.iykyk.constants

// Geometry of the 1080x1920 story collage.
object CollageSpec {
    const val WIDTH = 1080
    const val HEIGHT = 1920
    const val MARGIN = 40f
    const val GUTTER = 18f
    const val HEADER_HEIGHT = 180f
    const val FOOTER_HEIGHT = 40f                // bottom margin
    const val CORNER_RADIUS = 28f
    const val SCRIM_HEIGHT = 150f
    const val CAPTION_INSET = 26f
    const val PILL_HEIGHT = 48f
    const val PILL_PADDING = 16f
    const val MIN_TILE_ASPECT = 0.5f             // width / height; never thinner than 9:16
    const val FACE_LINE = 0.38f                  // face centre sits this far down the tile
    const val BACKGROUND_TOP = 0xFF15181F.toInt()
    const val BACKGROUND_BOTTOM = 0xFF2A2138.toInt()
}
