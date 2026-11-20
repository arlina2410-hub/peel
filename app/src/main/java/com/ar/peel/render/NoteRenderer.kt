package com.ar.peel.render

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.ar.peel.util.NoteGeometry
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * The one and only note painter. The Compose preview and the wallpaper bitmap both call
 * [draw], so what the user sees is exactly what gets stuck on the screen.
 */
class NoteRenderer(private val typefaces: NoteTypefaces) {

    private val grain: Bitmap by lazy { makeGrain() }

    fun measureHeightNu(visual: NoteVisual): Float {
        val body = measureRawHeightNu(visual)
        return body.coerceIn(NoteGeometry.MIN_HEIGHT, NoteGeometry.MAX_HEIGHT)
    }

    fun isOverflowing(visual: NoteVisual): Boolean =
        measureRawHeightNu(visual) > NoteGeometry.MAX_HEIGHT

    private fun measureRawHeightNu(visual: NoteVisual): Float {
        val stamp = stampLayout(visual.timeLabel)
        val content = if (visual.listMode) {
            if (visual.body.isBlank()) {
                0f
            } else {
                val paint = listPaint(checked = false)
                visual.body.split('\n').sumOf { line ->
                    (itemLayout(line.ifBlank { " " }, paint).height + ITEM_GAP).toDouble()
                }.toFloat() - ITEM_GAP
            }
        } else {
            if (visual.body.isBlank()) 0f else bodyLayout(visual.body, bodyPaint()).height.toFloat()
        }
        return NoteGeometry.PADDING + content + STAMP_GAP + stamp.height + NoteGeometry.PADDING
    }

    fun draw(
        canvas: Canvas,
        visual: NoteVisual,
        centerX: Float,
        centerY: Float,
        widthPx: Float,
        highlight: Boolean = false,
    ) {
        val scale = widthPx / NoteGeometry.BASE_WIDTH
        val heightPx = measureHeightNu(visual) * scale
        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.rotate(visual.rotationDeg)
        canvas.translate(-widthPx / 2f, -heightPx / 2f)
        canvas.scale(scale, scale)
        drawBaseNote(canvas, visual, measureHeightNu(visual), highlight)
        canvas.restore()
    }

