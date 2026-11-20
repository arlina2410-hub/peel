package com.ar.peel.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ar.peel.render.NoteRenderer
import com.ar.peel.render.NoteVisual
import kotlin.math.min

/** Draws the real note renderer at whatever size the caller gives it. */
@Composable
fun NotePreview(
    renderer: NoteRenderer,
    visual: NoteVisual,
    modifier: Modifier = Modifier,
    padding: Dp = 4.dp,
    highlight: Boolean = false,
) {
    Canvas(modifier) {
        val inset = padding.toPx()
        val availableWidth = size.width - inset * 2
        val availableHeight = size.height - inset * 2
        if (availableWidth <= 0f || availableHeight <= 0f) return@Canvas
        val heightNu = renderer.measureHeightNu(visual)
        val scale = min(availableWidth / 300f, availableHeight / heightNu)
        if (scale <= 0f) return@Canvas
        renderer.draw(
            canvas = drawContext.canvas.nativeCanvas,
            visual = visual,
            centerX = size.width / 2f,
            centerY = size.height / 2f,
            widthPx = 300f * scale,
            highlight = highlight,
        )
    }
}
