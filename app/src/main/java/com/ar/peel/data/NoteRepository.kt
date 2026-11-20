package com.ar.peel.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao) {
    val active: Flow<List<NoteEntity>> = dao.observeActive()
    val deleted: Flow<List<NoteEntity>> = dao.observeDeleted()

    suspend fun activeOnce(): List<NoteEntity> = dao.activeOnce()
    suspend fun get(id: String): NoteEntity? = dao.get(id)
    suspend fun upsert(note: NoteEntity) = dao.upsert(note)
    suspend fun softDelete(id: String) = dao.softDelete(id, System.currentTimeMillis())
    suspend fun restore(id: String) = dao.restore(id, System.currentTimeMillis())
    suspend fun deleteForever(id: String) = dao.deleteForever(id)
    suspend fun emptyBin() = dao.emptyBin()
}
