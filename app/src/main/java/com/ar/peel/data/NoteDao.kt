package com.ar.peel.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    suspend fun activeOnce(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: String): NoteEntity?

    @Upsert
    suspend fun upsert(note: NoteEntity)

    @Query("UPDATE notes SET deletedAt = :at, updatedAt = :at WHERE id = :id")
    suspend fun softDelete(id: String, at: Long)

    @Query("UPDATE notes SET deletedAt = NULL, updatedAt = :at WHERE id = :id")
    suspend fun restore(id: String, at: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteForever(id: String)

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL")
    suspend fun emptyBin()
}
