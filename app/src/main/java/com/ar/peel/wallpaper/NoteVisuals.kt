package com.ar.peel.wallpaper

import android.content.Context
import android.text.format.DateFormat
import com.ar.peel.data.NoteDraft
import com.ar.peel.data.NoteEntity
import com.ar.peel.data.NoteMode
import com.ar.peel.render.NotePalette
import com.ar.peel.render.NoteVisual
import com.ar.peel.util.CheckedItems
import com.ar.peel.util.TimeFormat

/** Builds the render model once, so preview and wallpaper use identical inputs. */
object NoteVisuals {
    fun from(note: NoteEntity, context: Context): NoteVisual = NoteVisual(
        body = note.body,
        listMode = NoteMode.from(note.mode) == NoteMode.LIST,
        checked = CheckedItems.parse(note.checked),
        color = NotePalette.byId(note.colorId),
        timeLabel = stamp(note.createdAt, context),
        rotationDeg = note.rotationDeg,
    )

    fun from(draft: NoteDraft, context: Context): NoteVisual = NoteVisual(
        body = if (draft.mode == NoteMode.LIST) {
            draft.items.map { it.text }.joinToString("\n")
        } else {
            draft.body
        },
        listMode = draft.mode == NoteMode.LIST,
        checked = draft.checkedIndexes(),
        color = NotePalette.byId(draft.colorId),
        timeLabel = stamp(draft.createdAt ?: System.currentTimeMillis(), context),
        rotationDeg = draft.rotationDeg,
    )

    fun stamp(epochMs: Long, context: Context): String {
        val locale = context.resources.configuration.locales[0]
        val is24Hour = DateFormat.is24HourFormat(context)
        return TimeFormat.stamp(epochMs, locale, is24Hour)
    }
}
