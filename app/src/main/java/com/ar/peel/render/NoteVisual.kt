package com.ar.peel.render

/** Everything the renderer needs. Shared by the live preview and the wallpaper bitmap. */
data class NoteVisual(
    val body: String,
    val listMode: Boolean,
    val checked: Set<Int>,
    val color: NoteColor,
    val timeLabel: String,
    val rotationDeg: Float,
)
