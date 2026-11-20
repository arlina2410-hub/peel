package com.ar.peel.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

sealed interface CaptureResult {
    data object Success : CaptureResult
    data class Failure(val message: String) : CaptureResult
}

/**
 * Strategy A reads the current wallpaper with WallpaperManager. When the platform refuses
 * (Android 13+ restriction, live wallpaper, no wallpaper) the UI offers Strategy B, the
 * system photo picker. We deliberately do not ask for All files access.
 */
class WallpaperCapture(private val context: Context, private val baseStore: BaseStore) {
    private val manager = WallpaperManager.getInstance(context)

    fun isLiveWallpaper(): Boolean = runCatching { manager.wallpaperInfo != null }.getOrDefault(false)

    suspend fun captureCurrentBoth(): CaptureResult = withContext(Dispatchers.IO) {
        if (isLiveWallpaper()) {
            return@withContext CaptureResult.Failure(
                "Your wallpaper is animated, so Peel can't copy it. Pick a still image to use as your base.",
            )
        }
        try {
            val home = readWallpaper(WallpaperManager.FLAG_SYSTEM)
                ?: return@withContext CaptureResult.Failure(
                    "Peel couldn't read your wallpaper. Pick a photo to use as your base instead.",
                )
            var lock = readWallpaper(WallpaperManager.FLAG_LOCK)
            if (lock == null) {
                lock = home
            }
            val savedHome = baseStore.save(WallpaperTarget.HOME, home)
            val savedLock = baseStore.save(WallpaperTarget.LOCK, lock)
            if (home !== lock) home.recycle()
            lock.recycle()
            if (savedHome && savedLock) {
                CaptureResult.Success
            } else {
                CaptureResult.Failure("Peel couldn't save your wallpaper. Please try again.")
            }
        } catch (e: SecurityException) {
            CaptureResult.Failure(
                "This Android version won't let Peel copy your wallpaper. Pick a photo to use as your base instead.",
            )
        } catch (t: Throwable) {
            CaptureResult.Failure("Peel couldn't read your wallpaper. Pick a photo instead.")
        }
    }

    suspend fun captureFromUri(uri: Uri): CaptureResult = withContext(Dispatchers.IO) {
        try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { BaseStore.decodeSampled(it) }
                ?: return@withContext CaptureResult.Failure("Peel couldn't open that photo. Try another one.")
            val oriented = orient(bitmap, readOrientation(uri))
            if (oriented !== bitmap) bitmap.recycle()
            val savedHome = baseStore.save(WallpaperTarget.HOME, oriented)
            val savedLock = baseStore.save(WallpaperTarget.LOCK, oriented)
            oriented.recycle()
            if (savedHome && savedLock) {
                CaptureResult.Success
            } else {
                CaptureResult.Failure("Peel couldn't save that photo. Please try another one.")
            }
        } catch (t: Throwable) {
            CaptureResult.Failure("Peel couldn't use that photo. Please try another one.")
        }
    }

    private fun readWallpaper(flag: Int): Bitmap? {
        val descriptor: ParcelFileDescriptor? =
            try {
                manager.getWallpaperFile(flag)
            } catch (e: SecurityException) {
                null
            }
        if (descriptor != null) {
            descriptor.use { pfd ->
                val bitmap = pfd.fileDescriptor.let { decodeDescriptor(it) }
                if (bitmap != null) return bitmap
            }
        }
        val drawable: Drawable? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                manager.getDrawable(flag)
            } else {
                manager.drawable
            }
        } catch (e: SecurityException) {
            null
        } catch (t: Throwable) {
            null
        }
        return drawable?.let { drawableToBitmap(it) }
    }

    private fun decodeDescriptor(descriptor: java.io.FileDescriptor): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFileDescriptor(descriptor, null, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while ((bounds.outWidth / sample).toLong() * (bounds.outHeight / sample) > 12_000_000L) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFileDescriptor(descriptor, null, options)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 1080
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun readOrientation(uri: Uri): Int =
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream: InputStream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    private fun orient(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        val result = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        return result
    }
}