    private fun drawBaseNote(canvas: Canvas, visual: NoteVisual, height: Float, highlight: Boolean) {
        val w = NoteGeometry.BASE_WIDTH
        val h = height
        val corner = 2f
        val bodyRect = RectF(0f, 0f, w, h)
        drawShadows(canvas, bodyRect, corner)

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, h,
                shade(visual.color.argb, 0.035f),
                shade(visual.color.argb, -0.045f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(bodyRect, corner, corner, bodyPaint)

        val clip = Path().apply {
            addRoundRect(bodyRect, corner, corner, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clip)

        // Faint adhesive strip along the top edge.
        val adhesive = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, 22f,
                0x18000000, 0x00000000,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w, 22f, adhesive)

        // Paper grain.
        val grainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(grain, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        }
        canvas.drawRect(bodyRect, grainPaint)

        // Lifted bottom-right corner.
        val curl = Path().apply {
            moveTo(w - 22f, h)
            lineTo(w, h - 22f)
            lineTo(w, h)
            close()
        }
        canvas.drawPath(curl, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x26000000 })

        canvas.restore()

        val stamp = stampLayout(visual.timeLabel)
        val stampHeight = stamp.height.toFloat()
        val contentBottom = h - NoteGeometry.PADDING - stampHeight - STAMP_GAP

        if (visual.listMode) {
            drawList(canvas, visual, contentBottom)
        } else {
            drawText(canvas, visual.body, contentBottom)
        }

        canvas.save()
        canvas.translate(NoteGeometry.PADDING, h - NoteGeometry.PADDING - stampHeight)
        stamp.draw(canvas)
        canvas.restore()

        if (highlight) {
            val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
                color = 0xFFFFB800.toInt()
            }
            canvas.drawRoundRect(
                RectF(-4f, -4f, w + 4f, h + 4f),
                corner + 4f,
                corner + 4f,
                ring,
            )
        }
    }

    private fun drawShadows(canvas: Canvas, body: RectF, corner: Float) {
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        // Layered translucent rounds approximate a blur and render identically on every canvas.
        for (i in 7 downTo 1) {
            shadow.alpha = 10
            val grow = i * 1.7f
            canvas.drawRoundRect(
                RectF(body.left - grow, body.top - grow + 6f, body.right + grow, body.bottom + grow + 6f),
                corner + grow,
                corner + grow,
                shadow,
            )
        }
        for (i in 2 downTo 1) {
            shadow.alpha = 12
            val grow = i * 0.8f
            canvas.drawRoundRect(
                RectF(body.left - grow, body.top - grow + 1.5f, body.right + grow, body.bottom + grow + 1.5f),
                corner + grow,
                corner + grow,
                shadow,
            )
        }
    }

    private fun drawText(canvas: Canvas, body: String, contentBottom: Float) {
        if (body.isBlank()) return
        val available = contentBottom - NoteGeometry.PADDING
        val paint = bodyPaint()
        var layout = bodyLayout(body, paint)
        if (layout.height > available) {
            val firstLine = layout.getLineBottom(0) - layout.getLineTop(0)
            val maxLines = max(1, (available / firstLine.coerceAtLeast(1f)).toInt())
            var lines = maxLines
            while (lines > 1) {
                layout = bodyLayout(body, paint, maxLines = lines)
                if (layout.height <= available) break
                lines--
            }
            layout = bodyLayout(body, paint, maxLines = lines)
        }
        canvas.save()
        canvas.translate(NoteGeometry.PADDING, NoteGeometry.PADDING)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawList(canvas: Canvas, visual: NoteVisual, contentBottom: Float) {
        val lines = visual.body.split('\n')
        var y = NoteGeometry.PADDING
        var truncated = false
        lines.forEachIndexed { index, line ->
            if (truncated) return@forEachIndexed
            val checked = index in visual.checked
            val paint = listPaint(checked)
            val layout = itemLayout(line.ifBlank { " " }, paint)
            val rowHeight = max(CHECKBOX_SIZE, layout.height.toFloat())
            if (y + rowHeight > contentBottom) {
                truncated = true
                return@forEachIndexed
            }
            val boxCenterY = y + min(layout.height.toFloat(), paint.fontSpacing) / 2f
            drawCheckbox(canvas, NoteGeometry.PADDING, boxCenterY, checked)
            canvas.save()
            canvas.translate(NoteGeometry.PADDING + CHECKBOX_SIZE + CHECKBOX_GAP, y)
            layout.draw(canvas)
            canvas.restore()
            y += rowHeight + ITEM_GAP
        }
        if (truncated) {
            val dots = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = typefaces.medium
                textSize = 20f
                color = NotePalette.ink
                alpha = 150
            }
            canvas.drawText("…", NoteGeometry.PADDING + CHECKBOX_SIZE + CHECKBOX_GAP, contentBottom - 4f, dots)
        }
    }

    private fun drawCheckbox(canvas: Canvas, x: Float, centerY: Float, checked: Boolean) {
        val top = centerY - CHECKBOX_SIZE / 2f
        val box = RectF(x, top, x + CHECKBOX_SIZE, top + CHECKBOX_SIZE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
            color = NotePalette.ink
            alpha = 210
        }
        if (checked) {
            canvas.drawRoundRect(box, 3.5f, 3.5f, Paint(paint).apply {
                style = Paint.Style.FILL
                alpha = 22
            })
        }
        canvas.drawRoundRect(box, 3.5f, 3.5f, paint)
        if (checked) {
            val tick = Path().apply {
                moveTo(x + 4f, top + 8.5f)
                lineTo(x + 7.2f, top + 11.6f)
                lineTo(x + 12.6f, top + 4.6f)
            }
            canvas.drawPath(tick, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = NotePalette.ink
            })
        }
    }

    private fun bodyPaint(): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = typefaces.medium
        textSize = 22f
        color = NotePalette.ink
    }

    private fun listPaint(checked: Boolean): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = typefaces.medium
        textSize = 20f
        color = NotePalette.ink
        if (checked) {
            alpha = 115
            isStrikeThruText = true
        }
    }

    private fun stampPaint(): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = typefaces.regular
        textSize = 11f
        color = NotePalette.ink
        alpha = 153
    }

    private fun bodyLayout(text: String, paint: TextPaint, maxLines: Int = Int.MAX_VALUE): StaticLayout =
        buildLayout(text, paint, NoteGeometry.BASE_WIDTH - 2 * NoteGeometry.PADDING, maxLines)

    private fun itemLayout(text: String, paint: TextPaint): StaticLayout =
        buildLayout(text, paint, ITEM_TEXT_WIDTH, Int.MAX_VALUE)

    private fun stampLayout(text: String): StaticLayout =
        buildLayout(
            text,
            stampPaint(),
            NoteGeometry.BASE_WIDTH - 2 * NoteGeometry.PADDING,
            maxLines = 1,
        )

    private fun buildLayout(
        text: String,
        paint: TextPaint,
        width: Float,
        maxLines: Int,
    ): StaticLayout {
        val builder = StaticLayout.Builder.obtain(
            text,
            0,
            text.length,
            paint,
            width.toInt().coerceAtLeast(1),
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .setLineSpacing(0f, 1.3f)
        if (maxLines != Int.MAX_VALUE) {
            builder.setMaxLines(maxLines)
            builder.setEllipsize(TextUtils.TruncateAt.END)
        }
        return builder.build()
    }

    private fun makeGrain(): Bitmap {
        val size = 48
        val random = Random(7)
        val pixels = IntArray(size * size) {
            val alpha = 3 + random.nextInt(9)
            (alpha shl 24)
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    /** Move a colour towards white (positive) or black (negative) by [amount] 0..1. */
    private fun shade(argb: Int, amount: Float): Int {
        val r = Color.red(argb)
        val g = Color.green(argb)
        val b = Color.blue(argb)
        fun channel(value: Int): Int =
            if (amount >= 0f) {
                (value + (255 - value) * amount).toInt().coerceIn(0, 255)
            } else {
                (value * (1f + amount)).toInt().coerceIn(0, 255)
            }
        return Color.argb(Color.alpha(argb), channel(r), channel(g), channel(b))
    }

    private companion object {
        const val ITEM_GAP = 9f
        const val STAMP_GAP = 16f
        const val CHECKBOX_SIZE = 16f
        const val CHECKBOX_GAP = 8f
        const val ITEM_TEXT_WIDTH = NoteGeometry.BASE_WIDTH - 2 * NoteGeometry.PADDING - CHECKBOX_SIZE - CHECKBOX_GAP
    }
}
