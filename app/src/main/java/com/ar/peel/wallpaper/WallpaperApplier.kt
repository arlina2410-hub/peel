package com.ar.peel.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.view.WindowManager
import com.ar.peel.data.AppSettings
import com.ar.peel.data.NoteEntity
import com.ar.peel.data.NoteRepository
import com.ar.peel.data.SettingsStore
import com.ar.peel.render.NoteRenderer
import com.ar.peel.util.NoteGeometry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

sealed interface ApplyStatus {
    data object Idle : ApplyStatus
    data object Working : ApplyStatus
    data object Success : ApplyStatus
    data object WallpaperChanged : ApplyStatus
    data class Error(val message: String) : ApplyStatus
}

/**
 * Draws every active note onto a copy of the original wallpaper and sets it.
 * Single-flight, debounced, and always catching errors so the app never crashes.
 */
@OptIn(FlowPreview::class)
class WallpaperApplier(
    private val context: Context,
    private val repository: NoteRepository,
    private val settingsStore: SettingsStore,
    private val baseStore: BaseStore,
    private val renderer: NoteRenderer,
    scope: CoroutineScope,
) {
    private val manager = WallpaperManager.getInstance(context)
    private val mutex = Mutex()
    private val requests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val _status = MutableStateFlow<ApplyStatus>(ApplyStatus.Idle)
    val status: StateFlow<ApplyStatus> = _status.asStateFlow()

    init {
        scope.launch {
            requests.debounce(800).collect {
                mutex.withLock { applyLocked() }
            }
        }
    }

    /** Fire-and-forget: quick successive edits become one wallpaper write. */
    fun requestApply() {
        requests.tryEmit(Unit)
    }

    fun acknowledge() {
        if (_status.value != ApplyStatus.Working) _status.value = ApplyStatus.Idle
    }

    suspend fun syncWallpaperIds() {
        WallpaperTarget.entries.forEach { target ->
            val current = currentWallpaperId(target)
            if (current > 0) settingsStore.setWallpaperId(target, current)
        }
    }

    suspend fun changedTargets(): List<WallpaperTarget> {
        val settings = settingsStore.settings.first()
        return unsynced(settings)
    }

    /** Puts the untouched original copy back, without notes. Returns false on failure. */
    suspend fun restoreOriginals(): Boolean = mutex.withLock {
        _status.value = ApplyStatus.Working
        try {
            val settings = settingsStore.settings.first()
            WallpaperTarget.entries.forEach { target ->
                val savedId = settings.wallpaperId(target)
                val currentId = currentWallpaperId(target)
                if (savedId > 0 && currentId > 0 && savedId != currentId) {
                    // Someone else changed the wallpaper. Their new one is already "original".
                    settingsStore.setWallpaperId(target, currentId)
                    settingsStore.setModified(target, false)
                    return@forEach
                }
                val (width, height) = screenSize()
                val base = baseStore.loadForScreen(target, width, height)
                if (base != null) {
                    try {
                        setBitmap(base, target)
                    } finally {
                        base.recycle()
                    }
                } else {
                    runCatching { manager.clear(flagOf(target)) }
                }
                settingsStore.setWallpaperId(target, currentWallpaperId(target))
                settingsStore.setModified(target, false)
            }
            _status.value = ApplyStatus.Idle
            true
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.w("Peel", "Restore failed", t)
            _status.value = ApplyStatus.Error(friendly(t))
            false
        }
    }

    private suspend fun applyLocked() {
        _status.value = ApplyStatus.Working
        try {
            val settings = settingsStore.settings.first()
            if (unsynced(settings).isNotEmpty()) {
                _status.value = ApplyStatus.WallpaperChanged
                return
            }
            val active = repository.activeOnce()
            WallpaperTarget.entries.forEach { target ->
                applyTarget(target, active)
            }
            _status.value = ApplyStatus.Success
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.w("Peel", "Apply failed", t)
            _status.value = ApplyStatus.Error(friendly(t))
        }
    }

    private suspend fun applyTarget(target: WallpaperTarget, allNotes: List<NoteEntity>) {
        val notes = allNotes
            .filter { if (target == WallpaperTarget.LOCK) it.onLock else it.onHome }
            .sortedBy { it.createdAt }
        val settings = settingsStore.settings.first()
        val (width, height) = screenSize()

        if (notes.isEmpty()) {
            if (settings.modified(target)) {
                val base = baseStore.loadForScreen(target, width, height)
                if (base != null) {
                    try {
                        setBitmap(base, target)
                    } finally {
                        base.recycle()
                    }
                } else {
                    runCatching { manager.clear(flagOf(target)) }
                }
                settingsStore.setWallpaperId(target, currentWallpaperId(target))
                settingsStore.setModified(target, false)
            }
            return
        }

        val base = baseStore.loadForScreen(target, width, height)
            ?: throw IllegalStateException("Peel can't find your saved wallpaper. Re-capture it in Settings.")

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(output)
            canvas.drawBitmap(base, 0f, 0f, null)
            notes.forEach { note ->
                val visual = NoteVisuals.from(note, context)
                val cx = (if (target == WallpaperTarget.LOCK) note.lockX else note.homeX) * width
                val cy = (if (target == WallpaperTarget.LOCK) note.lockY else note.homeY) * height
                val noteWidth = NoteGeometry.noteWidthPx(width, height, note.widthFraction)
                renderer.draw(
                    canvas = canvas,
                    visual = visual,
                    centerX = NoteGeometry.clampX(cx / width) * width,
                    centerY = NoteGeometry.clampY(cy / height) * height,
                    widthPx = noteWidth,
                )
            }
            setBitmap(output, target)
        } finally {
            base.recycle()
            output.recycle()
        }

        settingsStore.setWallpaperId(target, currentWallpaperId(target))
        settingsStore.setModified(target, true)
    }

    private fun currentWallpaperId(target: WallpaperTarget): Long =
        runCatching { manager.getWallpaperId(flagOf(target)).toLong() }.getOrDefault(-1L)

    private fun setBitmap(bitmap: Bitmap, target: WallpaperTarget) {
        manager.setBitmap(bitmap, null, false, flagOf(target))
    }

    private fun flagOf(target: WallpaperTarget): Int =
        if (target == WallpaperTarget.LOCK) WallpaperManager.FLAG_LOCK else WallpaperManager.FLAG_SYSTEM

    private fun unsynced(settings: AppSettings): List<WallpaperTarget> =
        WallpaperTarget.entries.filter { target ->
            val saved = settings.wallpaperId(target)
            if (saved <= 0) return@filter false
            val current = currentWallpaperId(target)
            current > 0 && current != saved
        }

    private fun screenSize(): Pair<Int, Int> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            return bounds.width() to bounds.height()
        }
        @Suppress("DEPRECATION")
        val point = Point()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealSize(point)
        return point.x to point.y
    }

    private fun friendly(t: Throwable): String {
        val message = t.message.orEmpty()
        return when {
            t is SecurityException ->
                "Android didn't let Peel change your wallpaper. Check your phone's wallpaper settings and try again."
            message.contains("saved wallpaper", ignoreCase = true) ->
                "Peel can't find your saved wallpaper. Re-capture it in Settings."
            else ->
                "Peel couldn't update your wallpaper. Please try again."
        }
    }
}
