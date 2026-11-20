package com.ar.peel

import android.app.Application
import android.content.Context
import com.ar.peel.data.AppDatabase
import com.ar.peel.data.NoteRepository
import com.ar.peel.data.SettingsStore
import com.ar.peel.render.NoteRenderer
import com.ar.peel.render.NoteTypefaces
import com.ar.peel.wallpaper.BaseStore
import com.ar.peel.wallpaper.WallpaperApplier
import com.ar.peel.wallpaper.WallpaperCapture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class PeelApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database = AppDatabase.build(appContext)
    val repository = NoteRepository(database.noteDao())
    val settingsStore = SettingsStore(appContext)
    val baseStore = BaseStore(appContext)
    val renderer = NoteRenderer(NoteTypefaces(appContext))
    val capture = WallpaperCapture(appContext, baseStore)
    val applier = WallpaperApplier(
        context = appContext,
        repository = repository,
        settingsStore = settingsStore,
        baseStore = baseStore,
        renderer = renderer,
        scope = scope,
    )
}
