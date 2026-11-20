package com.ar.peel.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ar.peel.AppContainer
import com.ar.peel.data.AppSettings
import com.ar.peel.data.NoteDraft
import com.ar.peel.data.NoteEntity
import com.ar.peel.data.ThemeMode
import com.ar.peel.util.NoteGeometry
import com.ar.peel.wallpaper.ApplyStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PeelViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings?> = container.settingsStore.settings.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        null,
    )

    val notes: StateFlow<List<NoteEntity>> = container.repository.active.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList(),
    )

    val deleted: StateFlow<List<NoteEntity>> = container.repository.deleted.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList(),
    )

    val applyStatus: StateFlow<ApplyStatus> = container.applier.status

    var draft by mutableStateOf<NoteDraft?>(null)
        private set

    var wallpaperChanged by mutableStateOf(false)
        private set

    var hasBase by mutableStateOf(container.baseStore.hasAny())
        private set

    var baseVersion by mutableIntStateOf(0)
        private set

    init {
        viewModelScope.launch {
            container.applier.status.collect { status ->
                if (status == ApplyStatus.WallpaperChanged) {
                    wallpaperChanged = true
                    container.applier.acknowledge()
                }
            }
        }
    }

    fun newDraft() {
        draft = NoteDraft(
            onLock = true,
            onHome = false,
            rotationDeg = NoteGeometry.randomTilt(),
        )
    }

    fun editDraft(note: NoteEntity) {
        draft = NoteDraft.from(note)
    }

    fun moveDraft(note: NoteEntity) {
        draft = NoteDraft.from(note)
    }

    fun discardDraft() {
        draft = null
    }

    fun updateDraft(transform: (NoteDraft) -> NoteDraft) {
        draft = draft?.let(transform)
    }

    fun saveDraft(onSaved: () -> Unit) {
        val current = draft ?: return
        if (current.savedBody().isBlank()) return
        viewModelScope.launch {
            container.repository.upsert(current.toEntity())
            draft = null
            container.applier.requestApply()
            onSaved()
        }
    }

    fun deleteDraft(onDone: () -> Unit) {
        val current = draft
        if (current?.id == null) {
            draft = null
            onDone()
            return
        }
        viewModelScope.launch {
            container.repository.softDelete(current.id)
            draft = null
            container.applier.requestApply()
            onDone()
        }
    }

    fun deleteNote(note: NoteEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            container.repository.softDelete(note.id)
            container.applier.requestApply()
            onDone()
        }
    }

    fun restoreNote(note: NoteEntity) {
        viewModelScope.launch {
            container.repository.restore(note.id)
            container.applier.requestApply()
        }
    }

    fun deleteForever(note: NoteEntity) {
        viewModelScope.launch { container.repository.deleteForever(note.id) }
    }

    fun emptyBin() {
        viewModelScope.launch { container.repository.emptyBin() }
    }

    fun changeColor(note: NoteEntity, colorId: String) {
        viewModelScope.launch {
            container.repository.upsert(note.copy(colorId = colorId, updatedAt = System.currentTimeMillis()))
            container.applier.requestApply()
        }
    }

    fun finishSetup(recapture: Boolean) {
        viewModelScope.launch {
            container.settingsStore.setOnboardingDone(true)
            hasBase = container.baseStore.hasAny()
            baseVersion++
            container.applier.syncWallpaperIds()
            wallpaperChanged = false
            if (recapture && container.repository.activeOnce().isNotEmpty()) {
                container.applier.requestApply()
            }
        }
    }

    fun skipSetup() {
        viewModelScope.launch {
            container.settingsStore.setOnboardingDone(true)
            hasBase = container.baseStore.hasAny()
        }
    }

    fun checkWallpaperChanged() {
        viewModelScope.launch {
            val current = container.settingsStore.settings.first()
            if (!current.onboardingDone) return@launch
            wallpaperChanged = container.applier.changedTargets().isNotEmpty()
        }
    }

    fun dismissWallpaperBanner() {
        wallpaperChanged = false
    }

    fun reapply() {
        container.applier.requestApply()
    }

    fun restoreOriginals(onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = container.applier.restoreOriginals()
            baseVersion++
            onDone(ok)
        }
    }

    fun acknowledgeApplySuccess() {
        container.applier.acknowledge()
    }

    fun clearApplyError() {
        container.applier.acknowledge()
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { container.settingsStore.setThemeMode(mode) }
    }

    fun setShowMockOverlay(show: Boolean) {
        viewModelScope.launch { container.settingsStore.setShowMockOverlayDefault(show) }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PeelViewModel(container) as T
    }
}
