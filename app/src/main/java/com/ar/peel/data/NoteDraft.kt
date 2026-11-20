package com.ar.peel.data

import com.ar.peel.util.CheckedItems
import com.ar.peel.util.NoteGeometry
import java.util.UUID
import kotlin.random.Random

enum class NoteMode {
    TEXT,
    LIST;

    companion object {
        fun from(raw: String): NoteMode =
            entries.firstOrNull { it.name == raw } ?: TEXT
    }
}

data class DraftItem(
    val text: String = "",
    val checked: Boolean = false,
)

/** The editable shape of a note, shared by the editor and preview screens. */
data class NoteDraft(
    val id: String? = null,
    val body: String = "",
    val mode: NoteMode = NoteMode.TEXT,
    val items: List<DraftItem> = listOf(DraftItem()),
    val colorId: String = "yellow",
    val lockX: Float = 0.5f,
    val lockY: Float = 0.55f,
    val homeX: Float = 0.5f,
    val homeY: Float = 0.30f,
    val widthFraction: Float = 0.62f,
    val rotationDeg: Float = NoteGeometry.randomTilt(Random.Default),
    val onLock: Boolean = true,
    val onHome: Boolean = false,
    val createdAt: Long? = null,
) {
    fun centerX(lock: Boolean): Float = if (lock) lockX else homeX
    fun centerY(lock: Boolean): Float = if (lock) lockY else homeY
    fun isOn(lock: Boolean): Boolean = if (lock) onLock else onHome

    fun withCenter(lock: Boolean, x: Float, y: Float): NoteDraft =
        if (lock) copy(lockX = x, lockY = y) else copy(homeX = x, homeY = y)

    fun checkedIndexes(): Set<Int> =
        items.mapIndexedNotNull { index, item -> if (item.checked) index else null }.toSet()

    fun savedBody(): String =
        if (mode == NoteMode.LIST) {
            items.map { it.text.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
        } else {
            body.trim()
        }

    fun savedChecked(): String {
        if (mode != NoteMode.LIST) return ""
        var savedIndex = 0
        val checked = mutableSetOf<Int>()
        items.forEach { item ->
            if (item.text.isNotBlank()) {
                if (item.checked) checked += savedIndex
                savedIndex++
            }
        }
        return CheckedItems.serialize(checked)
    }

    fun toEntity(now: Long = System.currentTimeMillis()): NoteEntity = NoteEntity(
        id = id ?: UUID.randomUUID().toString(),
        body = savedBody(),
        mode = mode.name,
        checked = savedChecked(),
        colorId = colorId,
        lockX = lockX,
        lockY = lockY,
        homeX = homeX,
        homeY = homeY,
        widthFraction = widthFraction,
        rotationDeg = rotationDeg,
        onLock = onLock,
        onHome = onHome,
        createdAt = createdAt ?: now,
        updatedAt = now,
        deletedAt = null,
    )

    companion object {
        fun from(note: NoteEntity): NoteDraft {
            val mode = NoteMode.from(note.mode)
            val lines = note.body.split('\n')
            val checked = CheckedItems.parse(note.checked)
            val items = if (lines.all { it.isEmpty() }) {
                listOf(DraftItem())
            } else {
                lines.mapIndexed { index, line -> DraftItem(line, mode == NoteMode.LIST && index in checked) }
            }
            return NoteDraft(
                id = note.id,
                body = note.body,
                mode = mode,
                items = items,
                colorId = note.colorId,
                lockX = note.lockX,
                lockY = note.lockY,
                homeX = note.homeX,
                homeY = note.homeY,
                widthFraction = note.widthFraction,
                rotationDeg = note.rotationDeg,
                onLock = note.onLock,
                onHome = note.onHome,
                createdAt = note.createdAt,
            )
        }
    }
}
