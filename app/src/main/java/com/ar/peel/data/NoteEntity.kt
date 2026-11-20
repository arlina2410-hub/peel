package com.ar.peel.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val body: String,
    val mode: String,
    val checked: String,
    val colorId: String,
    val lockX: Float,
    val lockY: Float,
    val homeX: Float,
    val homeY: Float,
    val widthFraction: Float,
    val rotationDeg: Float,
    val onLock: Boolean,
    val onHome: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)
