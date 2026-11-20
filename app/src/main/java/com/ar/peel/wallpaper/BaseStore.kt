package com.ar.peel.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.math.max
import kotlin.math.roundToInt

enum class WallpaperTarget(val fileName: String, val label: String) {
    LOCK("lock", "Lock screen"),
    HOME("home", "Home screen"),
}

/** Lossless copies of the user's own wallpapers, kept in app-private storage. */
class BaseStore(private val context: Context) {
    private val dir = File(context.filesDir, "base")

    private fun file(target: WallpaperTarget): File = File(dir, "${target.fileName}.png")

    fun has(target: WallpaperTarget): Boolean = file(target).isFile

    fun hasAny(): Boolean = WallpaperTarget.entries.any { has(it) }

    suspend fun save(target: WallpaperTarget, bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val targetFile = file(target)
        val temp = File(dir, "${target.fileName}.png.tmp")
        try {
            temp.outputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            Files.move(
                temp.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
            )
            true
        } catch (t: Throwable) {
            Log.w("Peel", "Could not save ${target.fileName} base", t)
            temp.delete()
            false
        }
    }

    /** Decodes and center-crops the original straight to screen size. Keeps memory low. */
    suspend fun loadForScreen(target: WallpaperTarget, width: Int, height: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            val source = file(target)
            if (!source.isFile || width <= 0 || height <= 0) return@withContext null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, width, height)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeFile(source.absolutePath, options) ?: return@withContext null
            try {
                val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                val scale = max(width.toFloat() / decoded.width, height.toFloat() / decoded.height)
                val drawWidth = decoded.width * scale
                val drawHeight = decoded.height * scale
                val left = (width - drawWidth) / 2f
                val top = (height - drawHeight) / 2f
                canvas.drawBitmap(
                    decoded,
                    null,
                    RectF(left, top, left + drawWidth, top + drawHeight),
                    Paint(Paint.FILTER_BITMAP_FLAG),
                )
                output
            } finally {
                decoded.recycle()
            }
        }

    suspend fun thumbnail(target: WallpaperTarget, size: Int): Bitmap? =
        withContext(Dispatchers.IO) {
            if (!has(target)) return@withContext null
            loadForScreen(target, size, size)
        }

    fun delete(target: WallpaperTarget) {
        file(target).delete()
    }

    private fun sampleSize(sourceWidth: Int, sourceHeight: Int, reqWidth: Int, reqHeight: Int): Int {
        var sample = 1
        while (sourceWidth / (sample * 2) >= reqWidth && sourceHeight / (sample * 2) >= reqHeight) {
            sample *= 2
        }
        return sample
    }

    companion object {
        fun decodeSampled(input: java.io.InputStream, maxPixels: Int = 12_000_000): Bitmap? {
            val bytes = input.readBytes()
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inSampleSize = sampleForPixels(bounds.outWidth, bounds.outHeight, maxPixels)
            }
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        }

        private fun sampleForPixels(width: Int, height: Int, maxPixels: Int): Int {
            var sample = 1
            while ((width / sample).toLong() * (height / sample) > maxPixels) {
                sample *= 2
            }
            return sample
        }
    }
}
