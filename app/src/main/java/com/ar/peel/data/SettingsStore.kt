package com.ar.peel.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ar.peel.wallpaper.WallpaperTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "peel_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val onboardingDone: Boolean = false,
    val lockWallpaperId: Long = -1L,
    val homeWallpaperId: Long = -1L,
    val lockModified: Boolean = false,
    val homeModified: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val showMockOverlayDefault: Boolean = true,
) {
    fun wallpaperId(target: WallpaperTarget): Long =
        if (target == WallpaperTarget.LOCK) lockWallpaperId else homeWallpaperId

    fun modified(target: WallpaperTarget): Boolean =
        if (target == WallpaperTarget.LOCK) lockModified else homeModified
}

class SettingsStore(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            onboardingDone = prefs[ONBOARDING] ?: false,
            lockWallpaperId = prefs[LOCK_ID] ?: -1L,
            homeWallpaperId = prefs[HOME_ID] ?: -1L,
            lockModified = prefs[LOCK_MODIFIED] ?: false,
            homeModified = prefs[HOME_MODIFIED] ?: false,
            themeMode = prefs[THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            showMockOverlayDefault = prefs[SHOW_MOCK] ?: true,
        )
    }

    suspend fun setOnboardingDone(done: Boolean) {
        store.edit { it[ONBOARDING] = done }
    }

    suspend fun setWallpaperId(target: WallpaperTarget, id: Long) {
        store.edit {
            if (target == WallpaperTarget.LOCK) it[LOCK_ID] = id else it[HOME_ID] = id
        }
    }

    suspend fun setModified(target: WallpaperTarget, modified: Boolean) {
        store.edit {
            if (target == WallpaperTarget.LOCK) it[LOCK_MODIFIED] = modified else it[HOME_MODIFIED] = modified
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun setShowMockOverlayDefault(show: Boolean) {
        store.edit { it[SHOW_MOCK] = show }
    }

    private companion object {
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val LOCK_ID = longPreferencesKey("lock_wallpaper_id")
        val HOME_ID = longPreferencesKey("home_wallpaper_id")
        val LOCK_MODIFIED = booleanPreferencesKey("lock_modified")
        val HOME_MODIFIED = booleanPreferencesKey("home_modified")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_MOCK = booleanPreferencesKey("show_mock_overlay")
    }
}
