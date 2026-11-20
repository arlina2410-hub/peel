package com.ar.peel.util

import kotlin.random.Random

/** All placement rules live here so the preview and the wallpaper agree. */
object NoteGeometry {
    const val BASE_WIDTH = 300f
    const val MIN_HEIGHT = 300f
    const val MAX_HEIGHT = 480f
    const val PADDING = 22f

    const val MIN_WIDTH_FRACTION = 0.30f
    const val MAX_WIDTH_FRACTION = 0.95f
    const val MIN_ROTATION = -15f
    const val MAX_ROTATION = 15f

    fun clampX(x: Float): Float = x.coerceIn(0.08f, 0.92f)
    fun clampY(y: Float): Float = y.coerceIn(0.05f, 0.95f)
    fun clampWidth(fraction: Float): Float =
        fraction.coerceIn(MIN_WIDTH_FRACTION, MAX_WIDTH_FRACTION)

    fun clampRotation(degrees: Float): Float =
        degrees.coerceIn(MIN_ROTATION, MAX_ROTATION)

    /**
     * The note must never be taller than the screen. A note is at most 1.6x its width,
     * so cap the width against screen height as well as the user's chosen fraction.
     */
    fun noteWidthPx(screenWidth: Int, screenHeight: Int, fraction: Float): Float =
        minOf(screenWidth * clampWidth(fraction), screenHeight / 1.6f)

    fun randomTilt(random: Random = Random.Default): Float =
        random.nextFloat() * 5f - 2.5f
}
